package org.openrdf.alibaba.exceptions;

/**
 * The server encountered an unexpected condition which prevented it from
 * fulfilling the request.
 * 
 * @author James Leigh
 * 
 */
public class InternalServerErrorException extends AlibabaException {
	private static final long serialVersionUID = 7336578806574261339L;

	public InternalServerErrorException() {
		super();
	}

	public InternalServerErrorException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	public InternalServerErrorException(String arg0) {
		super(arg0);
	}

	public InternalServerErrorException(Throwable arg0) {
		super(arg0);
	}

	@Override
	public int getErrorCode() {
		return 500;
	}

}
