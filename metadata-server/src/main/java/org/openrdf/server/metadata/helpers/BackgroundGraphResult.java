package org.openrdf.server.metadata.helpers;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.UndeclaredThrowableException;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import org.openrdf.cursor.QueueCursor;
import org.openrdf.model.Statement;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;

public class BackgroundGraphResult implements
		GraphQueryResult, Runnable, RDFHandler, Closeable {
	private volatile boolean closed;
	private volatile Thread parserThread;
	private RDFParser parser;
	private Charset charset;
	private InputStream in;
	private String baseURI;
	private CountDownLatch namespacesReady = new CountDownLatch(1);
	private Map<String, String> namespaces = new ConcurrentHashMap<String, String>();
	private QueueCursor<Statement> queue;

	public BackgroundGraphResult(RDFParser parser, InputStream in,
			Charset charset, String baseURI) {
		this(new QueueCursor<Statement>(10), parser, in, charset, baseURI);
	}

	public BackgroundGraphResult(QueueCursor<Statement> queue,
			RDFParser parser, InputStream in, Charset charset, String baseURI) {
		this.queue = queue;
		this.parser = parser;
		this.in = in;
		this.charset = charset;
		this.baseURI = baseURI;
	}

	public boolean hasNext() throws QueryEvaluationException {
		return queue.hasNext();
	}

	public Statement next() throws QueryEvaluationException {
		return queue.next();
	}

	public void remove() throws QueryEvaluationException {
		queue.remove();
	}

	public void close() {
		closed = true;
		if (parserThread != null) {
			parserThread.interrupt();
		}
		try {
			queue.close();
			in.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (QueryEvaluationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void run() {
		parserThread = Thread.currentThread();
		try {
			parser.setRDFHandler(this);
			if (charset == null) {
				parser.parse(in, baseURI);
			} else {
				parser.parse(new InputStreamReader(in, charset), baseURI);
			}
		} catch (RDFHandlerException e) {
			// parsing was cancelled or interrupted
		} catch (RDFParseException e) {
			queue.toss(e);
		} catch (IOException e) {
			queue.toss(e);
		} finally {
			parserThread = null;
			queue.done();
		}
	}

	public void startRDF() throws RDFHandlerException {
		// no-op
	}

	public Map<String, String> getNamespaces() {
		try {
			namespacesReady.await();
			return namespaces;
		} catch (InterruptedException e) {
			throw new UndeclaredThrowableException(e);
		}
	}

	public void handleComment(String comment) throws RDFHandlerException {
		// ignore
	}

	public void handleNamespace(String prefix, String uri)
			throws RDFHandlerException {
		namespaces.put(prefix, uri);
	}

	public void handleStatement(Statement st) throws RDFHandlerException {
		namespacesReady.countDown();
		if (closed)
			throw new RDFHandlerException("Result closed");
		try {
			queue.put(st);
		} catch (InterruptedException e) {
			throw new RDFHandlerException(e);
		}
	}

	public void endRDF() throws RDFHandlerException {
		namespacesReady.countDown();
	}

}
