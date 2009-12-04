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
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Map;

import org.openrdf.repository.object.ObjectFactory;

/**
 * Writes a percent encoded form from a {@link Map}.
 * 
 * @author James Leigh
 * 
 */
public class FormMapMessageWriter implements
		MessageBodyWriter<Map<String, String[]>> {
	private final Type mapType;

	public FormMapMessageWriter() {
		ParameterizedType iface = (ParameterizedType) this.getClass()
				.getGenericInterfaces()[0];
		mapType = iface.getActualTypeArguments()[0];
	}

	public boolean isWriteable(String mimeType, Class<?> type,
			Type genericType, ObjectFactory of) {
		if (type != Map.class || type != genericType
				&& !mapType.equals(genericType))
			return false;
		return mimeType == null || mimeType.startsWith("*")
				|| mimeType.startsWith("application/*")
				|| mimeType.startsWith("application/x-www-form-urlencoded");
	}

	public long getSize(String mimeType, Class<?> type, Type genericType,
			ObjectFactory of, Map<String, String[]> map, Charset charset) {
		return -1;
	}

	public String getContentType(String mimeType, Class<?> type,
			Type genericType, ObjectFactory of, Charset charset) {
		return "application/x-www-form-urlencoded";
	}

	public void writeTo(String mimeType, Class<?> type, Type genericType,
			ObjectFactory of, Map<String, String[]> result, String base,
			Charset charset, OutputStream out, int bufSize) throws IOException {
		if (charset == null) {
			charset = Charset.forName("ISO-8859-1");
		}
		Writer writer = new OutputStreamWriter(out, charset);
		try {
			if (result == null)
				return;
			boolean first = true;
			for (Map.Entry<String, String[]> e : result.entrySet()) {
				if (e.getKey() != null) {
					String name = URLEncoder.encode(e.getKey(), "ISO-8859-1");
					if (e.getValue() == null || e.getValue().length < 1) {
						if (first) {
							first = false;
						} else {
							writer.append("&");
						}
						writer.append(name);
					} else {
						for (String value : e.getValue()) {
							if (first) {
								first = false;
							} else {
								writer.append("&");
							}
							writer.append(name).append("=").append(
									URLEncoder.encode(value, "ISO-8859-1"));
						}
					}
				}
			}
		} finally {
			writer.close();
		}
	}
}
