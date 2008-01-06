package org.openrdf.alibaba.exceptions;

/**
 * The server has not found anything matching the Request-URI. No indication is
 * given of whether the condition is temporary or permanent. The 410 (Gone)
 * status code SHOULD be used if the server knows, through some internally
 * configurable mechanism, that an old resource is permanently unavailable and
 * has no forwarding address. This status code is commonly used when the server
 * does not wish to reveal exactly why the request has been refused, or when no
 * other response is applicable.
 * 
 * @author James Leigh
 * 
 */
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
