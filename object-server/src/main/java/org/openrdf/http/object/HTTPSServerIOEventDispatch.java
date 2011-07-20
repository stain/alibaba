/**
 * 
 */
package org.openrdf.http.object;

import java.io.IOException;
import java.security.cert.Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestFactory;
import org.apache.http.MethodNotSupportedException;
import org.apache.http.RequestLine;
import org.apache.http.impl.nio.DefaultNHttpServerConnection;
import org.apache.http.impl.nio.codecs.DefaultHttpRequestParser;
import org.apache.http.impl.nio.reactor.SSLSetupHandler;
import org.apache.http.impl.nio.ssl.SSLServerIOEventDispatch;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.nio.NHttpMessageParser;
import org.apache.http.nio.NHttpServerIOTarget;
import org.apache.http.nio.NHttpServiceHandler;
import org.apache.http.nio.reactor.IOSession;
import org.apache.http.nio.reactor.SessionInputBuffer;
import org.apache.http.params.HttpParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HTTPSServerIOEventDispatch extends SSLServerIOEventDispatch {
	private static final String SCHEME = "http.protocol.scheme";
	private static final String CERTIFICATE = "java.security.cert.Certificate";
	private final HttpParams params;

	public HTTPSServerIOEventDispatch(NHttpServiceHandler handler,
			SSLContext sslcontext, HttpParams params) {
		super(handler, sslcontext, new SSLSetupHandler() {
			private final Logger logger = LoggerFactory
					.getLogger(HTTPSServerIOEventDispatch.class);

			public void initalize(SSLEngine sslengine, HttpParams params) {
			}

			public void verify(IOSession iosession, SSLSession sslsession) {
				try {
					Certificate[] certs = sslsession.getPeerCertificates();
					iosession.setAttribute(CERTIFICATE, certs);
				} catch (SSLPeerUnverifiedException e) {
					logger.trace(e.toString(), e);
				}
			}
		}, params);
		this.params = params;
	}

	@Override
	protected NHttpServerIOTarget createConnection(IOSession session) {
		return new DefaultNHttpServerConnection(session,
				createHttpRequestFactory(), createByteBufferAllocator(),
				this.params) {
			@Override
			protected NHttpMessageParser<HttpRequest> createRequestParser(
					SessionInputBuffer buffer,
					HttpRequestFactory requestFactory, HttpParams params) {
				return new DefaultHttpRequestParser(buffer, null,
						requestFactory, params) {
					@Override
					public HttpRequest parse() throws IOException,
							HttpException {
						return initializeRequest(super.parse(), session);
					}
				};
			}

			@Override
			public String toString() {
				return super.toString() + session.toString();
			}
		};
	}

	@Override
	protected HttpRequestFactory createHttpRequestFactory() {
		return new HttpRequestFactory() {
			public HttpRequest newHttpRequest(RequestLine requestline)
					throws MethodNotSupportedException {
				return new BasicHttpEntityEnclosingRequest(requestline);
			}

			public HttpRequest newHttpRequest(String method, String uri)
					throws MethodNotSupportedException {
				return new BasicHttpEntityEnclosingRequest(method, uri);
			};
		};
	}

	private HttpRequest initializeRequest(HttpRequest msg, IOSession session) {
		if (msg != null) {
			HttpParams params = msg.getParams();
			params.setParameter(SCHEME, "https");
			Object certs = session.getAttribute(CERTIFICATE);
			if (certs != null) {
				params.setParameter(CERTIFICATE, certs);
			}
			msg.setParams(params);
		}
		if (msg instanceof HttpEntityEnclosingRequest
				&& !msg.containsHeader("Content-Length")
				&& !msg.containsHeader("Transfer-Encoding")) {
			HttpEntityEnclosingRequest body = (HttpEntityEnclosingRequest) msg;
			BasicHttpRequest req = new BasicHttpRequest(body.getRequestLine());
			req.setHeaders(body.getAllHeaders());
			req.setParams(msg.getParams());
			return req;
		}
		return msg;
	}
}