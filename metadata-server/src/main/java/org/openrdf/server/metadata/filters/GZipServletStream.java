package org.openrdf.server.metadata.filters;

import java.io.IOException;
import java.util.zip.GZIPOutputStream;

import javax.servlet.ServletOutputStream;

public class GZipServletStream extends ServletOutputStream {
	private GZIPOutputStream out;

	public GZipServletStream(ServletOutputStream out) throws IOException {
		this.out = new GZIPOutputStream(out);
	}

	public void close() throws IOException {
		out.close();
	}

	public boolean equals(Object obj) {
		return out.equals(obj);
	}

	public void finish() throws IOException {
		out.finish();
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

	public void write(byte[] buf, int off, int len) throws IOException {
		out.write(buf, off, len);
	}

	public void write(byte[] b) throws IOException {
		out.write(b);
	}

	public void write(int b) throws IOException {
		out.write(b);
	}

}
