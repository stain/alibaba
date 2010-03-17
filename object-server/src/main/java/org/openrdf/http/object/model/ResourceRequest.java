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
package org.openrdf.http.object.model;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.openrdf.http.object.annotations.type;
import org.openrdf.http.object.concepts.HTTPFileObject;
import org.openrdf.http.object.traits.VersionedObject;
import org.openrdf.http.object.util.Accepter;
import org.openrdf.http.object.util.ChannelUtil;
import org.openrdf.http.object.util.GenericType;
import org.openrdf.http.object.writers.AggregateWriter;
import org.openrdf.http.object.writers.MessageBodyWriter;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectFactory;
import org.openrdf.repository.object.ObjectRepository;
import org.openrdf.repository.object.traits.RDFObjectBehaviour;
import org.openrdf.result.Result;

/**
 * Tracks the target resource with the request.
 * 
 * @author James Leigh
 * 
 */
public class ResourceRequest extends Request {
	private static Type parameterMapType;
	static {
		try {
			parameterMapType = ResourceRequest.class.getDeclaredMethod(
					"getParameterMap").getGenericReturnType();
		} catch (NoSuchMethodException e) {
			throw new AssertionError(e);
		}
	}
	private ObjectFactory of;
	private ValueFactory vf;
	private ObjectConnection con;
	private File dataDir;
	private File file;
	private VersionedObject target;
	private URI uri;
	private MessageBodyWriter writer = AggregateWriter.getInstance();
	private BodyEntity body;
	private Accepter accepter;
	private List<String> vary = new ArrayList<String>();
	private Result<HTTPFileObject> result;

	public ResourceRequest(File dataDir, HttpEntityEnclosingRequest request,
			ObjectRepository repository) throws QueryEvaluationException,
			RepositoryException, MimeTypeParseException {
		super(request);
		this.con = repository.getConnection();
		con.setAutoCommit(false); // begin()
		this.dataDir = dataDir;
		this.vf = con.getValueFactory();
		this.of = con.getObjectFactory();
		this.uri = vf.createURI(getURI());
		Enumeration headers = getVaryHeaders("Accept");
		if (headers.hasMoreElements()) {
			StringBuilder sb = new StringBuilder();
			while (headers.hasMoreElements()) {
				if (sb.length() > 0) {
					sb.append(", ");
				}
				sb.append((String) headers.nextElement());
			}
			accepter = new Accepter(sb.toString());
		} else {
			accepter = new Accepter();
		}
		result = con.getObjects(HTTPFileObject.class, uri);
	}

	public void init() throws RepositoryException, QueryEvaluationException,
			MimeTypeParseException {
		if (target == null) {
			target = result.singleResult();
			if (target instanceof HTTPFileObject) {
				File base = new File(dataDir, safe(getAuthority()));
				String path = getPath();
				if (path == null) {
					file = new File(base, safe(uri.stringValue()));
				} else {
					file = new File(base, safe(path));
				}
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
				((HTTPFileObject) target).initLocalFileObject(file, isSafe());
			}
		}
	}

	public Enumeration getVaryHeaders(String name) {
		if (!vary.contains(name)) {
			vary.add(name);
		}
		return getHeaderEnumeration(name);
	}

	public List<String> getVary() {
		return vary;
	}

	public ResponseEntity createResultEntity(Object result, Class<?> ctype,
			Type gtype, String[] mimeTypes) {
		GenericType<?> type = new GenericType(ctype, gtype);
		if (result != null && type.isSet()) {
			Set set = (Set) result;
			Iterator iter = set.iterator();
			try {
				if (!iter.hasNext()) {
					result = null;
					ctype = type.getComponentClass();
					gtype = type.getComponentType();
				} else {
					Object object = iter.next();
					if (!iter.hasNext()) {
						result = object;
						ctype = type.getComponentClass();
						gtype = type.getComponentType();
					}
				}
			} finally {
				getObjectConnection().close(iter);
			}
		} else if (result != null && type.isArray()) {
			int len = Array.getLength(result);
			if (len == 0) {
				result = null;
				ctype = type.getComponentClass();
				gtype = type.getComponentType();
			} else if (len == 1) {
				result = Array.get(result, 0);
				ctype = type.getComponentClass();
				gtype = type.getComponentType();
			}
		}
		if (result instanceof RDFObjectBehaviour) {
			result = ((RDFObjectBehaviour) result).getBehaviourDelegate();
		}
		return new ResponseEntity(mimeTypes, result, ctype, gtype, uri
				.stringValue(), con);
	}

	public URI createURI(String uriSpec) {
		return vf.createURI(parseURI(uriSpec).toString());
	}

	public void flush() throws RepositoryException, QueryEvaluationException,
			IOException {
		ObjectConnection con = getObjectConnection();
		con.commit(); // flush()
		this.target = con.getObject(HTTPFileObject.class, target.getResource());
	}

	public void rollback() throws RepositoryException {
		ObjectConnection con = getObjectConnection();
		con.rollback();
		con.setAutoCommit(true); // rollback()
	}

	public void commit() throws IOException, RepositoryException {
		try {
			ObjectConnection con = getObjectConnection();
			con.setAutoCommit(true); // commit()
		} catch (RepositoryException e) {
			rollback();
		}
	}

	public void close() throws RepositoryException {
		ObjectConnection con = getObjectConnection();
		con.rollback();
		con.close();
	}

	public Entity getBody() throws MimeTypeParseException {
		if (body != null)
			return body;
		String mediaType = getHeader("Content-Type");
		String mime = removeParamaters(mediaType);
		String location = getResolvedHeader("Content-Location");
		if (location != null) {
			location = createURI(location).stringValue();
		}
		Charset charset = getCharset(mediaType);
		return body = new BodyEntity(mime, isMessageBody(), charset, uri
				.stringValue(), location, con) {

			@Override
			protected ReadableByteChannel getReadableByteChannel() throws IOException {
				HttpEntity entity = getEntity();
				if (entity == null)
					return null;
				if (entity instanceof HttpEntityChannel)
					return ((HttpEntityChannel) entity)
							.getReadableByteChannel();
				return ChannelUtil.newChannel(entity.getContent());
			}
		};
	}

	public String getContentType(Method method) throws MimeTypeParseException {
		Class<?> type = method.getReturnType();
		Type genericType = method.getGenericReturnType();
		if (method.isAnnotationPresent(type.class)) {
			String[] mediaTypes = method.getAnnotation(type.class).value();
			for (MimeType m : accepter.getAcceptable(mediaTypes)) {
				if (writer.isWriteable(m.toString(), type, genericType, of)) {
					return getContentType(type, genericType, m);
				}
			}
		} else {
			for (MimeType m : accepter.getAcceptable()) {
				if (writer.isWriteable(m.toString(), type, genericType, of)) {
					return getContentType(type, genericType, m);
				}
			}
		}
		return null;
	}

	public File getFile() {
		return file;
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

	public Entity getHeader(String[] mediaTypes, String... names) {
		String[] values = getHeaderValues(names);
		return new ParameterEntity(mediaTypes, "text/plain", values, uri
				.stringValue(), con);
	}

	public Entity getParameter(String[] mediaTypes, String... names) {
		String[] values = getParameterValues(names);
		return new ParameterEntity(mediaTypes, "text/plain", values, uri
				.stringValue(), con);
	}

	public Entity getQueryString(String[] mediaTypes) {
		String mimeType = "application/x-www-form-urlencoded";
		String value = getQueryString();
		if (value == null) {
			return new ParameterEntity(mediaTypes, mimeType, new String[0], uri
					.stringValue(), con);
		}
		return new ParameterEntity(mediaTypes, mimeType,
				new String[] { value }, uri.stringValue(), con);
	}

	public VersionedObject getRequestedResource() {
		return target;
	}

	public boolean isAcceptable(Class<?> type, Type genericType)
			throws MimeTypeParseException {
		return isAcceptable(null, type, genericType);
	}

	public boolean isAcceptable(Method method) throws MimeTypeParseException {
		Class<?> type = method.getReturnType();
		Type genericType = method.getGenericReturnType();
		if (method.isAnnotationPresent(type.class)) {
			for (String media : method.getAnnotation(type.class).value()) {
				if (isAcceptable(media, type, genericType))
					return true;
			}
			return false;
		}
		return isAcceptable(type, genericType);
	}

	public boolean isAcceptable(String mediaType) throws MimeTypeParseException {
		return isAcceptable(mediaType, null, null);
	}

	public boolean isAcceptable(String mediaType, Class<?> type,
			Type genericType) throws MimeTypeParseException {
		if (type == null)
			return accepter.isAcceptable(mediaType);
		for (MimeType accept : accepter.getAcceptable(mediaType)) {
			String mime = accept.getPrimaryType() + "/" + accept.getSubType();
			if (writer.isWriteable(mime, type, genericType, of))
				return true;
		}
		return false;
	}

	public boolean isQueryStringPresent() {
		return getQueryString() != null;
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
		Enumeration matchs = getHeaderEnumeration("If-None-Match");
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
		Enumeration matchs = getHeaderEnumeration("If-Match");
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

	private String getContentType(Class<?> type, Type genericType, MimeType m) {
		Charset charset = null;
		String cname = m.getParameters().get("charset");
		try {
			if (cname != null) {
				charset = Charset.forName(cname);
				return writer.getContentType(m.toString(), type, genericType,
						of, charset);
			}
		} catch (UnsupportedCharsetException e) {
			// ignore
		}
		if (charset == null) {
			int rating = 0;
			Enumeration<String> accept = getHeaderEnumeration("Accept-Charset");
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
		String contentType = writer.getContentType(m.toString(), type,
				genericType, of, charset);
		if (contentType.contains("charset=")) {
			getVaryHeaders("Accept-Charset");
		}
		return contentType;
	}

	private Map<String, String[]> getParameterMap() {
		try {
			return getQueryString(null).read(Map.class, parameterMapType,
					new String[] { "application/x-www-form-urlencoded" });
		} catch (Exception e) {
			return Collections.emptyMap();
		}
	}

	private String[] getParameterValues(String... names) {
		if (names.length == 0) {
			return new String[0];
		} else {
			Map<String, String[]> map = getParameterMap();
			if (map == null) {
				return null;
			} else if (names.length == 1) {
				return map.get(names[0]);
			} else {
				List<String> list = new ArrayList<String>(names.length * 2);
				for (String name : names) {
					list.addAll(Arrays.asList(map.get(name)));
				}
				return list.toArray(new String[list.size()]);
			}
		}
	}

	private String[] getHeaderValues(String... names) {
		if (names.length == 0)
			return new String[0];
		List<String> list = new ArrayList<String>(names.length * 2);
		for (String name : names) {
			Enumeration en = getVaryHeaders(name);
			while (en.hasMoreElements()) {
				list.add((String) en.nextElement());
			}
		}
		return list.toArray(new String[list.size()]);
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

	private boolean match(String tag, String match) {
		if (tag == null)
			return false;
		if ("*".equals(match))
			return true;
		if (match.equals(tag))
			return true;
		int md = match.indexOf('-');
		int td = tag.indexOf('-');
		if (td >= 0 && md >= 0)
			return false;
		if (md < 0) {
			md = match.lastIndexOf('"');
		}
		if (td < 0) {
			td = tag.lastIndexOf('"');
		}
		int mq = match.indexOf('"');
		int tq = tag.indexOf('"');
		if (mq < 0 || tq < 0 || md < 0 || td < 0)
			return false;
		return match.substring(mq, md).equals(tag.substring(tq, td));
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
		if (path == null)
			return "";
		path = path.replace('/', File.separatorChar);
		path = path.replace('\\', File.separatorChar);
		path = path.replace(':', File.separatorChar);
		return path.replaceAll("[^a-zA-Z0-9/\\\\]", "_");
	}
}
