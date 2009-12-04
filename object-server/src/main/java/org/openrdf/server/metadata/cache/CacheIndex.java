package org.openrdf.server.metadata.cache;

import info.aduna.concurrent.locks.Lock;
import info.aduna.net.ParsedURI;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CacheIndex extends
		LinkedHashMap<String, WeakReference<CachedRequest>> {
	private File dir;
	private int maxCapacity;

	public CacheIndex(File dir, int maxCapacity) {
		super(maxCapacity, 0.75f, true);
		this.dir = dir;
		this.maxCapacity = maxCapacity;
	}

	public int getMaxCapacity() {
		return maxCapacity;
	}

	public void setMaxCapacity(int maxCapacity) {
		this.maxCapacity = maxCapacity;
	}

	public void invalidate(String... locations) throws IOException,
			InterruptedException {
		List<String> urls = new ArrayList(locations.length);
		for (String location : locations) {
			if (location == null)
				continue;
			ParsedURI parsed = new ParsedURI(location);
			File base = new File(dir, safe(parsed.getAuthority()));
			File path = new File(base, safe(parsed.getPath()));
			StringBuilder sb = new StringBuilder(64);
			sb.append(parsed.getScheme());
			sb.append("://");
			sb.append(parsed.getAuthority());
			sb.append(parsed.getPath());
			String code = Integer.toHexString(sb.toString().hashCode());
			File dir = new File(path, '$' + code);
			File[] files = dir.listFiles();
			if (files == null)
				continue;
			for (File file : files) {
				String url = CachedRequest.getURL(file);
				if (url == null)
					continue;
				urls.add(url);
			}
		}
		for (String url : urls) {
			CachedRequest index = findCachedRequest(url);
			Lock lock = index.lock();
			try {
				index.stale();
			} finally {
				lock.release();
			}
		}
	}

	public synchronized CachedRequest findCachedRequest(String url)
			throws IOException {
		CachedRequest index;
		WeakReference<CachedRequest> ref = get(url);
		if (ref == null) {
			index = new CachedRequest(getFile(url));
			put(url, new WeakReference<CachedRequest>(index));
		} else {
			index = ref.get();
			if (index == null) {
				index = new CachedRequest(getFile(url));
				put(url, new WeakReference<CachedRequest>(index));
			}
		}
		return index;
	}

	@Override
	protected boolean removeEldestEntry(
			Map.Entry<String, WeakReference<CachedRequest>> eldest) {
		if (size() <= maxCapacity)
			return false;
		CachedRequest index = eldest.getValue().get();
		if (index == null) {
			File file = getFile(eldest.getKey());
			CachedRequest.delete(file);
			deldirs(file.getParentFile());
			return true;
		}
		try {
			Lock lock = index.lock();
			try {
				index.delete();
				deldirs(index.getDirectory().getParentFile());
			} finally {
				lock.release();
			}
			return true;
		} catch (InterruptedException e) {
			return false;
		}
	}

	private void deldirs(File file) {
		if (file != null && file.delete()) {
			deldirs(file.getParentFile());
		}
	}

	private File getFile(String url) {
		ParsedURI parsed = new ParsedURI(url);
		File base = new File(dir, safe(parsed.getAuthority()));
		File path = new File(base, safe(parsed.getPath()));
		StringBuilder sb = new StringBuilder(64);
		sb.append(parsed.getScheme());
		sb.append("://");
		sb.append(parsed.getAuthority());
		sb.append(parsed.getPath());
		String uri = '$' + Integer.toHexString(sb.toString().hashCode());
		String name = Integer.toHexString(url.hashCode());
		return new File(new File(path, uri), '$' + name);
	}

	private String safe(String path) {
		path = path.replace('/', File.separatorChar);
		path = path.replace('\\', File.separatorChar);
		return path.replaceAll("[^a-zA-Z0-9/\\\\]", "_");
	}

}
