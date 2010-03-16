package org.openrdf.http.object.handlers;

import java.io.IOException;
import java.io.InputStream;

import javax.activation.MimeTypeParseException;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.openrdf.http.object.model.Handler;
import org.openrdf.http.object.model.ResourceOperation;
import org.openrdf.http.object.model.Response;
import org.openrdf.http.object.model.ResponseEntity;
import org.openrdf.http.object.util.Accepter;

public class HttpResponseHandler implements Handler {
	private static final String[] EMPTY = new String[0];
	private Handler delegate;
	private Accepter envelopeType;
	private String core;

	public HttpResponseHandler(Handler delegate) {
		this.delegate = delegate;
	}

	public Response verify(ResourceOperation request) throws Exception {
		return delegate.verify(request);
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

	public Response handle(ResourceOperation request) throws Exception {
		if (envelopeType == null)
			return delegate.handle(request);
		Header accept = request.getFirstHeader("Accept");
		if (accept != null) {
			String value = accept.getValue() + "," + envelopeType + ";q=0.1";
			request.setHeader("Accept", value);
		}
		Response response = delegate.handle(request);
		String type = response.getHeader("Content-Type");
		if (type != null && type.startsWith(core)) {
			if (HttpResponse.class.equals(response.getEntityType())
					&& envelopeType.isAcceptable(type)) {
				return unwrap(request, response);
			}
		}
		return response;
	}

	private Response unwrap(ResourceOperation request, Response resp)
			throws IOException {
		Object entity = resp.getResponseEntity().getEntity();
		if (entity instanceof HttpResponse) {
			HttpResponse http = (HttpResponse) entity;
			resp.removeHeaders("Content-Type");
			resp.removeHeaders("Content-Length");
			resp.removeHeaders("Transfer-Encoding");
			for (Header hd : http.getAllHeaders()) {
				resp.removeHeaders(hd.getName());
			}
			StatusLine status = http.getStatusLine();
			resp.status(status.getStatusCode(), status.getReasonPhrase());
			for (Header hd : http.getAllHeaders()) {
				resp.addHeader(hd);
			}
			String base = resp.getHeader("Content-Location");
			if (base == null) {
				base = request.getURI();
			}
			HttpEntity e = http.getEntity();
			if (e == null) {
				resp.entity(null);
			} else {
				if (e.getContentEncoding() != null) {
					resp.setHeader(e.getContentEncoding());
				}
				if (e.getContentType() != null) {
					resp.setHeader(e.getContentType());
				}
				if (e.getContentLength() >= 0) {
					String length = Long.toString(e.getContentLength());
					resp.setHeader("Content-Length", length);
				}
				InputStream in = e.getContent();
				Class<InputStream> et = InputStream.class;
				resp.entity(new ResponseEntity(EMPTY, in, et, et, base, null));
			}
		}
		return resp;
	}

}
