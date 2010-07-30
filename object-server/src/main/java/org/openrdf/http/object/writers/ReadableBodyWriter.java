/*
 * Copyright 2009-2010, James Leigh and Zepheira LLC Some rights reserved.
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
package org.openrdf.http.object.writers;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.CharBuffer;
import java.nio.channels.Pipe;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.channels.Pipe.SinkChannel;
import java.nio.charset.Charset;
import java.util.concurrent.Executor;

import org.openrdf.http.object.threads.ManagedExecutors;
import org.openrdf.http.object.util.ChannelUtil;
import org.openrdf.http.object.util.ErrorReadableByteChannel;
import org.openrdf.repository.object.ObjectFactory;

/**
 * Writes a Readable object into an OutputStream.
 */
public class ReadableBodyWriter implements MessageBodyWriter<Readable> {
	private static Executor executor = ManagedExecutors.getWriterThreadPool();

	public boolean isText(String mimeType, Class<?> type, Type genericType,
			ObjectFactory of) {
		return true;
	}

	public boolean isWriteable(String mimeType, Class<?> type,
			Type genericType, ObjectFactory of) {
		if (!Readable.class.isAssignableFrom(type))
			return false;
		return mimeType == null || mimeType.startsWith("text/")
				|| mimeType.startsWith("*");
	}

	public long getSize(String mimeType, Class<?> type, Type genericType,
			ObjectFactory of, Readable t, Charset charset) {
		return -1;
	}

	public String getContentType(String mimeType, Class<?> type,
			Type genericType, ObjectFactory of, Charset charset) {
		if (charset == null) {
			charset = Charset.defaultCharset();
		}
		if (mimeType == null || mimeType.startsWith("*")
				|| mimeType.startsWith("text/*")) {
			mimeType = "text/plain";
		}
		if (mimeType.contains("charset="))
			return mimeType;
		return mimeType + ";charset=" + charset.name();
	}

	public ReadableByteChannel write(final String mimeType,
			final Class<?> type, final Type genericType,
			final ObjectFactory of, final Readable result, final String base,
			final Charset charset) throws IOException {
		Pipe pipe = Pipe.open();
		final SinkChannel out = pipe.sink();
		final ErrorReadableByteChannel in = new ErrorReadableByteChannel(pipe) {
			public String toString() {
				return result.toString();
			}
		};
		executor.execute(new Runnable() {
			public void run() {
				try {
					try {
						writeTo(mimeType, type, genericType, of, result, base,
								charset, out, 1024);
					} finally {
						out.close();
					}
				} catch (IOException e) {
					in.error(e);
				} catch (Exception e) {
					in.error(new IOException(e));
				} catch (Error e) {
					in.error(new IOException(e));
				}
			}
		});
		return in;
	}

	public void writeTo(String mimeType, Class<?> type, Type genericType,
			ObjectFactory of, Readable result, String base, Charset charset,
			WritableByteChannel out, int bufSize) throws IOException {
		try {
			if (charset == null) {
				charset = Charset.defaultCharset();
			}
			Writer writer = new OutputStreamWriter(ChannelUtil
					.newOutputStream(out), charset);
			CharBuffer cb = CharBuffer.allocate(bufSize);
			while (result.read(cb) >= 0) {
				cb.flip();
				writer.write(cb.array(), cb.position(), cb.limit());
				cb.clear();
			}
			writer.flush();
		} finally {
			if (result instanceof Closeable) {
				((Closeable) result).close();
			}
		}
	}
}
