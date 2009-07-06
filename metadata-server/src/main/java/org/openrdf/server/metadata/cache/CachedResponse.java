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

	private String allow;
	private final File body;
	private String cacheControl;
	private Map<String, String> cacheDirectives = new HashMap<String, String>();
	private String contentEncoding;
	private String contentLanguage;
	private String contentLocation;
	private String contentMD5;
	/** locked by locker */
	private String contentType;
	private long date;
	private String eTag;
	private final File head;
	private long lastModified;
	private String link;
	private String location;
	private ReadWriteLockManager locker = new WritePrefReadWriteLockManager();
	private final String method;
	/** synchronised on CacheIndex.lock() */
	private Set<Map<String, String>> requests = new HashSet<Map<String, String>>();
	private volatile boolean stale;
	/** locked by locker */
	private Integer status;
	/** locked by locker */
	private String statusText;
	private final String url;
	private String[] vary;
	private String warning;

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
				if ("stale".equals(name)) {
					stale = Boolean.parseBoolean(value);
				} else if ("Content-Type".equalsIgnoreCase(name)) {
					contentType = value;
				} else if ("Content-Encoding".equalsIgnoreCase(name)) {
					contentEncoding = value;
				} else if ("Content-MD5".equalsIgnoreCase(name)) {
					contentMD5 = value;
				} else if ("Content-MD5".equalsIgnoreCase(name)) {
					contentMD5 = value;
				} else if ("Content-Location".equalsIgnoreCase(name)) {
					contentLocation = value;
				} else if ("Content-Language".equalsIgnoreCase(name)) {
					contentLanguage = value;
				} else if ("Cache-Control".equalsIgnoreCase(name)) {
					setCacheControl(value);
				} else if ("Allow".equalsIgnoreCase(name)) {
					allow = value;
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
				} else if ("Link".equalsIgnoreCase(name)) {
					link = value;
				} else if ("Location".equalsIgnoreCase(name)) {
					location = value;
				} else {
					assert false;
				}
			}
		} finally {
			reader.close();
		}
	}

	public CachedResponse(String method, String url, FileResponse store,
			File head, File body) throws IOException {
		this.method = method;
		this.url = url;
		this.stale = false;
		this.head = head;
		this.body = body;
		Map<String, String> headers = store.getHeaders();
		for (Map.Entry<String, String> e : headers.entrySet()) {
			if ("Content-Type".equalsIgnoreCase(e.getKey())) {
				contentType = e.getValue();
			} else if ("Content-Encoding".equalsIgnoreCase(e.getKey())) {
				contentEncoding = e.getValue();
			} else if ("Content-MD5".equalsIgnoreCase(e.getKey())) {
				contentMD5 = e.getValue();
			} else if ("Content-Location".equalsIgnoreCase(e.getKey())) {
				contentLocation = e.getValue();
			} else if ("Content-Language".equalsIgnoreCase(e.getKey())) {
				contentLanguage = e.getValue();
			} else if ("Cache-Control".equalsIgnoreCase(e.getKey())) {
				setCacheControl(e.getValue());
			} else if ("Allow".equalsIgnoreCase(e.getKey())) {
				allow = e.getValue();
			} else if ("ETag".equalsIgnoreCase(e.getKey())) {
				eTag = e.getValue();
			} else if ("Vary".equalsIgnoreCase(e.getKey())) {
				setVary(e.getValue());
			} else if ("Date".equalsIgnoreCase(e.getKey())) {
				date = parseDate(e.getValue());
			} else if ("Last-Modified".equalsIgnoreCase(e.getKey())) {
				lastModified = parseDate(e.getValue());
			} else if ("Warning".equalsIgnoreCase(e.getKey())) {
				warning = e.getValue();
			} else if ("Link".equalsIgnoreCase(e.getKey())) {
				link = e.getValue();
			} else if ("Location".equalsIgnoreCase(e.getKey())) {
				location = e.getValue();
			} else if ("Content-Length".equalsIgnoreCase(e.getKey())) {
				// ignore
			} else if ("Transfer-Encoding".equalsIgnoreCase(e.getKey())) {
				// ignore
			} else {
				// ignore unknown header
			}
		}
		head.getParentFile().mkdirs();
		try {
			setResponse(store);
		} catch (InterruptedException e) {
			throw new AssertionError(e);
		}
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
				contentType = store.getContentType();
				contentEncoding = store.getHeader("Content-Encoding");
				contentMD5 = store.getHeader("Content-MD5");
				contentLocation = store.getHeader("Content-Location");
				location = store.getHeader("Location");
				contentLanguage = store.getHeader("Content-Language");
				setCacheControl(store.getHeader("Cache-Control"));
				allow = store.getHeader("Allow");
				setVary(store.getHeader("Vary"));
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
		return map.containsKey("must-revalidate")
				|| map.containsKey("proxy-revalidate");
	}

	public int getAge(long now) {
		if (now <= date)
			return 0;
		return (int) ((now - date) / 1000);
	}

	public String getAllow() {
		return allow;
	}

	public String getCacheControl() {
		return cacheControl;
	}

	public String getContentEncoding() {
		return contentEncoding;
	}

	public String getContentLanguage() {
		return contentLanguage;
	}

	public String getContentLength() {
		if (body.exists())
			return Long.toString(body.length());
		return null;
	}

	public String getContentLocation() {
		return contentLocation;
	}

	public String getContentMD5() {
		return contentMD5;
	}

	public String getContentType() {
		return contentType;
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

	public String getLink() {
		return link;
	}

	public String getLocation() {
		return location;
	}

	public String getMethod() {
		return method;
	}

	public int getStatus() {
		return status;
	}

	public String getStatusText() {
		return statusText.length() == 0 ? null : statusText;
	}

	public String getURL() {
		return url;
	}

	public String getVary() {
		if (vary == null)
			return null;
		StringBuilder sb = new StringBuilder();
		for (String name : vary) {
			if (sb.length() > 0) {
				sb.append(",");
			}
			sb.append(name);
		}
		return sb.toString();
	}

	public String getWarning() {
		return warning;
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
		cacheControl = value;
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
		} else {
			vary = value.split("\\s*,\\s");
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
			if (contentType != null) {
				writer.print("content-type:");
				writer.println(contentType);
			}
			if (contentEncoding != null) {
				writer.print("content-encoding:");
				writer.println(contentEncoding);
			}
			if (contentMD5 != null) {
				writer.print("content-md5:");
				writer.println(contentMD5);
			}
			if (contentLocation != null) {
				writer.print("content-location:");
				writer.println(contentLocation);
			}
			if (contentLanguage != null) {
				writer.print("content-language:");
				writer.println(contentLanguage);
			}
			if (cacheControl != null) {
				writer.print("cache-control:");
				writer.println(cacheControl);
			}
			if (allow != null) {
				writer.print("allow:");
				writer.println(allow);
			}
			if (eTag != null) {
				writer.print("etag:");
				writer.println(eTag);
			}
			if (vary != null) {
				writer.print("vary:");
				for (int i = 0; i < vary.length; i++) {
					if (i > 0) {
						writer.print(",");
					}
					writer.print(vary[i]);
				}
				writer.println();
			}
			if (date > 0) {
				writer.print("date:");
				writer.println(format.get().format(new Date(date)));
			}
			if (lastModified > 0) {
				writer.print("last-modified:");
				writer.println(format.get().format(new Date(lastModified)));
			}
			if (warning != null) {
				writer.print("warning:");
				writer.println(warning);
			}
			if (link != null) {
				writer.print("link:");
				writer.println(link);
			}
			if (location != null) {
				writer.print("location:");
				writer.println(location);
			}
			writer.print("stale:");
			writer.println(stale);
		} finally {
			writer.close();
		}
	}

}
