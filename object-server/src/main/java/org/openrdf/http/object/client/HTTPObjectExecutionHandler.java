package org.openrdf.http.object.client;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.http.Header;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.nio.entity.ConsumingNHttpEntity;
import org.apache.http.nio.entity.ProducingNHttpEntity;
import org.apache.http.nio.protocol.NHttpRequestExecutionHandler;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.nio.reactor.SessionRequest;
import org.apache.http.nio.reactor.SessionRequestCallback;
import org.apache.http.protocol.HttpContext;
import org.openrdf.http.object.model.ConsumingHttpEntity;
import org.openrdf.http.object.model.Filter;
import org.openrdf.http.object.model.ReadableHttpEntityChannel;
import org.openrdf.http.object.model.Request;
import org.openrdf.http.object.util.NamedThreadFactory;
import org.openrdf.http.object.util.ReadableContentListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HTTPObjectExecutionHandler implements
		NHttpRequestExecutionHandler, SessionRequestCallback {
	private static final String CONN_ATTR = HTTPConnection.class.getName();
	private static ScheduledExecutorService executor = Executors
			.newSingleThreadScheduledExecutor(new NamedThreadFactory(
					"HTTP Idle Connection"));
	private Logger logger = LoggerFactory
			.getLogger(HTTPObjectExecutionHandler.class);
	private Map<SocketAddress, Queue<FutureRequest>> queues = new HashMap<SocketAddress, Queue<FutureRequest>>();
	private Map<SocketAddress, List<HTTPConnection>> connections = new HashMap<SocketAddress, List<HTTPConnection>>();
	private Filter filter;
	private ConnectingIOReactor connector;
	private String agent;

	public HTTPObjectExecutionHandler(Filter filter,
			ConnectingIOReactor connector) {
		this.filter = filter;
		this.connector = connector;
	}

	public String getAgentName() {
		return agent;
	}

	public void setAgentName(String agent) {
		this.agent = agent;
	}

	public synchronized Future<HttpResponse> submitRequest(
			final SocketAddress remoteAddress, HttpRequest request)
			throws IOException {
		assert !(request instanceof HttpEntityEnclosingRequest)
				|| ((HttpEntityEnclosingRequest) request).getEntity() instanceof ProducingNHttpEntity;
		FutureRequest result = new FutureRequest(request) {
			protected boolean cancel() {
				return remove(remoteAddress, this);
			}
		};
		HttpResponse interception = filter.intercept(new Request(request));
		if (interception == null) {
			submitRequest(remoteAddress, result);
		} else {
			logger.debug("{} was {}", request.getRequestLine(), interception.getStatusLine());
			result.set(interception);
		}
		return result;
	}

	public synchronized void completed(SessionRequest request) {
		HTTPConnection conn = (HTTPConnection) request.getAttachment();
		conn.setIOSession(request.getSession());
	}

	public synchronized void cancelled(SessionRequest request) {
		HTTPConnection conn = (HTTPConnection) request.getAttachment();
		conn.setCancelled(true);
		logger.debug("{} cancelled", conn);
	}

	public synchronized void failed(SessionRequest request) {
		HTTPConnection conn = (HTTPConnection) request.getAttachment();
		conn.setIOException(request.getException());
		logger.debug("{} failed", conn);
	}

	public synchronized void timeout(SessionRequest request) {
		HTTPConnection conn = (HTTPConnection) request.getAttachment();
		conn.setTimedOut(true);
		logger.debug("{} timeout", conn);
	}

	public void initalizeContext(HttpContext context, Object conn) {
		assert conn != null;
		context.setAttribute(CONN_ATTR, conn);
	}

	public synchronized void finalizeContext(HttpContext context) {
		HTTPConnection conn = getHTTPConnection(context);
		SocketAddress remoteAddress = conn.getRemoteAddress();
		List<HTTPConnection> list = connections.get(remoteAddress);
		if (list != null && list.isEmpty()) {
			connections.remove(remoteAddress);
		} else if (list != null) {
			list.remove(conn);
		}
		FutureRequest freq;
		while ((freq = conn.removeRequest()) != null) {
			submitRequest(remoteAddress, freq);
		}
	}

	public synchronized HttpRequest submitRequest(HttpContext context) {
		final HTTPConnection conn = getHTTPConnection(context);
		if (conn.getReading() != null) {
			logger.debug("{} blocked", conn);
			return null; // don't submit request if reading previous response
		}
		SocketAddress addr = conn.getRemoteAddress();
		Queue<FutureRequest> queue = queues.get(addr);
		if (queue == null)
			return null;
		try {
			FutureRequest freq = queue.poll();
			conn.addRequest(freq);
			HttpRequest req = freq.getHttpRequest();
			req.setHeader("Connection", "keep-alive");
			req.setHeader("User-Agent", agent);
			logger.debug("{} sent {}", conn, req.getRequestLine());
			Request request = new Request(req);
			Request filtered = filter.filter(request);
			freq.setRequest(filtered);
			HttpResponse interception = filter.intercept(request);
			if (interception != null) {
				freq.set(interception);
				return submitRequest(context);
			}
			if (filtered.getEntity() == null) {
				req = new BasicHttpRequest(filtered.getRequestLine());
				req.setHeaders(filtered.getAllHeaders());
			} else {
				req = filtered;
			}
			return req;
		} catch (IOException e) {
			logger.error(e.toString(), e);
			return null;
		} finally {
			if (queue.isEmpty()) {
				queues.remove(addr);
			}
		}
	}

	public ConsumingNHttpEntity responseEntity(HttpResponse response,
			HttpContext context) throws IOException {
		HTTPConnection conn = getHTTPConnection(context);
		FutureRequest req = conn.removeRequest();
		conn.setReading(req);
		ConsumingNHttpEntity consume = filter.consume(req.getRequest(),
				response);
		if (consume == null) {
			ReadableContentListener in = new ReadableContentListener();
			String type = getHeader(response, "Content-Type");
			String length = getHeader(response, "Content-Length");
			long size = -1;
			if (length != null) {
				size = Long.parseLong(length);
			}
			response.setEntity(new ReadableHttpEntityChannel(type, size, in));
			req.set(response);
			logger.debug("{} reading {}", conn, req);
			return new ConsumingHttpEntity(response.getEntity(), in);
		} else {
			logger.debug("{} caching {}", conn, req);
			return consume;
		}
	}

	public void handleResponse(HttpResponse response, HttpContext context)
			throws IOException {
		final HTTPConnection conn = getHTTPConnection(context);
		FutureRequest req = conn.getReading();
		if (req == null) {
			req = conn.removeRequest();
			req.set(filter.filter(req.getRequest(), response));
			logger.debug("{} {} responded", conn, req);
		} else {
			if (req.poll() == null) {
				req.set(filter.filter(req.getRequest(), response));
			}
			logger.debug("{} {} completed", conn, req);
			conn.setReading(null); // input will no longer block new requests
			conn.requestOutput();
		}
		final int count = conn.getRequestCount();
		if (!conn.isPendingRequest()) {
			executor.schedule(new Runnable() {
				public void run() {
					removeIdleConnection(conn, count);
				}
			}, 15, TimeUnit.SECONDS);
		}
	}

	private void submitRequest(final SocketAddress remoteAddress,
			FutureRequest request) {
		Queue<FutureRequest> queue = queues.get(remoteAddress);
		if (queue == null) {
			queues.put(remoteAddress, queue = new LinkedList<FutureRequest>());
		}
		queue.add(request);
		Collection<HTTPConnection> sessions = connections.get(remoteAddress);
		if (sessions != null && !sessions.isEmpty()) {
			for (HTTPConnection session : sessions) {
				if (session.getIOSession() == null)
					return;
				if (session.getReading() == null) {
					session.requestOutput();
					return;
				}
			}
		}
		connect(remoteAddress);
	}

	private synchronized void removeIdleConnection(HTTPConnection conn,
			int count) {
		if (conn.getRequestCount() == count) {
			SocketAddress remoteAddress = conn.getRemoteAddress();
			List<HTTPConnection> list = connections.get(remoteAddress);
			if (list != null && list.isEmpty()) {
				connections.remove(remoteAddress);
			} else if (list != null) {
				list.remove(conn);
			}
			try {
				logger.debug("{} closed", conn);
				conn.shutdown();
			} catch (IOException e) {
				logger.warn(e.toString(), e);
			}
		}
	}

	private String getHeader(HttpResponse response, String name) {
		Header hd = response.getFirstHeader(name);
		if (hd == null)
			return null;
		return hd.getValue();
	}

	private synchronized void connect(SocketAddress remoteAddress) {
		HTTPConnection conn = new HTTPConnection(remoteAddress);
		connector.connect(remoteAddress, null, conn, this);
		List<HTTPConnection> sessions = connections.get(remoteAddress);
		if (sessions == null) {
			connections.put(remoteAddress,
					sessions = new LinkedList<HTTPConnection>());
		}
		sessions.add(conn);
	}

	private synchronized boolean remove(SocketAddress addr,
			FutureRequest request) {
		Queue<FutureRequest> queue = queues.get(addr);
		if (queue == null)
			return false;
		try {
			return queue.remove(request);
		} finally {
			if (queue.isEmpty()) {
				queues.remove(addr);
			}
		}
	}

	private HTTPConnection getHTTPConnection(HttpContext context) {
		return (HTTPConnection) context.getAttribute(CONN_ATTR);
	}
}