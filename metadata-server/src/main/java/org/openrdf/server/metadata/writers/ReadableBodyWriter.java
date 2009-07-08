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
package org.openrdf.server.metadata.writers;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

import org.openrdf.repository.object.ObjectFactory;

public class ReadableBodyWriter implements MessageBodyWriter<Readable> {

	public boolean isWriteable(String mimeType, Class<?> type, ObjectFactory of) {
		if (!Readable.class.isAssignableFrom(type))
			return false;
		return mimeType.startsWith("text/") || mimeType.startsWith("*");
	}

	public long getSize(String mimeType, Class<?> type, ObjectFactory of,
			Readable t) {
		return -1;
	}

	public String getContentType(String mimeType, Class<?> type,
			ObjectFactory of, Charset charset) {
		if (charset == null) {
			charset = Charset.forName("UTF-8");
		}
		if (mimeType.startsWith("*")) {
			mimeType = "text/plain";
		}
		return mimeType + ";charset=" + charset.name();
	}

	public void writeTo(String mimeType, Class<?> type, ObjectFactory of,
			Readable result, String base, Charset charset, OutputStream out,
			int bufSize) throws IOException {
		if (charset == null) {
			charset = Charset.forName("UTF-8");
		}
		Writer writer = new OutputStreamWriter(out, charset);
		CharBuffer cb = CharBuffer.allocate(bufSize);
		while (result.read(cb) >= 0) {
			cb.flip();
			writer.write(cb.array(), cb.position(), cb.limit());
			cb.clear();
		}
		writer.flush();
	}
}
