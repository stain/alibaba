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
package org.openrdf.http.object.cache;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Allows multiple InputStreams to appear as one.
 * 
 * @author James Leigh
 *
 */
public class CatInputStream extends InputStream {
	private Queue<InputStream> queue = new LinkedList<InputStream>();
	private ByteArrayOutputStream out;
	private OutputStreamWriter writer;

	public void append(InputStream in) throws IOException {
		peek();
		queue.add(in);
	}

	public void print(CharSequence csq) throws IOException {
		if (writer == null) {
			out = new ByteArrayOutputStream();
			writer = new OutputStreamWriter(out, Charset.forName("ISO-8859-1"));
		}
		writer.append(csq);
	}

	public void println(CharSequence csq) throws IOException {
		print(csq);
		print("\r\n");
	}

	public void println() throws IOException {
		print("\r\n");
	}

	private InputStream peek() throws IOException {
		if (writer != null) {
			writer.close();
			writer = null;
			append(new ByteArrayInputStream(out.toByteArray()));
		}
		return queue.peek();
	}

	@Override
	public void close() throws IOException {
		IOException ioe = null;
		RuntimeException re = null;
		Error er = null;
		for (InputStream in : queue) {
			try {
				in.close();
			} catch (IOException e) {
				ioe = e;
			} catch (RuntimeException e) {
				re = e;
			} catch (Error e) {
				er = e;
			}
		}
		if (er != null)
			throw er;
		if (re != null)
			throw re;
		if (ioe != null)
			throw ioe;
	}

	@Override
	public int available() throws IOException {
		InputStream in = peek();
		if (in == null)
			return 0;
		return in.available();
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		InputStream in = peek();
		if (in == null)
			return -1;
		int read = in.read(b, off, len);
		if (read < 0) {
			queue.remove().close();
			return read(b, off, len);
		}
		return read;
	}

	@Override
	public long skip(long n) throws IOException {
		InputStream in = peek();
		if (in == null)
			return -1;
		long read = super.skip(n);
		if (read < 0) {
			queue.remove().close();
			return skip(n);
		}
		return read;
	}

	@Override
	public int read() throws IOException {
		InputStream in = peek();
		if (in == null)
			return -1;
		int read = in.read();
		if (read < 0) {
			queue.remove().close();
			return read();
		}
		return read;
	}
}
