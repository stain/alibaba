package org.openrdf.alibaba.exceptions;

public class NotImplementedException extends AlibabaException {
	private static final long serialVersionUID = -91003010725669482L;

	public NotImplementedException() {
		super();
	}

	public NotImplementedException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	public NotImplementedException(String arg0) {
		super(arg0);
	}

	public NotImplementedException(Throwable arg0) {
		super(arg0);
	}

	@Override
	public int getErrorCode() {
		return 501;
	}

}
