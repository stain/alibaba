package org.openrdf.server.metadata.http.writers;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import javax.ws.rs.core.MediaType;

public class StringBodyWriter implements MessageBodyWriter<String> {

	public boolean isWriteable(Class<?> type, MediaType mediaType) {
		return String.class.isAssignableFrom(type);
	}

	public long getSize(String t, MediaType mediaType) {
		return t.length();
	}

	public String getContentType(Class<?> type, MediaType mediaType) {
		return mediaType.toString();
	}

	public void writeTo(String result, String base, MediaType mediaType,
			OutputStream out) throws IOException {
		Writer writer = new OutputStreamWriter(out);
		writer.write(result);
		writer.flush();
	}
}
