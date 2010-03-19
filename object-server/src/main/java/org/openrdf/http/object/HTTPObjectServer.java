/*
 * Copyright 2009-2010, James Leigh and Zepheira LLC Some rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution. 
 * - Neither the name of the openrdf.org nor the names of its contributors may
 *   be used to endorse or promote products derived from this software without
 *   specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 */
package org.openrdf.http.object;

import static org.apache.http.params.CoreConnectionPNames.SOCKET_BUFFER_SIZE;
import static org.apache.http.params.CoreConnectionPNames.SO_TIMEOUT;
import static org.apache.http.params.CoreConnectionPNames.STALE_CONNECTION_CHECK;
import static org.apache.http.params.CoreConnectionPNames.TCP_NODELAY;
import info.aduna.io.MavenUtil;

import java.io.File;
import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.activation.MimeTypeParseException;

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpMessage;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestFactory;
import org.apache.http.MethodNotSupportedException;
import org.apache.http.RequestLine;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.nio.DefaultNHttpServerConnection;
import org.apache.http.impl.nio.DefaultServerIOEventDispatch;
import org.apache.http.impl.nio.codecs.HttpRequestParser;
import org.apache.http.impl.nio.reactor.DefaultListeningIOReactor;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.nio.NHttpMessageParser;
import org.apache.http.nio.NHttpServerIOTarget;
import org.apache.http.nio.protocol.AsyncNHttpServiceHandler;
import org.apache.http.nio.protocol.NHttpRequestHandler;
import org.apache.http.nio.protocol.NHttpRequestHandlerResolver;
import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.nio.reactor.IOReactorStatus;
import org.apache.http.nio.reactor.IOSession;
import org.apache.http.nio.reactor.ListeningIOReactor;
import org.apache.http.nio.reactor.SessionInputBuffer;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpProcessor;
import org.openrdf.http.object.cache.CachingFilter;
import org.openrdf.http.object.filters.GUnzipFilter;
import org.openrdf.http.object.filters.GZipFilter;
import org.openrdf.http.object.filters.IndentityPathFilter;
import org.openrdf.http.object.filters.KeepAliveFilter;
import org.openrdf.http.object.filters.MD5ValidationFilter;
import org.openrdf.http.object.filters.ServerNameFilter;
import org.openrdf.http.object.filters.TraceFilter;
import org.openrdf.http.object.handlers.AlternativeHandler;
import org.openrdf.http.object.handlers.AuthenticationHandler;
import org.openrdf.http.object.handlers.ContentHeadersHandler;
import org.openrdf.http.object.handlers.DateHandler;
import org.openrdf.http.object.handlers.HttpResponseHandler;
import org.openrdf.http.object.handlers.InvokeHandler;
import org.openrdf.http.object.handlers.LinksHandler;
import org.openrdf.http.object.handlers.MethodNotAllowedHandler;
import org.openrdf.http.object.handlers.ModifiedSinceHandler;
import org.openrdf.http.object.handlers.NotFoundHandler;
import org.openrdf.http.object.handlers.OptionsHandler;
import org.openrdf.http.object.handlers.ResponseExceptionHandler;
import org.openrdf.http.object.handlers.UnmodifiedSinceHandler;
import org.openrdf.http.object.model.Filter;
import org.openrdf.http.object.model.Handler;
import org.openrdf.http.object.util.NamedThreadFactory;
import org.openrdf.repository.Repository;
import org.openrdf.repository.object.ObjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the start and stop stages of the server.
 * 
 * @author James Leigh
 * 
 */
public class HTTPObjectServer {
	private static final String VERSION = MavenUtil.loadVersion(
			"org.openrdf.alibaba", "alibaba-server-object", "devel");
	private static final String APP_NAME = "OpenRDF AliBaba object-server";
	protected static final String DEFAULT_NAME = APP_NAME + "/" + VERSION;
	private static Executor executor = Executors
			.newCachedThreadPool(new NamedThreadFactory("HTTP Object Server"));

	private Logger logger = LoggerFactory.getLogger(HTTPObjectServer.class);
	private ListeningIOReactor server;
	private IOEventDispatch dispatch;
	private ObjectRepository repository;
	private int port;
	private ServerNameFilter name;
	private IndentityPathFilter abs;
	private HttpResponseHandler env;

	public HTTPObjectServer(ObjectRepository repository, File www, File cache,
			String passwd) throws IOException {
		this.repository = repository;
		HttpParams params = new BasicHttpParams();
		int timeout = 0;
		params.setIntParameter(SO_TIMEOUT, timeout);
		params.setIntParameter(SOCKET_BUFFER_SIZE, 8 * 1024);
		params.setBooleanParameter(STALE_CONNECTION_CHECK, false);
		params.setBooleanParameter(TCP_NODELAY, false);
		int n = Runtime.getRuntime().availableProcessors();
		Handler handler = new MethodNotAllowedHandler();
		handler = new InvokeHandler(handler);
		handler = new NotFoundHandler(handler);
		handler = new AlternativeHandler(handler);
		handler = new ResponseExceptionHandler(handler);
		handler = new OptionsHandler(handler);
		handler = new LinksHandler(handler);
		handler = new ModifiedSinceHandler(handler);
		handler = new UnmodifiedSinceHandler(handler);
		handler = new ContentHeadersHandler(handler);
		handler = new AuthenticationHandler(handler, passwd);
		handler = new DateHandler(handler);
		handler = env = new HttpResponseHandler(handler);
		Filter filter = new GZipFilter(null);
		filter = new CachingFilter(filter, cache, 1024);
		filter = new GUnzipFilter(filter);
		filter = new MD5ValidationFilter(filter);
		filter = abs = new IndentityPathFilter(filter);
		filter = new TraceFilter(filter);
		filter = new KeepAliveFilter(filter, timeout);
		filter = name = new ServerNameFilter(DEFAULT_NAME, filter);
		final HTTPObjectRequestHandler triage;
		triage = new HTTPObjectRequestHandler(filter, handler, repository, www);
		AsyncNHttpServiceHandler async = new AsyncNHttpServiceHandler(
				new BasicHttpProcessor(), new DefaultHttpResponseFactory(),
				new DefaultConnectionReuseStrategy(), params);
		async.setExpectationVerifier(triage);
		async.setHandlerResolver(new NHttpRequestHandlerResolver() {
			public NHttpRequestHandler lookup(String requestURI) {
				return triage;
			}
		});
		dispatch = new DefaultServerIOEventDispatch(async, params) {
			@Override
			protected NHttpServerIOTarget createConnection(IOSession session) {
				return new DefaultNHttpServerConnection(session,
						createHttpRequestFactory(), this.allocator, this.params) {
					@Override
					protected NHttpMessageParser createRequestParser(
							SessionInputBuffer buffer,
							HttpRequestFactory requestFactory, HttpParams params) {
						return new HttpRequestParser(buffer, null,
								requestFactory, params) {
							@Override
							public HttpMessage parse() throws IOException,
									HttpException {
								return removeEntityIfNoContent(super.parse());
							}
						};
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
		};
		server = new DefaultListeningIOReactor(n, params);
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public Repository getRepository() {
		return repository;
	}

	public String getServerName() {
		return name.getServerName();
	}

	public void setServerName(String serverName) {
		this.name.setServerName(serverName);
	}

	public String getIdentityPathPrefix() {
		return abs.getIdentityPathPrefix();
	}

	public void setIdentityPathPrefix(String prefix) {
		abs.setIdentityPathPrefix(prefix);
	}

	public String getEnvelopeType() {
		return env.getEnvelopeType();
	}

	public void setEnvelopeType(String type) throws MimeTypeParseException {
		env.setEnvelopeType(type);
	}

	public void start() throws BindException, Exception {
		final CountDownLatch latch = new CountDownLatch(1);
		server.listen(new InetSocketAddress(getPort()));
		executor.execute(new Runnable() {
			public void run() {
				try {
					latch.countDown();
					server.execute(dispatch);
				} catch (IOException e) {
					logger.error(e.toString(), e);
				}
			}
		});
		latch.await();
		for (int i = 0; i < 100; i++) {
			Thread.sleep(100);
			if (isRunning())
				break;
		}
		if (!isRunning())
			throw new BindException("Could not bind to port " + getPort());
	}

	public boolean isRunning() {
		return server.getStatus() == IOReactorStatus.ACTIVE;
	}

	public void stop() throws Exception {
		server.shutdown();
	}

	private HttpMessage removeEntityIfNoContent(HttpMessage msg) {
		if (msg instanceof HttpEntityEnclosingRequest
				&& !msg.containsHeader("Content-Length")
				&& !msg.containsHeader("Transfer-Encoding")) {
			HttpEntityEnclosingRequest body = (HttpEntityEnclosingRequest) msg;
			BasicHttpRequest req = new BasicHttpRequest(body.getRequestLine());
			req.setHeaders(body.getAllHeaders());
			return req;
		}
		return msg;
	}

}
