package org.openrdf.server.metadata.http.writers;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

public class FileBodyWriter implements MessageBodyWriter<File> {

	public boolean isWriteable(Class<?> type, String mimeType) {
		return File.class.isAssignableFrom(type);
	}

	public long getSize(File t, String mimeType) {
		return t.length();
	}

	public String getContentType(Class<?> type, String mimeType, Charset charset) {
		return mimeType.toString();
	}

	public void writeTo(File result, String base, String mimeType,
			OutputStream out, Charset charset) throws IOException {
		InputStream in = new BufferedInputStream(new FileInputStream(result));
		try {
			int read;
			final byte[] data = new byte[2048];
			while ((read = in.read(data)) != -1) {
				out.write(data, 0, read);
			}
		} finally {
			in.close();
		}
	}
}
