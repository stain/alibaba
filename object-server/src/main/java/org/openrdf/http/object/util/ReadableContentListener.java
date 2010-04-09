package org.openrdf.http.object.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.entity.ContentListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReadableContentListener implements ReadableByteChannel,
		ContentListener {
	private Logger logger = LoggerFactory
			.getLogger(ReadableContentListener.class);
	private int read;
	private ByteBuffer pending;
	private volatile boolean completed;
	private volatile IOControl ioctrl;

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
				ioctrl.suspendInput();
			}
		}
	}

	public synchronized boolean contentAvailable(ContentDecoder decoder) throws IOException {
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
