/*
 * Copyright (c) 2009, Zepheira All rights reserved.
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
package org.openrdf.server.metadata.http;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.openrdf.OpenRDFException;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.query.resultio.QueryResultParseException;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectFactory;
import org.openrdf.server.metadata.concepts.WebResource;
import org.openrdf.server.metadata.readers.MessageBodyReader;
import org.openrdf.server.metadata.writers.MessageBodyWriter;
import org.xml.sax.SAXException;

public class ResultEntity implements Entity {
	private final class PipedStream extends PipedInputStream {
		private OutputStream out;
		private IOException exception;

		public PipedStream() throws IOException {
			out = new PipedOutputStream(this);
		}

		public OutputStream getOutputStream() {
			return out;
		}

		public void fatal(IOException e) {
			if (exception != null) {
				exception = e;
			}
		}

		@Override
		public void close() throws IOException {
			super.close();
			if (exception != null)
				throw exception;
		}
	}

	private static Executor executor = Executors.newCachedThreadPool();
	private MessageBodyWriter writer;
	private MessageBodyReader reader;
	private String[] mimeTypes;
	private Object result;
	private Class<?> type;
	private Type genericType;
	private String base;
	private ObjectConnection con;
	private ObjectFactory of;

	public ResultEntity(MessageBodyWriter writer, MessageBodyReader reader,
			String[] mimeTypes, Object result, Class<?> type, Type genericType,
			String base, ObjectConnection con) {
		this.writer = writer;
		this.reader = reader;
		this.mimeTypes = mimeTypes;
		this.result = result;
		this.type = type;
		this.genericType = genericType;
		this.base = base;
		this.con = con;
		this.of = con.getObjectFactory();
		if (mimeTypes == null || mimeTypes.length < 1) {
			this.mimeTypes = new String[] { "/*" };
		}
	}

	public void close() throws IOException {
		if (result instanceof Closeable) {
			((Closeable) result).close();
		}
	}

	public boolean isReadable(Class<?> type, Type genericType) {
		if (this.type.equals(type) && this.genericType.equals(genericType))
			return true;
		for (String mimeType : mimeTypes) {
			if (writer.isWriteable(mimeType, this.type, of)) {
				String contentType = writer.getContentType(mimeType, this.type,
						of, null);
				String mime = removeParamaters(contentType);
				if (reader.isReadable(type, genericType, mime, con))
					return true;
			}
		}
		return false;
	}

	public <T> T read(Class<T> type, Type genericType)
			throws QueryResultParseException, TupleQueryResultHandlerException,
			QueryEvaluationException, RepositoryException,
			TransformerConfigurationException, IOException, XMLStreamException,
			ParserConfigurationException, SAXException, TransformerException,
			MimeTypeParseException {
		if (this.type.equals(type) && this.genericType.equals(genericType))
			return (T) (result);
		for (final String mimeType : mimeTypes) {
			if (writer.isWriteable(mimeType, this.type, of)) {
				String contentType = writer.getContentType(mimeType, this.type,
						of, null);
				String mime = removeParamaters(contentType);
				Charset charset = getCharset(contentType);
				if (reader.isReadable(type, genericType, mime, con)) {
					final PipedStream in = new PipedStream();
					final OutputStream out = in.getOutputStream();
					executor.execute(new Runnable() {
						public void run() {
							try {
								writer.writeTo(mimeType,
										ResultEntity.this.type, of, result,
										base, null, out, 1024);
							} catch (IOException e) {
								in.fatal(e);
							} catch (Exception e) {
								in.fatal(new IOException(e));
							} finally {
								try {
									out.close();
								} catch (IOException e) {
									in.fatal(e);
								}
							}
						}
					});
					return (T) (reader.readFrom(type, genericType, mime, in,
							charset, base, null, con));
				}
			}
		}
		throw new AssertionError();
	}

	public boolean isRedirect() {
		return result instanceof URL;
	}

	public boolean isSeeOther() {
		if (result instanceof WebResource) {
			WebResource rdf = (WebResource) result;
			Resource resource = rdf.getResource();
			return resource instanceof URI
					&& !resource.stringValue().equals(base);
		}
		return false;
	}

	public boolean isNoContent() {
		return result == null;
	}

	public boolean isException() {
		return result instanceof Exception;
	}

	public String getLocation() {
		if (isRedirect())
			return result.toString();
		if (isSeeOther())
			return ((WebResource) result).getResource().stringValue();
		return null;
	}

	public Exception getException() {
		return (Exception) result;
	}

	public boolean isWriteable(String mimeType) {
		return writer.isWriteable(mimeType, type, of);
	}

	public long getSize(String mimeType, Charset charset) {
		return writer.getSize(mimeType, type, of, result, charset);
	}

	public void writeTo(String mimeType, Charset charset, OutputStream out,
			int bufSize) throws IOException, OpenRDFException,
			XMLStreamException, TransformerException,
			ParserConfigurationException {
		writer.writeTo(mimeType, type, of, result, base, charset, out, bufSize);
	}

	private String removeParamaters(String mediaType) {
		if (mediaType == null)
			return null;
		int idx = mediaType.indexOf(';');
		if (idx > 0)
			return mediaType.substring(0, idx);
		return mediaType;
	}

	private Charset getCharset(String mediaType) throws MimeTypeParseException {
		if (mediaType == null)
			return null;
		MimeType m = new MimeType(mediaType);
		String name = m.getParameters().get("charset");
		if (name == null)
			return null;
		return Charset.forName(name);
	}

}
