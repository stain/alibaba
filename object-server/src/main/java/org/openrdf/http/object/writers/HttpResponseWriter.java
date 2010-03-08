package org.openrdf.http.object.writers;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.Charset;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.openrdf.OpenRDFException;
import org.openrdf.http.object.util.CatInputStream;
import org.openrdf.repository.object.ObjectFactory;

public class HttpResponseWriter implements MessageBodyWriter<HttpResponse> {

	public String getContentType(String mimeType, Class<?> type,
			Type genericType, ObjectFactory of, Charset charset) {
		if (mimeType == null || mimeType.startsWith("*")
				|| mimeType.startsWith("message/*"))
			return "message/http";
		if (mimeType.startsWith("application/*"))
			return "application/http";
		return mimeType;
	}

	public long getSize(String mimeType, Class<?> type, Type genericType,
			ObjectFactory of, HttpResponse result, Charset charset) {
		return -1;
	}

	public boolean isWriteable(String mimeType, Class<?> type,
			Type genericType, ObjectFactory of) {
		return HttpResponse.class.equals(type);
	}

	public InputStream write(String mimeType, Class<?> ctype, Type genericType,
			ObjectFactory of, HttpResponse resp, String base, Charset charset)
			throws IOException, OpenRDFException, XMLStreamException,
			TransformerException, ParserConfigurationException {
		CatInputStream cat = new CatInputStream();
		print(cat, resp.getStatusLine());
		for (Header hd : resp.getAllHeaders()) {
			print(cat, hd);
		}
		HttpEntity entity = resp.getEntity();
		Header type = entity.getContentType();
		if (!resp.containsHeader("Content-Type") && type != null) {
			print(cat, type);
		}
		Header encoding = entity.getContentEncoding();
		if (!resp.containsHeader("Content-Encoding") && encoding != null) {
			print(cat, encoding);
		}
		long length = entity.getContentLength();
		if (!resp.containsHeader("Content-Length") && length >= 0) {
			print(cat, length);
		}
		cat.println();
		cat.append(entity.getContent());
		return cat;
	}

	private void print(CatInputStream cat, StatusLine status)
			throws IOException {
		ProtocolVersion ver = status.getProtocolVersion();
		cat.print(ver.getProtocol());
		cat.print("/");
		cat.print(Integer.toString(ver.getMajor()));
		cat.print(".");
		cat.print(Integer.toString(ver.getMinor()));
		cat.print(" ");
		cat.print(Integer.toString(status.getStatusCode()));
		cat.print(" ");
		cat.println(status.getReasonPhrase());
	}

	private void print(CatInputStream cat, Header hd) throws IOException {
		cat.print(hd.getName());
		cat.print(": ");
		cat.println(hd.getValue());
	}

	private void print(CatInputStream cat, long length) throws IOException {
		cat.print("Content-Length: ");
		cat.println(Long.toString(length));
	}

}
