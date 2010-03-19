package org.openrdf.http.object.client;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.openrdf.http.object.model.Request;

public abstract class FutureRequest implements Future<HttpResponse> {
	private boolean cancelled;
	private ExecutionException ex;
	private HttpRequest req;
	private Request request;
	private HttpResponse result;
	private HTTPConnection conn;

	public FutureRequest(HttpRequest req) {
		this.req = req;
	}

	public synchronized void attached(HTTPConnection conn) {
		this.conn = conn;
		notifyAll();
	}

	public String toString() {
		if (result == null)
			return req.getRequestLine().toString();
		return req.getRequestLine().toString() + result.getStatusLine().toString();
	}

	public HttpRequest getHttpRequest() {
		return req;
	}

	public Request getRequest() {
		return request;
	}

	public void setRequest(Request req) {
		this.request = req;
	}

	public synchronized void set(HttpResponse result) {
		assert result != null;
		this.result = result;
		notifyAll();
	}

	public synchronized void set(ExecutionException ex) {
		assert ex != null;
		this.ex = ex;
		notifyAll();
	}

	public synchronized HttpResponse get() throws InterruptedException,
			ExecutionException {
		while (!isDone()) {
			if (conn != null) {
				conn.requestInput();
			}
			wait();
		}
		if (ex != null)
			throw ex;
		return poll();
	}

	public synchronized HttpResponse poll() {
		return result;
	}

	public synchronized HttpResponse get(long timeout, TimeUnit unit)
			throws InterruptedException, ExecutionException, TimeoutException {
		if (!isDone()) {
			if (conn != null) {
				conn.requestInput();
			}
			wait(unit.toMillis(timeout));
			if (!isDone())
				throw new TimeoutException("Timeout while waiting for "
						+ req.getRequestLine().toString());
		}
		if (ex != null)
			throw ex;
		return poll();
	}

	public synchronized boolean cancel(boolean mayInterruptIfRunning) {
		cancelled = cancel();
		notifyAll();
		return cancelled;
	}

	public synchronized boolean isCancelled() {
		return cancelled;
	}

	public synchronized boolean isDone() {
		return ex != null || result != null || isCancelled();
	}

	protected abstract boolean cancel();

}
