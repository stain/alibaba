package org.openrdf.alibaba.exceptions;

public class NotAcceptableException extends AlibabaException {
	private static final long serialVersionUID = 793537007180540987L;

	public NotAcceptableException() {
		super();
	}

	public NotAcceptableException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	public NotAcceptableException(String arg0) {
		super(arg0);
	}

	public NotAcceptableException(Throwable arg0) {
		super(arg0);
	}

	@Override
	public int getErrorCode() {
		return 406;
	}

}
