package org.openrdf.alibaba.exceptions;

public class UnsupportedMediaTypeException extends AlibabaException {
	private static final long serialVersionUID = 2232843678414873104L;

	public UnsupportedMediaTypeException() {
		super();
	}

	public UnsupportedMediaTypeException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	public UnsupportedMediaTypeException(String arg0) {
		super(arg0);
	}

	public UnsupportedMediaTypeException(Throwable arg0) {
		super(arg0);
	}

	@Override
	public int getErrorCode() {
		return 415;
	}

}
