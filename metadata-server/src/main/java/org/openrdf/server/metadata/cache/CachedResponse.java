package org.openrdf.server.metadata.cache;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

public class CachedResponse {
	private long now;
	private RandomAccessFile reqFile;
	private RandomAccessFile headFile;
	private SimpleDateFormat format;
	private File req;
	private File head;
	private File body;

	public CachedResponse(SimpleDateFormat format, long now, File req,
			File head, File body) {
		this.now = now;
		this.format = format;
		this.req = req;
		this.head = head;
		this.body = body;
	}

	public CachedResponse(SimpleDateFormat format, long now, File dir,
			String line) {
		this.now = now;
		this.format = format;
		String[] cached = line.split(" ", 3);
		req = new File(dir, cached[0]);
		head = new File(dir, cached[1]);
		if (cached[2].length() > 0) {
			body = new File(dir, cached[2]);
		}
	}

	public void close() throws IOException {
		if (reqFile != null) {
			reqFile.close();
			reqFile = null;
		}
		if (headFile != null) {
			headFile.close();
			headFile = null;
		}
	}

	public String store() throws IOException {
		close();
		StringBuilder sb = new StringBuilder();
		sb.append(req.getName()).append(" ");
		sb.append(head.getName()).append(" ");
		if (body != null && body.exists()) {
			sb.append(body.getName());
		}
		return sb.toString();
	}

	public void stale() throws IOException {
		close();
		req.delete();
		head.delete();
		if (body != null) {
			body.delete();
		}
	}

	public int getAge() throws IOException {
		long date = getDateHeader("Date");
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

	public String getETag() throws IOException {
		return getHeader("ETag");
	}

	public String getMethod() throws IOException {
		if (reqFile == null) {
			reqFile = new RandomAccessFile(req, "r");
		} else {
			reqFile.seek(0);
		}
		return readWord(reqFile);
	}

	public void setMethod(String method) throws IOException {
		if (reqFile == null) {
			reqFile = new RandomAccessFile(req, "rw");
		} else {
			reqFile.close();
			reqFile = new RandomAccessFile(req, "rw");
			reqFile.setLength(0);
		}
		reqFile.writeBytes(method);
		reqFile.writeBytes(" ");
	}

	public String getURL() throws IOException {
		return reqFile.readLine();
	}

	public void setURL(String url) throws IOException {
		reqFile.writeBytes(url);
		reqFile.writeBytes("\n");
	}

	public void setRequestHeader(String name, String value) throws IOException {
		assert value.indexOf('\n') < 0 && value.indexOf('\r') < 0;
		reqFile.writeBytes(name);
		reqFile.writeBytes(": ");
		reqFile.writeBytes(value);
		reqFile.writeBytes("\n");
	}

	public String getRequestHeaderName() throws IOException {
		return readWord(reqFile);
	}

	public String getRequestHeaderValue() throws IOException {
		return reqFile.readLine();
	}

	public int getStatus() throws IOException {
		if (headFile == null) {
			headFile = new RandomAccessFile(head, "r");
		} else {
			headFile.seek(0);
		}
		return Integer.parseInt(readWord(headFile));
	}

	public String getStatusText() throws IOException {
		String line = headFile.readLine();
		if (line.length() == 0)
			return null;
		return line;
	}

	public void setStatus(int status, String statusText) throws IOException {
		if (headFile == null) {
			head.getParentFile().mkdirs();
			headFile = new RandomAccessFile(head, "rw");
		} else {
			headFile.close();
			headFile = new RandomAccessFile(head, "rw");
			headFile.setLength(0);
		}
		headFile.writeBytes(Integer.toString(status));
		headFile.writeBytes(" ");
		if (statusText != null) {
			headFile.writeBytes(statusText);
		}
		headFile.writeBytes("\n");
	}

	public String getHeader(String name) throws IOException {
		if (headFile == null) {
			headFile = new RandomAccessFile(head, "r");
			headFile.readLine();
		} else {
			headFile.seek(0);
			headFile.readLine();
		}
		String header;
		while ((header = getHeaderName()) != null) {
			String value = getHeaderValue();
			if (header.equalsIgnoreCase(name)) {
				return value;
			}
		}
		return null;
	}

	public Map<String, String> getHeaderValues(String name) throws IOException {
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

	public long getDateHeader(String name) throws IOException {
		String value = getHeader(name);
		if (value == null)
			return 0;
		try {
			return format.parse(value).getTime();
		} catch (ParseException e) {
			return 0;
		}
	}

	public String getHeaderName() throws IOException {
		return readWord(headFile);
	}

	public String getHeaderValue() throws IOException {
		return headFile.readLine();
	}

	public void setHeader(String name, String value) throws IOException {
		if (!"Content-Length".equalsIgnoreCase(name)) {
			assert value.indexOf('\n') < 0 && value.indexOf('\r') < 0;
			headFile.writeBytes(name);
			headFile.writeBytes(": ");
			headFile.writeBytes(value);
			headFile.writeBytes("\n");
		}
	}

	public void setDateHeader(String name, long value) throws IOException {
		setHeader(name, format.format(new Date(value)));
	}

	public void setHeaders(Map<String, String> headers) {
		// TODO update ETag and also replace any stored headers with corresponding headers received in the incoming response
		
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

	private String readWord(RandomAccessFile file) throws IOException {
		StringBuffer input = new StringBuffer();
		int c = -1;
		boolean eol = false;

		while (!eol) {
			switch (c = file.read()) {
			case -1:
			case ' ':
			case '\t':
			case '\n':
				eol = true;
				break;
			case '\r':
				eol = true;
				long cur = file.getFilePointer();
				if ((file.read()) != '\n') {
					file.seek(cur);
				}
				break;
			case ':':
				eol = true;
				long cur2 = file.getFilePointer();
				if ((file.read()) != ' ') {
					file.seek(cur2);
				}
				break;
			default:
				input.append((char) c);
				break;
			}
		}

		if ((c == -1) && (input.length() == 0)) {
			return null;
		}
		return input.toString();
	}

}
