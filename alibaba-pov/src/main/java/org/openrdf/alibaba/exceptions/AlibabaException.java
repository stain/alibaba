package org.openrdf.alibaba.exceptions;

import java.io.PrintWriter;

public abstract class AlibabaException extends Exception {
	public abstract int getErrorCode();

	public AlibabaException() {
		super();
	}

	public AlibabaException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	public AlibabaException(String arg0) {
		super(arg0);
	}

	public AlibabaException(Throwable arg0) {
		super(arg0);
	}

	public String getContentType() {
		return "text/plain";
	}

	public void printContent(PrintWriter out) {
		printStackTrace(out);
	}
}
