/*
 * Copyright (c) 2009, James Leigh All rights reserved.
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
package org.openrdf.http.object.controllers;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.zip.GZIPOutputStream;

/**
 * Factory class to create an InputStream that converts an uncompressed stream as a compressed stream.
 */
public class GzipInputStream extends InputStream {
	private static Executor executor = Executors.newCachedThreadPool();

	public static InputStream create(final InputStream in)
			throws IOException {
		final PipedInputStream zipped = new PipedInputStream();
		final PipedOutputStream pipe = new PipedOutputStream(zipped);
		final GzipInputStream unzipped = new GzipInputStream(zipped, in);
		executor.execute(new Runnable() {
			public void run() {
				try {
					OutputStream out = new GZIPOutputStream(pipe);
					try {
						byte[] buf = new byte[512];
						int read;
						while ((read = in.read(buf)) >= 0) {
							out.write(buf, 0, read);
						}
					} finally {
						out.close();
					}
				} catch (IOException e) {
					unzipped.error(e);
				} finally {
					unzipped.finished();
				}
			}
		});
		return unzipped;
	}

	private IOException e;
	private InputStream in;
	private Closeable resource;
	private CountDownLatch latch = new CountDownLatch(1);

	private GzipInputStream(InputStream in, Closeable resource) {
		this.in = in;
		this.resource = resource;
	}

	public void close() throws IOException {
		throwIOException();
		in.close();
		try {
			latch.await();
		} catch (InterruptedException e) {
			// close everything down
		} finally {
			resource.close();
		}
	}

	public int available() throws IOException {
		throwIOException();
		return in.available();
	}

	public void mark(int readlimit) {
		in.mark(readlimit);
	}

	public boolean markSupported() {
		return in.markSupported();
	}

	public int read() throws IOException {
		throwIOException();
		return in.read();
	}

	public int read(byte[] b, int off, int len) throws IOException {
		throwIOException();
		return in.read(b, off, len);
	}

	public int read(byte[] b) throws IOException {
		throwIOException();
		return in.read(b);
	}

	public void reset() throws IOException {
		throwIOException();
		in.reset();
	}

	public long skip(long n) throws IOException {
		throwIOException();
		return in.skip(n);
	}

	public String toString() {
		return in.toString();
	}

	protected void error(IOException e) {
		this.e = e;
	}

	protected void finished() {
		latch.countDown();
	}

	private void throwIOException() throws IOException {
		IOException exc = this.e;
		this.e = null;
		if (exc != null)
			throw exc;
	}

}
