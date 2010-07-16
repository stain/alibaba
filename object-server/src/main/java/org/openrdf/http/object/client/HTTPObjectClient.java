/*
 * Copyright (c) 2010, Zepheira LLC Some rights reserved.
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
package org.openrdf.http.object.client;

import static org.apache.http.params.CoreConnectionPNames.CONNECTION_TIMEOUT;
import static org.apache.http.params.CoreConnectionPNames.SOCKET_BUFFER_SIZE;
import static org.apache.http.params.CoreConnectionPNames.SO_TIMEOUT;
import static org.apache.http.params.CoreConnectionPNames.STALE_CONNECTION_CHECK;
import static org.apache.http.params.CoreConnectionPNames.TCP_NODELAY;
import info.aduna.io.MavenUtil;
import info.aduna.net.ParsedURI;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.activation.MimeTypeParseException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.nio.DefaultClientIOEventDispatch;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.nio.NHttpConnection;
import org.apache.http.nio.protocol.AsyncNHttpClientHandler;
import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.nio.reactor.IOReactorStatus;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpProcessor;
import org.openrdf.http.object.cache.CacheIndex;
import org.openrdf.http.object.cache.CachingFilter;
import org.openrdf.http.object.exceptions.GatewayTimeout;
import org.openrdf.http.object.filters.ClientGZipFilter;
import org.openrdf.http.object.filters.ClientMD5ValidationFilter;
import org.openrdf.http.object.model.Filter;
import org.openrdf.http.object.mxbeans.ConnectionBean;
import org.openrdf.http.object.mxbeans.HTTPObjectAgentMXBean;
import org.openrdf.http.object.util.FileUtil;
import org.openrdf.http.object.util.NamedThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the connections and cache for outgoing requests.
 * 
 * @author James Leigh
 * 
 */
public class HTTPObjectClient implements HTTPService, HTTPObjectAgentMXBean {
	private static final String MXBEAN_TYPE = "org.openrdf:type=" + HTTPObjectClient.class.getSimpleName();
	private static final String VERSION = MavenUtil.loadVersion(
			"org.openrdf.alibaba", "alibaba-server-object", "devel");
	private static final String APP_NAME = "OpenRDF AliBaba object-client";
	protected static final String DEFAULT_NAME = APP_NAME + "/" + VERSION;
	private static Executor executor = Executors
			.newCachedThreadPool(new NamedThreadFactory("HTTP Object Client", true));
	private static HTTPObjectClient instance;

	public static synchronized HTTPObjectClient getInstance()
			throws IOException {
		if (instance == null || !instance.isRunning()) {
			File dir = File.createTempFile("http-client-cache", "");
			dir.delete();
			dir.mkdir();
			FileUtil.deleteOnExit(dir);
			instance = new HTTPObjectClient(dir, 1024);
			instance.start();
		}
		return instance;
	}

	public static synchronized void setInstance(File dir, int maxCapacity)
			throws Exception {
		if (instance != null && instance.isRunning()) {
			instance.stop();
		}
		instance = new HTTPObjectClient(dir, maxCapacity);
		instance.start();
	}

	private Logger logger = LoggerFactory.getLogger(HTTPObjectClient.class);
	private HTTPObjectExecutionHandler client;
	private DefaultConnectingIOReactor connector;
	private IOEventDispatch dispatch;
	private String envelopeType;
	private CachingFilter cache;
	private ConcurrentMap<InetSocketAddress, HTTPService> proxies = new ConcurrentHashMap<InetSocketAddress, HTTPService>();
	private String from;

	private HTTPObjectClient(File dir, int maxCapacity) throws IOException {
		HttpParams params = new BasicHttpParams();
		params.setIntParameter(SO_TIMEOUT, 0);
		params.setIntParameter(CONNECTION_TIMEOUT, 10000);
		params.setIntParameter(SOCKET_BUFFER_SIZE, 8 * 1024);
		params.setBooleanParameter(STALE_CONNECTION_CHECK, false);
		params.setBooleanParameter(TCP_NODELAY, false);
		int n = Runtime.getRuntime().availableProcessors();
		connector = new DefaultConnectingIOReactor(n, params);
		Filter filter = new ClientMD5ValidationFilter(null);
		filter = cache = new CachingFilter(filter, new CacheIndex(dir,
				maxCapacity));
		filter = new ClientGZipFilter(cache);
		client = new HTTPObjectExecutionHandler(filter, connector);
		client.setAgentName(DEFAULT_NAME);
		AsyncNHttpClientHandler handler = new AsyncNHttpClientHandler(
				new BasicHttpProcessor(), client,
				new DefaultConnectionReuseStrategy(), params);
		dispatch = new DefaultClientIOEventDispatch(handler, params);
		try {
			from = System.getProperty("mail.from");
		} catch (SecurityException e) {
			// ignore
		}
	}

	public String getName() {
		return client.getAgentName();
	}

	public void setName(String agent) {
		client.setAgentName(agent);
	}

	public String getEnvelopeType() {
		return envelopeType;
	}

	public void setEnvelopeType(String type) throws MimeTypeParseException {
		this.envelopeType = type;
	}

	public HTTPService setProxy(InetSocketAddress destination, HTTPService proxy) {
		return proxies.put(destination, proxy);
	}

	public boolean removeProxy(InetSocketAddress destination, HTTPService proxy) {
		return proxies.remove(destination, proxy);
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

	public void invalidateCache() throws IOException, InterruptedException {
		cache.invalidate();
		HTTPObjectTransformerFactory.invalidateTransforms();
	}

	public void resetCache() throws IOException, InterruptedException {
		cache.reset();
		HTTPObjectTransformerFactory.resetTransforms();
	}

	public String getFrom() {
		return from;
	}

	public void setFrom(String from) {
		this.from = from;
	}

	public synchronized void start() {
		final CountDownLatch latch = new CountDownLatch(1);
		executor.execute(new Runnable() {
			public void run() {
				try {
					latch.countDown();
					connector.execute(dispatch);
				} catch (IOException e) {
					logger.error(e.toString(), e);
				}
			}
		});
		try {
			latch.await();
		} catch (InterruptedException e) {
			return;
		}
		try {
			MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
		    mbs.registerMBean(this, new ObjectName(getMXBeanName()));
		} catch (Exception e) {
			logger.info(e.toString(), e);
		}
	}

	public boolean isRunning() {
		return connector.getStatus() == IOReactorStatus.ACTIVE;
	}

	/**
	 * {@link HttpEntity#consumeContent()} or
	 * {@link HttpEntity#writeTo(java.io.OutputStream)} must be called if
	 * {@link HttpResponse#getEntity()} is non-null.
	 */
	public Future<HttpResponse> submitRequest(InetSocketAddress server,
			HttpRequest request) throws IOException {
		if (!request.containsHeader("Host")) {
			String host = server.getHostName();
			if (server.getPort() != 80) {
				host += ":" + server.getPort();
			}
			request.setHeader("Host", host);
		}
		if (!request.containsHeader("From")) {
			if (from != null && from.length() > 0) {
				request.setHeader("From", from);
			}
		}
		if (proxies.containsKey(server)) {
			FutureRequest freq = new FutureRequest(request);
			try {
				freq.set(proxy(server, request));
			} catch (IOException e) {
				freq.set(e);
			} catch (RuntimeException e) {
				freq.set(e);
			}
			return freq;
		}
		if (logger.isDebugEnabled()) {
			logger.debug("{} requesting {}", Thread.currentThread(), request
					.getRequestLine());
		}
		return client.submitRequest(server, request);
	}

	/**
	 * {@link HttpEntity#consumeContent()} or
	 * {@link HttpEntity#writeTo(java.io.OutputStream)} must be called if
	 * {@link HttpResponse#getEntity()} is non-null.
	 */
	public Future<HttpResponse> submitRequest(HttpRequest request) throws IOException {
		Header host = request.getFirstHeader("Host");
		if (host == null || host.getValue().length() == 0) {
			String uri = request.getRequestLine().getUri();
			return submitRequest(resolve(uri), request);
		}
		return submitRequest(resolve(host.getValue(), 80), request);
	}

	/**
	 * {@link HttpEntity#consumeContent()} or
	 * {@link HttpEntity#writeTo(java.io.OutputStream)} must be called if
	 * {@link HttpResponse#getEntity()} is non-null.
	 */
	public HttpResponse service(InetSocketAddress server, HttpRequest request)
			throws IOException, GatewayTimeout {
		if (proxies.containsKey(server)) {
			return proxy(server, request);
		}
		try {
			return submitRequest(server, request).get();
		} catch (ExecutionException e) {
			try {
				throw e.getCause();
			} catch (RuntimeException cause) {
				if (cause.getCause() == null) {
					cause.initCause(new RuntimeException(e.toString()));
				}
				throw cause;
			} catch (Error cause) {
				throw cause;
			} catch (Throwable cause) {
				throw new IOException(e);
			}
		} catch (InterruptedException e) {
			throw new GatewayTimeout(e);
		}
	}

	/**
	 * {@link HttpEntity#consumeContent()} or
	 * {@link HttpEntity#writeTo(java.io.OutputStream)} must be called if
	 * {@link HttpResponse#getEntity()} is non-null.
	 */
	public HttpResponse service(HttpRequest request) throws IOException,
			GatewayTimeout {
		Header host = request.getFirstHeader("Host");
		if (host == null || host.getValue().length() == 0) {
			String uri = request.getRequestLine().getUri();
			return service(resolve(uri), request);
		}
		return service(resolve(host.getValue(), 80), request);
	}

	public synchronized void stop() throws Exception {
		connector.shutdown();
		resetConnections();
		try {
			MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
		    mbs.unregisterMBean(new ObjectName(getMXBeanName()));
		} catch (Exception e) {
			logger.info(e.toString(), e);
		}
	}

	public synchronized void destroy() throws Exception {
		stop();
	}

	public void resetConnections() throws IOException {
		for (HTTPConnection conn : client.getConnections()) {
			conn.shutdown();
		}
	}

	public void poke() {
		System.gc();
		for (HTTPConnection conn : client.getConnections()) {
			conn.requestOutput();
			conn.requestInput();
		}
	}

	public String getStatus() {
		return connector.getStatus().toString();
	}

	public ConnectionBean[] getConnections() {
		HTTPConnection[] connections = client.getConnections();
		ConnectionBean[] beans = new ConnectionBean[connections.length];
		for (int i = 0; i < beans.length; i++) {
			ConnectionBean bean = new ConnectionBean();
			HTTPConnection conn = connections[i];
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
			SocketAddress remote = conn.getRemoteAddress();
			SocketAddress local = conn.getLocalAddress();
			bean.setStatus(bean.getStatus() + " " + remote + "<-" + local);
			FutureRequest req = conn.getReading();
			if (req != null) {
				bean.setConsuming(req.toString());
			}
			FutureRequest[] pending = conn.getPendingRequests();
			String[] list = new String[pending.length];
			for (int j=0;j<list.length;j++) {
				list[j] = pending[j].toString();
			}
			bean.setPending(list);
		}
		return beans;
	}

	private String getMXBeanName() {
		String name = MXBEAN_TYPE;
		if (instance != this) {
			name += ",instance=" + System.identityHashCode(this);
		}
		return name;
	}

	private HttpResponse proxy(InetSocketAddress server, HttpRequest request)
			throws IOException {
		if (logger.isDebugEnabled()) {
			logger.debug("{} servicing {}", Thread.currentThread(), request
					.getRequestLine());
		}
		HttpResponse response = proxies.get(server).service(request);
		if (logger.isDebugEnabled()) {
			logger.debug("{} serviced {}", Thread.currentThread(), response
					.getStatusLine());
		}
		return response;
	}

	private InetSocketAddress resolve(String uri) {
		if (!uri.startsWith("http"))
			return null;
		ParsedURI parsed = new ParsedURI(uri);
		int port = 80;
		if ("http".equalsIgnoreCase(parsed.getScheme())) {
			port = 80;
		} else if ("https".equalsIgnoreCase(parsed.getScheme())) {
			port = 443;
		} else {
			return null;
		}
		return resolve(parsed.getAuthority(), port);
	}

	private InetSocketAddress resolve(String authority, int port) {
		if (authority.contains("@")) {
			authority = authority.substring(authority.indexOf('@') + 1);
		}
		String hostname = authority;
		if (hostname.contains(":")) {
			hostname = hostname.substring(0, hostname.indexOf(':'));
		}
		if (authority.contains(":")) {
			int idx = authority.indexOf(':') + 1;
			port = Integer.parseInt(authority.substring(idx));
		}
		return new InetSocketAddress(hostname, port);
	}
}
