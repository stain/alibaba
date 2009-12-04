package org.openrdf.http.object.filters;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletInputStream;

public class InputServletStream extends ServletInputStream {
	private InputStream in;

	public InputServletStream(InputStream out) {
		this.in = out;
	}

	public int available() throws IOException {
		return in.available();
	}

	public void close() throws IOException {
		in.close();
	}

	public void mark(int readlimit) {
		in.mark(readlimit);
	}

	public boolean markSupported() {
		return in.markSupported();
	}

	public int read() throws IOException {
		return in.read();
	}

	public int read(byte[] b, int off, int len) throws IOException {
		return in.read(b, off, len);
	}

	public int read(byte[] b) throws IOException {
		return in.read(b);
	}

	public void reset() throws IOException {
		in.reset();
	}

	public long skip(long n) throws IOException {
		return in.skip(n);
	}

	public String toString() {
		return in.toString();
	}

}
