package org.openrdf.alibaba.servlet;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Locale;

public interface Response {
	public abstract Locale getLocale();

	public abstract void setLocale(Locale locale);

	public abstract String[] getAcceptedTypes();

	public abstract void setContentType(String contentType);

	public abstract OutputStream getOutputStream() throws IOException;

	public abstract PrintWriter getWriter() throws IOException;
}
