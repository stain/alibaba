package org.openrdf.alibaba.exceptions;

public class RequestURITooLongException extends AlibabaException {
	private static final long serialVersionUID = 4723144695460817707L;

	public RequestURITooLongException() {
		super();
	}

	public RequestURITooLongException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	public RequestURITooLongException(String arg0) {
		super(arg0);
	}

	public RequestURITooLongException(Throwable arg0) {
		super(arg0);
	}

	@Override
	public int getErrorCode() {
		return 414;
	}

}
