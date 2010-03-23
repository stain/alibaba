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
	private volatile boolean completed;
	private IOControl ioctrl;

	public boolean isOpen() {
		return !completed;
	}

	public synchronized void finished() {
		completed = true;
		notify();
	}

	public synchronized void close() throws IOException {
		completed = true;
		notify();
		if (ioctrl != null) {
			ioctrl.requestInput();
		}
	}

	public synchronized int read(ByteBuffer dst) throws IOException {
		if (completed)
			return -1;
		assert pending == null;
		pending = dst;
		read = 0;
		if (ioctrl != null) {
			ioctrl.requestInput();
		}
		try {
			wait();
		} catch (InterruptedException e) {
			return 0;
		} finally {
			pending = null;
		}
		if (read == 0 && completed)
			return -1;
		return read;
	}

	public synchronized void contentAvailable(ContentDecoder decoder,
			IOControl ioctrl) throws IOException {
		this.ioctrl = ioctrl;
		if (completed) {
			if (pending == null) {
				pending = ByteBuffer.allocate(1024 * 8);
			}
			while (decoder.read(pending) > 0) {
				pending.clear();
			}
			read = 0;
		} else if (pending != null && pending.remaining() > 0) {
			int r = decoder.read(pending);
			if (r > 0) {
				read += r;
			}
			if (decoder.isCompleted()) {
				completed = true;
			}
			notify();
		} else if (decoder.isCompleted()
				|| decoder.read(ByteBuffer.allocate(0)) < 0
				|| decoder.isCompleted()) {
			completed = true;
			notify();
		} else {
			ioctrl.suspendInput();
		}
	}

}
