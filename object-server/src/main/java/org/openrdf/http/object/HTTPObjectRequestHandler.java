/*
 * Copyright 2010, Zepheira LLC Some rights reserved.
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

import info.aduna.concurrent.locks.Lock;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PipedOutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.entity.ConsumingNHttpEntity;
import org.apache.http.nio.entity.ConsumingNHttpEntityTemplate;
import org.apache.http.nio.entity.ContentListener;
import org.apache.http.nio.protocol.NHttpRequestHandler;
import org.apache.http.nio.protocol.NHttpResponseTrigger;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpExpectationVerifier;
import org.openrdf.OpenRDFException;
import org.openrdf.http.object.exceptions.InternalServerError;
import org.openrdf.http.object.exceptions.ResponseException;
import org.openrdf.http.object.locks.FileLockManager;
import org.openrdf.http.object.model.ErrorInputStream;
import org.openrdf.http.object.model.Filter;
import org.openrdf.http.object.model.Handler;
import org.openrdf.http.object.model.InputStreamHttpEntity;
import org.openrdf.http.object.model.Request;
import org.openrdf.http.object.model.ResourceOperation;
import org.openrdf.http.object.model.Response;
import org.openrdf.http.object.util.NamedThreadFactory;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectRepository;
import org.openrdf.sail.optimistic.exceptions.ConcurrencyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Applies the filters and handles the HTTP requests.
 * 
 * @author James Leigh
 *
 */
public class HTTPObjectRequestHandler implements NHttpRequestHandler,
		HttpExpectationVerifier {
	private static final String HANDLER_ATTR = Task.class.getName();

	private final class Task implements Runnable {
		private Request request;
		private ResourceOperation operation;
		private CountDownLatch latch = new CountDownLatch(1);
		private NHttpResponseTrigger trigger;
		private Task child;
		private volatile boolean done;

		private Task(Request request) {
			this.request = request;
		}

		private Task(Request request, ResourceOperation operation) {
			this.request = request;
			this.operation = operation;
		}

		public boolean isOperation() {
			return operation != null;
		}

		public boolean isStorable() {
			return request.isStorable();
		}

		public boolean isSafe() {
			return request.isSafe();
		}

		public long getReceivedOn() {
			return request.getReceivedOn();
		}

		public void setTrigger(NHttpResponseTrigger trigger) {
			this.trigger = trigger;
			if (child != null) {
				child.setTrigger(trigger);
			}
			if (trigger != null) {
				latch.countDown();
			}
		}

		public void run() {
			try {
				try {
					if (operation == null) {
						processRequest(request);
					} else {
						handleOperation(operation);
					}
				} catch (HttpException e) {
					latch.await(1, TimeUnit.SECONDS);
					if (trigger == null) {
						logger.error(e.toString(), e);
					} else {
						trigger.handleException(e);
					}
				} catch (IOException e) {
					latch.await(1, TimeUnit.SECONDS);
					if (trigger == null) {
						logger.error(e.toString(), e);
					} else {
						trigger.handleException(e);
					}
				}
			} catch (InterruptedException e) {
				logger.error(e.toString(), e);
			}
		}

		private void processRequest(Request req) throws HttpException,
				IOException, InterruptedException {
			HttpResponse resp = intercept(req);
			if (resp == null) {
				try {
					ResourceOperation op = new ResourceOperation(dataDir,
							request, repository);
					child = new Task(request, op);
					child.setTrigger(trigger);
					executor.execute(child);
				} catch (Exception e) {
					latch.await(1, TimeUnit.SECONDS);
					if (trigger == null) {
						logger.error(e.toString(), e);
					} else {
						try {
							resp = createHttpResponse(new Response().server(e));
							trigger.submitResponse(filter(req, resp));
						} catch (IOException e1) {
							trigger.handleException(e1);
						} catch (Exception e1) {
							trigger.handleException(new IOException(e1));
						}
					}
				}
			} else {
				latch.await();
				trigger.submitResponse(filter(req, resp));
			}
		}

		private void handleOperation(ResourceOperation operation)
				throws HttpException, IOException, InterruptedException {
			Response resp;
			try {
				resp = handle(operation);
			} finally {
				done = true;
				HttpEntity entity = operation.getEntity();
				if (entity != null) {
					entity.consumeContent();
				}
			}
			latch.await();
			trigger.submitResponse(filter(request, resp));
		}

		public boolean isDone() {
			return done;
		}

		@Override
		public String toString() {
			return request.toString();
		}
	}

	private final class Listener implements ContentListener {
		private final ErrorInputStream in;
		private final Task task;
		private final PipedOutputStream out;
		private ByteBuffer buf = ByteBuffer.allocate(1024);

		private Listener(ErrorInputStream in, Task task, PipedOutputStream out) {
			this.in = in;
			this.task = task;
			this.out = out;
		}

		public void contentAvailable(ContentDecoder decoder, IOControl ioctrl)
				throws IOException {
			try {
				decoder.read(buf);
				if (!task.isDone() && out != null) {
					int p = buf.position();
					out.write(buf.array(), buf.arrayOffset(), p);
				}
				buf.clear();
				if (decoder.isCompleted() && out != null) {
					out.close();
				}
			} catch (IOException e) {
				in.error(e);
				throw e;
			}
		}

		public void finished() {
			try {
				if (out != null) {
					out.close();
				}
			} catch (IOException e) {
				in.error(e);
			}
		}
	}

	private static final Executor executor;
	static {
		int n = Runtime.getRuntime().availableProcessors();
		Comparator<Runnable> cmp = new Comparator<Runnable>() {
			public int compare(Runnable o1, Runnable o2) {
				if (!(o1 instanceof Task) || !(o2 instanceof Task))
					return 0;
				Task t1 = (Task) o1;
				Task t2 = (Task) o2;
				if (!t1.isOperation() && t2.isOperation())
					return -1;
				if (t1.isOperation() && !t2.isOperation())
					return 1;
				if (t1.isSafe() && !t2.isSafe())
					return -1;
				if (!t1.isSafe() && t2.isSafe())
					return 1;
				if (t1.isStorable() && !t2.isStorable())
					return -1;
				if (!t1.isStorable() && t2.isStorable())
					return 1;
				if (t1.getReceivedOn() < t2.getReceivedOn())
					return -1;
				if (t1.getReceivedOn() > t2.getReceivedOn())
					return 1;
				return System.identityHashCode(t1)
						- System.identityHashCode(t2);
			};
		};
		BlockingQueue<Runnable> queue = new PriorityBlockingQueue<Runnable>(
				n * 10, cmp);
		executor = new ThreadPoolExecutor(n, n * 5, 60L, TimeUnit.SECONDS,
				queue, new NamedThreadFactory("HTTP Handler"));
	}

	private Logger logger = LoggerFactory
			.getLogger(HTTPObjectRequestHandler.class);
	private Filter filter;
	private Handler handler;
	private ObjectRepository repository;
	private File dataDir;
	private FileLockManager locks = new FileLockManager();

	public HTTPObjectRequestHandler(Filter filter, Handler handler,
			ObjectRepository repository, File dataDir) {
		this.filter = filter;
		this.handler = handler;
		this.repository = repository;
		this.dataDir = dataDir;
	}

	public void verify(HttpRequest request, HttpResponse response,
			HttpContext context) throws HttpException {
		// TODO Auto-generated method stub

	}

	public ConsumingNHttpEntity entityRequest(
			HttpEntityEnclosingRequest request, HttpContext context)
			throws HttpException, IOException {
		PipedOutputStream out = new PipedOutputStream();
		ErrorInputStream in = new ErrorInputStream(out);
		Task handler = new Task(process(request, in));
		context.setAttribute(HANDLER_ATTR, handler);
		executor.execute(handler);
		ContentListener listener = new Listener(in, handler, out);
		return new ConsumingNHttpEntityTemplate(request.getEntity(), listener);
	}

	public void handle(HttpRequest request, HttpResponse response,
			NHttpResponseTrigger trigger, HttpContext context)
			throws HttpException, IOException {
		Task handler = (Task) context.getAttribute(HANDLER_ATTR);
		if (handler == null) {
			Task task = new Task(process(request, null));
			task.setTrigger(trigger);
			executor.execute(task);
		} else {
			context.removeAttribute(HANDLER_ATTR);
			handler.setTrigger(trigger);
		}
	}

	private Request process(HttpRequest request, InputStream in)
			throws IOException {
		Request req = new Request(request);
		if (in == null) {
			req.setEntity(null);
		} else {
			String type = req.getHeader("Content-Type");
			String length = req.getHeader("Content-Length");
			long size = -1;
			if (length != null) {
				size = Long.parseLong(length);
			}
			req.setEntity(new InputStreamHttpEntity(type, size, in));
		}
		return filter.filter(req);
	}

	private HttpResponse intercept(Request request) throws HttpException,
			IOException {
		return filter.intercept(request);
	}

	private Response handle(final ResourceOperation req) throws HttpException,
			IOException {
		try {
			boolean close = true;
			try {
				req.init();
				final Lock lock = createFileLock(req.getMethod(), req.getFile());
				try {
					Response resp = handler.handle(req);
					if (req.isSafe()) {
						req.rollback();
					} else {
						req.commit();
					}
					if (resp.isContent() && !resp.isException()
							&& !resp.isHead()) {
						resp.onClose(new Runnable() {
							public void run() {
								try {
									req.close();
								} catch (RepositoryException e) {
									logger.error(e.toString(), e);
								}
							}
						});
						close = false;
					}
					return resp;
				} finally {
					if (lock != null) {
						lock.release();
					}
				}
			} finally {
				if (close) {
					req.close();
				}
			}
		} catch (HttpException e) {
			throw e;
		} catch (IOException e) {
			throw e;
		} catch (ResponseException e) {
			return new Response().exception(e);
		} catch (Exception e) {
			return new Response().exception(new InternalServerError(e));
		}
	}

	private HttpResponse filter(Request request, Response resp)
			throws IOException {
		HttpResponse response;
		try {
			try {
				response = createHttpResponse(resp);
			} catch (ResponseException e) {
				response = createHttpResponse(new Response().exception(e));
			} catch (ConcurrencyException e) {
				response = createHttpResponse(new Response().conflict(e));
			} catch (MimeTypeParseException e) {
				response = createHttpResponse(new Response().status(406,
						"Not Acceptable"));
			} catch (Exception e) {
				logger.error(e.toString(), e);
				response = createHttpResponse(new Response().server(e));
			}
		} catch (Exception e) {
			logger.error(e.toString(), e);
			ProtocolVersion ver = new ProtocolVersion("HTTP", 1, 1);
			response = new BasicHttpResponse(ver, 500, "Internal Server Error");
		}
		return filter(request, response);
	}

	private HttpResponse filter(Request request, HttpResponse response)
			throws IOException {
		return filter.filter(request, response);
	}

	private HttpResponse createHttpResponse(Response resp) throws IOException,
			OpenRDFException, XMLStreamException, TransformerException,
			ParserConfigurationException, MimeTypeParseException {
		ProtocolVersion ver = new ProtocolVersion("HTTP", 1, 1);
		int code = resp.getStatus();
		String phrase = resp.getMessage();
		HttpResponse response = new BasicHttpResponse(ver, code, phrase);
		for (Header hd : resp.getAllHeaders()) {
			response.addHeader(hd);
		}
		if (resp.isException()) {
			String type = "text/plain;charset=UTF-8";
			response.setHeader("Content-Type", type);
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			Writer writer = new OutputStreamWriter(out, "UTF-8");
			PrintWriter print = new PrintWriter(writer);
			try {
				resp.getException().printTo(print);
			} finally {
				print.close();
			}
			byte[] body = out.toByteArray();
			int size = body.length;
			response.setHeader("Content-Length", String.valueOf(size));
			ByteArrayInputStream in = new ByteArrayInputStream(body);
			List<Runnable> onClose = resp.getOnClose();
			response.setEntity(new InputStreamHttpEntity(type, size, in,
					onClose));
		} else if (resp.isContent()) {
			String type = resp.getHeader("Content-Type");
			MimeType mediaType = new MimeType(type);
			Charset charset = getCharset(mediaType);
			long size = resp.getSize(type, charset);
			if (size >= 0) {
				response.setHeader("Content-Length", String.valueOf(size));
			} else {
				response.setHeader("Transfer-Encoding", "chunked");
			}
			InputStream in = resp.write(type, charset);
			List<Runnable> onClose = resp.getOnClose();
			response.setEntity(new InputStreamHttpEntity(type, size, in,
					onClose));
		}
		return response;
	}

	private Lock createFileLock(String method, File file)
			throws InterruptedException {
		if (!method.equals("PUT") && (file == null || !file.exists()))
			return null;
		boolean shared = method.equals("GET") || method.equals("HEAD")
				|| method.equals("OPTIONS") || method.equals("TRACE")
				|| method.equals("POST") || method.equals("PROPFIND");
		return locks.lock(file, shared);
	}

	private Charset getCharset(MimeType m) {
		if (m == null)
			return null;
		String name = m.getParameters().get("charset");
		if (name == null)
			return null;
		return Charset.forName(name);
	}

}
