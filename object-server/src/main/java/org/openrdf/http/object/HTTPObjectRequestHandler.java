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
package org.openrdf.http.object;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.channels.ReadableByteChannel;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpInetConnection;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.nio.NHttpConnection;
import org.apache.http.nio.entity.ConsumingNHttpEntity;
import org.apache.http.nio.entity.ConsumingNHttpEntityTemplate;
import org.apache.http.nio.protocol.EventListener;
import org.apache.http.nio.protocol.NHttpRequestHandler;
import org.apache.http.nio.protocol.NHttpResponseTrigger;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpExpectationVerifier;
import org.openrdf.http.object.client.HTTPService;
import org.openrdf.http.object.model.ConsumingHttpEntity;
import org.openrdf.http.object.model.Filter;
import org.openrdf.http.object.model.Handler;
import org.openrdf.http.object.model.ReadableHttpEntityChannel;
import org.openrdf.http.object.model.Request;
import org.openrdf.http.object.tasks.Task;
import org.openrdf.http.object.tasks.TaskFactory;
import org.openrdf.http.object.util.ReadableContentListener;
import org.openrdf.repository.object.ObjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Applies the filters and handles the HTTP requests.
 * 
 * @author James Leigh
 * 
 */
public class HTTPObjectRequestHandler implements NHttpRequestHandler,
		HttpExpectationVerifier, EventListener, HTTPService {
	private static class ResponseTrigger implements NHttpResponseTrigger {
		private HttpResponse response;
		private IOException io;
		private HttpException http;

		public void submitResponse(HttpResponse response) {
			this.response = response;
		}

		public void handleException(IOException ex) {
			this.io = ex;
		}

		public void handleException(HttpException ex) {
			this.http = ex;
		}
	}

	private static final String HANDLER_ATTR = Task.class.getName();
	private static final String CONSUMING_ATTR = ConsumingNHttpEntityTemplate.class
			.getName();

	private Logger logger = LoggerFactory
			.getLogger(HTTPObjectRequestHandler.class);
	private TaskFactory factory;

	public HTTPObjectRequestHandler(Filter filter, Handler handler,
			ObjectRepository repository, File dataDir) {
		factory = new TaskFactory(dataDir, repository, filter, handler);
	}

	public HttpResponse service(HttpRequest request) throws IOException {
		Request req = new Request(request, InetAddress.getLocalHost());
		Task task = factory.createForegroundTask(req);
		ResponseTrigger trigger = new ResponseTrigger();
		task.setTrigger(trigger);
		try {
			if (trigger.io != null) {
				throw trigger.io;
			}
			if (trigger.http != null) {
				throw new IOException(trigger.http);
			}
			return trigger.response;
		} catch (IOException io) {
			if (trigger.response != null) {
				HttpEntity entity = trigger.response.getEntity();
				if (entity != null) {
					try {
						entity.consumeContent();
					} catch (IOException e) {
						logger.debug(e.toString(), e);
					}
				}
			}
			throw io;
		}
	}

	public void verify(HttpRequest request, HttpResponse response,
			HttpContext ctx) throws HttpException {
		ReadableContentListener in = null;
		try {
			if (request instanceof HttpEntityEnclosingRequest) {
				in = new ReadableContentListener();
			}
			Task task = factory.createBackgroundTask(process(request, in, ctx));
			ctx.setAttribute(HANDLER_ATTR, task);
			if (request instanceof HttpEntityEnclosingRequest) {
				HttpEntityEnclosingRequest req = (HttpEntityEnclosingRequest) request;
				ConsumingNHttpEntity reader = new ConsumingNHttpEntityTemplate(
						req.getEntity(), in);
				ctx.setAttribute(CONSUMING_ATTR, reader);
			}
			task.awaitVerification(1, TimeUnit.SECONDS); // block TCP stream
			HttpResponse resp = task.getHttpResponse();
			if (resp != null) {
				response.setStatusLine(resp.getStatusLine());
				response.setHeaders(resp.getAllHeaders());
				response.setEntity(resp.getEntity());
			}
		} catch (IOException e) {
			throw new HttpException(e.toString(), e);
		} catch (InterruptedException e) {
			logger.warn(e.toString(), e);
		}
	}

	public ConsumingNHttpEntity entityRequest(
			HttpEntityEnclosingRequest request, HttpContext ctx)
			throws HttpException, IOException {
		ConsumingNHttpEntity reader = (ConsumingNHttpEntity) ctx
				.removeAttribute(CONSUMING_ATTR);
		if (reader == null) {
			ReadableContentListener in = new ReadableContentListener();
			Task task = factory.createBackgroundTask(process(request, in, ctx));
			ctx.setAttribute(HANDLER_ATTR, task);
			return new ConsumingHttpEntity(request.getEntity(), in);
		} else {
			return reader;
		}
	}

	public void handle(HttpRequest request, HttpResponse response,
			NHttpResponseTrigger trigger, HttpContext ctx)
			throws HttpException, IOException {
		Task task = (Task) ctx.removeAttribute(HANDLER_ATTR);
		if (task == null) {
			task = factory.createBackgroundTask(process(request, null, ctx));
			task.setTrigger(trigger);
		} else {
			task.setTrigger(trigger);
		}
	}

	public void connectionClosed(NHttpConnection conn) {
		abort(conn.getContext());
	}

	public void connectionOpen(NHttpConnection conn) {
		logger.debug("{} openned", conn);
	}

	public void connectionTimeout(NHttpConnection conn) {
		abort(conn.getContext());
	}

	public void fatalIOException(IOException ex, NHttpConnection conn) {
		abort(conn.getContext());
	}

	public void fatalProtocolException(HttpException ex, NHttpConnection conn) {
		abort(conn.getContext());
	}

	private Request process(HttpRequest request, ReadableByteChannel in,
			HttpContext context) throws IOException {
		InetAddress addr = getRemoteAddress(context);
		Request req = new Request(request, addr);
		if (in == null) {
			req.setEntity(null);
		} else {
			String type = req.getHeader("Content-Type");
			String length = req.getHeader("Content-Length");
			long size = -1;
			if (length != null) {
				size = Long.parseLong(length);
			}
			req.setEntity(new ReadableHttpEntityChannel(type, size, in));
		}
		return req;
	}

	private InetAddress getRemoteAddress(HttpContext context) {
		HttpInetConnection con = (HttpInetConnection) context
				.getAttribute(ExecutionContext.HTTP_CONNECTION);
		return con.getRemoteAddress();
	}

	private void abort(HttpContext context) {
		ConsumingNHttpEntity reader = (ConsumingNHttpEntity) context
				.removeAttribute(CONSUMING_ATTR);
		if (reader != null) {
			try {
				reader.finish();
			} catch (IOException e) {
				logger.debug(e.toString(), e);
			}
		}
		Task task = (Task) context.removeAttribute(HANDLER_ATTR);
		if (task != null) {
			ResponseTrigger trigger = new ResponseTrigger();
			task.setTrigger(trigger);
			if (trigger.response != null) {
				HttpEntity entity = trigger.response.getEntity();
				if (entity != null) {
					try {
						entity.consumeContent();
					} catch (IOException e) {
						logger.debug(e.toString(), e);
					}
				}
			}
			if (trigger.io != null) {
				logger.debug(trigger.io.toString(), trigger.io);
			}

			if (trigger.http != null) {
				logger.debug(trigger.http.toString(), trigger.http);
			}
			task.close();
		}
	}

}