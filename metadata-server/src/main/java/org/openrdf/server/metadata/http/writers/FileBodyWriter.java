package org.openrdf.server.metadata.http.writers;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.ws.rs.core.MediaType;

public class FileBodyWriter implements MessageBodyWriter<File> {

	public boolean isReadable(Class<?> type, MediaType mediaType) {
		return File.class == type;
	}

	public boolean isWriteable(Class<?> type, MediaType mediaType) {
		return File.class.isAssignableFrom(type);
	}

	public long getSize(File t, MediaType mediaType) {
		return t.length();
	}

	public String getContentType(Class<?> type, MediaType mediaType) {
		return mediaType.toString();
	}

	public void writeTo(File result, String base, MediaType mediaType,
			OutputStream out) throws IOException {
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
