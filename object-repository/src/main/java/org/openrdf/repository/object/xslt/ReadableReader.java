/**
 * 
 */
package org.openrdf.repository.object.xslt;

import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;
import java.nio.CharBuffer;

class ReadableReader extends Reader {
	private final Readable reader;

	ReadableReader(Readable reader) {
		this.reader = reader;
	}

	@Override
	public void close() throws IOException {
		if (reader instanceof Closeable) {
			((Closeable) reader).close();
		}
	}

	@Override
	public int read(char[] cbuf, int off, int len) throws IOException {
		return reader.read(CharBuffer.wrap(cbuf, off, len));
	}

	@Override
	public int read(CharBuffer cbuf) throws IOException {
		return reader.read(cbuf);
	}
}