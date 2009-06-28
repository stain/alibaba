package org.openrdf.server.metadata.cache;

import info.aduna.concurrent.locks.ExclusiveLockManager;
import info.aduna.concurrent.locks.Lock;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.openrdf.server.metadata.http.RequestHeader;

public class CacheIndex {
	private File dir;
	private ExclusiveLockManager locker = new ExclusiveLockManager();
	private Map<String, List<CachedResponse>> responses;

	public CacheIndex(File file) throws IOException {
		dir = file.getParentFile();
		if (dir.exists()) {
			responses = load(dir);
		} else {
			responses = new HashMap<String, List<CachedResponse>>();
		}
	}

	public Lock lock() throws InterruptedException {
		return locker.getExclusiveLock();
	}

	public File getDirectory() {
		return dir;
	}

	public CachedResponse create(FileResponse response) throws IOException {
		String method = response.getMethod();
		String url = response.getUrl();
		String entityTag = response.getEntityTag();
		File body = response.getMessageBody();
		String name;
		if (body == null) {
			String hex = Integer.toHexString(url.hashCode());
			name = "$" + method + '-' + hex + '-' + entityTag + "-head";
		} else {
			name = body.getName() + "-head";
		}
		File head = new File(dir, name);
		return new CachedResponse(method, url, head, response);
	}

	public CachedResponse find(RequestHeader req) throws IOException {
		String method = req.getMethod();
		if ("HEAD".equals(method)) {
			method = "GET";
		}
		String url = req.getRequestURL();
		List<CachedResponse> list = responses.get(url);
		if (list == null)
			return null;
		for (CachedResponse cached : list) {
			String cachedMethod = cached.getMethod();
			String cachedURL = cached.getURL();
			if (cachedMethod.equals(method) && cachedURL.equals(url)) {
				if (cached.isVariation(req))
					return cached;
				// TODO check Vary headers
				// TODO check Accept-Encoding headers
				// TODO check Cache-Control headers
				// TODO check Authorization and Cache-Control public
			}
		}
		return null;
	}

	public void replace(CachedResponse stale, CachedResponse fresh)
			throws FileNotFoundException, IOException {
		if (responses.isEmpty()) {
			dir.mkdirs();
		}
		if (stale != null) {
			remove(responses, stale);
		}
		add(responses, fresh);
	}

	public void stale() throws IOException {
		if (!responses.isEmpty()) {
			for (List<CachedResponse> list : responses.values()) {
				for (CachedResponse cached : list) {
					cached.setStale(true);
				}
			}
		}
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (List<CachedResponse> list : responses.values()) {
			for (CachedResponse cached : list) {
				sb.append(cached.toString()).append("\n");
			}
		}
		return sb.toString();
	}

	private Map<String, List<CachedResponse>> load(File dir)
			throws FileNotFoundException, IOException {
		Map<String, List<CachedResponse>> responses;
		responses = new HashMap<String, List<CachedResponse>>();
		for (File file : dir.listFiles()) {
			String name = file.getName();
			if (name.endsWith("-head")) {
				File body = new File(dir, name.substring(0, name.length() - 5));
				if (!body.exists()) {
					body = null;
				}
				add(responses, new CachedResponse(file, body));
			}
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
			// TODO remove files
		}
	}

}
