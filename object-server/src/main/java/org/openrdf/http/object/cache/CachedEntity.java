package org.openrdf.http.object.cache;

import info.aduna.concurrent.locks.Lock;
import info.aduna.concurrent.locks.ReadWriteLockManager;
import info.aduna.concurrent.locks.WritePrefReadWriteLockManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import org.openrdf.http.object.http.RequestHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CachedEntity {
	private static ThreadLocal<DateFormat> format = new ThreadLocal<DateFormat>() {
		@Override
		protected DateFormat initialValue() {
			SimpleDateFormat format = new SimpleDateFormat(
					HTTP_RESPONSE_DATE_HEADER, Locale.US);
			format.setTimeZone(TimeZone.getTimeZone("GMT"));
			return format;
		}
	};
	private static final String HTTP_RESPONSE_DATE_HEADER = "EEE, dd MMM yyyy HH:mm:ss zzz";
	private static final String[] CONTENT_HEADERS = { "Content-Type",
			"Content-Encoding", "Content-MD5", "Content-Location", "Location",
			"Content-Language", "Cache-Control", "Allow", "Vary", "Link",
			"Access-Control-Allow-Origin", "Access-Control-Allow-Methods",
			"Access-Control-Allow-Headers", "Access-Control-Max-Age" };

	public static String getURL(File head) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(head));
		try {
			String line = reader.readLine();
			int idx = line.indexOf(' ');
			return line.substring(idx + 1);
		} finally {
			reader.close();
		}
	}

	private Logger logger = LoggerFactory.getLogger(CachedEntity.class);
	private final File body;
	private Map<String, String> cacheDirectives = new HashMap<String, String>();
	private long date;
	private String eTag;
	private final File head;
	private long lastModified;
	private ReadWriteLockManager locker = new WritePrefReadWriteLockManager();
	private final String method;
	/** synchronised on CacheIndex.lock() */
	private Set<Map<String, String>> requests = new HashSet<Map<String, String>>();
	private volatile boolean stale;
	/** locked by locker */
	private Integer status;
	/** locked by locker */
	private String statusText;
	private Long contentLength;
	private final String url;
	private String[] vary;
	private String warning;
	private Map<String, String> headers = new HashMap<String, String>();

	public CachedEntity(File head, File body) throws IOException {
		this.head = head;
		this.body = body;
		BufferedReader reader = new BufferedReader(new FileReader(head));
		try {
			String line = reader.readLine();
			int idx = line.indexOf(' ');
			method = line.substring(0, idx);
			url = line.substring(idx + 1);
			Map<String, String> map = new HashMap<String, String>();
			while ((line = reader.readLine()) != null) {
				if (line.length() == 0) {
					requests.add(map);
					map = new HashMap<String, String>();
				} else if (Character.isDigit(line.charAt(0))) {
					break;
				} else {
					idx = line.indexOf(':');
					String name = line.substring(0, idx);
					String value = line.substring(idx + 1);
					add(map, name, value);
				}
			}
			idx = line.indexOf(' ');
			status = Integer.valueOf(line.substring(0, idx));
			statusText = line.substring(idx + 1);
			while ((line = reader.readLine()) != null) {
				idx = line.indexOf(':');
				String name = line.substring(0, idx);
				String value = line.substring(idx + 1);
				setHeader(name, value);
			}
		} finally {
			reader.close();
		}
	}

	public CachedEntity(String method, String url, FileResponse store,
			File head, File body) throws IOException {
		this.method = method;
		this.url = url;
		this.stale = false;
		this.head = head;
		this.body = body;
		Map<String, String> headers = store.getHeaders();
		for (Map.Entry<String, String> e : headers.entrySet()) {
			String name = e.getKey();
			String value = e.getValue();
			setHeader(name, value);
		}
		head.getParentFile().mkdirs();
		try {
			setResponse(store);
		} catch (InterruptedException e) {
			throw new AssertionError(e);
		}
	}

	public boolean isMissing() {
		return contentLength != null && !body.exists();
	}

	public Lock open() throws InterruptedException {
		return locker.getReadLock();
	}

	public void delete() throws InterruptedException {
		Lock lock = locker.getWriteLock();
		try {
			head.delete();
			if (body.exists()) {
				body.delete();
			}
		} finally {
			lock.release();
		}
	}

	public void addRequest(RequestHeader req) throws IOException {
		Map<String, String> map;
		map = Collections.emptyMap();
		if (vary != null) {
			map = new LinkedHashMap<String, String>();
			for (String name : vary) {
				Enumeration values = req.getHeaders(name);
				while (values.hasMoreElements()) {
					String value = (String) values.nextElement();
					String existing = map.get(name);
					if (existing == null) {
						map.put(name.toLowerCase(), value);
					} else {
						map.put(name.toLowerCase(), existing + "," + value);
					}
				}
			}
		}
		this.requests.add(map);
		writeHeaders(head);
	}

	public void setResponse(FileResponse store) throws IOException,
			InterruptedException {
		Lock lock = locker.getWriteLock();
		try {
			warning = store.getHeader("Warning");
			date = store.getDate();
			lastModified = store.getLastModified();
			if (store.isModified()) {
				this.status = store.getStatus();
				String statusText = store.getStatusText();
				this.statusText = statusText == null ? "" : statusText;
				for (String name : CONTENT_HEADERS) {
					setHeader(name, store.getHeader(name));
				}
				setHeader("Content-MD5", store.getContentMD5());
				File tmp = store.getMessageBody();
				if (body.exists()) {
					body.delete();
				}
				if (tmp == null) {
					contentLength = null;
				} else {
					contentLength = tmp.length();
					tmp.renameTo(body);
				}
			}
			writeHeaders(head);
		} finally {
			lock.release();
		}
	}

	public boolean isVariation(RequestHeader req) {
		if (vary == null)
			return true;
		search: for (Map<String, String> headers : requests) {
			for (String name : vary) {
				String match = headers.get(name.toLowerCase());
				if (!equals(match, req.getHeaders(name)))
					continue search;
			}
			return true;
		}
		return false;
	}

	public boolean mustRevalidate() {
		Map<String, String> map = cacheDirectives;
		return "0".equals(map.get("max-age")) && !map.containsKey("s-maxage")
				|| "0".equals(map.get("s-maxage"))
				|| map.containsKey("must-revalidate")
				|| map.containsKey("proxy-revalidate");
	}

	public int getAge(long now) {
		if (now <= date)
			return 0;
		return (int) ((now - date) / 1000);
	}

	public Long contentLength() {
		return contentLength;
	}

	public String getContentLength() {
		if (contentLength != null)
			return Long.toString(contentLength);
		return null;
	}

	public String getContentHeader(String name) {
		return headers.get(name.toLowerCase());
	}

	public Map<String, String> getContentHeaders() {
		return headers;
	}

	public long date() {
		return date;
	}

	public String getDate() {
		return format.get().format(date);
	}

	public String getEntityTag() {
		String tag = getETag();
		if (tag == null)
			return null;
		int start = tag.indexOf('"');
		int end = tag.lastIndexOf('"');
		return tag.substring(start + 1, end);
	}

	public String getETag() {
		return eTag;
	}

	public long lastModified() {
		return lastModified;
	}

	public String getLastModified() {
		return format.get().format(lastModified);
	}

	public int getLifeTime() throws IOException {
		Map<String, String> control = cacheDirectives;
		String maxage = control.get("s-maxage");
		if (maxage == null) {
			maxage = control.get("max-age");
		}
		if (maxage == null)
			return 0;
		try {
			return Integer.parseInt(maxage);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	public String getMethod() {
		return method;
	}

	public Integer getStatus() {
		return status;
	}

	public String getStatusText() {
		return statusText.length() == 0 ? null : statusText;
	}

	public String getURL() {
		return url;
	}

	public String getWarning() {
		return warning;
	}

	public String getContentType() {
		return headers.get("content-type");
	}

	public boolean isPublic() {
		return cacheDirectives.containsKey("public");
	}

	public boolean isStale() {
		return stale || cacheDirectives.containsKey("no-cache");
	}

	public void setStale(boolean stale) throws IOException {
		this.stale = stale;
		writeHeaders(head);
	}

	public boolean isBodyPresent() {
		return contentLength != null;
	}

	public void writeBodyTo(OutputStream out, int blockSize) throws IOException {
		InputStream in = new FileInputStream(body);
		try {
			byte[] buf = new byte[blockSize];
			int read;
			while ((read = in.read(buf)) >= 0) {
				out.write(buf, 0, read);
			}
		} finally {
			in.close();
		}
	}

	public void writeBodyTo(OutputStream out, int blockSize, long start,
			long length) throws IOException {
		InputStream in = new FileInputStream(body);
		try {
			byte[] buf = new byte[blockSize];
			int read;
			in.skip(start);
			long rest = length;
			while ((read = in.read(buf, 0, (int) Math.min(rest, buf.length))) >= 0) {
				out.write(buf, 0, read);
				rest -= read;
				if (rest == 0)
					break;
			}
		} finally {
			in.close();
		}
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(method).append(' ').append(url).append(' ').append(status);
		sb.append(' ').append(getETag());
		return sb.toString();
	}

	private void add(Map<String, String> map, String name, String value) {
		String key = name.toLowerCase();
		if (map.containsKey(key)) {
			map.put(key, map.get(key) + "," + value);
		} else {
			map.put(key, value);
		}
	}

	private boolean equals(String s1, Enumeration<String> s2) {
		if (s1 == null)
			return !s2.hasMoreElements();
		if (!s2.hasMoreElements())
			return s1 == null;
		String first = s2.nextElement();
		if (!s2.hasMoreElements())
			return s1.equals(first);
		StringBuilder sb = new StringBuilder();
		sb.append(first);
		while (s2.hasMoreElements()) {
			sb.append(",");
			sb.append(s2.nextElement());
		}
		return s1.equals(sb.toString());
	}

	private void setHeader(String name, String value) {
		if ("stale".equalsIgnoreCase(name)) {
			stale = Boolean.parseBoolean(value);
		} else if ("Cache-Control".equalsIgnoreCase(name)) {
			setCacheControl(value);
		} else if ("ETag".equalsIgnoreCase(name)) {
			eTag = value;
		} else if ("Vary".equalsIgnoreCase(name)) {
			setVary(value);
		} else if ("Date".equalsIgnoreCase(name)) {
			date = parseDate(value);
		} else if ("Last-Modified".equalsIgnoreCase(name)) {
			lastModified = parseDate(value);
		} else if ("Warning".equalsIgnoreCase(name)) {
			warning = value;
		} else if ("Content-Length".equalsIgnoreCase(name)) {
			contentLength = Long.valueOf(value);
		} else if ("Transfer-Encoding".equalsIgnoreCase(name)) {
			// ignore
		} else if (value == null || value.length() < 1) {
			headers.remove(name.toLowerCase());
		} else {
			headers.put(name.toLowerCase(), value);
		}
	}

	private long parseDate(String value) {
		if (value == null)
			return 0;
		try {
			return format.get().parse(value).getTime();
		} catch (ParseException e) {
			return 0;
		}
	}

	private void setCacheControl(String value) {
		headers.put("cache-control", value);
		if (value == null) {
			cacheDirectives = Collections.emptyMap();
		} else {
			Map<String, String> map = new LinkedHashMap<String, String>();
			for (String v : value.split(",")) {
				int idx = v.indexOf('=');
				if (idx < 0) {
					map.put(v, null);
				} else {
					map.put(v.substring(0, idx), v.substring(idx + 1));
				}
			}
			cacheDirectives = map;
		}
	}

	private void setVary(String value) {
		if (value == null) {
			vary = null;
			headers.remove("vary");
		} else {
			vary = value.split("\\s*,\\s");
			headers.put("vary", value);
		}
	}

	private void writeHeaders(File file) throws IOException {
		PrintWriter writer = new PrintWriter(new FileWriter(file));
		try {
			writer.print(method);
			writer.print(' ');
			writer.println(url);
			for (Map<String, String> map : requests) {
				for (Map.Entry<String, String> e : map.entrySet()) {
					writer.print(e.getKey());
					writer.print(':');
					writer.println(e.getValue());
				}
				writer.println();
			}
			writer.print(status);
			writer.print(' ');
			writer.println(statusText);
			if (eTag != null) {
				writer.print("ETag:");
				writer.println(eTag);
			}
			if (contentLength != null) {
				writer.print("Content-Length:");
				writer.println(contentLength);
			}
			if (vary != null) {
				writer.print("Vary:");
				for (int i = 0; i < vary.length; i++) {
					if (i > 0) {
						writer.print(",");
					}
					writer.print(vary[i]);
				}
				writer.println();
			}
			if (date > 0) {
				writer.print("Date:");
				writer.println(format.get().format(new Date(date)));
			}
			if (lastModified > 0) {
				writer.print("Last-Modified:");
				writer.println(format.get().format(new Date(lastModified)));
			}
			if (warning != null) {
				writer.print("Warning:");
				writer.println(warning);
			}
			for (String name : headers.keySet()) {
				writer.print(name);
				writer.print(":");
				writer.println(headers.get(name));
			}
			writer.print("Stale:");
			writer.println(stale);
		} finally {
			writer.close();
		}
	}

}
