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
package org.openrdf.server.metadata.readers;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

import org.openrdf.repository.object.ObjectConnection;

/**
 * Readers a percent encoded form into a {@link Map}.
 * 
 * @author James Leigh
 * 
 */
public final class FormMapMessageReader implements
		MessageBodyReader<Map<String, String[]>> {

	private final Type mapType;
	private StringBodyReader delegate = new StringBodyReader();

	public FormMapMessageReader() {
		ParameterizedType iface = (ParameterizedType) this.getClass()
				.getGenericInterfaces()[0];
		mapType = iface.getActualTypeArguments()[0];
	}

	public boolean isReadable(Class<?> type, Type genericType, String mimeType,
			ObjectConnection con) {
		return mimeType != null
				&& mimeType.startsWith("application/x-www-form-urlencoded")
				&& type == Map.class
				&& (type == genericType || mapType.equals(genericType));
	}

	public Map<String, String[]> readFrom(Class<?> type, Type genericType,
			String mimeType, InputStream in, Charset charset, String base,
			String location, ObjectConnection con) throws IOException {
		if (charset == null) {
			charset = Charset.forName("ISO-8859-1");
		}
		String encoded = delegate.readFrom(String.class, String.class,
				mimeType, in, charset, base, location, con);
		try {
			Map<String, String[]> parameters = new LinkedHashMap<String, String[]>();
			Scanner scanner = new Scanner(encoded);
			scanner.useDelimiter("&");
			while (scanner.hasNext()) {
				String[] nameValue = scanner.next().split("=", 2);
				if (nameValue.length == 0 || nameValue.length > 2)
					continue;
				String name = URLDecoder.decode(nameValue[0], "ISO-8859-1");
				if (nameValue.length < 2) {
					if (!parameters.containsKey(name)) {
						parameters.put(name, new String[0]);
					}
				} else {
					add(parameters, name, URLDecoder.decode(nameValue[1], "ISO-8859-1"));
				}
			}
			return parameters;
		} catch (UnsupportedEncodingException e) {
			throw new AssertionError(e);
		}
	}

	private void add(Map<String, String[]> map, String key, String value) {
		String[] values = map.get(key);
		if (values == null) {
			values = new String[] { value };
		} else {
			values = Arrays.copyOf(values, values.length + 1);
			values[values.length - 1] = value;
		}
		map.put(key, values);
	}
}
