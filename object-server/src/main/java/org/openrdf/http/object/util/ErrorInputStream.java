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

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

/**
 * Pipes the data and error messages of an OuputStream as an InputStream.
 * 
 * @author James Leigh
 *
 */
public class ErrorInputStream extends InputStream {
	private InputStream delegate;
	private IOException e;

	public ErrorInputStream(PipedOutputStream pipe) throws IOException {
		this.delegate = new PipedInputStream(pipe);
	}

	public void error(IOException e) {
		this.e = e;
	}

	public int available() throws IOException {
		throwIOException();
		return delegate.available();
	}

	public void close() throws IOException {
		throwIOException();
		delegate.close();
	}

	public void mark(int readlimit) {
		delegate.mark(readlimit);
	}

	public boolean markSupported() {
		return delegate.markSupported();
	}

	public int read() throws IOException {
		throwIOException();
		return delegate.read();
	}

	public int read(byte[] b, int off, int len) throws IOException {
		throwIOException();
		return delegate.read(b, off, len);
	}

	public int read(byte[] b) throws IOException {
		throwIOException();
		return delegate.read(b);
	}

	public void reset() throws IOException {
		throwIOException();
		delegate.reset();
	}

	public long skip(long n) throws IOException {
		throwIOException();
		return delegate.skip(n);
	}

	public String toString() {
		return delegate.toString();
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