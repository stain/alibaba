package org.openrdf.repository.object.exceptions;

import org.openrdf.repository.RepositoryException;

public class ObjectCompileException extends RepositoryException {
	private static final long serialVersionUID = -1504254095791564613L;

	public ObjectCompileException(String msg, Throwable t) {
		super(msg, t);
	}

	public ObjectCompileException(String msg) {
		super(msg);
	}

	public ObjectCompileException(Throwable t) {
		super(t);
	}

}
