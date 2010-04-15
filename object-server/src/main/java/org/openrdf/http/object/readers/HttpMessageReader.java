package org.openrdf.http.object.readers;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;

import org.apache.http.Header;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpMessage;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.RequestLine;
import org.apache.http.StatusLine;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.io.AbstractSessionInputBuffer;
import org.apache.http.impl.io.ChunkedInputStream;
import org.apache.http.impl.io.ContentLengthInputStream;
import org.apache.http.io.SessionInputBuffer;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicLineParser;
import org.apache.http.message.LineParser;
import org.apache.http.params.BasicHttpParams;
import org.openrdf.http.object.util.ChannelUtil;
import org.openrdf.repository.object.ObjectConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpMessageReader implements MessageBodyReader<HttpMessage> {
	private Logger logger = LoggerFactory.getLogger(HttpMessageReader.class);

	public boolean isReadable(Class<?> type, Type genericType, String mimeType,
			ObjectConnection con) {
		if (Object.class.equals(type) && mimeType != null)
			return mimeType.startsWith("message/http");
		return HttpResponse.class.equals(type)
				|| HttpMessage.class.equals(type)
				|| HttpRequest.class.equals(type)
				|| HttpEntityEnclosingRequest.class.equals(type);
	}

	public HttpMessage readFrom(Class<?> ctype, Type genericType,
			String mimeType, ReadableByteChannel in, Charset charset,
			String base, String location, ObjectConnection con)
			throws IOException {
		return readFrom(mimeType, in);
	}

	public HttpMessage readFrom(String mimeType, ReadableByteChannel in)
			throws IOException {
		if (in == null)
			return null;
		LineParser parser = getParser(mimeType);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		final BufferedInputStream bin = new BufferedInputStream(ChannelUtil
				.newInputStream(in));
		String line = readLine(bin, out);
		HttpMessage msg;
		if (line.startsWith("HTTP/")) {
			StatusLine status = BasicLineParser.parseStatusLine(line, parser);
			parser = new BasicLineParser(status.getProtocolVersion());
			msg = new BasicHttpResponse(status);
		} else {
			RequestLine req = BasicLineParser.parseRequestLine(line, parser);
			parser = new BasicLineParser(req.getProtocolVersion());
			msg = new BasicHttpEntityEnclosingRequest(req);
		}
		line = readLine(bin, out);
		for (; line.length() > 2; line = readLine(bin, out)) {
			Header hd = BasicLineParser.parseHeader(line, parser);
			msg.addHeader(hd);
		}
		Header length = msg.getFirstHeader("Content-Length");
		Header encoding = msg.getFirstHeader("Transfer-Encoding");
		if (encoding == null && length == null) {
			bin.close();
			if (msg instanceof BasicHttpEntityEnclosingRequest) {
				BasicHttpEntityEnclosingRequest req = (BasicHttpEntityEnclosingRequest) msg;
				msg = new BasicHttpRequest(req.getRequestLine());
				msg.setHeaders(req.getAllHeaders());
			}
			return msg;
		}
		BasicHttpEntity entity = new BasicHttpEntity() {
			public void writeTo(OutputStream outstream) throws IOException {
				try {
					super.writeTo(outstream);
				} finally {
					consumeContent();
				}
			}};
		if (encoding != null && "chunked".equals(encoding)) {
			entity.setChunked(true);
			entity.setContentLength(-1);
			entity.setContent(new ChunkedInputStream(createBuffer(bin)) {
				public void close() throws IOException {
					super.close();
					bin.close();
				}
			});
		} else if (encoding != null) { // identity
			entity.setChunked(false);
			entity.setContentLength(-1);
			entity.setContent(bin);
		} else if (length != null) {
			long len = Long.parseLong(length.getValue());
			entity.setChunked(false);
			entity.setContentLength(len);
			entity.setContent(new ContentLengthInputStream(createBuffer(bin), len) {
				public void close() throws IOException {
					super.close();
					bin.close();
				}
			});
		}
		Header contentTypeHeader = msg.getFirstHeader("Content-Type");
		if (contentTypeHeader != null) {
			entity.setContentType(contentTypeHeader);
		}
		Header contentEncodingHeader = msg.getFirstHeader("Content-Encoding");
		if (contentEncodingHeader != null) {
			entity.setContentEncoding(contentEncodingHeader);
		}
		if (msg instanceof BasicHttpEntityEnclosingRequest) {
			((BasicHttpEntityEnclosingRequest) msg).setEntity(entity);
		} else if (msg instanceof HttpResponse) {
			((HttpResponse) msg).setEntity(entity);
		}
		return msg;
	}

	private SessionInputBuffer createBuffer(final BufferedInputStream bin) {
		return new AbstractSessionInputBuffer() {
			{
				init(bin, 1024, new BasicHttpParams());
			}

			public boolean isDataAvailable(int timeout) throws IOException {
				return bin.available() > 0;
			}
		};
	}

	private String readLine(BufferedInputStream bin, ByteArrayOutputStream out)
			throws IOException {
		int read;
		do {
			read = bin.read();
			if (read < 0)
				break;
			out.write(read);
		} while ('\n' != read);
		String line = new String(out.toByteArray(), Charset
				.forName("ISO-8859-1"));
		out.reset();
		return line.trim();
	}

	private LineParser getParser(String mimeType) {
		try {
			BasicLineParser parser = null;
			if (mimeType == null)
				return new BasicLineParser();
			if (!mimeType.startsWith("message/http")
					&& !mimeType.startsWith("application/http"))
				return new BasicLineParser();
			if (!mimeType.contains("version"))
				return new BasicLineParser();
			MimeType m = new MimeType(mimeType);
			String version = m.getParameter("version");
			if (version == null)
				return new BasicLineParser();
			int idx = version.indexOf('.');
			if (idx < 0)
				return new BasicLineParser();
			int major = Integer.parseInt(version.substring(0, idx));
			int minor = Integer.parseInt(version.substring(idx + 1));
			ProtocolVersion ver = new ProtocolVersion("HTTP", major, minor);
			parser = new BasicLineParser(ver);
			if (parser == null)
				return new BasicLineParser();
			return parser;
		} catch (MimeTypeParseException e) {
			logger.debug(e.toString(), e);
			return new BasicLineParser();
		} catch (NumberFormatException e) {
			logger.debug(e.toString(), e);
			return new BasicLineParser();
		}
	}
}
