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
package org.openrdf.http.object.writers;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;

import org.openrdf.repository.object.ObjectFactory;

/**
 * Converts an OutputStream into a ReadableByteChannel.
 */
public class ReadableByteChannelBodyWriter implements
		MessageBodyWriter<ReadableByteChannel> {

	public boolean isWriteable(String mimeType, Class<?> type,
			Type genericType, ObjectFactory of) {
		if (!ReadableByteChannel.class.isAssignableFrom(type))
			return false;
		if (mimeType != null && mimeType.contains("*")
				&& !mimeType.startsWith("*")
				&& !mimeType.startsWith("application/*"))
			return false;
		return true;
	}

	public long getSize(String mimeType, Class<?> type, Type genericType,
			ObjectFactory of, ReadableByteChannel t, Charset charset) {
		return -1;
	}

	public String getContentType(String mimeType, Class<?> type,
			Type genericType, ObjectFactory of, Charset charset) {
		if (mimeType == null || mimeType.startsWith("*")
				|| mimeType.startsWith("application/*"))
			return "application/octet-stream";
		return mimeType;
	}

	public void writeTo(String mimeType, Class<?> type, Type genericType,
			ObjectFactory of, ReadableByteChannel result, String base,
			Charset charset, OutputStream out, int bufSize) throws IOException {
		ByteBuffer buf = ByteBuffer.allocate(bufSize);
		while (result.read(buf) >= 0) {
			buf.flip();
			out.write(buf.array(), buf.position(), buf.limit());
			buf.clear();
		}
	}
}
