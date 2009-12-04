package org.openrdf.server.metadata.cache;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.codec.binary.Base64;

public class ContentMD5Stream extends OutputStream {
	private final OutputStream delegate;
	private final MessageDigest digest;

	public ContentMD5Stream(OutputStream delegate) throws NoSuchAlgorithmException {
		this.delegate = delegate;
		digest = MessageDigest.getInstance("MD5");
	}

	public String getContentMD5() throws UnsupportedEncodingException {
		byte[] hash = Base64.encodeBase64(digest.digest());
		return new String(hash, "UTF-8");
	}

	public void close() throws IOException {
		delegate.close();
	}

	public void flush() throws IOException {
		delegate.flush();
	}

	public String toString() {
		try {
			return getContentMD5();
		} catch (UnsupportedEncodingException e) {
			return delegate.toString();
		}
	}

	public void write(byte[] b, int off, int len) throws IOException {
		delegate.write(b, off, len);
		digest.update(b, off, len);
	}

	public void write(byte[] b) throws IOException {
		delegate.write(b);
		digest.update(b);
	}

	public void write(int b) throws IOException {
		delegate.write(b);
		digest.update((byte) b);
	}

}
