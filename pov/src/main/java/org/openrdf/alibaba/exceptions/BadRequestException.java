package org.openrdf.alibaba.exceptions;

/**
 * The request could not be understood by the server due to malformed syntax.
 * 
 * @author James Leigh
 * 
 */
public class BadRequestException extends AlibabaException {
	private static final long serialVersionUID = -728717747069944256L;

	public BadRequestException() {
		super();
	}

	public BadRequestException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	public BadRequestException(String arg0) {
		super(arg0);
	}

	public BadRequestException(Throwable arg0) {
		super(arg0);
	}

	@Override
	public int getErrorCode() {
		return 400;
	}

}
