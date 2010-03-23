package org.openrdf.http.object.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.Queue;

import org.apache.http.nio.NHttpConnection;
import org.apache.http.nio.reactor.IOSession;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;

public class HTTPConnection {
	private int requests;
	private InetSocketAddress remoteAddress;
	private IOException io;
	private volatile boolean cancelled;
	private volatile boolean timedOut;
	private IOSession session;
	private HttpContext context;
	private Queue<FutureRequest> queue = new LinkedList<FutureRequest>();
	private FutureRequest reading;
	private NHttpConnection conn;

	public HTTPConnection(InetSocketAddress remoteAddress) {
		this.remoteAddress = remoteAddress;
	}

	@Override
	public String toString() {
		return (session == null ? "" : session.getLocalAddress()) + "->"
				+ remoteAddress.toString();
	}

	public InetSocketAddress getRemoteAddress() {
		return remoteAddress;
	}

	public IOException getIOException() {
		return io;
	}

	public void setIOException(IOException io) {
		this.io = io;
	}

	public void requestInput() {
		if (conn != null) {
			conn.requestInput();
		}
	}

	public void requestOutput() {
		if (conn != null) {
			conn.requestOutput();
		}
	}

	public void shutdown() throws IOException {
		conn.shutdown();
	}

	public boolean isCancelled() {
		return cancelled;
	}

	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}

	public boolean isTimedOut() {
		return timedOut;
	}

	public void setTimedOut(boolean timedOut) {
		this.timedOut = timedOut;
	}

	public int getRequestCount() {
		return requests;
	}

	public synchronized boolean isPendingRequest() {
		return !queue.isEmpty();
	}

	public FutureRequest getReading() {
		return reading;
	}

	public void setReading(FutureRequest req) {
		this.reading = req;
	}

	public IOSession getIOSession() {
		return session;
	}

	public synchronized void setIOSession(IOSession session) {
		this.session = session;
		conn = (NHttpConnection) session
				.getAttribute(ExecutionContext.HTTP_CONNECTION);
		for (FutureRequest req : queue) {
			req.attached(this);
		}
	}

	public HttpContext getHttpContext() {
		return context;
	}

	public void setHttpContext(HttpContext context) {
		this.context = context;
	}

	public synchronized void addRequest(FutureRequest req) {
		requests++;
		queue.add(req);
		req.attached(this);
	}

	public synchronized FutureRequest removeRequest() {
		return queue.poll();
	}

}
