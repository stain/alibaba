/*
 * Copyright (c) 2009, James Leigh All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution. 
 * - Neither the name of the openrdf.org nor the names of its contributors may
 *   be used to endorse or promote products derived from this software without
 *   specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 */
package org.openrdf.http.object.filters;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.codec.binary.Base64;

/**
 * Calculates the MD5 sum of the stream as it is written.
 */
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
