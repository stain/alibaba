package org.openrdf.alibaba.exceptions;

public class RequestEntityTooLargeException extends AlibabaException {
	private static final long serialVersionUID = 4246715861428006202L;

	public RequestEntityTooLargeException() {
		super();
	}

	public RequestEntityTooLargeException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	public RequestEntityTooLargeException(String arg0) {
		super(arg0);
	}

	public RequestEntityTooLargeException(Throwable arg0) {
		super(arg0);
	}

	@Override
	public int getErrorCode() {
		return 413;
	}

}
