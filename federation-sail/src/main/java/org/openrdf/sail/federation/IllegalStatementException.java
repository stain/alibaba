package org.openrdf.sail.federation;

import org.openrdf.sail.SailException;

public class IllegalStatementException extends SailException {

	private static final long serialVersionUID = 3653794267690820761L;

	public IllegalStatementException(String string) {
		super(string);
	}

}
