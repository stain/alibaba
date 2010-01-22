package org.openrdf.http.object.writers;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.Charset;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;

import org.openrdf.OpenRDFException;
import org.openrdf.repository.object.ObjectFactory;

public class FormStringMessageWriter implements MessageBodyWriter<String> {

	public boolean isWriteable(String mimeType, Class<?> type,
			Type genericType, ObjectFactory of) {
		if (!String.class.equals(type))
			return false;
		return mimeType == null || mimeType.startsWith("*")
		|| mimeType.startsWith("application/*")
		|| mimeType.startsWith("application/x-www-form-urlencoded");
	}

	public String getContentType(String mimeType, Class<?> type,
			Type genericType, ObjectFactory of, Charset charset) {
		return "application/x-www-form-urlencoded";
	}

	public long getSize(String mimeType, Class<?> type, Type genericType,
			ObjectFactory of, String str, Charset charset) {
		if (charset == null)
			return str.length(); // ISO-8859-1
		return charset.encode(str).limit();
	}

	public void writeTo(String mimeType, Class<?> type, Type genericType,
			ObjectFactory of, String result, String base, Charset charset,
			OutputStream out, int bufSize) throws IOException,
			OpenRDFException, XMLStreamException, TransformerException,
			ParserConfigurationException {
		if (charset == null) {
			charset = Charset.forName("ISO-8859-1");
		}
		Writer writer = new OutputStreamWriter(out, charset);
		writer.write(result);
		writer.flush();
	}

}
