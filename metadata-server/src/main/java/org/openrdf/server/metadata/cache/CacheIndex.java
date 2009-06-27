package org.openrdf.server.metadata.cache;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
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

	public CacheIndex(File file) {
		this.file = file;
		File dir = file.getParentFile();
		index = new File(dir, "$index" + file.getName());
		format = new SimpleDateFormat(HTTP_RESPONSE_DATE_HEADER, Locale.US);
		format.setTimeZone(TimeZone.getTimeZone("GMT"));
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
		return new CachedResponse(format, rfile, hfile, bfile);
	}

	public synchronized void add(CachedResponse cached)
			throws FileNotFoundException, IOException {
		RandomAccessFile raf = new RandomAccessFile(index, "rw");
		try {
			raf.seek(raf.length());
			raf.writeBytes(cached.store());
			raf.writeBytes("\n");
		} finally {
			raf.close();
		}
	}

	public synchronized void remove(CachedResponse cached)
			throws FileNotFoundException, IOException {
		RandomAccessFile raf = new RandomAccessFile(index, "rw");
		try {
			// TODO remove stale cached response
		} finally {
			raf.close();
		}
	}

	public synchronized void stale() throws IOException {
		if (index.exists()) {
			File dir = index.getParentFile();
			RandomAccessFile raf = new RandomAccessFile(index, "rw");
			try {
				String line;
				while ((line = raf.readLine()) != null) {
					CachedResponse cached = new CachedResponse(format, dir,
							line);
					try {
						cached.stale();
					} finally {
						cached.close();
					}
				}
				raf.setLength(0);
			} finally {
				raf.close();
			}
		}
	}

	public synchronized CachedResponse findCacheFor(RequestHeader req)
			throws IOException {
		File dir = index.getParentFile();
		String method = req.getMethod();
		if ("HEAD".equals(method)) {
			method = "GET";
		}
		String url = req.getRequestURL();
		RandomAccessFile raf = new RandomAccessFile(index, "r");
		try {
			String line;
			search: while ((line = raf.readLine()) != null) {
				CachedResponse cached = new CachedResponse(format, dir, line);
				try {
					String cachedMethod = cached.getMethod();
					String cachedURL = cached.getURL();
					if (cachedMethod.equals(method) && cachedURL.equals(url)) {
						String vary = cached.getHeader("Vary");
						if (vary != null) {
							for (String name : vary.split("\\s*,\\s*")) {
								String match = cached.getRequestHeader(name);
								if (!equals(match, req.getHeader(name)))
									continue search;
							}
						}
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
			raf.close();
		}
		return null;
	}

	public boolean exists() {
		return index.exists();
	}

	private boolean equals(String s1, String s2) {
		return s1 == s2 || s1 != null && s1.equals(s2);
	}

}
