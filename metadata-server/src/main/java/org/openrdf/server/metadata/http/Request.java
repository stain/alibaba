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
package org.openrdf.server.metadata.http;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.servlet.http.HttpServletRequest;

import org.openrdf.OpenRDFException;
import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectFactory;
import org.openrdf.server.metadata.annotations.type;
import org.openrdf.server.metadata.concepts.RDFResource;
import org.openrdf.server.metadata.concepts.WebResource;
import org.openrdf.server.metadata.readers.MessageBodyReader;
import org.openrdf.server.metadata.writers.MessageBodyWriter;

/**
 * Utility class for {@link HttpServletRequest}.
 * 
 * @author James Leigh
 * 
 */
public class Request extends RequestHeader {
	private static final List<String> HTTP_METHODS = Arrays.asList("OPTIONS",
			"GET", "HEAD", "PUT", "DELETE");
	protected ObjectFactory of;
	protected ValueFactory vf;
	private ObjectConnection con;
	private MessageBodyReader reader;
	private HttpServletRequest request;
	private RDFResource target;
	private URI uri;
	private MessageBodyWriter writer;

	public Request(MessageBodyReader reader, MessageBodyWriter writer,
			File dataDir, HttpServletRequest request, ObjectConnection con)
			throws QueryEvaluationException, RepositoryException {
		super(dataDir, request);
		this.reader = reader;
		this.writer = writer;
		this.request = request;
		this.con = con;
		this.vf = con.getValueFactory();
		this.of = con.getObjectFactory();
		this.uri = vf.createURI(getURI());
		target = con.getObject(WebResource.class, uri);
	}

	public URI createURI(String uriSpec) {
		return vf.createURI(parseURI(uriSpec).toString());
	}

	public Object getBody(Class<?> class1, Type type) throws IOException,
			MimeTypeParseException {
		if (!isMessageBody() && getHeader("Content-Location") == null)
			return null;
		String mediaType = getContentType();
		String mime = removeParamaters(mediaType);
		if (mediaType == null && !reader.isReadable(class1, type, mime, con)) {
			return null;
		}
		String location = getHeader("Content-Location");
		if (location != null) {
			location = createURI(location).stringValue();
		}
		Charset charset = getCharset(mediaType);
		try {
			return reader.readFrom(class1, type, mime,
					request.getInputStream(), charset, uri.stringValue(),
					location, con);
		} catch (OpenRDFException e) {
			throw new IOException(e);
		}
	}

	public InputStream getInputStream() throws IOException {
		return request.getInputStream();
	}

	public ObjectConnection getObjectConnection() {
		return con;
	}

	public String getOperation() {
		String method = request.getMethod();
		if (!HTTP_METHODS.contains(method))
			return null;
		Map<String, String[]> params = request.getParameterMap();
		for (String key : params.keySet()) {
			String[] values = params.get(key);
			if (values == null || values.length == 0 || values.length == 1
					&& values[0].length() == 0) {
				return key;
			}
		}
		return null;
	}

	public Object getParameter(String[] names, Type type, Class<?> klass)
			throws RepositoryException {
		Class<?> componentType = Object.class;
		if (klass.equals(Set.class)) {
			if (type instanceof ParameterizedType) {
				ParameterizedType ptype = (ParameterizedType) type;
				Type[] args = ptype.getActualTypeArguments();
				if (args.length == 1) {
					if (args[0] instanceof Class) {
						componentType = (Class<?>) args[0];
					}
				}
			}
		}
		Set<Object> result = new LinkedHashSet<Object>();
		for (String name : names) {
			if (request.getParameter(name) != null) {
				String[] values = request.getParameterValues(name);
				if (klass.equals(Set.class)) {
					for (String value : values) {
						result.add(toObject(value, componentType));
					}
				} else if (values.length == 0) {
					return null;
				} else {
					return toObject(values[0], klass);
				}
			}
		}
		if (klass.equals(Set.class))
			return result;
		return null;
	}

	public Map getParameterMap() {
		return request.getParameterMap();
	}

	public RDFResource getRequestedResource() {
		return target;
	}

	public void flush() throws RepositoryException, QueryEvaluationException {
		ObjectConnection con = target.getObjectConnection();
		con.setAutoCommit(true); // flush()
		this.target = con.getObject(WebResource.class, target.getResource());
	}

	public boolean isAcceptable(Method method) throws MimeTypeParseException {
		Class<?> type = method.getReturnType();
		if (method.isAnnotationPresent(type.class)) {
			for (String media : method.getAnnotation(type.class).value()) {
				if (isAcceptable(media, type))
					return true;
			}
			return false;
		}
		return isAcceptable(type);
	}

	public boolean isAcceptable(Class<?> type) throws MimeTypeParseException {
		return isAcceptable(null, type);
	}

	public boolean isAcceptable(String mediaType, Class<?> type)
			throws MimeTypeParseException {
		MimeType media = mediaType == null ? null : new MimeType(mediaType);
		Collection<? extends MimeType> acceptable = getAcceptable();
		for (MimeType m : acceptable) {
			if (media != null && !isCompatible(media, m))
				continue;
			if (type != null
					&& !writer.isWriteable(m.getPrimaryType() + "/"
							+ m.getSubType(), type, of))
				continue;
			return true;
		}
		return false;
	}

	public boolean isAcceptable(String mediaType) throws MimeTypeParseException {
		return isAcceptable(mediaType, null);
	}

	public boolean isQueryStringPresent() {
		return request.getQueryString() != null;
	}

	public boolean isReadable(Class<?> class1, Type type) {
		if (!isMessageBody() && getHeader("Content-Location") == null)
			return true;
		String mime = removeParamaters(getContentType());
		return mime == null || reader.isReadable(class1, type, mime, con);
	}

	public String getContentType(Method method) throws MimeTypeParseException {
		Class<?> type = method.getReturnType();
		if (method.isAnnotationPresent(type.class)) {
			String[] mediaTypes = method.getAnnotation(type.class).value();
			Collection<? extends MimeType> acceptable = getAcceptable();
			for (MimeType m : acceptable) {
				for (String mediaType : mediaTypes) {
					String mime = removeParamaters(mediaType);
					MimeType media = new MimeType(mime);
					if (isCompatible(m, media)) {
						if (writer.isWriteable(mime, type, of)) {
							Charset charset = getCharset(m);
							return writer.getContentType(mime, type, of,
									charset);
						}
					}
				}
			}
		} else {
			Collection<? extends MimeType> acceptable = getAcceptable();
			for (MimeType m : acceptable) {
				String mime = m.getPrimaryType() + "/" + m.getSubType();
				if (writer.isWriteable(mime, type, of)) {
					Charset charset = getCharset(m);
					return writer.getContentType(mime, type, of, charset);
				}
			}
		}
		return null;
	}

	private Charset getCharset(MimeType m) {
		if (m == null)
			return null;
		String name = m.getParameters().get("charset");
		if (name == null)
			return null;
		return Charset.forName(name);
	}

	public boolean modifiedSince(String entityTag)
			throws MimeTypeParseException {
		boolean notModified = false;
		try {
			long modified = getDateHeader("If-Modified-Since");
			long lastModified = getFile().lastModified();
			long m = target.lastModified();
			if (m > lastModified) {
				lastModified = m;
			}
			notModified = lastModified > 0 && modified > 0;
			if (notModified && modified < lastModified)
				return true;
		} catch (IllegalArgumentException e) {
			// invalid date header
		}
		Enumeration matchs = getHeaders("If-None-Match");
		boolean mustMatch = matchs.hasMoreElements();
		if (mustMatch) {
			while (matchs.hasMoreElements()) {
				String match = (String) matchs.nextElement();
				if (match(entityTag, match))
					return false;
			}
		}
		return !notModified || mustMatch;
	}

	public boolean unmodifiedSince(String entityTag)
			throws MimeTypeParseException {
		Enumeration matchs = getHeaders("If-Match");
		boolean mustMatch = matchs.hasMoreElements();
		try {
			long unmodified = getDateHeader("If-Unmodified-Since");
			long lastModified = getFile().lastModified();
			if (unmodified > 0 && lastModified > unmodified)
				return false;
			lastModified = target.lastModified();
			if (unmodified > 0 && lastModified > unmodified)
				return false;
		} catch (IllegalArgumentException e) {
			// invalid date header
		}
		while (matchs.hasMoreElements()) {
			String match = (String) matchs.nextElement();
			if (match(entityTag, match))
				return true;
		}
		return !mustMatch;
	}

	private boolean match(String tag, String match) {
		if (tag == null)
			return false;
		if ("*".equals(match))
			return true;
		if (match.equals(tag))
			return true;
		if (!"DELETE".equals(request.getMethod()))
			return false;
		// DELETE only has to match the revision, not the serialised variant
		return match.contains(tag.substring(2, tag.length() - 1));
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

	private boolean isCompatible(MimeType media, MimeType m) {
		if (media.match(m))
			return true;
		if ("*".equals(media.getPrimaryType()))
			return true;
		if ("*".equals(m.getPrimaryType()))
			return true;
		if (!media.getPrimaryType().equals(m.getPrimaryType()))
			return false;
		if ("*".equals(media.getSubType()))
			return true;
		if ("*".equals(m.getSubType()))
			return true;
		return false;
	}

	private String removeParamaters(String mediaType) {
		if (mediaType == null)
			return null;
		int idx = mediaType.indexOf(';');
		if (idx > 0)
			return mediaType.substring(0, idx);
		return mediaType;
	}

	private <T> T toObject(String value, Class<T> klass)
			throws RepositoryException {
		if (String.class.equals(klass)) {
			return klass.cast(value);
		} else if (klass.isInterface() || of.isNamedConcept(klass)) {
			return klass.cast(con.getObject(createURI(value)));
		} else {
			URI datatype = vf.createURI("java:", klass.getName());
			Literal lit = vf.createLiteral(value, datatype);
			return klass.cast(of.createObject(lit));
		}
	}

}
