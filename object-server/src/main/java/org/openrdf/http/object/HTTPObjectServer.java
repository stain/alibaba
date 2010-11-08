/*
 * Copyright (c) 2009-2010, James Leigh and Zepheira LLC Some rights reserved.
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
import static org.openrdf.http.object.HTTPObjectRequestHandler.HANDLER_ATTR;
import static org.openrdf.http.object.HTTPObjectRequestHandler.PENDING_ATTR;
import info.aduna.io.MavenUtil;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import javax.activation.MimeTypeParseException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpInetConnection;
import org.apache.http.HttpMessage;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestFactory;
import org.apache.http.HttpResponse;
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
import org.apache.http.nio.NHttpConnection;
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
import org.apache.http.protocol.HttpContext;
import org.openrdf.http.object.cache.CachingFilter;
import org.openrdf.http.object.client.HTTPObjectClient;
import org.openrdf.http.object.client.HTTPService;
import org.openrdf.http.object.filters.DateHeaderFilter;
import org.openrdf.http.object.filters.GUnzipFilter;
import org.openrdf.http.object.filters.GZipFilter;
import org.openrdf.http.object.filters.HttpResponseFilter;
import org.openrdf.http.object.filters.IdentityPrefix;
import org.openrdf.http.object.filters.KeepAliveFilter;
import org.openrdf.http.object.filters.MD5ValidationFilter;
import org.openrdf.http.object.filters.ServerNameFilter;
import org.openrdf.http.object.filters.TraceFilter;
import org.openrdf.http.object.handlers.AlternativeHandler;
import org.openrdf.http.object.handlers.AuthenticationHandler;
import org.openrdf.http.object.handlers.ContentHeadersHandler;
import org.openrdf.http.object.handlers.InvokeHandler;
import org.openrdf.http.object.handlers.LinksHandler;
import org.openrdf.http.object.handlers.ModifiedSinceHandler;
import org.openrdf.http.object.handlers.NotFoundHandler;
import org.openrdf.http.object.handlers.OptionsHandler;
import org.openrdf.http.object.handlers.ResponseExceptionHandler;
import org.openrdf.http.object.handlers.UnmodifiedSinceHandler;
import org.openrdf.http.object.model.Filter;
import org.openrdf.http.object.model.Handler;
import org.openrdf.http.object.tasks.Task;
import org.openrdf.http.object.threads.ManagedExecutors;
import org.openrdf.http.object.threads.NamedThreadFactory;
import org.openrdf.repository.Repository;
import org.openrdf.repository.object.ObjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the start and stop stages of the server.
 * 
 * @author James Leigh
 * @param <a>
 * 
 */
public class HTTPObjectServer implements HTTPService, HTTPObjectAgentMXBean {
	private static final String MXBEAN_TYPE = "org.openrdf:type="
			+ HTTPObjectServer.class.getSimpleName();
	private static final String VERSION = MavenUtil.loadVersion(
			"org.openrdf.alibaba", "alibaba-server-object", "devel");
	private static final String APP_NAME = "OpenRDF AliBaba object-server";
	protected static final String DEFAULT_NAME = APP_NAME + "/" + VERSION;
	private static NamedThreadFactory executor = new NamedThreadFactory("HTTP Object Server", false);
	private static final List<HTTPObjectServer> instances = new ArrayList<HTTPObjectServer>();

	public static HTTPObjectServer[] getInstances() {
		synchronized (instances) {
			return instances.toArray(new HTTPObjectServer[instances.size()]);
		}
	}

	public static void resetAllCache() {
		for (HTTPObjectServer server : getInstances()) {
			server.resetCache();
		}
	}

	private Logger logger = LoggerFactory.getLogger(HTTPObjectServer.class);
	private ListeningIOReactor server;
	private IOEventDispatch dispatch;
	private ObjectRepository repository;
	private int[] ports;
	private ServerNameFilter name;
	private IdentityPrefix abs;
	private HttpResponseFilter env;
	private boolean started = false;
	private boolean stopped = true;
	private HTTPObjectRequestHandler service;
	private LinksHandler links;
	private ModifiedSinceHandler remoteCache;
	private CachingFilter cache;

	/**
	 * @param basic username:password
	 */
	public HTTPObjectServer(ObjectRepository repository, File www, File cacheDir,
			String basic) throws IOException {
		this.repository = repository;
		HttpParams params = new BasicHttpParams();
		int timeout = 0;
		params.setIntParameter(SO_TIMEOUT, timeout);
		params.setIntParameter(SOCKET_BUFFER_SIZE, 8 * 1024);
		params.setBooleanParameter(STALE_CONNECTION_CHECK, false);
		params.setBooleanParameter(TCP_NODELAY, false);
		int n = Runtime.getRuntime().availableProcessors();
		Handler handler = new InvokeHandler();
		handler = new NotFoundHandler(handler);
		handler = new AlternativeHandler(handler);
		handler = new ResponseExceptionHandler(handler);
		handler = new OptionsHandler(handler);
		handler = links = new LinksHandler(handler);
		handler = remoteCache = new ModifiedSinceHandler(handler);
		handler = new UnmodifiedSinceHandler(handler);
		handler = new ContentHeadersHandler(handler);
		handler = new AuthenticationHandler(handler, basic);
		Filter filter = env = new HttpResponseFilter(null);
		filter = new DateHeaderFilter(filter);
		filter = new GZipFilter(filter);
		filter = cache = new CachingFilter(filter, cacheDir, 1024);
		filter = new GUnzipFilter(filter);
		filter = new MD5ValidationFilter(filter);
		filter = abs = new IdentityPrefix(filter);
		filter = new TraceFilter(filter);
		filter = new KeepAliveFilter(filter, timeout);
		filter = name = new ServerNameFilter(DEFAULT_NAME, filter);
		service = new HTTPObjectRequestHandler(filter, handler, repository, www);
		AsyncNHttpServiceHandler async = new AsyncNHttpServiceHandler(
				new BasicHttpProcessor(), new DefaultHttpResponseFactory(),
				new DefaultConnectionReuseStrategy(), params);
		async.setExpectationVerifier(service);
		async.setEventListener(service);
		async.setHandlerResolver(new NHttpRequestHandlerResolver() {
			public NHttpRequestHandler lookup(String requestURI) {
				return service;
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
		};
		server = new DefaultListeningIOReactor(n, params);
		repository.addSchemaListener(new Runnable() {
			public String toString() {
				return "reset cache";
			}
			public void run() {
				resetCache();
			}
		});
	}

	public String getErrorXSLT() {
		return service.getErrorXSLT();
	}

	public void setErrorXSLT(String url) {
		service.setErrorXSLT(url);
	}

	public Repository getRepository() {
		return repository;
	}

	public String getName() {
		return name.getServerName();
	}

	public void setName(String serverName) {
		this.name.setServerName(serverName);
	}

	public String[] getIdentityPrefix() {
		return abs.getIdentityPrefix();
	}

	public void setIdentityPrefix(String[] prefix) {
		abs.setIdentityPrefix(prefix);
	}

	public String getEnvelopeType() {
		return env.getEnvelopeType();
	}

	public void setEnvelopeType(String type) throws MimeTypeParseException {
		env.setEnvelopeType(type);
		links.setEnvelopeType(type);
	}

	public boolean isCacheAggressive() {
		return cache.isAggressive();
	}

	public boolean isCacheDisconnected() {
		return cache.isDisconnected();
	}

	public boolean isCacheEnabled() {
		return cache.isEnabled();
	}

	public void setCacheAggressive(boolean cacheAggressive) {
		cache.setAggressive(cacheAggressive);
	}

	public void setCacheDisconnected(boolean cacheDisconnected) {
		cache.setDisconnected(cacheDisconnected);
	}

	public void setCacheEnabled(boolean cacheEnabled) {
		cache.setEnabled(cacheEnabled);
	}

	public int getCacheCapacity() {
		return cache.getMaxCapacity();
	}

	public void setCacheCapacity(int capacity) {
		cache.setMaxCapacity(capacity);
	}

	public int getCacheSize() {
		return cache.getSize();
	}

	public String getFrom() {
		return null;
	}

	public void setFrom(String from) {
		throw new UnsupportedOperationException();
	}

	public void invalidateCache() throws IOException, InterruptedException {
		cache.invalidate();
		HTTPObjectClient.getInstance().invalidateCache();
		remoteCache.invalidate();
	}

	public void resetCache() {
		ManagedExecutors.getTimeoutThreadPool().execute(new Runnable() {
			public String toString() {
				return "reset cache";
			}

			public void run() {
				try {
					cache.reset();
					HTTPObjectClient.getInstance().resetCache();
					remoteCache.invalidate();
				} catch (Error e) {
					logger.error(e.toString(), e);
				} catch (RuntimeException e) {
					logger.error(e.toString(), e);
				} catch (IOException e) {
					logger.error(e.toString(), e);
				} catch (InterruptedException e) {
					logger.info(e.toString(), e);
				}
			}
		});
	}

	public void resetConnections() throws IOException {
		NHttpConnection[] connections = service.getConnections();
		for (int i = 0; i < connections.length; i++) {
			connections[i].shutdown();
		}
	}

	public synchronized void listen(int... ports) throws Exception {
		assert ports.length > 0;
		if (isRunning())
			throw new IllegalStateException("Server is already running");
		name.setPort(ports[0]);
		this.ports = ports;
		for (int port : ports) {
			server.listen(new InetSocketAddress(port));
		}
		server.pause();
		started = false;
		stopped = false;
		executor.newThread(new Runnable() {
			public void run() {
				try {
					synchronized (HTTPObjectServer.this) {
						started = true;
						HTTPObjectServer.this.notifyAll();
					}
					server.execute(dispatch);
				} catch (IOException e) {
					logger.error(e.toString(), e);
				} finally {
					synchronized (HTTPObjectServer.this) {
						stopped = true;
						HTTPObjectServer.this.notifyAll();
					}
				}
			}
		}).start();
		while (!started) {
			wait();
		}
		Thread.sleep(100);
		if (!isRunning())
			throw new BindException("Could not bind to port " + ports[0]
					+ " server is " + server.getStatus());
		synchronized (instances) {
			instances.add(this);
		}
		try {
			MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
		    mbs.registerMBean(this, new ObjectName(MXBEAN_TYPE + ",port=" + ports[0]));
		} catch (Exception e) {
			logger.info(e.toString(), e);
		}
	}

	public synchronized void start() throws Exception {
		for (int port : ports) {
			registerService(HTTPObjectClient.getInstance(), port);
		}
		server.resume();
	}

	public boolean isRunning() {
		return !stopped && server.getStatus() == IOReactorStatus.ACTIVE;
	}

	public synchronized void stop() throws Exception {
		for (int port : ports) {
			deregisterService(HTTPObjectClient.getInstance(), port);
		}
		server.pause();
	}

	public synchronized void destroy() throws Exception {
		stop();
		server.shutdown();
		resetConnections();
		while (!stopped) {
			wait();
		}
		Thread.sleep(100);
		while (server.getStatus() != IOReactorStatus.SHUT_DOWN
				&& server.getStatus() != IOReactorStatus.INACTIVE) {
			Thread.sleep(1000);
			if (isRunning())
				throw new HttpException("Could not shutdown server");
		}
		synchronized (instances) {
			instances.remove(this);
		}
		try {
			MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
		    mbs.unregisterMBean(new ObjectName(MXBEAN_TYPE + ",port=" + ports[0]));
		} catch (Exception e) {
			logger.info(e.toString(), e);
		}
	}

	public void poke() {
		System.gc();
		for (NHttpConnection conn : service.getConnections()) {
			conn.requestInput();
			conn.requestOutput();
		}
	}

	public String getStatus() {
		return server.getStatus().toString();
	}

	public ConnectionBean[] getConnections() {
		NHttpConnection[] connections = service.getConnections();
		ConnectionBean[] beans = new ConnectionBean[connections.length];
		for (int i = 0; i < beans.length; i++) {
			ConnectionBean bean = new ConnectionBean();
			NHttpConnection conn = connections[i];
			beans[i] = bean;
			switch (conn.getStatus()) {
			case NHttpConnection.ACTIVE:
				if (conn.isOpen()) {
					bean.setStatus("OPEN");
				} else if (conn.isStale()) {
					bean.setStatus("STALE");
				} else {
					bean.setStatus("ACTIVE");
				}
				break;
			case NHttpConnection.CLOSING:
				bean.setStatus("CLOSING");
				break;
			case NHttpConnection.CLOSED:
				bean.setStatus("CLOSED");
				break;
			}
			if (conn instanceof HttpInetConnection) {
				HttpInetConnection inet = (HttpInetConnection) conn;
				InetAddress ra = inet.getRemoteAddress();
				int rp = inet.getRemotePort();
				InetAddress la = inet.getLocalAddress();
				int lp = inet.getLocalPort();
				InetSocketAddress remote = new InetSocketAddress(ra, rp);
				InetSocketAddress local = new InetSocketAddress(la, lp);
				bean.setStatus(bean.getStatus() + " " + remote + "->" + local);
			}
			HttpRequest req = conn.getHttpRequest();
			if (req != null) {
				bean.setRequest(req.getRequestLine().toString());
			}
			HttpResponse resp = conn.getHttpResponse();
			if (resp != null) {
				bean.setResponse(resp.getStatusLine().toString() + " "
						+ resp.getEntity());
			}
			HttpContext ctx = conn.getContext();
			Object handler = ctx.getAttribute(HANDLER_ATTR);
			if (handler != null) {
				bean.setConsuming(handler.toString());
			}
			Queue queue = (Queue) ctx.getAttribute(PENDING_ATTR);
			if (queue != null) {
				Object[] array = null;
				synchronized (queue) {
					if (!queue.isEmpty()) {
						array = queue.toArray(new Task[queue.size()]);
					}
				}
				if (array != null) {
					String[] pending = new String[queue.size()];
					for (int j=0;j<pending.length;j++) {
						pending[j] = array[j].toString();
					}
					bean.setPending(pending);
				}
			}
		}
		return beans;
	}

	public HttpResponse service(HttpRequest request) throws IOException {
		return service.service(request);
	}

	private void registerService(HTTPObjectClient client, int port) {
		for (InetAddress addr : getAllLocalAddresses()) {
			client.setProxy(new InetSocketAddress(addr, port), service);
		}
	}

	private void deregisterService(HTTPObjectClient client, int port) {
		for (InetAddress addr : getAllLocalAddresses()) {
			client.removeProxy(new InetSocketAddress(addr, port), service);
		}
	}

	private Set<InetAddress> getAllLocalAddresses() {
		Set<InetAddress> result = new HashSet<InetAddress>();
		try {
			result.addAll(Arrays.asList(InetAddress.getAllByName(null)));
		} catch (UnknownHostException e) {
			// no loop back device
		}
		try {
			InetAddress local = InetAddress.getLocalHost();
			result.add(local);
			try {
				result.addAll(Arrays.asList(InetAddress.getAllByName(local
						.getCanonicalHostName())));
			} catch (UnknownHostException e) {
				// no canonical name
			}
		} catch (UnknownHostException e) {
			// no network
		}
		try {
			Enumeration<NetworkInterface> interfaces;
			interfaces = NetworkInterface.getNetworkInterfaces();
			while (interfaces != null && interfaces.hasMoreElements()) {
				NetworkInterface iface = interfaces.nextElement();
				Enumeration<InetAddress> addrs = iface.getInetAddresses();
				while (addrs != null && addrs.hasMoreElements()) {
					result.add(addrs.nextElement());
				}
			}
		} catch (SocketException e) {
			// broken network configuration
		}
		return result;
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
