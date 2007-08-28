package org.openrdf.alibaba.exceptions;

public class NotFoundException extends AlibabaException {
	private static final long serialVersionUID = -4038056223006620519L;

	public NotFoundException() {
		super();
	}

	public NotFoundException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	public NotFoundException(String arg0) {
		super(arg0);
	}

	public NotFoundException(Throwable arg0) {
		super(arg0);
	}

	@Override
	public int getErrorCode() {
		return 404;
	}

}
