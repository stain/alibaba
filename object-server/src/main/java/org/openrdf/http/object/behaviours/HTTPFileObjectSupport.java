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

import static java.lang.Integer.toHexString;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.datatype.XMLGregorianCalendar;

import org.openrdf.http.object.annotations.header;
import org.openrdf.http.object.annotations.method;
import org.openrdf.http.object.annotations.operation;
import org.openrdf.http.object.annotations.parameter;
import org.openrdf.http.object.annotations.type;
import org.openrdf.http.object.concepts.HTTPFileObject;
import org.openrdf.http.object.concepts.Transaction;
import org.openrdf.http.object.exceptions.ResponseException;
import org.openrdf.http.object.writers.AggregateWriter;
import org.openrdf.http.object.writers.MessageBodyWriter;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectFactory;
import org.openrdf.repository.object.annotations.parameterTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class HTTPFileObjectSupport extends FileObjectSupport implements HTTPFileObject {
	private Logger logger = LoggerFactory.getLogger(HTTPFileObjectSupport.class);
	private File file;
	private boolean readOnly;
	private MessageBodyWriter writer = AggregateWriter.getInstance();

	public void initLocalFileObject(File file, boolean readOnly) {
		this.file = file;
		this.readOnly = readOnly;
	}

	public String revisionTag() {
		Transaction trans = getRevision();
		if (trans == null)
			return null;
		String uri = trans.getResource().stringValue();
		String revision = toHexString(uri.hashCode());
		return "W/" + '"' + revision + '"';
	}

	public String variantTag(String mediaType) {
		if (mediaType == null)
			return revisionTag();
		String variant = toHexString(mediaType.hashCode());
		String schema = toHexString(getObjectConnection().getSchemaRevision());
		Transaction trans = getRevision();
		if (trans == null)
			return null;
		String uri = trans.getResource().stringValue();
		String revision = toHexString(uri.hashCode());
		return "W/" + '"' + revision + '-' + variant + '-' + schema + '"';
	}

	public long getLastModified() {
		long lastModified = super.getLastModified();
		Transaction trans = getRevision();
		if (trans != null) {
			XMLGregorianCalendar xgc = trans.getCommittedOn();
			if (xgc != null) {
				GregorianCalendar cal = xgc.toGregorianCalendar();
				cal.set(Calendar.MILLISECOND, 0);
				long committed = cal.getTimeInMillis();
				if (lastModified < committed)
					return committed;
			}
		}
		return lastModified;
	}

	public InputStream openInputStream() throws IOException {
		if (toFile() == null) {
			String accept = "*/*";
			RemoteConnection con = openConnection("GET", null);
			con.addHeader("Accept", accept);
			int status = con.getResponseCode();
			if (status >= 400)
				throw new IOException(con.readString());
			if (status < 200 || status >= 300)
				return null;
			return con.readStream();
		} else {
			return super.openInputStream();
		}
	}

	public OutputStream openOutputStream() throws IOException {
		if (readOnly)
			throw new IllegalStateException(
					"Cannot modify entity within a safe methods");
		if (toFile() == null) {
			return openConnection("PUT", null).writeStream();
		} else {
			return super.openOutputStream();
		}
	}

	public boolean delete() {
		if (readOnly)
			throw new IllegalStateException(
					"Cannot modify entity within a safe methods");
		if (toFile() == null) {
			try {
				RemoteConnection con = openConnection("DELETE", null);
				int status = con.getResponseCode();
				if (status >= 400) {
					logger.warn(con.getResponseMessage(), con.readString());
					return false;
				}
				return status >= 200 && status < 300;
			} catch (IOException e) {
				logger.warn(e.toString(), e);
				return false;
			}
		} else {
			setRevision(null);
			return super.delete();
		}
	}

	public Object invokeRemote(Method method, Object[] parameters)
			throws Exception {
		String rm = getRequestMethod(method, parameters);
		String uri = getResource().stringValue();
		String qs = getQueryString(method, parameters);
		String accept = getAcceptHeader(method);
		Annotation[][] panns = method.getParameterAnnotations();
		int body = getRequestBodyParameterIndex(panns, parameters);
		assert body < 0 || !method.isAnnotationPresent(parameterTypes.class);
		RemoteConnection con = openConnection(rm, qs);
		Map<String, List<String>> headers = getHeaders(method, parameters);
		for (Map.Entry<String, List<String>> e : headers.entrySet()) {
			for (String value : e.getValue()) {
				con.addHeader(e.getKey(), value);
			}
		}
		if (accept != null && !headers.containsKey("accept")) {
			con.addHeader("Accept", accept);
		}
		if (body >= 0) {
			Object result = parameters[body];
			Class<?> ptype = method.getParameterTypes()[body];
			Type gtype = method.getGenericParameterTypes()[body];
			String media = getParameterMediaType(panns[body], ptype, gtype);
			con.write(media, ptype, gtype, result);
		}
		int status = con.getResponseCode();
		Class<?> rtype = method.getReturnType();
		if (body < 0 && Set.class.equals(rtype) && status == 404) {
			Type gtype = method.getGenericReturnType();
			Set values = new HashSet();
			ObjectConnection oc = getObjectConnection();
			return new RemoteSetSupport(uri, qs, gtype, values, oc);
		} else if (body < 0 && status == 404) {
			return null;
		} else if (status >= 300) {
			String msg = con.getResponseMessage();
			String stack = con.readString();
			throw ResponseException.create(status, msg, stack);
		} else if (Void.TYPE.equals(rtype)) {
			return null;
		} else if (body < 0 && Set.class.equals(rtype)) {
			Type gtype = method.getGenericReturnType();
			Set values = (Set) con.read(gtype, rtype);
			ObjectConnection oc = getObjectConnection();
			return new RemoteSetSupport(uri, qs, gtype, values, oc);
		} else {
			Type gtype = method.getGenericReturnType();
			return con.read(gtype, rtype);
		}
	}

	protected File toFile() {
		return file;
	}

	private Map<String, List<String>> getHeaders(Method method, Object[] param)
			throws Exception {
		Map<String, List<String>> map = new HashMap<String, List<String>>();
		Annotation[][] panns = method.getParameterAnnotations();
		Class<?>[] ptypes = method.getParameterTypes();
		Type[] gtypes = method.getGenericParameterTypes();
		for (int i = 0; i < panns.length; i++) {
			if (param[i] == null)
				continue;
			for (Annotation ann : panns[i]) {
				if (ann.annotationType().equals(header.class)) {
					Charset cs = Charset.forName("ISO-8859-1");
					String m = getParameterMediaType(panns[i], ptypes[i],
							gtypes[i]);
					String txt = m == null ? "text/plain" : m;
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					writeTo(txt, ptypes[i], gtypes[i], param[i], out, cs);
					String value = out.toString("ISO-8859-1");
					for (String name : ((header) ann).value()) {
						List<String> list = map.get(name.toLowerCase());
						if (list == null) {
							map.put(name.toLowerCase(),
									list = new LinkedList<String>());
						}
						list.add(value);
					}
				}
			}
		}
		return map;
	}

	private RemoteConnection openConnection(String method, String qs)
			throws IOException {
		String uri = getResource().stringValue();
		ObjectConnection oc = getObjectConnection();
		return new RemoteConnection(method, uri, qs, oc);
	}

	private String getRequestMethod(Method method, Object[] parameters) {
		Class<?> rt = method.getReturnType();
		Annotation[][] panns = method.getParameterAnnotations();
		String rm = getPropertyMethod(rt, panns, parameters);
		if (method.isAnnotationPresent(method.class)) {
			String[] values = method.getAnnotation(method.class).value();
			for (String value : values) {
				if (value.equals(rm))
					return value;
			}
			if (values.length > 0)
				return values[0];
		}
		return rm;
	}

	private String getPropertyMethod(Class<?> rt, Annotation[][] panns,
			Object[] parameters) {
		int body = getRequestBodyParameterIndex(panns, parameters);
		if (!Void.TYPE.equals(rt) && body < 0) {
			return "GET";
		}
		if (Void.TYPE.equals(rt) && body >= 0) {
			if (parameters[body] == null)
				return "DELETE";
			return "PUT";
		}
		return "POST";
	}

	private String getQueryString(Method method, Object[] param)
			throws Exception {
		StringBuilder sb = new StringBuilder();
		if (method.isAnnotationPresent(operation.class)) {
			String[] values = method.getAnnotation(operation.class).value();
			if (values.length > 0) {
				sb.append(enc(values[0]));
			}
		}
		Class<?>[] ptypes = method.getParameterTypes();
		Type[] gtypes = method.getGenericParameterTypes();
		Annotation[][] panns = method.getParameterAnnotations();
		for (int i = 0; i < panns.length; i++) {
			if (param[i] == null)
				continue;
			for (Annotation ann : panns[i]) {
				if (parameter.class.equals(ann.annotationType())) {
					String name = ((parameter) ann).value()[0];
					append(ptypes[i], gtypes[i], panns[i], name, param, sb);
				}
			}
		}
		if (sb.length() == 0)
			return null;
		return sb.toString();
	}

	private void append(Class<?> ptype, Type gtype, Annotation[] panns,
			String name, Object param, StringBuilder sb) throws Exception {
		String m = getParameterMediaType(panns, ptype, gtype);
		if ("*".equals(name)) {
			String form = m == null ? "application/x-www-form-urlencoded" : m;
			Charset cs = Charset.forName("ISO-8859-1");
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			writeTo(form, ptype, gtype, param, out, cs);
			String qs = out.toString("ISO-8859-1");
			if (qs.length() > 0) {
				if (sb.length() > 0) {
					sb.append("&");
				}
				sb.append(qs); // FIXME need to merge qs here
			}
		} else {
			String txt = m == null ? "text/plain" : m;
			Charset cs = Charset.forName("ISO-8859-1");
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			writeTo(txt, ptype, gtype, param, out, cs);
			String value = out.toString("ISO-8859-1");
			if (sb.length() > 0) {
				sb.append("&");
			}
			sb.append(enc(name)).append("=").append(enc(value));
		}
	}

	private String getParameterMediaType(Annotation[] anns, Class<?> ptype,
			Type gtype) {
		ObjectFactory of = getObjectConnection().getObjectFactory();
		for (Annotation ann : anns) {
			if (ann.annotationType().equals(type.class)) {
				for (String media : ((type) ann).value()) {
					if (writer.isWriteable(media, ptype, gtype, of))
						return media;
				}
			}
		}
		return null;
	}

	private void writeTo(String mediaType, Class<?> ptype, Type gtype,
			Object result, OutputStream out, Charset charset) throws Exception {
		String uri = getResource().stringValue();
		ObjectFactory of = getObjectConnection().getObjectFactory();
		writer.writeTo(mediaType, ptype, gtype, of, result, uri, charset, out,
				4096);
	}

	private String enc(String value) throws AssertionError {
		try {
			return URLEncoder.encode(value, "ISO-8859-1");
		} catch (UnsupportedEncodingException e) {
			throw new AssertionError(e);
		}
	}

	private String getAcceptHeader(Method method) {
		if (method.isAnnotationPresent(type.class)) {
			String[] types = method.getAnnotation(type.class).value();
			if (types.length == 1)
				return types[0];
			StringBuilder sb = new StringBuilder();
			for (String type : types) {
				if (sb.length() > 0) {
					sb.append(", ");
				}
				sb.append(type);
			}
			return sb.toString();
		} else {
			return "*/*";
		}
	}

	private int getRequestBodyParameterIndex(Annotation[][] panns,
			Object[] parameters) {
		for (int i = 0; i < panns.length; i++) {
			boolean body = true;
			for (Annotation ann : panns[i]) {
				if (parameter.class.equals(ann.annotationType())) {
					body = false;
				} else if (header.class.equals(ann.annotationType())) {
					body = false;
				}
			}
			if (body && i < parameters.length) {
				return i;
			}
		}
		return -1;
	}

}
