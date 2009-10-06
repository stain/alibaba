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
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;

import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectFactory;
import org.openrdf.repository.object.traits.RDFObjectBehaviour;
import org.openrdf.server.metadata.annotations.type;
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
	private static Type parameterMapType;
	static {
		try {
			parameterMapType = Request.class.getDeclaredMethod(
					"getParameterMap").getGenericReturnType();
		} catch (NoSuchMethodException e) {
			throw new AssertionError(e);
		}
	}
	protected ObjectFactory of;
	protected ValueFactory vf;
	private ObjectConnection con;
	private File file;
	private MessageBodyReader reader;
	private HttpServletRequest request;
	private WebResource target;
	private URI uri;
	private MessageBodyWriter writer;

	public Request(MessageBodyReader reader, MessageBodyWriter writer,
			File dataDir, HttpServletRequest request, ObjectConnection con)
			throws QueryEvaluationException, RepositoryException {
		super(request);
		this.reader = reader;
		this.writer = writer;
		this.request = request;
		this.con = con;
		this.vf = con.getValueFactory();
		this.of = con.getObjectFactory();
		this.uri = vf.createURI(getURI());
		target = con.getObject(WebResource.class, uri);
		File base = new File(dataDir, safe(getAuthority()));
		file = new File(base, safe(getPath()));
		if (!file.isFile()) {
			int dot = file.getName().lastIndexOf('.');
			String name = Integer.toHexString(uri.hashCode());
			if (dot > 0) {
				name = '$' + name + file.getName().substring(dot);
			} else {
				name = '$' + name;
			}
			file = new File(file, name);
		}
	}

	public ExceptionEntity createExceptionEntity(Exception e) {
		return new ExceptionEntity(writer, reader, e, e.getClass(), e
				.getClass(), uri.stringValue(), con);
	}

	public FileEntity createFileEntity() throws MimeTypeParseException {
		String mediaType = target.getMediaType();
		String mimeType = removeParamaters(mediaType);
		Charset charset = getCharset(mediaType);
		String base = uri.stringValue();
		return new FileEntity(writer, reader, mimeType, getFile(), charset,
				base, con);
	}

	public ResultEntity createResultEntity(Object result, Class<?> type,
			Type genericType, String[] mimeTypes) {
		if (type.equals(Set.class)) {
			Set set = (Set) result;
			Iterator iter = set.iterator();
			try {
				if (!iter.hasNext()) {
					result = null;
					genericType = type = getParameterClass(genericType);
				} else {
					Object object = iter.next();
					if (!iter.hasNext()) {
						result = object;
						genericType = type = getParameterClass(genericType);
					}
				}
			} finally {
				target.getObjectConnection().close(iter);
			}
		}
		if (result instanceof RDFObjectBehaviour) {
			result = ((RDFObjectBehaviour) result).getBehaviourDelegate();
		}
		return new ResultEntity(writer, reader, mimeTypes, result, type,
				genericType, uri.stringValue(), con);
	}

	public URI createURI(String uriSpec) {
		return vf.createURI(parseURI(uriSpec).toString());
	}

	public void flush() throws RepositoryException, QueryEvaluationException {
		ObjectConnection con = target.getObjectConnection();
		con.setAutoCommit(true); // flush()
		this.target = con.getObject(WebResource.class, target.getResource());
	}

	public Entity getBody() throws MimeTypeParseException, IOException {
		String mediaType = getContentType();
		String mime = removeParamaters(mediaType);
		String location = getHeader("Content-Location");
		if (location != null) {
			location = createURI(location).stringValue();
		}
		Charset charset = getCharset(mediaType);
		ServletInputStream in = isMessageBody() ? request.getInputStream()
				: null;
		return new BodyEntity(reader, mime, in, charset, uri.stringValue(),
				location, con);
	}

	public String getContentType(Method method) throws MimeTypeParseException {
		Class<?> type = method.getReturnType();
		if (method.isAnnotationPresent(type.class)) {
			String[] mediaTypes = method.getAnnotation(type.class).value();
			Collection<? extends MimeType> acceptable = getAcceptable();
			for (MimeType m : acceptable) {
				for (String mediaType : mediaTypes) {
					MimeType media = new MimeType(mediaType);
					if (isCompatible(m, media)) {
						String mime = removeParamaters(mediaType);
						if (writer.isWriteable(mime, type, of)) {
							return getContentType(type, media, m, mime);
						}
					}
				}
			}
		} else {
			Collection<? extends MimeType> acceptable = getAcceptable();
			for (MimeType m : acceptable) {
				String mime = m.getPrimaryType() + "/" + m.getSubType();
				if (writer.isWriteable(mime, type, of)) {
					return getContentType(type, null, m, mime);
				}
			}
		}
		return null;
	}

	public File getFile() {
		return file;
	}

	public InputStream getInputStream() throws IOException {
		return request.getInputStream();
	}

	public ObjectConnection getObjectConnection() {
		return con;
	}

	public String getOperation() {
		Map<String, String[]> params = getParameterMap();
		if (params != null) {
			for (String key : params.keySet()) {
				String[] values = params.get(key);
				if (values == null || values.length == 0 || values.length == 1
						&& (values[0] == null || values[0].length() == 0)) {
					return key;
				}
			}
		}
		return null;
	}

	public Entity getParameter(String... names) {
		String[] values = getParameterValues(names);
		return new ParameterEntity(reader, "text/plain", values, uri
				.stringValue(), con);
	}

	public Entity getQueryString() {
		String mimeType = "application/x-www-form-urlencoded";
		String value = request.getQueryString();
		if (value == null) {
			return new ParameterEntity(reader, mimeType, new String[0], uri
					.stringValue(), con);
		}
		return new ParameterEntity(reader, mimeType, new String[] { value },
				uri.stringValue(), con);
	}

	public WebResource getRequestedResource() {
		return target;
	}

	public boolean isAcceptable(Class<?> type) throws MimeTypeParseException {
		return isAcceptable(null, type);
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

	public boolean isAcceptable(String mediaType) throws MimeTypeParseException {
		return isAcceptable(mediaType, null);
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

	public boolean isQueryStringPresent() {
		return request.getQueryString() != null;
	}

	public boolean isReadable(Class<?> class1, Type type) {
		if (!isMessageBody() && getHeader("Content-Location") == null)
			return true;
		String mime = removeParamaters(getContentType());
		return mime == null || reader.isReadable(class1, type, mime, con);
	}

	public boolean modifiedSince(String entityTag, long lastModified)
			throws MimeTypeParseException {
		boolean notModified = false;
		try {
			if (lastModified > 0) {
				long modified = getDateHeader("If-Modified-Since");
				notModified = modified > 0;
				if (notModified && modified < lastModified)
					return true;
			}
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

	public boolean unmodifiedSince(String entityTag, long lastModified)
			throws MimeTypeParseException {
		Enumeration matchs = getHeaders("If-Match");
		boolean mustMatch = matchs.hasMoreElements();
		try {
			if (lastModified > 0) {
				long unmodified = getDateHeader("If-Unmodified-Since");
				if (unmodified > 0 && lastModified > unmodified)
					return false;
			}
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

	private Charset getCharset(String mediaType) throws MimeTypeParseException {
		if (mediaType == null)
			return null;
		MimeType m = new MimeType(mediaType);
		String name = m.getParameters().get("charset");
		if (name == null)
			return null;
		return Charset.forName(name);
	}

	private String getContentType(Class<?> type, MimeType m1, MimeType m2,
			String mime) {
		Charset charset = null;
		if (m1 != null) {
			String name = m1.getParameters().get("charset");
			try {
				if (name != null) {
					charset = Charset.forName(name);
					// m1 is not varied on request
					return writer.getContentType(mime, type, of, charset);
				}
			} catch (UnsupportedCharsetException e) {
				// ignore
			}
		}
		if (m2 != null) {
			String name = m2.getParameters().get("charset");
			try {
				if (name != null)
					charset = Charset.forName(name);
			} catch (UnsupportedCharsetException e) {
				// ignore
			}
		}
		if (charset == null) {
			int rating = 0;
			Enumeration<String> accept = getHeaders("Accept-Charset");
			while (accept.hasMoreElements()) {
				String header = accept.nextElement().replaceAll("\\s", "");
				for (String item : header.split(",")) {
					int q = 1;
					String name = item;
					int c = item.indexOf(';');
					if (c > 0) {
						name = item.substring(0, c);
						q = getQuality(item);
					}
					if (q > rating) {
						try {
							charset = Charset.forName(name);
							rating = q;
						} catch (UnsupportedCharsetException e) {
							// ignore
						}
					}
				}
			}
		}
		String contentType = writer.getContentType(mime, type, of, charset);
		if (contentType.contains("charset=")) {
			getVaryHeaders("Accept-Charset");
		}
		return contentType;
	}

	private Class<?> getParameterClass(Type type) {
		if (type instanceof ParameterizedType) {
			ParameterizedType ptype = (ParameterizedType) type;
			Type[] args = ptype.getActualTypeArguments();
			if (args.length == 1) {
				if (args[0] instanceof Class) {
					return (Class<?>) args[0];
				}
			}
		}
		return Object.class;
	}

	private Map<String, String[]> getParameterMap() {
		try {
			return getQueryString().read(Map.class, parameterMapType);
		} catch (Exception e) {
			return Collections.emptyMap();
		}
	}

	private String[] getParameterValues(String... names) {
		if (names.length == 0) {
			return new String[0];
		} else if (names.length == 1) {
			return request.getParameterValues(names[0]);
		} else {
			List<String> list = new ArrayList<String>(names.length * 2);
			for (String name : names) {
				list.addAll(Arrays.asList(request.getParameterValues(name)));
			}
			return list.toArray(new String[list.size()]);
		}
	}

	private int getQuality(String item) {
		int s = item.indexOf(";q=");
		if (s > 0) {
			int e = item.indexOf(';', s + 1);
			if (e < 0) {
				e = item.length();
			}
			try {
				return Integer.parseInt(item.substring(s + 3, e));
			} catch (NumberFormatException exc) {
				// ignore q
			}
		}
		return 1;
	}

	private boolean isCompatible(MimeType media, MimeType m) {
		if (media.match(m))
			return true;
		// TODO check parameters
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

	private boolean match(String tag, String match) {
		if (tag == null)
			return false;
		if ("*".equals(match))
			return true;
		if (match.equals(tag))
			return true;
		String method = request.getMethod();
		if ("GET".equals(method) || "PUT".equals(method)
				|| "HEAD".equals(method) || tag.indexOf('-') > 0)
			return false;
		// other methods only have to match the revision tag, not the serialised
		// variant
		if (match.startsWith(tag.substring(0, tag.length() - 1)))
			return true;
		return match.startsWith(tag.substring(2, tag.length() - 1));
	}

	private String removeParamaters(String mediaType) {
		if (mediaType == null)
			return null;
		int idx = mediaType.indexOf(';');
		if (idx > 0)
			return mediaType.substring(0, idx);
		return mediaType;
	}

	private String safe(String path) {
		path = path.replace('/', File.separatorChar);
		path = path.replace('\\', File.separatorChar);
		return path.replaceAll("[^a-zA-Z0-9/\\\\]", "_");
	}

}
