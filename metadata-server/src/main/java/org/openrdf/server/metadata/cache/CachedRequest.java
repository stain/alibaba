package org.openrdf.server.metadata.cache;

import info.aduna.concurrent.locks.ExclusiveLockManager;
import info.aduna.concurrent.locks.Lock;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.openrdf.server.metadata.http.RequestHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CachedRequest {
	private static Logger logger = LoggerFactory.getLogger(CachedRequest.class);

	public static void delete(File dir) {
		File[] files = dir.listFiles();
		if (files == null)
			return;
		for (File file : files) {
			String name = file.getName();
			if (name.endsWith("-head")) {
				File body = new File(dir, name.substring(0, name.length() - 5));
				file.delete();
				body.delete();
			}
		}
		dir.delete();
	}

	public static String getURL(File dir) {
		File[] files = dir.listFiles();
		if (files == null)
			return null;
		for (File file : files) {
			String name = file.getName();
			if (name.endsWith("-head")) {
				try {
					return CachedEntity.getURL(file);
				} catch (IOException e) {
					// try the next one
					logger.warn(e.toString(), e);
				}
			}
		}
		return null;
	}

	private File dir;
	private ExclusiveLockManager locker = new ExclusiveLockManager();
	private List<CachedEntity> responses;

	public CachedRequest(File dir) throws IOException {
		this.dir = dir;
		if (dir.exists()) {
			responses = load(dir);
		} else {
			responses = new LinkedList<CachedEntity>();
		}
	}

	public void delete() {
		for (CachedEntity cached : responses) {
			try {
				cached.delete();
			} catch (InterruptedException e) {
				// leave it
			}
		}
		dir.delete();
	}

	public Lock lock() throws InterruptedException {
		return locker.getExclusiveLock();
	}

	public File getDirectory() {
		return dir;
	}

	public CachedEntity find(RequestHeader req) {
		String method = req.getMethod();
		if ("HEAD".equals(method)) {
			method = "GET";
		}
		String url = req.getRequestURL();
		boolean authorized = req.getHeader("Authorization") != null;
		Iterator<CachedEntity> iter = responses.iterator();
		while (iter.hasNext()) {
			CachedEntity cached = iter.next();
			if ((!authorized || cached.isPublic())
					&& cached.getMethod().equals(method)
					&& cached.isVariation(req) && cached.getURL().equals(url)) {
				if (cached.isMissing()) {
					iter.remove();
				} else {
					return cached;
				}
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
		for (CachedEntity cached : responses) {
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

	public CachedEntity find(FileResponse response) throws IOException,
			InterruptedException {
		String method = response.getMethod();
		String url = response.getUrl();
		String entityTag = response.getEntityTag();
		for (CachedEntity cached : responses) {
			if (cached.getEntityTag().equals(entityTag)
					&& cached.getMethod().equals(method)
					&& cached.getURL().equals(url)) {
				cached.setResponse(response);
				return cached;
			}
		}
		assert response.isModified();
		String hex = Integer.toHexString(url.hashCode());
		String name = "$" + method + '-' + hex + '-' + entityTag;
		File body = new File(dir, name);
		File head = new File(dir, name + "-head");
		return new CachedEntity(method, url, response, head, body);
	}

	public void replace(CachedEntity stale, CachedEntity fresh)
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
		for (CachedEntity cached : responses) {
			cached.setStale(true);
		}
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (CachedEntity cached : responses) {
			sb.append(cached.toString()).append("\n");
		}
		return sb.toString();
	}

	private List<CachedEntity> load(File dir) {
		List<CachedEntity> responses;
		responses = new LinkedList<CachedEntity>();
		for (File file : dir.listFiles()) {
			String name = file.getName();
			if (name.endsWith("-head")) {
				try {
					File body = new File(dir, name.substring(0,
							name.length() - 5));
					CachedEntity response = new CachedEntity(file, body);
					responses.add(response);
				} catch (Exception e) {
					// skip file
					logger.warn(e.toString(), e);
				}
			}
		}
		return responses;
	}

}
