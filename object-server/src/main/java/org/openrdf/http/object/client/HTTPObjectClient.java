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

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.activation.MimeTypeParseException;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.nio.DefaultClientIOEventDispatch;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.nio.protocol.AsyncNHttpClientHandler;
import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.nio.reactor.IOReactorStatus;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpProcessor;
import org.openrdf.http.object.cache.CacheIndex;
import org.openrdf.http.object.cache.CachingFilter;
import org.openrdf.http.object.model.Filter;
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
public class HTTPObjectClient {
	private static final String VERSION = MavenUtil.loadVersion(
			"org.openrdf.alibaba", "alibaba-server-object", "devel");
	private static final String APP_NAME = "OpenRDF AliBaba object-client";
	protected static final String DEFAULT_NAME = APP_NAME + "/" + VERSION;
	private static Executor executor = Executors
			.newCachedThreadPool(new NamedThreadFactory("HTTP Object Client"));
	private static HTTPObjectClient instance;

	public static synchronized HTTPObjectClient getInstance()
			throws IOException {
		if (instance == null) {
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
			throws IOException {
		instance = new HTTPObjectClient(dir, maxCapacity);
		instance.start();
	}

	private Logger logger = LoggerFactory.getLogger(HTTPObjectClient.class);
	private HTTPObjectExecutionHandler client;
	private DefaultConnectingIOReactor connector;
	private IOEventDispatch dispatch;
	private String envelopeType;

	private HTTPObjectClient(File dir, int maxCapacity) throws IOException {
		HttpParams params = new BasicHttpParams();
		params.setIntParameter(SO_TIMEOUT, 0);
		params.setIntParameter(CONNECTION_TIMEOUT, 10000);
		params.setIntParameter(SOCKET_BUFFER_SIZE, 8 * 1024);
		params.setBooleanParameter(STALE_CONNECTION_CHECK, false);
		params.setBooleanParameter(TCP_NODELAY, false);
		int n = Runtime.getRuntime().availableProcessors();
		connector = new DefaultConnectingIOReactor(n, params);
		Filter filter = new CachingFilter(null,
				new CacheIndex(dir, maxCapacity));
		client = new HTTPObjectExecutionHandler(filter, connector);
		client.setAgentName(DEFAULT_NAME);
		AsyncNHttpClientHandler handler = new AsyncNHttpClientHandler(
				new BasicHttpProcessor(), client,
				new DefaultConnectionReuseStrategy(), params);
		dispatch = new DefaultClientIOEventDispatch(handler, params);
	}

	public String getAgentName() {
		return client.getAgentName();
	}

	public void setAgentName(String agent) {
		client.setAgentName(agent);
	}

	public String getEnvelopeType() {
		return envelopeType;
	}

	public void setEnvelopeType(String type) throws MimeTypeParseException {
		this.envelopeType = type;
	}

	public void start() {
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
	}

	public boolean isRunning() {
		return connector.getStatus() == IOReactorStatus.ACTIVE;
	}

	public Future<HttpResponse> submitRequest(InetSocketAddress remoteAddress,
			HttpRequest request) throws IOException {
		return client.submitRequest(remoteAddress, request);
	}

	public void stop() throws Exception {
		connector.shutdown();
	}
}
