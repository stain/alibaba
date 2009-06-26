package org.openrdf.server.metadata.cache;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicLong;

import org.openrdf.server.metadata.http.RequestHeader;

public class CacheIndex {
	private static final String HTTP_RESPONSE_DATE_HEADER = "EEE, dd MMM yyyy HH:mm:ss zzz";
	private static final String prefix = "t"
			+ Long.toHexString(System.currentTimeMillis()) + "x";
	private static final AtomicLong seq = new AtomicLong(0);
	private File index;
	private File file;
	private SimpleDateFormat format;
	private long now;

	public CacheIndex(RequestHeader headers) {
		file = headers.getFile();
		File dir = file.getParentFile();
		index = new File(dir, "$index" + file.getName());
		format = new SimpleDateFormat(HTTP_RESPONSE_DATE_HEADER, Locale.US);
		format.setTimeZone(TimeZone.getTimeZone("GMT"));
		now = System.currentTimeMillis();
	}

	public File getIndex() {
		return index;
	}

	public CachedResponse createCachedResponse() {
		File dir = file.getParentFile();
		String code = Long.toHexString(seq.incrementAndGet());
		String rfileName = "$request" + prefix + code + file.getName();
		String hfileName = "$head" + prefix + code + file.getName();
		String bfileName = "$body" + prefix + code + file.getName();
		File rfile = new File(dir, rfileName);
		File hfile = new File(dir, hfileName);
		File bfile = new File(dir, bfileName);
		return new CachedResponse(format, now, rfile, hfile, bfile);
	}

	public void add(CachedResponse cached) throws FileNotFoundException,
			IOException {
		RandomAccessFile raf = new RandomAccessFile(index, "rw");
		try {
			FileLock lock = raf.getChannel().lock();
			try {
				raf.seek(raf.length());
				raf.writeBytes(cached.store());
				raf.writeBytes("\n");
			} finally {
				lock.release();
			}
		} finally {
			raf.close();
		}
	}

	public void remove(CachedResponse cached) throws FileNotFoundException,
			IOException {
		RandomAccessFile raf = new RandomAccessFile(index, "rw");
		try {
			FileLock lock = raf.getChannel().lock();
			try {
				// TODO remove stale cached response
			} finally {
				lock.release();
			}
		} finally {
			raf.close();
		}
	}

	public void stale() throws IOException {
		if (index.exists()) {
			File dir = index.getParentFile();
			RandomAccessFile raf = new RandomAccessFile(index, "rw");
			try {
				FileLock lock = raf.getChannel().lock();
				try {
					String line;
					while ((line = raf.readLine()) != null) {
						CachedResponse cached = new CachedResponse(format, now, dir, line);
						try {
							cached.stale();
						} finally {
							cached.close();
						}
					}
					raf.setLength(0);
				} finally {
					lock.release();
				}
			} finally {
				raf.close();
			}
		}
	}

	public CachedResponse findCacheFor(RequestHeader req) throws IOException {
		File dir = index.getParentFile();
		String method = req.getMethod();
		String url = req.getRequestURL();
		RandomAccessFile raf = new RandomAccessFile(index, "r");
		try {
			FileLock lock = raf.getChannel().lock(0, Long.MAX_VALUE, true);
			try {
				String line;
				while ((line = raf.readLine()) != null) {
					CachedResponse cached = new CachedResponse(format, now, dir, line);
					try {
						String cachedMethod = cached.getMethod();
						String cachedURL = cached.getURL();
						// TODO use cached GET request for HEAD requests
						if (cachedMethod.equals(method)
								&& cachedURL.equals(url)) {
							// TODO check Vary headers
							// TODO check Accept-Encoding headers
							// TODO check Cache-Control headers
							// TODO check Authorization and Cache-Control public
							return cached;
						}
					} finally {
						cached.close();
					}
				}
			} finally {
				lock.release();
			}
		} finally {
			raf.close();
		}
		return null;
	}

	public boolean exists() {
		return index.exists();
	}

}
