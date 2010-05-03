package org.openrdf.http.object.tasks;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.nio.protocol.NHttpResponseTrigger;
import org.openrdf.OpenRDFException;
import org.openrdf.http.object.exceptions.ResponseException;
import org.openrdf.http.object.model.Filter;
import org.openrdf.http.object.model.ReadableHttpEntityChannel;
import org.openrdf.http.object.model.Request;
import org.openrdf.http.object.model.ResourceOperation;
import org.openrdf.http.object.model.Response;
import org.openrdf.http.object.util.ChannelUtil;
import org.openrdf.sail.optimistic.exceptions.ConcurrencyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Task implements Runnable {
	private Logger logger = LoggerFactory.getLogger(Task.class);
	private Executor executor;
	private final Request req;
	private NHttpResponseTrigger trigger;
	private Task child;
	private volatile boolean done;
	private HttpException http;
	private IOException io;
	private HttpResponse resp;
	private Filter filter;

	public Task(Request request, Filter filter) {
		assert request != null;
		assert !(request instanceof ResourceOperation);
		this.req = request;
		this.filter = filter;
	}

	public final boolean isStorable() {
		return req.isStorable();
	}

	public final boolean isSafe() {
		return req.isSafe();
	}

	public final long getReceivedOn() {
		return req.getReceivedOn();
	}

	public synchronized void setTrigger(NHttpResponseTrigger trigger) {
		this.trigger = trigger;
		if (http != null) {
			trigger.handleException(http);
		} else if (io != null) {
			trigger.handleException(io);
		} else if (resp != null) {
			trigger.submitResponse(resp);
		}
		if (child != null) {
			child.setTrigger(trigger);
		}
	}

	public void run() {
		try {
			perform();
		} catch (HttpException e) {
			handleException(e);
		} catch (IOException e) {
			handleException(e);
		} catch (ResponseException e) {
			submitException(e);
		} catch (Exception e) {
			submitException(e);
		} catch (Error e) {
			abort();
			throw e;
		} finally {
			if (child == null) {
				done = true;
			}
		}
	}

	public abstract int getGeneration();

	abstract void perform() throws Exception;

	public synchronized <T extends Task> T bear(T child) {
		assert this != child;
		this.child = child;
		if (trigger != null) {
			child.setTrigger(trigger);
		}
		assert executor != null;
		child.go(executor);
		return child;
	}

	public void go(Executor executor) {
		this.executor = executor;
		executor.execute(this);
	}

	public boolean isDone() {
		if (child == null)
			return done;
		return child.isDone();
	}

	public void awaitVerification(long time, TimeUnit unit) throws InterruptedException {
		if (child != null) {
			child.awaitVerification(time, unit);
		}
	}

	public void abort() {
		close();
	}

	public void close() {
		try {
			HttpEntity entity = req.getEntity();
			if (entity != null) {
				entity.consumeContent();
			}
		} catch (IOException e) {
			logger.error(e.toString(), e);
		}
	}

	public HttpResponse getHttpResponse() throws HttpException, IOException {
		if (child != null)
			return child.getHttpResponse();
		if (io != null)
			throw io;
		if (http != null)
			throw http;
		return resp;
	}

	public void submitResponse(Response resp) throws Exception {
		HttpResponse response;
		try {
			try {
				response = createHttpResponse(req, resp);
			} catch (ResponseException e) {
				response = createHttpResponse(req, new Response().exception(e));
			} catch (ConcurrencyException e) {
				response = createHttpResponse(req, new Response().conflict(e));
			} catch (MimeTypeParseException e) {
				response = createHttpResponse(req, new Response().status(406,
						"Not Acceptable"));
			} catch (Exception e) {
				logger.error(e.toString(), e);
				response = createHttpResponse(req, new Response().server(e));
			}
		} catch (Exception e) {
			logger.error(e.toString(), e);
			ProtocolVersion ver = new ProtocolVersion("HTTP", 1, 1);
			response = new BasicHttpResponse(ver, 500, "Internal Server Error");
		}
		submitResponse(response);
	}

	public void submitResponse(HttpResponse response) throws IOException {
		try {
			HttpResponse resp = filter(req, response);
			HttpEntity entity = resp.getEntity();
			if ("HEAD".equals(req.getMethod()) && entity != null) {
				entity.consumeContent();
				resp.setEntity(null);
			}
			triggerResponse(resp);
		} catch (RuntimeException e) {
			handleException(new IOException(e));
		} catch (IOException e) {
			handleException(e);
		}
	}

	@Override
	public String toString() {
		return req.toString();
	}

	private synchronized void triggerResponse(HttpResponse response) {
		resp = response;
		try {
			close();
		} finally {
			if (trigger != null) {
				trigger.submitResponse(resp);
			}
		}
	}

	private void submitException(ResponseException e) {
		try {
			submitResponse(createHttpResponse(req, new Response().exception(e)));
		} catch (IOException e1) {
			handleException(e1);
		} catch (Exception e1) {
			handleException(new IOException(e1));
		}
	}

	private void submitException(Exception e) {
		try {
			submitResponse(createHttpResponse(req, new Response().server(e)));
		} catch (IOException e1) {
			handleException(e1);
		} catch (Exception e1) {
			handleException(new IOException(e1));
		}
	}

	private synchronized void handleException(HttpException ex) {
		http = ex;
		try {
			abort();
		} finally {
			if (trigger != null) {
				trigger.handleException(ex);
			}
		}
	}

	private synchronized void handleException(IOException ex) {
		io = ex;
		try {
			abort();
		} finally {
			if (trigger != null) {
				trigger.handleException(ex);
			}
		}
	}

	private HttpResponse filter(Request request, HttpResponse response)
			throws IOException {
		return filter.filter(request, response);
	}

	private HttpResponse createHttpResponse(Request req, Response resp)
			throws IOException, OpenRDFException, XMLStreamException,
			TransformerException, ParserConfigurationException,
			MimeTypeParseException {
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
			ReadableByteChannel in = ChannelUtil.newChannel(body);
			List<Runnable> onClose = resp.getOnClose();
			HttpEntity entity = new ReadableHttpEntityChannel(type, size, in,
					onClose);
			response.setEntity(entity);
		} else if (resp.isContent()) {
			String type = resp.getHeader("Content-Type");
			Charset charset = getCharset(type);
			long size = resp.getSize(type, charset);
			if (size >= 0) {
				response.setHeader("Content-Length", String.valueOf(size));
			} else if (!response.containsHeader("Content-Length")) {
				response.setHeader("Transfer-Encoding", "chunked");
			}
			ReadableByteChannel in = resp.write(type, charset);
			List<Runnable> onClose = resp.getOnClose();
			HttpEntity entity = new ReadableHttpEntityChannel(type, size, in,
					onClose);
			response.setEntity(entity);
		} else {
			response.setHeader("Content-Length", "0");
		}
		return response;
	}

	private Charset getCharset(String type) {
		if (type == null)
			return null;
		try {
			MimeType m = new MimeType(type);
			String name = m.getParameters().get("charset");
			if (name == null)
				return null;
			return Charset.forName(name);
		} catch (MimeTypeParseException e) {
			logger.debug(e.toString(), e);
			return null;
		}
	}
}