package org.openrdf.http.object.filters;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.Arrays;
import java.util.List;

import javax.activation.MimeTypeParseException;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.openrdf.http.object.model.Filter;
import org.openrdf.http.object.model.Request;
import org.openrdf.http.object.readers.HttpMessageReader;
import org.openrdf.http.object.util.Accepter;
import org.openrdf.http.object.util.ChannelUtil;

public class HttpResponseFilter extends Filter {
	private static final List<String> CONTENT_HD = Arrays.asList(
			"Content-Type", "Content-Length", "Transfer-Encoding");
	private static final HttpMessageReader reader = new HttpMessageReader();
	private Accepter envelopeType;
	private String core;

	public HttpResponseFilter(Filter delegate) {
		super(delegate);
	}

	public String getEnvelopeType() {
		if (envelopeType == null)
			return null;
		return envelopeType.toString();
	}

	public void setEnvelopeType(String type) throws MimeTypeParseException {
		if (type == null) {
			envelopeType = null;
			core = null;
		} else {
			envelopeType = new Accepter(type);
			core = type;
			if (core.contains(";")) {
				core = core.substring(0, core.indexOf(';'));
			}
		}
	}

	@Override
	public Request filter(Request request) throws IOException {
		if (envelopeType != null && request.containsHeader("Accept")) {
			Header accept = request.getFirstHeader("Accept");
			String value = accept.getValue() + "," + envelopeType + ";q=0.1";
			request.setHeader("Accept", value);
		}
		return super.filter(request);
	}

	@Override
	public HttpResponse filter(Request request, HttpResponse response)
			throws IOException {
		response = super.filter(request, response);
		if (envelopeType == null)
			return response;
		Header type = response.getFirstHeader("Content-Type");
		if (type != null && type.getValue().startsWith(core)) {
			try {
				if (envelopeType.isAcceptable(type.getValue())) {
					return unwrap(request, type.getValue(), response);
				}
			} catch (MimeTypeParseException e) {
				return response;
			} catch (IOException e) {
				return response;
			}
		}
		return response;
	}

	private HttpResponse unwrap(Request request, String type, HttpResponse resp)
			throws IOException {
		final HttpEntity entity = resp.getEntity();
		if (entity == null)
			return resp;
		InputStream in = entity.getContent();
		final ReadableByteChannel cin = ChannelUtil.newChannel(in);
		ReadableByteChannel ch = new ReadableByteChannel() {
			public boolean isOpen() {
				return cin.isOpen();
			}
			public void close() throws IOException {
				entity.consumeContent();
			}
			public int read(ByteBuffer dst) throws IOException {
				return cin.read(dst);
			}
		};
		HttpResponse response = (HttpResponse) reader.readFrom(type, ch);
		for (Header hd : resp.getAllHeaders()) {
			String name = hd.getName();
			if (!CONTENT_HD.contains(name) && !response.containsHeader(name)) {
				response.addHeader(hd);
			}
		}
		HttpEntity body = response.getEntity();
		if (body == null && !response.containsHeader("Content-Length")
				 && !response.containsHeader("Transfer-Encoding")) {
			response.setHeader("Content-Length", "0");
		}
		return response;
	}

}
