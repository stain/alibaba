package org.openrdf.server.metadata.writers;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.nio.charset.Charset;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;

import org.openrdf.OpenRDFException;
import org.openrdf.model.URI;
import org.openrdf.repository.object.ObjectFactory;

public class URIWriter implements MessageBodyWriter<URI> {
	private StringBodyWriter delegate = new StringBodyWriter();

	public String getContentType(String mimeType, Class<?> type,
			Type genericType, ObjectFactory of, Charset charset) {
		if (mimeType == null || mimeType.startsWith("*") || mimeType.startsWith("text/*")) {
			mimeType = "text/uri-list";
		}
		Class<String> t = String.class;
		return delegate.getContentType(mimeType, t, t, of, charset);
	}

	public long getSize(String mimeType, Class<?> type, Type genericType,
			ObjectFactory of, URI result, Charset charset) {
		if (mimeType == null || mimeType.startsWith("*") || mimeType.startsWith("text/*")) {
			mimeType = "text/uri-list";
		}
		Class<String> t = String.class;
		return delegate.getSize(mimeType, t, t, of, result.stringValue(), charset);
	}

	public boolean isWriteable(String mimeType, Class<?> type,
			Type genericType, ObjectFactory of) {
		Class<String> t = String.class;
		return URI.class.equals(type) && delegate.isWriteable(mimeType, t, t, of);
	}

	public void writeTo(String mimeType, Class<?> type, Type genericType,
			ObjectFactory of, URI result, String base, Charset charset,
			OutputStream out, int bufSize) throws IOException,
			OpenRDFException, XMLStreamException, TransformerException,
			ParserConfigurationException {
		if (mimeType == null || mimeType.startsWith("*") || mimeType.startsWith("text/*")) {
			mimeType = "text/uri-list";
		}
		Class<String> t = String.class;
		delegate.writeTo(mimeType, t, t, of, result.stringValue(), base, charset, out, bufSize);
	}

}
