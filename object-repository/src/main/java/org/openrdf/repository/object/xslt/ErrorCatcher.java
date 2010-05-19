/**
 * 
 */
package org.openrdf.repository.object.xslt;

import java.io.IOException;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.TransformerException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ErrorCatcher implements ErrorListener {
	private Logger logger = LoggerFactory.getLogger(ErrorCatcher.class);
	private String target;
	private TransformerException error;
	private TransformerException fatal;
	private IOException io;

	public ErrorCatcher(String target) {
		if (target == null) {
			target = "";
		}
		this.target = target;
	}

	public boolean isFatal() {
		return fatal != null;
	}

	public TransformerException getFatalError() {
		return fatal;
	}

	public boolean isIOException() {
		return io != null;
	}

	public IOException getIOException() {
		return new IOException(io);
	}

	public void ioException(IOException exception) {
		if (io == null) {
			io = exception;
		}
		logger.info(exception.toString(), exception);
	}

	public void error(TransformerException ex) {
		logger.warn("{} in {}", ex.getMessageAndLocation(), target);
		if (error != null && ex.getCause() == null) {
			ex.initCause(error);
		}
		error = ex;
	}

	public void fatalError(TransformerException ex) {
		logger.error("{} in {}", ex.getMessageAndLocation(), target);
		if (error != null && ex.getCause() == null) {
			ex.initCause(error);
		}
		if (fatal == null) {
			fatal = ex;
		}
	}

	public void warning(TransformerException exception) {
		logger.info(exception.toString(), exception);
	}
}