package org.openrdf.server.metadata.http.writers;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;


public class StringBodyWriter implements MessageBodyWriter<String> {

	public boolean isWriteable(Class<?> type, String mimeType) {
		return String.class.isAssignableFrom(type);
	}

	public long getSize(String t, String mimeType) {
		return t.length();
	}

	public String getContentType(Class<?> type, String mimeType, Charset charset) {
		return mimeType.toString();
	}

	public void writeTo(String result, String base, String mimeType,
			OutputStream out, Charset charset) throws IOException {
		Writer writer = new OutputStreamWriter(out);
		writer.write(result);
		writer.flush();
	}
}
