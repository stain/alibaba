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
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpMessage;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.RequestLine;
import org.apache.http.StatusLine;
import org.apache.http.message.BasicHeader;
import org.openrdf.OpenRDFException;
import org.openrdf.http.object.util.CatInputStream;
import org.openrdf.repository.object.ObjectFactory;

public class HttpMessageWriter implements MessageBodyWriter<HttpMessage> {

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
			ObjectFactory of, HttpMessage result, Charset charset) {
		return -1;
	}

	public boolean isWriteable(String mimeType, Class<?> type,
			Type genericType, ObjectFactory of) {
		if (Object.class.equals(type) && mimeType != null)
			return mimeType.startsWith("message/http");
		return HttpResponse.class.equals(type)
				|| HttpMessage.class.equals(type)
				|| HttpRequest.class.equals(type)
				|| HttpEntityEnclosingRequest.class.equals(type);
	}

	public InputStream write(String mimeType, Class<?> ctype, Type genericType,
			ObjectFactory of, HttpMessage msg, String base, Charset charset)
			throws IOException, OpenRDFException, XMLStreamException,
			TransformerException, ParserConfigurationException {
		CatInputStream cat = new CatInputStream();
		if (msg instanceof HttpResponse) {
			print(cat, ((HttpResponse) msg).getStatusLine());
		} else if (msg instanceof HttpRequest) {
			print(cat, ((HttpRequest) msg).getRequestLine());
		}
		for (Header hd : msg.getAllHeaders()) {
			print(cat, hd);
		}
		HttpEntity entity = getEntity(msg);
		if (entity == null) {
			cat.println();
		} else {
			Header type = entity.getContentType();
			if (!msg.containsHeader("Content-Type") && type != null) {
				print(cat, type);
			}
			Header encoding = entity.getContentEncoding();
			if (!msg.containsHeader("Content-Encoding") && encoding != null) {
				print(cat, encoding);
			}
			long length = entity.getContentLength();
			if (!msg.containsHeader("Content-Length") && length >= 0) {
				print(cat, length);
			}
			cat.println();
			if (msg.containsHeader("Content-Length") || length >= 0) {
				cat.append(entity.getContent());
			} else if (msg.containsHeader("Transfer-Encoding")) {
				cat.append(entity.getContent());
			} else {
				print(cat, new BasicHeader("Transfer-Encoding", "identity"));
				cat.append(entity.getContent());
			}
		}
		return cat;
	}

	private HttpEntity getEntity(HttpMessage msg) {
		if (msg instanceof HttpResponse) {
			return ((HttpResponse) msg).getEntity();
		} else if (msg instanceof HttpEntityEnclosingRequest) {
			return ((HttpEntityEnclosingRequest) msg).getEntity();
		} else {
			return null;
		}
	}

	private void print(CatInputStream cat, RequestLine line) throws IOException {
		cat.print(line.getMethod());
		cat.print(" ");
		cat.print(line.getUri());
		cat.print(" ");
		print(cat, line.getProtocolVersion());
	}

	private void print(CatInputStream cat, StatusLine status)
			throws IOException {
		print(cat, status.getProtocolVersion());
		cat.print(" ");
		cat.print(Integer.toString(status.getStatusCode()));
		cat.print(" ");
		cat.println(status.getReasonPhrase());
	}

	private void print(CatInputStream cat, ProtocolVersion ver)
			throws IOException {
		cat.print(ver.getProtocol());
		cat.print("/");
		cat.print(Integer.toString(ver.getMajor()));
		cat.print(".");
		cat.print(Integer.toString(ver.getMinor()));
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
