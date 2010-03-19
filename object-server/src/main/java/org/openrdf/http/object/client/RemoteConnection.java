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
package org.openrdf.http.object.client;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.channels.Pipe.SinkChannel;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.zip.GZIPInputStream;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicHttpRequest;
import org.openrdf.http.object.model.ReadableHttpEntityChannel;
import org.openrdf.http.object.readers.AggregateReader;
import org.openrdf.http.object.readers.MessageBodyReader;
import org.openrdf.http.object.util.ChannelUtil;
import org.openrdf.http.object.writers.AggregateWriter;
import org.openrdf.http.object.writers.MessageBodyWriter;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A light weight abstraction over HTTPURLConnection that can convert message
 * bodies.
 */
public class RemoteConnection {
	private Logger logger = LoggerFactory.getLogger(RemoteConnection.class);
	private MessageBodyReader reader = AggregateReader.getInstance();
	private MessageBodyWriter writer = AggregateWriter.getInstance();
	private String uri;
	private ObjectConnection oc;
	private HttpRequest req;
	private Future<HttpResponse> resp;
	private SocketAddress addr;
	private HTTPObjectClient client;

	public RemoteConnection(SocketAddress remoteAddress, String method,
			String uri, String qs, ObjectConnection oc) throws IOException {
		this.addr = remoteAddress;
		this.uri = uri;
		this.oc = oc;
		String url = qs == null ? uri : (uri + '?' + qs);
		req = new BasicHttpRequest(method, url);
		req.addHeader("Host", "");
		req.addHeader("Accept-Encoding", "gzip");
		client = HTTPObjectClient.getInstance();
	}

	public String toString() {
		return req.getRequestLine().toString();
	}

	public String getEnvelopeType() {
		return client.getEnvelopeType();
	}

	public void addHeader(String name, String value) {
		req.addHeader(name, value);
	}

	public OutputStream writeStream() throws IOException {
		Pipe pipe = Pipe.open();
		final SinkChannel sink = pipe.sink();
		HttpEntityEnclosingRequest heer = getHttpEntityRequest();
		heer.setEntity(new ReadableHttpEntityChannel(null, -1, pipe.source()));
		return ChannelUtil.newOutputStream(new WritableByteChannel() {
			public boolean isOpen() {
				return sink.isOpen();
			}

			public void close() throws IOException {
				sink.close();
				if (getResponseCode() >= 400) {
					throw new IOException(readString());
				}
			}

			public int write(ByteBuffer src) throws IOException {
				return sink.write(src);
			}
		});
	}

	public void write(String media, Class<?> ptype, Type gtype, Object result)
			throws Exception {
		ObjectFactory of = oc.getObjectFactory();
		String mediaType = writer.getContentType(media, ptype, gtype, of, null);
		if (mediaType != null && !req.containsHeader("Content-Type")) {
			req.addHeader("Content-Type", mediaType);
		}
		long size = writer.getSize(null, ptype, gtype, of, result, null);
		if (size >= 0 && !req.containsHeader("Content-Length")) {
			req.addHeader("Content-Length", String.valueOf(size));
		} else if (size < 0) {
			req.addHeader("Transfer-Encoding", "chunked");
		}
		if (size > 500) {
			req.addHeader("Expect", "100-continue");
		}
		HttpEntityEnclosingRequest heer = getHttpEntityRequest();
		ReadableByteChannel in = writer.write(mediaType, ptype, gtype, of,
				result, uri, null);
		heer.setEntity(new ReadableHttpEntityChannel(mediaType, size, in));
	}

	public int getResponseCode() throws IOException {
		return getHttpResponse().getStatusLine().getStatusCode();
	}

	public String getResponseMessage() throws IOException {
		return getHttpResponse().getStatusLine().getReasonPhrase();
	}

	/**
	 * Called if not reading body.
	 */
	public void close() throws IOException {
		HttpEntity entity = getHttpResponse().getEntity();
		if (entity != null) {
			entity.consumeContent();
		}
	}

	public InputStream readStream() throws IOException {
		String encoding = getHeaderField("Content-Encoding");
		InputStream in = getInputStream();
		if (in == null)
			return null;
		if ("gzip".equals(encoding))
			return new GZIPInputStream(in);
		return new FilterInputStream(in) {
			public void close() throws IOException {
				super.close();
				getHttpResponse().getEntity().consumeContent();
			}
		};
	}

	public Object read(Type gtype, Class<?> rtype) throws Exception {
		String loc = getHeaderField("Location");
		InputStream in = getInputStream();
		String media = getHeaderField("Content-Type");
		ReadableByteChannel cin = null;
		if (in != null) {
			String encoding = getHeaderField("Content-Encoding");
			if ("gzip".equals(encoding)) {
				in = new GZIPInputStream(in);
			}
			final ReadableByteChannel delegate = ChannelUtil.newChannel(in);
			cin = new ReadableByteChannel() {

				public boolean isOpen() {
					return delegate.isOpen();
				}

				public void close() throws IOException {
					delegate.close();
					getHttpResponse().getEntity().consumeContent();
				}

				public int read(ByteBuffer dst) throws IOException {
					return delegate.read(dst);
				}
			};
		}
		return reader.readFrom(rtype, gtype, media, cin, null, uri, loc, oc);
	}

	public String readString() throws IOException {
		try {
			StringWriter string = new StringWriter();
			InputStream in = readStream();
			if (in == null)
				return null; // no response
			if ("gzip".equals(getHeaderField("Content-Encoding"))) {
				in = new GZIPInputStream(in);
			}
			InputStreamReader reader = new InputStreamReader(in, "UTF-8");
			try {
				int read;
				char[] cbuf = new char[1024];
				while ((read = reader.read(cbuf)) >= 0) {
					string.write(cbuf, 0, read);
				}
			} finally {
				reader.close();
			}
			return string.toString();
		} catch (UnsupportedEncodingException e) {
			throw new AssertionError(e);
		} catch (IOException e) {
			logger.info(e.toString(), e);
			return null;
		}
	}

	private HttpEntityEnclosingRequest getHttpEntityRequest() {
		HttpEntityEnclosingRequest heer = new BasicHttpEntityEnclosingRequest(
				req.getRequestLine());
		heer.setHeaders(req.getAllHeaders());
		req = heer;
		return heer;
	}

	private InputStream getInputStream() throws IOException {
		HttpEntity entity = getHttpResponse().getEntity();
		if (entity == null)
			return null;
		return entity.getContent();
	}

	public HttpResponse getHttpResponse() throws IOException {
		if (resp == null) {
			resp = client.submitRequest(addr, req);
		}
		try {
			return resp.get();
		} catch (InterruptedException e) {
			throw new IOException(e);
		} catch (ExecutionException e) {
			try {
				throw e.getCause();
			} catch (RuntimeException cause) {
				throw cause;
			} catch (IOException cause) {
				throw cause;
			} catch (Error cause) {
				throw cause;
			} catch (Throwable cause) {
				throw new IOException(cause);
			}
		}
	}

	private String getHeaderField(String name) throws IOException {
		Header hd = getHttpResponse().getFirstHeader(name);
		if (hd == null)
			return null;
		return hd.getValue();
	}
}
