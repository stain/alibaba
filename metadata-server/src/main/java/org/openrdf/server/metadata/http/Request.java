package org.openrdf.server.metadata.http;

import info.aduna.net.ParsedURI;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MediaType;

import org.openrdf.OpenRDFException;
import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectFactory;
import org.openrdf.server.metadata.http.readers.MessageBodyReader;
import org.openrdf.server.metadata.http.writers.MessageBodyWriter;

import com.sun.jersey.core.header.reader.HttpHeaderReader;

public class Request {
	private static final List<String> HTTP_METHODS = Arrays.asList("OPTIONS",
			"GET", "HEAD", "PUT", "DELETE");
	private MessageBodyReader reader;
	private MessageBodyWriter writer;
	private HttpServletRequest request;
	private File dataDir;
	private URI uri;
	private ObjectConnection con;
	protected ValueFactory vf;
	protected ObjectFactory of;

	public Request(MessageBodyReader reader, MessageBodyWriter writer,
			HttpServletRequest request, File dataDir, URI uri,
			ObjectConnection con) {
		this.reader = reader;
		this.writer = writer;
		this.request = request;
		this.dataDir = dataDir;
		this.uri = uri;
		this.con = con;
		this.vf = con.getValueFactory();
		this.of = con.getObjectFactory();
	}

	public String getHeader(String name) {
		return request.getHeader(name);
	}

	public String getOperation() {
		String method = request.getMethod();
		if (!HTTP_METHODS.contains(method))
			return method;
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

	public boolean isAcceptable(String mediaType) throws ParseException {
		return isAcceptable(null, mediaType);
	}

	public boolean isAcceptable(Class<?> type) throws ParseException {
		return isAcceptable(type, null);
	}

	public boolean isAcceptable(Class<?> type, String mediaType)
			throws ParseException {
		StringBuilder sb = new StringBuilder();
		Enumeration headers = request.getHeaders("Accept");
		while (headers.hasMoreElements()) {
			if (sb.length() > 0) {
				sb.append(", ");
			}
			sb.append((String) headers.nextElement());
		}
		MediaType media = mediaType == null ? null : MediaType
				.valueOf(mediaType);
		List<? extends MediaType> acceptable = HttpHeaderReader
				.readAcceptMediaType(sb.toString());
		for (MediaType m : acceptable) {
			if (media != null && !media.isCompatible(m))
				continue;
			if (type != null
					&& !writer.isWriteable(type, m.getType() + "/"
							+ m.getSubtype()))
				continue;
			return true;
		}
		return false;
	}

	public boolean isReadable(Class<?> class1, Type type) {
		if (request.getHeader("Content-Length") == null
				&& request.getHeader("Transfer-Encoding") == null
				&& request.getHeader("Content-Location") == null)
			return true;
		String contentType = request.getContentType();
		MediaType media = contentType == null ? null : MediaType
				.valueOf(contentType);
		String mime = media == null ? null : media.getType() + "/"
				+ media.getSubtype();
		return media == null || reader.isReadable(class1, type, mime, con);
	}

	public Object getBody(Class<?> class1, Type type) throws IOException {
		if (request.getHeader("Content-Length") == null
				&& request.getHeader("Transfer-Encoding") == null
				&& request.getHeader("Content-Location") == null)
			return null;
		String contentType = request.getContentType();
		MediaType media = contentType == null ? null : MediaType
				.valueOf(contentType);
		String mime = media == null ? null : media.getType() + "/"
				+ media.getSubtype();
		if (media == null && !reader.isReadable(class1, type, mime, con)) {
			return null;
		}
		String location = request.getHeader("Content-Location");
		if (location != null) {
			location = createURI(location).stringValue();
		}
		Charset charset = getCharset(media, null);
		try {
			return reader.readFrom(class1, type, mime,
					request.getInputStream(), charset, uri.stringValue(),
					location, con);
		} catch (OpenRDFException e) {
			throw new IOException(e);
		}
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

	private URI createURI(String uriSpec) {
		ParsedURI base = new ParsedURI(uri.stringValue());
		base.normalize();
		ParsedURI uri = new ParsedURI(uriSpec);
		return vf.createURI(base.resolve(uri).toString());
	}

	public Map getParameterMap() {
		return request.getParameterMap();
	}

	public InputStream getInputStream() throws IOException {
		return request.getInputStream();
	}

	private Charset getCharset(MediaType m, Charset defCharset) {
		if (m == null)
			return defCharset;
		String name = m.getParameters().get("charset");
		if (name == null)
			return defCharset;
		return Charset.forName(name);
	}

}
