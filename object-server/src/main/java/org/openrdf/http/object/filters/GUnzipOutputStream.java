package org.openrdf.http.object.filters;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.zip.GZIPInputStream;

public class GUnzipOutputStream extends OutputStream {
	private static Executor executor = Executors.newCachedThreadPool();

	public static OutputStream create(final OutputStream out)
			throws IOException {
		final PipedOutputStream zipped = new PipedOutputStream();
		final PipedInputStream pipe = new PipedInputStream(zipped);
		final GUnzipOutputStream unzipped = new GUnzipOutputStream(zipped, out);
		executor.execute(new Runnable() {
			public void run() {
				try {
					InputStream in = new GZIPInputStream(pipe);
					try {
						byte[] buf = new byte[512];
						int read;
						while ((read = in.read(buf)) >= 0) {
							out.write(buf, 0, read);
						}
					} finally {
						in.close();
					}
				} catch (IOException e) {
					unzipped.error(e);
				} finally {
					unzipped.finished();
				}
			}
		});
		return unzipped;
	}

	private IOException e;
	private OutputStream out;
	private Closeable resource;
	private CountDownLatch latch = new CountDownLatch(1);

	private GUnzipOutputStream(OutputStream out, Closeable resource) {
		this.out = out;
		this.resource = resource;
	}

	public void close() throws IOException {
		throwIOException();
		out.close();
		try {
			latch.await();
		} catch (InterruptedException e) {
			// close everything down
		} finally {
			resource.close();
		}
	}

	public void flush() throws IOException {
		throwIOException();
		out.flush();
	}

	public void write(int b) throws IOException {
		throwIOException();
		out.write(b);
	}

	public void write(byte[] b) throws IOException {
		throwIOException();
		out.write(b);
	}

	public void write(byte[] b, int off, int len) throws IOException {
		throwIOException();
		out.write(b, off, len);
	}

	public String toString() {
		return out.toString();
	}

	protected void error(IOException e) {
		this.e = e;
	}

	protected void finished() {
		latch.countDown();
	}

	private void throwIOException() throws IOException {
		IOException exc = this.e;
		this.e = null;
		if (exc != null)
			throw exc;
	}

}
