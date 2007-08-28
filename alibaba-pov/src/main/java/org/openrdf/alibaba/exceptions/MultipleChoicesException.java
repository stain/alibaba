package org.openrdf.alibaba.exceptions;

public class MultipleChoicesException extends AlibabaException {
	private static final long serialVersionUID = -4108293367125291440L;

	public MultipleChoicesException() {
		super();
	}

	public MultipleChoicesException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	public MultipleChoicesException(String arg0) {
		super(arg0);
	}

	public MultipleChoicesException(Throwable arg0) {
		super(arg0);
	}

	@Override
	public int getErrorCode() {
		return 300;
	}

}
