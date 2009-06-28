package org.openrdf.server.metadata.cache;

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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

	private final File head;
	private final File body;
	private boolean stale;
	private final String method;
	private final String url;
	/** synchronised on CacheIndex.lock() */
	private List<Map<String, String>> varies = new ArrayList<Map<String, String>>();
	private final Integer status;
	private final String statusText;
	/** copy on write */
	private Map<String, String> headers = new HashMap<String, String>();

	public CachedResponse(String method, String url, File head,
			FileResponse store) throws IOException {
		this.method = method;
		this.url = url;
		this.head = head;
		String statusText = store.getStatusText();
		Map<String, String> headers = store.getHeaders();
		long lastModified = store.getLastModified();
		Map<String, String> map = new HashMap<String, String>(
				headers.size() + 2);
		for (Map.Entry<String, String> e : headers.entrySet()) {
			add(map, e.getKey(), e.getValue());
		}
		DateFormat formatter = format.get();
		add(map, "date", formatter.format(new Date(store.getDate())));
		if (lastModified > 0) {
			add(map, "last-modified", formatter.format(new Date(lastModified)));
		}
		this.status = store.getStatus();
		this.statusText = statusText == null ? "" : statusText;
		this.headers = map;
		if (!head.exists()) {
			head.getParentFile().mkdirs();
		}
		this.stale = false;
		this.body = store.getMessageBody();
		head.getParentFile().mkdirs();
		writeHeaders(head);
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

	public void store(PrintWriter writer) {
		StringBuilder sb = new StringBuilder();
		sb.append(stale).append(" ");
		sb.append(head.getName()).append(" ");
		if (body != null && body.exists()) {
			sb.append(body.getName());
		}
		writer.println(sb.toString());
	}

	public boolean isStale() {
		return stale;
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
		Map<String, String> control = getHeaderValues("Cache-Control");
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

	public String getETag() {
		return getHeader("ETag");
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

	public Map<String, String> getHeaderValues(String name) {
		String value = getHeader(name);
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

	public void setResponse(FileResponse store)
			throws IOException {
		Map<String, String> map = new HashMap<String, String>(this.headers);
		// TODO update ETag and also replace any stored headers with
		// corresponding headers received in the incoming response
		this.headers = map;
		writeHeaders(head);
	}

	public boolean isBodyPresent() {
		return body != null;
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

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(method).append(' ').append(url).append(' ').append(status);
		sb.append(' ').append(getETag());
		return sb.toString();
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
