package org.openrdf.alibaba.exceptions;

/**
 * The server is refusing to service the request because the entity of the
 * request is in a format not supported by the requested resource for the
 * requested method.
 * 
 * @author James Leigh
 * 
 */
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
