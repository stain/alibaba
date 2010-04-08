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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.openrdf.http.object.model.Request;

/**
 * Handle used to track received responses with their requests.
 * 
 * @author James Leigh
 * 
 */
public class FutureRequest implements Future<HttpResponse> {
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
		return req.getRequestLine().toString()
				+ result.getStatusLine().toString();
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

	protected boolean cancel() {
		// allow subclasses to override
		return false;
	}

}
