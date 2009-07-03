package org.openrdf.server.metadata.cache;

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

import org.openrdf.server.metadata.http.RequestHeader;

public class CachedResponse {
	private static final String HTTP_RESPONSE_DATE_HEADER = "EEE, dd MMM yyyy HH:mm:ss zzz";
	private static ThreadLocal<DateFormat> format = new ThreadLocal<DateFormat>() {
		@Override
		protected DateFormat initialValue() {
			SimpleDateFormat format = new SimpleDateFormat(
					HTTP_RESPONSE_DATE_HEADER, Locale.US);
			format.setTimeZone(TimeZone.getTimeZone("GMT"));
			return format;
		}
	};

	private ReadWriteLockManager locker = new WritePrefReadWriteLockManager();
	private final File head;
	private final File body;
	private volatile boolean stale;
	private final String method;
	private final String url;
	/** synchronised on CacheIndex.lock() */
	private Set<Map<String, String>> varies = new HashSet<Map<String, String>>();
	/** locked by locker */
	private Integer status;
	/** locked by locker */
	private String statusText;
	/** locked by locker */
	private Map<String, String> headers = new HashMap<String, String>();

	public CachedResponse(String method, String url, FileResponse store,
			File head, File body) throws IOException {
		this.method = method;
		this.url = url;
		this.stale = false;
		this.head = head;
		this.body = body;
		Map<String, String> headers = store.getHeaders();
		Map<String, String> map = new HashMap<String, String>(
				headers.size() + 3);
		for (Map.Entry<String, String> e : headers.entrySet()) {
			add(map, e.getKey(), e.getValue());
		}
		this.headers = map;
		head.getParentFile().mkdirs();
		try {
			setResponse(store);
		} catch (InterruptedException e) {
			throw new AssertionError(e);
		}
	}

	public CachedResponse(File head, File body) throws IOException {
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
					varies.add(map);
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
				if ("stale".equals(name)) {
					stale = Boolean.parseBoolean(value);
				} else {
					add(headers, name, value);
				}
			}
		} finally {
			reader.close();
		}
	}

	public boolean isStale() {
		return stale || getCacheControl().containsKey("no-cache");
	}

	public void setStale(boolean stale) throws IOException {
		this.stale = stale;
		writeHeaders(head);
	}

	public int getAge(long now) {
		long date = getDateHeader("Date");
		if (now <= date)
			return 0;
		return (int) ((now - date) / 1000);
	}

	public int getLifeTime() throws IOException {
		Map<String, String> control = getCacheControl();
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

	public boolean isPublic() {
		Map<String, String> control = getCacheControl();
		return control.containsKey("public");
	}

	public boolean mustRevalidate() {
		Map<String, String> map = getCacheControl();
		return map.containsKey("must-revalidate") || map.containsKey("proxy-revalidate");
	}

	public String getEntityTag() {
		String tag = getHeader("ETag");
		if (tag == null)
			return null;
		int start = tag.indexOf('"');
		int end = tag.lastIndexOf('"');
		return tag.substring(start + 1, end);
	}

	public String getMethod() {
		return method;
	}

	public String getURL() {
		return url;
	}

	public boolean isVariation(RequestHeader req) {
		String vary = getHeader("Vary");
		if (vary == null)
			return true;
		String[] varied = vary.split("\\s*,\\s*");
		search: for (Map<String, String> headers : varies) {
			for (String name : varied) {
				String match = headers.get(name.toLowerCase());
				if (!equals(match, req.getHeaders(name)))
					continue search;
			}
			return true;
		}
		return false;
	}

	public void addRequest(RequestHeader req) throws IOException {
		Map<String, String> map;
		map = Collections.emptyMap();
		String vary = getHeader("Vary");
		if (vary != null) {
			map = new LinkedHashMap<String, String>();
			for (String name : vary.split("\\s*,\\s*")) {
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
		this.varies.add(map);
		writeHeaders(head);
	}

	public int getStatus() {
		return status;
	}

	public String getStatusText() {
		return statusText.length() == 0 ? null : statusText;
	}

	public String getHeader(String name) {
		return headers.get(name.toLowerCase());
	}

	public long getDateHeader(String name) {
		String value = getHeader(name);
		if (value == null)
			return 0;
		try {
			return format.get().parse(value).getTime();
		} catch (ParseException e) {
			return 0;
		}
	}

	public Map<String, String> getHeaders() throws IOException {
		return headers;
	}

	public void setResponse(FileResponse store) throws IOException,
			InterruptedException {
		Lock lock = locker.getWriteLock();
		try {
			setHeaders(store);
			if (store.isModified()) {
				this.status = store.getStatus();
				String statusText = store.getStatusText();
				this.statusText = statusText == null ? "" : statusText;
				File tmp = store.getMessageBody();
				if (body.exists()) {
					body.delete();
				}
				if (tmp != null) {
					tmp.renameTo(body);
				}
			}
			writeHeaders(head);
		} finally {
			lock.release();
		}
	}

	public boolean isBodyPresent() {
		return body.exists();
	}

	public void writeBodyTo(OutputStream out) throws IOException {
		InputStream in = new FileInputStream(body);
		try {
			byte[] buf = new byte[256];
			int read;
			while ((read = in.read(buf)) >= 0) {
				out.write(buf, 0, read);
			}
		} finally {
			in.close();
		}
	}

	public long getContentLength() {
		return body.length();
	}

	public Lock open() throws InterruptedException {
		return locker.getReadLock();
	}

	public synchronized void delete() throws InterruptedException {
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

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(method).append(' ').append(url).append(' ').append(status);
		sb.append(' ').append(getHeader("ETag"));
		return sb.toString();
	}

	private Map<String, String> getCacheControl() {
		String value = getHeader("Cache-Control");
		if (value == null)
			return Collections.emptyMap();
		Map<String, String> map = new LinkedHashMap<String, String>();
		for (String v : value.split(",")) {
			int idx = v.indexOf('=');
			if (idx < 0) {
				map.put(v, null);
			} else {
				map.put(v.substring(0, idx), v.substring(idx + 1));
			}
		}
		return map;
	}

	private void setHeaders(FileResponse store) {
		// TODO update ETag and also replace any stored headers with
		// corresponding headers received in the incoming response
		DateFormat formatter = format.get();
		headers.put("date", formatter.format(new Date(store.getDate())));
		long lastModified = store.getLastModified();
		if (lastModified > 0) {
			headers.put("last-modified", formatter
					.format(new Date(lastModified)));
		}
	}

	private void writeHeaders(File file) throws IOException {
		PrintWriter writer = new PrintWriter(new FileWriter(file));
		try {
			writer.print(method);
			writer.print(' ');
			writer.println(url);
			for (Map<String, String> map : varies) {
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
			for (Map.Entry<String, String> e : headers.entrySet()) {
				writer.print(e.getKey());
				writer.print(':');
				writer.println(e.getValue());
			}
			writer.print("stale:");
			writer.println(stale);
		} finally {
			writer.close();
		}
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

}
