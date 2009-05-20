package org.openrdf.server.metadata.http.readers;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.openrdf.repository.object.ObjectConnection;

public final class FormMapMessageReader implements
		MessageBodyReader<Map<String, String[]>> {

	private final Type mapType;
	private StringBodyReader delegate = new StringBodyReader();

	public FormMapMessageReader() {
		ParameterizedType iface = (ParameterizedType) this.getClass().getGenericInterfaces()[0];
		mapType = iface.getActualTypeArguments()[0];
	}

	public boolean isReadable(Class<?> type, Type genericType, String mimeType,
			ObjectConnection con) {
		return delegate.isReadable(String.class, String.class, mimeType, con)
				&& type == Map.class
				&& (type == genericType || mapType.equals(genericType));
	}

	public Map<String, String[]> readFrom(
			Class<? extends Map<String, String[]>> type, Type genericType,
			String mimeType, InputStream in, Charset charset, String base,
			String location, ObjectConnection con) throws IOException {
		String encoded = delegate.readFrom(String.class, String.class,
				mimeType, in, charset, base, location, con);

		Map<String, String[]> map = new HashMap<String, String[]>();
		StringTokenizer tokenizer = new StringTokenizer(encoded, "&");
		String token;
		while (tokenizer.hasMoreTokens()) {
			token = tokenizer.nextToken();
			int idx = token.indexOf('=');
			if (idx < 0) {
				add(map, URLDecoder.decode(token, "UTF-8"), null);
			} else if (idx > 0) {
				add(map, URLDecoder.decode(token.substring(0, idx), "UTF-8"),
						URLDecoder.decode(token.substring(idx + 1), "UTF-8"));
			}
		}
		return map;
	}

	private void add(Map<String, String[]> map, String key, String value) {
		String[] values = map.get(key);
		if (values == null) {
			values = new String[] { value };
		} else {
			Arrays.copyOf(values, values.length + 1);
			values[values.length - 1] = value;
		}
		map.put(key, values);
	}
}