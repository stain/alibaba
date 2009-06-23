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

import info.aduna.net.ParsedURI;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

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
import org.openrdf.server.metadata.concepts.RDFResource;
import org.openrdf.server.metadata.concepts.WebResource;
import org.openrdf.server.metadata.http.readers.MessageBodyReader;
import org.openrdf.server.metadata.http.writers.MessageBodyWriter;

/**
 * Utility class for {@link HttpServletRequest}.
 * 
 * @author James Leigh
 * 
 */
public class Request {
	private static final List<String> HTTP_METHODS = Arrays.asList("OPTIONS",
			"GET", "HEAD", "PUT", "DELETE");
	protected ObjectFactory of;
	protected ValueFactory vf;
	private ObjectConnection con;
	private File file;
	private MessageBodyReader reader;
	private HttpServletRequest request;
	private RDFResource target;
	private URI uri;
	private MessageBodyWriter writer;

	public Request(MessageBodyReader reader, MessageBodyWriter writer,
			File dataDir, HttpServletRequest request, ObjectConnection con)
			throws QueryEvaluationException, RepositoryException {
		this.reader = reader;
		this.writer = writer;
		this.request = request;
		this.con = con;
		this.vf = con.getValueFactory();
		this.of = con.getObjectFactory();
		String host = getHost(request);
		String url;
		try {
			String scheme = request.getScheme();
			String path = getPath(request);
			url = new java.net.URI(scheme, host, path, null).toASCIIString();
		} catch (URISyntaxException e) {
			// bad Host header
			StringBuffer url1 = request.getRequestURL();
			int idx = url1.indexOf("?");
			if (idx > 0) {
				url = url1.substring(0, idx);
			} else {
				url = url1.toString();
			}
		}
		this.uri = vf.createURI(url);
		target = con.getObject(WebResource.class, uri);
		File base = new File(dataDir, safe(host));
		file = new File(base, safe(getPath(request)));
		if (!file.isFile()) {
			int dot = file.getName().lastIndexOf('.');
			String name = Integer.toHexString(url.hashCode());
			if (dot > 0) {
				name = '$' + name + file.getName().substring(dot);
			} else {
				name = '$' + name;
			}
			file = new File(file, name);
		}
	}

	public Collection<? extends MimeType> getAcceptable(String accept)
			throws MimeTypeParseException {
		StringBuilder sb = new StringBuilder();
		if (accept != null) {
			sb.append(accept);
		}
		Enumeration headers = getHeaders("Accept");
		while (headers.hasMoreElements()) {
			if (sb.length() > 0) {
				sb.append(", ");
			}
			sb.append((String) headers.nextElement());
		}
		String[] mediaTypes = sb.toString().split(",\\s*");
		Set<MimeType> list = new TreeSet<MimeType>(new Comparator<MimeType>(){

			public int compare(MimeType o1, MimeType o2) {
				String s1 = o1.getParameter("q");
				String s2 = o2.getParameter("q");
				Double q1 = s1 == null ? 1 : Double.valueOf(s1);
				Double q2 = s2 == null ? 1 : Double.valueOf(s2);
				return q2.compareTo(q1);
			}});
		for (String mediaType : mediaTypes) {
			if (mediaType.startsWith("*;")) {
				list.add(new MimeType("*/" + mediaType));
			} else {
				list.add(new MimeType(mediaType));
			}
		}
		return list;
	}

	public Object getBody(Class<?> class1, Type type) throws IOException,
			MimeTypeParseException {
		if (request.getHeader("Content-Length") == null
				&& request.getHeader("Transfer-Encoding") == null
				&& request.getHeader("Content-Location") == null)
			return null;
		String mediaType = request.getContentType();
		String mime = removeParamaters(mediaType);
		if (mediaType == null && !reader.isReadable(class1, type, mime, con)) {
			return null;
		}
		String location = request.getHeader("Content-Location");
		if (location != null) {
			location = createURI(location).stringValue();
		}
		Charset charset = getCharset(mediaType, null);
		try {
			return reader.readFrom(class1, type, mime,
					request.getInputStream(), charset, uri.stringValue(),
					location, con);
		} catch (OpenRDFException e) {
			throw new IOException(e);
		}
	}

	public File getFile() {
		return file;
	}

	public String getHeader(String name) {
		return request.getHeader(name);
	}

	public Enumeration getHeaderNames() {
		return request.getHeaderNames();
	}

	public Enumeration getHeaders(String header) {
		return request.getHeaders(header);
	}

	public InputStream getInputStream() throws IOException {
		return request.getInputStream();
	}

	public String getMethod() {
		return request.getMethod();
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

	public String getRequestURI() {
		return request.getRequestURI();
	}

	public String getRequestURL() {
		String qs = request.getQueryString();
		if (qs == null)
			return uri.stringValue();
		return uri.stringValue() + "?" + qs;
	}

	public String getURI() {
		return uri.stringValue();
	}

	public boolean isAcceptable(Class<?> type) throws MimeTypeParseException {
		return isAcceptable(type, null);
	}

	public boolean isAcceptable(Class<?> type, String mediaType)
			throws MimeTypeParseException {
		MimeType media = mediaType == null ? null : new MimeType(mediaType);
		Collection<? extends MimeType> acceptable = getAcceptable(null);
		for (MimeType m : acceptable) {
			if (media != null && !isCompatible(media, m))
				continue;
			if (type != null
					&& !writer.isWriteable(type, m.getPrimaryType() + "/"
							+ m.getSubType()))
				continue;
			return true;
		}
		return false;
	}

	public boolean isAcceptable(String mediaType) throws MimeTypeParseException {
		return isAcceptable(null, mediaType);
	}

	public boolean isQueryStringPresent() {
		return request.getQueryString() != null;
	}

	public boolean isReadable(Class<?> class1, Type type) {
		if (request.getHeader("Content-Length") == null
				&& request.getHeader("Transfer-Encoding") == null
				&& request.getHeader("Content-Location") == null)
			return true;
		String mime = removeParamaters(request.getContentType());
		return mime == null || reader.isReadable(class1, type, mime, con);
	}

	public boolean modifiedSince() {
		try {
			long modified = request.getDateHeader("If-Modified-Since");
			long lastModified = getFile().lastModified();
			long m = target.lastModified();
			if (m > lastModified) {
				lastModified = m;
			}
			if (lastModified > 0 && modified > 0)
				return modified < lastModified;
		} catch (IllegalArgumentException e) {
			// invalid date header
		}
		Enumeration matchs = getHeaders("If-None-Match");
		if (matchs.hasMoreElements()) {
			String tag = target.eTag();
			while (matchs.hasMoreElements()) {
				String match = (String) matchs.nextElement();
				if (tag != null && ("*".equals(match) || tag.equals(match)))
					return false;
			}
		}
		return true;
	}

	public boolean unmodifiedSince() {
		Enumeration matchs = request.getHeaders("If-Match");
		boolean mustMatch = matchs.hasMoreElements();
		try {
			long unmodified = request.getDateHeader("If-Unmodified-Since");
			long lastModified = getFile().lastModified();
			if (unmodified > 0 && lastModified > unmodified)
				return false;
			lastModified = target.lastModified();
			if (unmodified > 0 && lastModified > unmodified)
				return false;
		} catch (IllegalArgumentException e) {
			// invalid date header
		}
		String tag = target.eTag();
		while (matchs.hasMoreElements()) {
			String match = (String) matchs.nextElement();
			if (tag != null && ("*".equals(match) || tag.equals(match)))
				return true;
		}
		return !mustMatch;
	}

	private URI createURI(String uriSpec) {
		ParsedURI base = new ParsedURI(uri.stringValue());
		base.normalize();
		ParsedURI uri = new ParsedURI(uriSpec);
		return vf.createURI(base.resolve(uri).toString());
	}

	private Charset getCharset(String mediaType, Charset defCharset)
			throws MimeTypeParseException {
		if (mediaType == null)
			return defCharset;
		MimeType m = new MimeType(mediaType);
		String name = m.getParameters().get("charset");
		if (name == null)
			return defCharset;
		return Charset.forName(name);
	}

	private String getHost(HttpServletRequest request) {
		String host = request.getHeader("Host");
		if (host == null)
			return request.getServerName();
		return host;
	}

	private String getPath(HttpServletRequest request) {
		String path = request.getRequestURI();
		int idx = path.indexOf('?');
		if (idx > 0) {
			path = path.substring(0, idx);
		}
		return path;
	}

	private boolean isCompatible(MimeType media, MimeType m) {
		if (media.match(m))
			return true;
		if ("*".equals(media.getPrimaryType()))
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

	private String safe(String path) {
		path = path.replace('/', File.separatorChar);
		path = path.replace('\\', File.separatorChar);
		path = path.replace('*', '_');
		path = path.replace('"', '_');
		path = path.replace('[', '_');
		path = path.replace(']', '_');
		path = path.replace(':', '_');
		path = path.replace(';', '_');
		path = path.replace('|', '_');
		path = path.replace('=', '_');
		path = path.replace('$', '_'); // used in getFile()
		return path;
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
