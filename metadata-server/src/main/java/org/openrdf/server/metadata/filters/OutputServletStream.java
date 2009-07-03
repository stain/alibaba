package org.openrdf.server.metadata.filters;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.ServletOutputStream;

public class OutputServletStream extends ServletOutputStream {
	private OutputStream out;

	public OutputServletStream(OutputStream out) {
		this.out = out;
	}

	public void close() throws IOException {
		out.close();
	}

	public boolean equals(Object obj) {
		return out.equals(obj);
	}

	public void flush() throws IOException {
		out.flush();
	}

	public int hashCode() {
		return out.hashCode();
	}

	public String toString() {
		return out.toString();
	}

	public void write(byte[] b, int off, int len) throws IOException {
		out.write(b, off, len);
	}

	public void write(byte[] b) throws IOException {
		out.write(b);
	}

	public void write(int b) throws IOException {
		out.write(b);
	}

}
