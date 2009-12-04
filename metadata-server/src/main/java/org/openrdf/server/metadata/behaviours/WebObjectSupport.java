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
package org.openrdf.server.metadata.behaviours;

import static java.lang.Integer.toHexString;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.activation.MimeTypeParseException;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.codec.binary.Base64;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectFactory;
import org.openrdf.repository.object.annotations.parameterTypes;
import org.openrdf.server.metadata.annotations.header;
import org.openrdf.server.metadata.annotations.method;
import org.openrdf.server.metadata.annotations.operation;
import org.openrdf.server.metadata.annotations.parameter;
import org.openrdf.server.metadata.annotations.type;
import org.openrdf.server.metadata.concepts.InternalWebObject;
import org.openrdf.server.metadata.concepts.Transaction;
import org.openrdf.server.metadata.exceptions.ResponseException;
import org.openrdf.server.metadata.writers.AggregateWriter;
import org.openrdf.server.metadata.writers.MessageBodyWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.medsea.mimeutil.MimeType;
import eu.medsea.mimeutil.MimeUtil;

public abstract class WebObjectSupport implements InternalWebObject {
	private static int counter;
	private Logger logger = LoggerFactory.getLogger(WebObjectSupport.class);
	private File file;
	private File pending;
	private boolean deleted;
	private boolean readOnly;
	private MessageBodyWriter writer = AggregateWriter.getInstance();

	public void initFileObject(File file, boolean readOnly) {
		this.file = file;
		this.readOnly = readOnly;
	}

	public java.net.URI toUri() {
		return java.net.URI.create(getResource().stringValue());
	}

	public String getName() {
		String uri = getResource().stringValue();
		int last = uri.length() - 1;
		int idx = uri.lastIndexOf('/', last - 1) + 1;
		if (idx > 0 && uri.charAt(last) != '/')
			return uri.substring(idx);
		if (idx > 0 && idx != last)
			return uri.substring(idx, last);
		return uri;
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
			return "W/" + '"' + '-' + variant + '-' + schema + '"';
		String uri = trans.getResource().stringValue();
		String revision = toHexString(uri.hashCode());
		return "W/" + '"' + revision + '-' + variant + '-' + schema + '"';
	}

	public long getLastModified() {
		long lastModified = 0;
		if (pending != null) {
			lastModified = pending.lastModified() / 1000 * 1000;
		} else if (file != null) {
			lastModified = file.lastModified() / 1000 * 1000;
		}
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

	public Charset charset() {
		String mediaType = getMediaType();
		if (mediaType == null)
			return null;
		try {
			javax.activation.MimeType m;
			m = new javax.activation.MimeType(mediaType);
			String name = m.getParameters().get("charset");
			if (name == null)
				return null;
			return Charset.forName(name);
		} catch (MimeTypeParseException e) {
			logger.info(e.toString(), e);
			return null;
		} catch (IllegalCharsetNameException e) {
			logger.info(e.toString(), e);
			return null;
		} catch (UnsupportedCharsetException e) {
			logger.info(e.toString(), e);
			return null;
		} catch (IllegalArgumentException e) {
			logger.info(e.toString(), e);
			return null;
		}
	}

	public String getAndSetMediaType() {
		String mediaType = getMediaType();
		if (mediaType == null && pending != null) {
			mediaType = getMimeType(pending);
			setMediaType(mediaType);
		} else if (mediaType == null && file != null) {
			mediaType = getMimeType(file);
			setMediaType(mediaType);
		}
		return mediaType;
	}

	public InputStream openInputStream() throws IOException {
		String encoding;
		InputStream in;
		if (file == null && pending == null) {
			String mediaType = getMediaType();
			String accept = "*/*";
			if (mediaType != null) {
				accept = mediaType + ", */*;q=0.1";
			}
			RemoteConnection con = openConnection("GET", null);
			con.addHeader("Accept", accept);
			int status = con.getResponseCode();
			if (status >= 400)
				throw new IOException(con.readString());
			if (status < 200 || status >= 300)
				return null;
			return con.readStream();
		} else if (pending != null && pending.canRead()) {
			encoding = getContentEncoding();
			in = new FileInputStream(pending);
			// TODO return delayed for use after sync
			if ("gzip".equals(encoding))
				return new GZIPInputStream(in);
			return in;
		} else if (file != null && file.canRead()) {
			encoding = getContentEncoding();
			in = new FileInputStream(file);
			if ("gzip".equals(encoding))
				return new GZIPInputStream(in);
			return in;
		} else {
			return null;
		}
	}

	public Reader openReader(boolean ignoreEncodingErrors) throws IOException {
		Charset charset = charset();
		InputStream in = openInputStream();
		if (in == null)
			return null;
		if (charset == null)
			return new InputStreamReader(in);
		return new InputStreamReader(in, charset);
	}

	public CharSequence getCharContent(boolean ignoreEncodingErrors)
			throws IOException {
		Reader reader = openReader(ignoreEncodingErrors);
		if (reader == null)
			return null;
		try {
			StringWriter writer = new StringWriter();
			int read;
			char[] cbuf = new char[1024];
			while ((read = reader.read(cbuf)) >= 0) {
				writer.write(cbuf, 0, read);
			}
			return writer.toString();
		} finally {
			reader.close();
		}
	}

	public OutputStream openOutputStream() throws IOException {
		if (readOnly)
			throw new IllegalStateException(
					"Cannot modify entity within a safe methods");
		final String mediaType = getMediaType();
		final String encoding = getDesiredEncoding(mediaType);
		if (file == null) {
			RemoteConnection con = openConnection("PUT", null);
			return con.writeStream(mediaType, encoding);
		} else {
			File dir = file.getParentFile();
			dir.mkdirs();
			if (!dir.canWrite() || file.exists() && !file.canWrite())
				throw new IOException("Cannot open file for writting");
			String name = "$partof" + file.getName() + "-" + (++counter);
			final File tmp = new File(dir, name);
			final OutputStream fout = new FileOutputStream(tmp);
			final MessageDigest md5 = getMessageDigest("MD5");
			OutputStream out = new FilterOutputStream(fout) {
				private IOException fatal;

				public void write(int b) throws IOException {
					try {
						fout.write(b);
						md5.update((byte) b);
					} catch (IOException e) {
						fatal = e;
					}
				}

				public void write(byte[] b, int off, int len)
						throws IOException {
					try {
						fout.write(b, off, len);
						md5.update(b, off, len);
					} catch (IOException e) {
						fatal = e;
					}
				}

				public void close() throws IOException {
					fout.close();
					if (fatal != null)
						throw fatal;
					byte[] b = Base64.encodeBase64(md5.digest());
					setContentMD5(new String(b, "UTF-8"));
					setContentEncoding(encoding);
					if (pending != null) {
						pending.delete();
					}
					deleted = false;
					pending = tmp;
				}
			};
			if ("gzip".equals(encoding))
				return new GZIPOutputStream(out);
			return out;
		}
	}

	public Writer openWriter() throws IOException {
		Charset charset = charset();
		if (charset == null)
			return new OutputStreamWriter(openOutputStream());
		return new OutputStreamWriter(openOutputStream(), charset);
	}

	public boolean delete() {
		if (readOnly)
			throw new IllegalStateException(
					"Cannot modify entity within a safe methods");
		if (file == null) {
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
			setContentMD5(null);
			setContentEncoding(null);
			setRevision(null);
			if (file.canWrite() && file.getParentFile().canWrite()) {
				if (pending != null) {
					pending.delete();
					pending = null;
				}
				deleted = true;
				return true;
			} else {
				return false;
			}
		}
	}

	public void commitFileSystemChanges() throws IOException {
		try {
			if (pending != null) {
				if (file.exists()) {
					file.delete();
				}
				if (!pending.renameTo(file))
					throw new IOException("Could not save file");
			} else if (deleted) {
				if (file.exists() && !file.delete())
					throw new IOException("Could not delete file");
			}
		} finally {
			pending = null;
			deleted = false;
		}
	}

	public void rollbackFileSystemChanges() {
		deleted = false;
		if (pending != null) {
			pending.delete();
			pending = null;
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

	private MessageDigest getMessageDigest(String algorithm) {
		try {
			return MessageDigest.getInstance(algorithm);
		} catch (NoSuchAlgorithmException e) {
			return null;
		}
	}

	private String getDesiredEncoding(String type) {
		if (type == null)
			return "identity";
		if (type.startsWith("text/") || type.startsWith("application/xml")
				|| type.startsWith("application/x-turtle")
				|| type.startsWith("application/trix")
				|| type.startsWith("application/x-trig")
				|| type.startsWith("application/postscript")
				|| type.startsWith("application/")
				&& (type.endsWith("+xml") || type.contains("+xml;")))
			return "gzip";
		return "identity";
	}

	private String getMimeType(File file) {
		Collection types = MimeUtil.getMimeTypes(file);
		MimeType mimeType = null;
		double specificity = 0;
		for (Iterator it = types.iterator(); it.hasNext();) {
			MimeType mt = (MimeType) it.next();
			int spec = mt.getSpecificity() * 2;
			if (!mt.getSubType().startsWith("x-")) {
				spec += 1;
			}
			if (spec > specificity) {
				mimeType = mt;
				specificity = spec;
			}
		}
		if (mimeType == null)
			return "application/octet-stream";
		return mimeType.toString();
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
