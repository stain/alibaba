package org.openrdf.http.object.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.entity.ContentListener;

public class ReadableContentListener implements ReadableByteChannel,
		ContentListener {
	private int read;
	private ByteBuffer pending;
	private volatile boolean closed;
	private IOControl ioctrl;

	public boolean isOpen() {
		return !closed;
	}

	public void finished() {
		closed = true;
	}

	public synchronized void close() throws IOException {
		closed = true;
		if (ioctrl != null) {
			ioctrl.requestInput();
		}
	}

	public synchronized int read(ByteBuffer dst) throws IOException {
		if (closed)
			return -1;
		pending = dst;
		if (ioctrl != null) {
			ioctrl.requestInput();
		}
		try {
			wait();
		} catch (InterruptedException e) {
			return 0;
		}
		pending = null;
		return read;
	}

	public synchronized void contentAvailable(ContentDecoder decoder,
			IOControl ioctrl) throws IOException {
		this.ioctrl = ioctrl;
		if (closed) {
			if (pending == null) {
				pending = ByteBuffer.allocate(1024 * 8);
			}
			while (decoder.read(pending) > 0) {
				pending.clear();
			}
		} else if (pending != null) {
			read = decoder.read(pending);
			notify();
		} else {
			ioctrl.suspendInput();
		}
		if (decoder.isCompleted()) {
			closed = true;
		}
	}

}
