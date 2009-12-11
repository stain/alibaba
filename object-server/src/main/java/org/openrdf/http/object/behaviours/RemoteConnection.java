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
package org.openrdf.http.object.behaviours;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import org.openrdf.http.object.readers.AggregateReader;
import org.openrdf.http.object.readers.MessageBodyReader;
import org.openrdf.http.object.writers.AggregateWriter;
import org.openrdf.http.object.writers.MessageBodyWriter;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A light weight abstraction over HTTPURLConnection that can convert message bodies.
 */
public class RemoteConnection {
	private Logger logger = LoggerFactory.getLogger(RemoteConnection.class);
	private MessageBodyReader reader = AggregateReader.getInstance();
	private MessageBodyWriter writer = AggregateWriter.getInstance();
	private String uri;
	private ObjectConnection oc;
	private HttpURLConnection con;
	private Set<String> headers = new HashSet<String>();

	public RemoteConnection(String method, String uri, String qs,
			ObjectConnection oc) throws IOException {
		this.uri = uri;
		this.oc = oc;
		URL url = new URL(qs == null ? uri : (uri + '?' + qs));
		con = (HttpURLConnection) url.openConnection();
		con.setRequestMethod(method);
		con.addRequestProperty("Accept-Encoding", "gzip");
	}

	public void addHeader(String name, String value) {
		con.addRequestProperty(name, value);
		headers.add(name.toLowerCase());
	}

	public OutputStream writeStream() throws IOException {
		con.setDoOutput(true);
		con.setInstanceFollowRedirects(false);
		con.setRequestMethod("PUT");
		final OutputStream hout = con.getOutputStream();
		return new FilterOutputStream(hout) {
			public void write(byte[] b, int off, int len)
					throws IOException {
				hout.write(b, off, len);
			}

			public void close() throws IOException {
				super.close();
				if (con.getResponseCode() >= 400) {
					throw new IOException(readString());
				}
			}
		};
	}

	public void write(String media, Class<?> ptype, Type gtype, Object result)
			throws Exception {
		con.setDoOutput(true);
		ObjectFactory of = oc.getObjectFactory();
		String mediaType = writer.getContentType(media, ptype, gtype, of, null);
		if (mediaType != null && !headers.contains("content-type")) {
			con.addRequestProperty("Content-Type", mediaType);
		}
		long size = writer.getSize(null, ptype, gtype, of, result, null);
		if (size >= 0 && !headers.contains("content-length")) {
			con.addRequestProperty("Content-Length", String.valueOf(size));
		}
		OutputStream out = con.getOutputStream();
		try {
			writer.writeTo(mediaType, ptype, gtype, of, result, uri, null, out,
					4096);
		} finally {
			out.close();
		}
	}

	public int getResponseCode() throws IOException {
		return con.getResponseCode();
	}

	public String getResponseMessage() throws IOException {
		return con.getResponseMessage();
	}

	public InputStream readStream() throws IOException {
		String encoding = con.getHeaderField("Content-Encoding");
		InputStream in = con.getInputStream();
		if ("gzip".equals(encoding))
			return new GZIPInputStream(in);
		return in;
	}

	public Object read(Type gtype, Class<?> rtype) throws Exception {
		String loc = con.getHeaderField("Location");
		InputStream in = con.getInputStream();
		String media = con.getHeaderField("Content-Type");
		String encoding = con.getHeaderField("Content-Encoding");
		if ("gzip".equals(encoding)) {
			in = new GZIPInputStream(in);
		}
		return reader.readFrom(rtype, gtype, media, in, null, uri, loc, oc);
	}

	public String readString() {
		try {
			StringWriter string = new StringWriter();
			InputStream in;
			try {
				in = con.getInputStream();
			} catch (IOException e) {
				// might not be a tcp IO exception
				in = con.getErrorStream();
			}
			if (in == null)
				return null; // no response
			if ("gzip".equals(con.getHeaderField("Content-Encoding"))) {
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
}
