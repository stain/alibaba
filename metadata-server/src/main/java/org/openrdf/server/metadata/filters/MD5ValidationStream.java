package org.openrdf.server.metadata.filters;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.codec.binary.Base64;

public class MD5ValidationStream extends InputStream {
	private InputStream delegate;
	private String md5;
	private MessageDigest digest;
	private boolean closed;

	public MD5ValidationStream(InputStream delegate, String md5)
			throws NoSuchAlgorithmException {
		this.delegate = delegate;
		this.md5 = md5;
		digest = MessageDigest.getInstance("MD5");
	}

	public int available() throws IOException {
		return delegate.available();
	}

	public void close() throws IOException {
		if (!closed) {
			closed = true;
			delegate.close();
			byte[] hash = Base64.encodeBase64(digest.digest());
			if (!md5.equals(new String(hash, "UTF-8"))) {
				throw new IOException(
						"Content-MD5 header does not match message body");
			}
		}
	}

	public int read() throws IOException {
		int read = delegate.read();
		if (read != -1) {
			digest.update((byte) read);
		}
		return read;
	}

	public int read(byte[] b, int off, int len) throws IOException {
		int read = delegate.read(b, off, len);
		if (read > 0) {
			digest.update(b, off, read);
		}
		return read;
	}

	public int read(byte[] b) throws IOException {
		int read = delegate.read(b);
		if (read > 0) {
			digest.update(b, 0, read);
		}
		return read;
	}

	public String toString() {
		return delegate.toString();
	}

}
