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

	public CachedResponse find(FileResponse response) throws IOException,
			InterruptedException {
		String method = response.getMethod();
		String url = response.getUrl();
		String entityTag = response.getEntityTag();
		List<CachedResponse> list = responses.get(url);
		if (list != null) {
			for (CachedResponse cached : list) {
				if (cached.getMethod().equals(method)
						&& cached.getEntityTag().equals(entityTag)) {
					cached.setResponse(response);
					return cached;
				}
			}
		}
		String hex = Integer.toHexString(url.hashCode());
		String name = "$" + method + '-' + hex + '-' + entityTag;
		File body = new File(dir, name);
		File head = new File(dir, name + "-head");
		return new CachedResponse(method, url, response, head, body);
	}

	public CachedResponse find(RequestHeader req) {
		String method = req.getMethod();
		if ("HEAD".equals(method)) {
			method = "GET";
		}
		String url = req.getRequestURL();
		List<CachedResponse> list = responses.get(url);
		if (list == null)
			return null;
		boolean authorized = req.getHeader("Authorization") != null;
		for (CachedResponse cached : list) {
			if (cached.getMethod().equals(method) && cached.isVariation(req)) {
				if (authorized && !cached.isPublic())
					continue;
				return cached;
			}
		}
		return null;
	}

	public void replace(CachedResponse stale, CachedResponse fresh)
			throws IOException, InterruptedException {
		if (stale == fresh)
			return;
		if (responses.isEmpty()) {
			dir.mkdirs();
		}
		if (stale != null) {
			List<CachedResponse> list = responses.get(stale.getURL());
			if (list != null) {
				list.remove(stale);
				stale.delete();
			}
		}
		List<CachedResponse> list = responses.get(fresh.getURL());
		if (list == null) {
			list = new LinkedList<CachedResponse>();
			responses.put(fresh.getURL(), list);
		}
		list.add(fresh);
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
				CachedResponse response = new CachedResponse(file, body);
				List<CachedResponse> list = responses.get(response.getURL());
				if (list == null) {
					list = new LinkedList<CachedResponse>();
					responses.put(response.getURL(), list);
				}
				list.add(response);
			}
		}
		return responses;
	}

}
