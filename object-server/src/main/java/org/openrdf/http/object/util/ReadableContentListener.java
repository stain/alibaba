/*
 * Copyright (c) 2010, Zepheira LLC, Some rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution. 
 * - Neither the name of the openrdf.org nor the names of its contributors may
 *   be used to endorse or promote products derived from this software without
 *   specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 */
package org.openrdf.http.object.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.entity.ContentListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link ContentListener} that can be read using {@link ReadableByteChannel}
 * interface.
 * 
 * @author James Leigh
 * 
 */
public class ReadableContentListener implements ReadableByteChannel,
		ContentListener {
	private Logger logger = LoggerFactory
			.getLogger(ReadableContentListener.class);
	private int read;
	private ByteBuffer pending;
	private volatile boolean completed;
	private volatile IOControl ioctrl;
	private volatile boolean requestedInput;
	private volatile boolean suspendedInput;

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(super.toString());
		if (completed) {
			sb.append(" completed");
		} else if (requestedInput) {
			sb.append(" requested input");
		} else if (suspendedInput) {
			sb.append(" suspended input");
		} else {
			sb.append(" idle");
		}
		return sb.toString();
	}

	public boolean isOpen() {
		return !completed;
	}

	public synchronized void finished() {
		debug("finished");
		completed = true;
		notifyAll();
	}

	public synchronized void close() throws IOException {
		debug("close");
		completed = true;
		notifyAll();
		if (ioctrl != null) {
			requestedInput = true;
			suspendedInput = false;
			ioctrl.requestInput();
		}
	}

	public synchronized int read(ByteBuffer dst) throws IOException {
		if (completed)
			return -1;
		assert pending == null;
		pending = dst;
		read = 0;
		if (ioctrl != null) {
			debug("requestInput");
			requestedInput = true;
			suspendedInput = false;
			ioctrl.requestInput();
		}
		try {
			debug("waiting");
			wait(1000);
		} catch (InterruptedException e) {
			debug("interrupted");
			return 0;
		} finally {
			pending = null;
		}
		if (logger.isDebugEnabled()) {
			logger.debug("{} read {}", Thread.currentThread(), read);
		}
		if (read == 0 && completed)
			return -1;
		return read;
	}

	public void contentAvailable(ContentDecoder decoder,
			IOControl ioctrl) throws IOException {
		this.ioctrl = ioctrl;
		if (!contentAvailable(decoder)) {
			debug("yield");
			Thread.yield();
			if (!contentAvailable(decoder)) {
				debug("suspendInput");
				suspendedInput = true;
				ioctrl.suspendInput();
			}
		}
	}

	public synchronized boolean contentAvailable(ContentDecoder decoder) throws IOException {
		requestedInput = false;
		if (completed) {
			debug("consume");
			if (pending == null) {
				pending = ByteBuffer.allocate(1024 * 8);
			}
			while (decoder.read(pending) > 0) {
				pending.clear();
			}
			read = 0;
			notifyAll();
			return true;
		} else if (pending != null && pending.remaining() > 0) {
			debug("contentAvailable");
			int r = decoder.read(pending);
			if (r > 0) {
				read += r;
			}
			if (decoder.isCompleted() || r < 0) {
				debug("completed");
				completed = true;
			}
			notifyAll();
			return true;
		} else if (decoder.isCompleted()
				|| decoder.read(ByteBuffer.allocate(0)) < 0
				|| decoder.isCompleted()) {
			debug("to content");
			completed = true;
			notifyAll();
			return true;
		} else {
			return false;
		}
	}

	private void debug(String msg) {
		if (logger.isDebugEnabled()) {
			logger.debug("{} {}", Thread.currentThread(), msg);
		}
	}

}
