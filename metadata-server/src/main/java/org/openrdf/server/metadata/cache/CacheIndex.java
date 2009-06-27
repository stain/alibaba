package org.openrdf.server.metadata.cache;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.openrdf.server.metadata.http.RequestHeader;

public class CacheIndex {
	private static final String prefix = "t"
			+ Long.toHexString(System.currentTimeMillis()) + "x";
	private static final AtomicLong seq = new AtomicLong(0);
	private File file;
	private File index;
	private Map<String, List<CachedResponse>> responses;

	public CacheIndex(File file) throws IOException {
		this.file = file;
		File dir = file.getParentFile();
		index = new File(dir, "$index" + file.getName());
		if (index.exists()) {
			responses = load(index);
		} else {
			responses = new HashMap<String, List<CachedResponse>>();
		}
	}

	public synchronized CachedResponse find(RequestHeader req)
			throws IOException {
		String method = req.getMethod();
		if ("HEAD".equals(method)) {
			method = "GET";
		}
		String url = req.getRequestURL();
		List<CachedResponse> list = responses.get(url);
		if (list == null)
			return null;
		search: for (CachedResponse cached : list) {
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
		}
		return null;
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
		return new CachedResponse(rfile, hfile, bfile);
	}

	public synchronized void replace(CachedResponse stale, CachedResponse fresh)
			throws FileNotFoundException, IOException {
		if (responses.isEmpty()) {
			index.getParentFile().mkdirs();
		}
		if (stale != null) {
			remove(responses, stale);
		}
		add(responses, fresh);
		store(responses, index);
	}

	public synchronized void stale() throws IOException {
		if (!responses.isEmpty()) {
			for (List<CachedResponse> list : responses.values()) {
				for (CachedResponse cached : list) {
					cached.setStale(true);
				}
			}
			store(responses, index);
		}
	}

	public synchronized String toString() {
		StringBuilder sb = new StringBuilder();
		for (List<CachedResponse> list : responses.values()) {
			for (CachedResponse cached : list) {
				sb.append(cached.toString()).append("\n");
			}
		}
		return sb.toString();
	}

	private boolean equals(String s1, String s2) {
		return s1 == s2 || s1 != null && s1.equals(s2);
	}

	private Map<String, List<CachedResponse>> load(File index)
			throws FileNotFoundException, IOException {
		Map<String, List<CachedResponse>> responses;
		responses = new HashMap<String, List<CachedResponse>>();
		File dir = index.getParentFile();
		BufferedReader reader = new BufferedReader(new FileReader(index));
		try {
			String line;
			while ((line = reader.readLine()) != null) {
				add(responses, new CachedResponse(dir, line));
			}
		} finally {
			reader.close();
		}
		return responses;
	}

	private void add(Map<String, List<CachedResponse>> responses,
			CachedResponse response) throws IOException {
		List<CachedResponse> list = responses.get(response.getURL());
		if (list == null) {
			list = new LinkedList<CachedResponse>();
			responses.put(response.getURL(), list);
		}
		list.add(response);
	}

	private void remove(Map<String, List<CachedResponse>> responses,
			CachedResponse response) throws IOException {
		List<CachedResponse> list = responses.get(response.getURL());
		if (list != null) {
			list.remove(response);
		}
	}

	private void store(Map<String, List<CachedResponse>> responses, File index)
			throws FileNotFoundException, IOException {
		PrintWriter writer = new PrintWriter(new FileWriter(index));
		try {
			for (List<CachedResponse> list : responses.values()) {
				for (CachedResponse cached : list) {
					writer.println(cached.store());
				}
			}
		} finally {
			writer.close();
		}
	}

}
