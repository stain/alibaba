package org.openrdf.alibaba.exceptions;

/**
 * The method specified in the Request-Line is not allowed for the resource
 * identified by the Request-URI. The response MUST include an Allow header
 * containing a list of valid methods for the requested resource.
 * 
 * @author James Leigh
 * 
 */
public class MethodNotAllowedException extends AlibabaException {
	private static final long serialVersionUID = 7240254255442474788L;

	public MethodNotAllowedException() {
		super();
	}

	public MethodNotAllowedException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	public MethodNotAllowedException(String arg0) {
		super(arg0);
	}

	public MethodNotAllowedException(Throwable arg0) {
		super(arg0);
	}

	@Override
	public int getErrorCode() {
		return 405;
	}

}
