package org.openrdf.server.metadata.cache;

import info.aduna.concurrent.locks.ExclusiveLockManager;
import info.aduna.concurrent.locks.Lock;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.openrdf.server.metadata.http.RequestHeader;

public class CacheIndex {
	private File dir;
	private ExclusiveLockManager locker = new ExclusiveLockManager();
	private List<CachedResponse> responses;

	public CacheIndex(File file) throws IOException {
		dir = file.getParentFile();
		if (dir.exists()) {
			responses = load(dir);
		} else {
			responses = new LinkedList<CachedResponse>();
		}
	}

	public Lock lock() throws InterruptedException {
		return locker.getExclusiveLock();
	}

	public File getDirectory() {
		return dir;
	}

	public CachedResponse find(RequestHeader req) {
		String method = req.getMethod();
		if ("HEAD".equals(method)) {
			method = "GET";
		}
		String url = req.getRequestURL();
		boolean authorized = req.getHeader("Authorization") != null;
		for (CachedResponse cached : responses) {
			if ((!authorized || cached.isPublic())
					&& cached.getMethod().equals(method)
					&& cached.isVariation(req) && cached.getURL().equals(url)) {
				return cached;
			}
		}
		return null;
	}

	public String findCachedETags(RequestHeader req) {
		String url = req.getRequestURL();
		String method = req.getMethod();
		boolean authorized = req.getHeader("Authorization") != null;
		if ("HEAD".equals(method)) {
			method = "GET";
		}
		StringBuilder sb = new StringBuilder();
		for (CachedResponse cached : responses) {
			if ((!authorized || cached.isPublic())
					&& cached.getMethod().equals(method)
					&& cached.getURL().equals(url)) {
				if (sb.length() > 0) {
					sb.append(",");
				}
				sb.append(cached.getETag());
			}
		}
		return sb.toString();
	}

	public CachedResponse find(FileResponse response) throws IOException,
			InterruptedException {
		String method = response.getMethod();
		String url = response.getUrl();
		String entityTag = response.getEntityTag();
		for (CachedResponse cached : responses) {
			if (cached.getEntityTag().equals(entityTag)
					&& cached.getMethod().equals(method)
					&& cached.getURL().equals(url)) {
				cached.setResponse(response);
				return cached;
			}
		}
		String hex = Integer.toHexString(url.hashCode());
		String name = "$" + method + '-' + hex + '-' + entityTag;
		File body = new File(dir, name);
		File head = new File(dir, name + "-head");
		return new CachedResponse(method, url, response, head, body);
	}

	public void replace(CachedResponse stale, CachedResponse fresh)
			throws IOException, InterruptedException {
		if (stale == fresh)
			return;
		if (responses.isEmpty()) {
			dir.mkdirs();
		}
		if (stale != null) {
			responses.remove(stale);
			stale.delete();
		}
		if (!responses.contains(fresh)) {
			responses.add(fresh);
		}
	}

	public void stale() throws IOException {
		for (CachedResponse cached : responses) {
			cached.setStale(true);
		}
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (CachedResponse cached : responses) {
			sb.append(cached.toString()).append("\n");
		}
		return sb.toString();
	}

	private List<CachedResponse> load(File dir) throws FileNotFoundException,
			IOException {
		List<CachedResponse> responses;
		responses = new LinkedList<CachedResponse>();
		for (File file : dir.listFiles()) {
			String name = file.getName();
			if (name.endsWith("-head")) {
				File body = new File(dir, name.substring(0, name.length() - 5));
				CachedResponse response = new CachedResponse(file, body);
				responses.add(response);
			}
		}
		return responses;
	}

}
