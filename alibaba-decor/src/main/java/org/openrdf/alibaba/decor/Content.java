package org.openrdf.alibaba.decor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

public interface Content {
	public abstract Locale getLocale();

	public abstract String getContentType();

	public abstract InputStream getInputStream() throws IOException;

	public abstract BufferedReader getReader() throws IOException;
}
