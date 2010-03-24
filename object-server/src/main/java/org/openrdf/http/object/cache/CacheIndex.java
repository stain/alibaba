/*
 * Copyright (c) 2009-2010, James Leigh and Zepheira LLC Some rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution. 
 * - Neither the name of the openrdf.org nor the names of its contributors may
 *   be used to endorse or promote products derived from this software without
 *   specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 */
package org.openrdf.http.object.cache;

import info.aduna.concurrent.locks.Lock;
import info.aduna.net.ParsedURI;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages multiple cache instances by URL.
 */
public class CacheIndex extends
		LinkedHashMap<String, WeakReference<CachedRequest>> {
	private static final long serialVersionUID = -833236420826697261L;
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
		File dir = new File(base, safe(parsed.getPath()));
		String uri;
		int idx = url.lastIndexOf('?');
		if (idx > 0) {
			uri = url.substring(0, idx);
		} else {
			uri = url;
		}
		String identity = '$' + Integer.toHexString(uri.hashCode());
		String name = Integer.toHexString(url.hashCode());
		return new File(new File(dir, identity), '$' + name);
	}

	private String safe(String path) {
		if (path == null)
			return "";
		path = path.replace('/', File.separatorChar);
		path = path.replace('\\', File.separatorChar);
		path = path.replace(':', File.separatorChar);
		return path.replaceAll("[^a-zA-Z0-9/\\\\]", "_");
	}

}
