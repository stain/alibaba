package org.openrdf.sail.optimistic.exceptions;

import org.openrdf.sail.SailException;


public class ConcurrencySailException extends SailException {
	private static final long serialVersionUID = 6505874891312495635L;

	public ConcurrencySailException() {
		super();
	}

	public ConcurrencySailException(String msg, Throwable t) {
		super(msg, t);
	}

	public ConcurrencySailException(String msg) {
		super(msg);
	}

	public ConcurrencySailException(Throwable t) {
		super(t);
	}

}
