package org.openrdf.server.metadata.cache;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

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
	private File req;
	private File head;
	private File body;
	private boolean stale;
	private String method;
	private String url;
	private Map<String, String> requestHeaders = new HashMap<String, String>();
	private Integer status;
	private String statusText = "";
	private Map<String, String> headers = new HashMap<String, String>();

	public CachedResponse(File req, File head, File body) {
		this.req = req;
		this.head = head;
		this.body = body;
	}

	public CachedResponse(File dir, String line) throws IOException {
		String[] cached = line.split(" ", 4);
		stale = Boolean.parseBoolean(cached[0]);
		req = new File(dir, cached[1]);
		head = new File(dir, cached[2]);
		if (cached[3].length() > 0) {
			body = new File(dir, cached[3]);
		}
		readRequest(req);
		readHeaders(head);
	}

	public String store() {
		StringBuilder sb = new StringBuilder();
		sb.append(stale).append(" ");
		sb.append(req.getName()).append(" ");
		sb.append(head.getName()).append(" ");
		if (body != null && body.exists()) {
			sb.append(body.getName());
		}
		return sb.toString();
	}

	public boolean isStale() {
		return stale;
	}

	public void setStale(boolean stale) {
		this.stale = stale;
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

	public Map<String, String> getRequestHeaders() {
		return requestHeaders;
	}

	public synchronized String getRequestHeader(String name) {
		return requestHeaders.get(name.toLowerCase());
	}

	public synchronized void setRequest(String method, String url,
			Map<String, String> headers) throws IOException {
		this.method = method;
		this.url = url;
		this.requestHeaders = headers;
		writeRequest(req);
	}

	public int getStatus() {
		return status;
	}

	public String getStatusText() {
		return statusText.length() == 0 ? null : statusText;
	}

	public synchronized String getHeader(String name) {
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

	public synchronized void setResponse(int status, String statusText,
			long date, Map<String, String> headers, long lastModified)
			throws IOException {
		Map<String, String> map = new HashMap<String, String>(headers.size() + 2);
		for (Map.Entry<String, String> e : headers.entrySet()) {
			add(map, e.getKey(), e.getValue());
		}
		DateFormat formatter = format.get();
		add(map, "date", formatter.format(new Date(date)));
		if (lastModified > 0) {
			add(map, "last-modified", formatter.format(new Date(lastModified)));
		}
		this.status = status;
		this.statusText = statusText == null ? "" : statusText;
		this.headers = map;
		if (!head.exists()) {
			head.getParentFile().mkdirs();
		}
		this.stale = false;
		writeHeaders(head);
	}

	public synchronized void setHeaders(long date, Map<String, String> headers,
			long lastModified) throws IOException {
		Map<String, String> map = new HashMap<String, String>(this.headers);
		// TODO update ETag and also replace any stored headers with
		// corresponding headers received in the incoming response
		this.headers = map;
		writeHeaders(head);
	}

	public boolean isBodyPresent() {
		return body != null && body.exists();
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

	public OutputStream createOutputStream() throws IOException {
		body.getParentFile().mkdirs();
		return new FileOutputStream(body);
	}

	public long getContentLength() {
		return body.length();
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(method).append(' ').append(url).append(' ').append(status);
		String vary = getHeader("Vary");
		if (vary != null) {
			for (String header : vary.split(" ")) {
				sb.append(' ').append(header).append(": ");
				sb.append(getRequestHeader(header));
			}
		}
		return sb.toString();
	}

	private void writeRequest(File file) throws IOException {
		PrintWriter writer = new PrintWriter(new FileWriter(file));
		try {
			writer.print(method);
			writer.print(' ');
			writer.println(url);
			for (Map.Entry<String, String> e : requestHeaders.entrySet()) {
				writer.print(e.getKey());
				writer.print(':');
				writer.println(e.getValue());
			}
		} finally {
			writer.close();
		}
	}

	private void readRequest(File file) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(file));
		try {
			String line = reader.readLine();
			int idx = line.indexOf(' ');
			method = line.substring(0, idx);
			url = line.substring(idx + 1);
			while ((line = reader.readLine()) != null) {
				idx = line.indexOf(':');
				String name = line.substring(0, idx);
				String value = line.substring(idx + 1);
				add(requestHeaders, name, value);
			}
		} finally {
			reader.close();
		}
	}

	private void writeHeaders(File file) throws IOException {
		PrintWriter writer = new PrintWriter(new FileWriter(file));
		try {
			writer.print(status);
			writer.print(' ');
			writer.println(statusText);
			for (Map.Entry<String, String> e : headers.entrySet()) {
				writer.print(e.getKey());
				writer.print(':');
				writer.println(e.getValue());
			}
		} finally {
			writer.close();
		}
	}

	private void readHeaders(File file) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(file));
		try {
			String line = reader.readLine();
			int idx = line.indexOf(' ');
			status = Integer.valueOf(line.substring(0, idx));
			statusText = line.substring(idx + 1);
			while ((line = reader.readLine()) != null) {
				idx = line.indexOf(':');
				String name = line.substring(0, idx);
				String value = line.substring(idx + 1);
				add(headers, name, value);
			}
		} finally {
			reader.close();
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

}
