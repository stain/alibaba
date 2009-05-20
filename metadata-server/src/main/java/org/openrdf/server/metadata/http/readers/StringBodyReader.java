package org.openrdf.server.metadata.http.readers;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.nio.charset.Charset;

import org.openrdf.repository.object.ObjectConnection;

public class StringBodyReader implements MessageBodyReader<String> {

	public boolean isReadable(Class<?> type, Type genericType,
			String mediaType, ObjectConnection con) {
		return String.class == type;
	}

	public String readFrom(Class<? extends String> type, Type genericType,
			String mimeType, InputStream in, Charset charset, String base,
			String location, ObjectConnection con) throws IOException {
		if (charset == null) {
			charset = Charset.defaultCharset();
		}
		StringWriter writer = new StringWriter();
		Reader reader = new InputStreamReader(in, charset);
		char[] cbuf = new char[512];
		int read;
		while ((read = reader.read(cbuf)) >= 0) {
			writer.write(cbuf, 0, read);
		}
		return writer.toString();
	}
}
