package org.openrdf.server.metadata.writers.base;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;

import org.openrdf.OpenRDFException;
import org.openrdf.repository.object.ObjectFactory;
import org.openrdf.server.metadata.writers.MessageBodyWriter;
import org.openrdf.server.metadata.writers.StringBodyWriter;

public class URIListWriter<URI> implements MessageBodyWriter<URI> {
	private static final Charset UTF8 = Charset.forName("UTF-8");
	private StringBodyWriter delegate = new StringBodyWriter();
	private Class<URI> componentType;

	public URIListWriter(Class<URI> componentType) {
		this.componentType = componentType;
	}

	public boolean isWriteable(String mimeType, Class<?> type,
			Type genericType, ObjectFactory of) {
		Class<String> t = String.class;
		if (Set.class.equals(type)) {
			if (genericType instanceof ParameterizedType) {
				ParameterizedType ptype = (ParameterizedType) genericType;
				Type ctype = ptype.getActualTypeArguments()[0];
				if (ctype instanceof Class && !componentType.equals(ctype))
					return false;
			}
		} else if (!componentType.equals(type)) {
			return false;
		}
		return delegate.isWriteable(mimeType, t, t, of);
	}

	public String getContentType(String mimeType, Class<?> type,
			Type genericType, ObjectFactory of, Charset charset) {
		if (mimeType == null || mimeType.startsWith("*")
				|| mimeType.startsWith("text/*")) {
			mimeType = "text/uri-list";
		}
		Class<String> t = String.class;
		return delegate.getContentType(mimeType, t, t, of, charset);
	}

	public long getSize(String mimeType, Class<?> type, Type genericType,
			ObjectFactory of, URI result, Charset charset) {
		if (result == null)
			return 0;
		if (Set.class.equals(type))
			return -1;
		if (mimeType == null || mimeType.startsWith("*")
				|| mimeType.startsWith("text/*")) {
			mimeType = "text/uri-list";
		}
		Class<String> t = String.class;
		return delegate.getSize(mimeType, t, t, of, toString(result), charset);
	}

	public void writeTo(String mimeType, Class<?> type, Type genericType,
			ObjectFactory of, URI result, String base, Charset charset,
			OutputStream out, int bufSize) throws IOException,
			OpenRDFException, XMLStreamException, TransformerException,
			ParserConfigurationException {
		if (result == null)
			return;
		if (mimeType == null || mimeType.startsWith("*")
				|| mimeType.startsWith("text/*")) {
			mimeType = "text/uri-list";
		}
		Class<String> t = String.class;
		if (Set.class.equals(type)) {
			if (charset == null) {
				charset = UTF8;
			}
			Writer writer = new OutputStreamWriter(out, charset);
			for (URI uri : ((Set<URI>) result)) {
				writer.write(uri.toString());
				writer.write("\r\n");
			}
			writer.flush();
		} else {
			delegate.writeTo(mimeType, t, t, of, toString(result), base,
					charset, out, bufSize);
		}
	}

	protected String toString(URI result) {
		return result.toString();
	}

}
