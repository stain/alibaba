/*
 * Copyright 2010, Zepheira LLC Some rights reserved.
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

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe;
import java.nio.channels.ReadableByteChannel;

/**
 * Pipes bytes and IOExceptions through a ReadableByteChannel.
 * 
 * @author James Leigh
 *
 */
public class PipeErrorSource implements ReadableByteChannel {
	private final ReadableByteChannel source;
	private final Closeable closeable;
	private IOException e;

	public PipeErrorSource(Pipe pipe) throws IOException {
		this(pipe.source(), null);
	}

	public PipeErrorSource(Pipe pipe, Closeable closeable) throws IOException {
		this(pipe.source(), closeable);
	}

	private PipeErrorSource(ReadableByteChannel source, Closeable closeable) throws IOException {
		this.source = source;
		this.closeable = closeable;
	}

	public void error(IOException e) {
		this.e = e;
	}

	public boolean isOpen() {
		return source.isOpen();
	}

	public int read(ByteBuffer dst) throws IOException {
		throwIOException();
		return source.read(dst);
	}

	public void close() throws IOException {
		try {
			throwIOException();
		} finally {
			try {
				if (closeable != null) {
					closeable.close();
				}
			} finally {
				source.close();
			}
		}
	}

	public String toString() {
		if (closeable != null)
			return closeable.toString();
		return source.toString();
	}

	private void throwIOException() throws IOException {
		try {
			if (e != null)
				throw e;
		} finally {
			e = null;
		}
	}
}