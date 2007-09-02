package org.openrdf.alibaba.exceptions;

import java.io.PrintWriter;

public class AlibabaException extends Exception {
	private static final long serialVersionUID = -29297097148902079L;

	public int getErrorCode() {
		return -1;
	}

	protected AlibabaException() {
		super();
	}

	protected AlibabaException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	protected AlibabaException(String arg0) {
		super(arg0);
	}

	protected AlibabaException(Throwable arg0) {
		super(arg0);
	}

	public String getContentType() {
		return "text/plain";
	}

	public void printContent(PrintWriter out) {
		printStackTrace(out);
	}
}
