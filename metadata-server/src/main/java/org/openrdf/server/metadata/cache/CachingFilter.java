package org.openrdf.server.metadata.cache;

import info.aduna.concurrent.locks.Lock;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Enumeration;
import java.util.Map;
import java.util.WeakHashMap;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openrdf.server.metadata.http.RequestHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CachingFilter implements Filter {
	private File dataDir;
	private Logger logger = LoggerFactory.getLogger(CachingFilter.class);
	private Map<File, WeakReference<CacheIndex>> cache = new WeakHashMap<File, WeakReference<CacheIndex>>();

	public CachingFilter(File dataDir) {
		this.dataDir = dataDir;
	}

	public void init(FilterConfig config) throws ServletException {
		// no-op
	}

	public void destroy() {
		// no-op
	}

	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		HttpServletRequest req = (HttpServletRequest) request;
		HttpServletResponse res = (HttpServletResponse) response;
		RequestHeader headers = new RequestHeader(dataDir, req);
		String method = headers.getMethod();
		boolean head = method.equals("HEAD");
		boolean safe = head || method.equals("GET") || method.equals("OPTIONS")
				|| method.equals("PROFIND");
		boolean storable = safe && !headers.isMessageBody();
		boolean nocache = false;
		boolean onlyifcached = false;
		if (storable) {
			Enumeration cc = headers.getHeaders("Cache-Control");
			while (cc.hasMoreElements()) {
				String value = (String) cc.nextElement();
				if (value.contains("no-store")) {
					storable = false;
				}
				if (value.contains("no-cache")) {
					nocache = true;
				}
				if (value.contains("only-if-cached")) {
					onlyifcached = true;
				}
			}
		}
		if (storable) {
			try {
				Lock used = null;
				try {
					long now = System.currentTimeMillis();
					CachedResponse cached = null;
					CacheIndex index = findCacheIndex(headers.getFile());
					Lock lock = index.lock();
					try {
						cached = index.find(headers);
						boolean stale = nocache || isStale(cached, headers, now);
						if (stale && !onlyifcached) {
							File dir = index.getDirectory();
							String url = headers.getRequestURL();
							// TODO provide list of possible entity tags
							CachableRequest cachable = new CachableRequest(req,
									cached);
							FileResponse body = new FileResponse(url, cachable,
									res, dir, lock);
							chain.doFilter(cachable, body);
							body.flushBuffer();
							if (body.isCachable()) {
								cached = cacheResponse(index, headers, body,
										cached);
								used = cached.open();
							}
						} else if (stale) {
							res.setStatus(504);
						} else {
							used = cached.open();
						}
					} finally {
						lock.release();
					}
					if (used != null) {
						respondWithCache(now, req, cached, res);
					}
				} finally {
					if (used != null) {
						used.release();
					}
				}
			} catch (InterruptedException e) {
				logger.warn(e.getMessage(), e);
				res.sendError(503);
				return;
			}
		} else {
			if (!safe && !method.equals("TRACE") && !method.equals("COPY")
					&& !method.equals("LOCK") && !method.equals("UNLOCK")) {
				// TODO check for Location and Content-Location to invalidate
				CacheIndex index = findCacheIndex(headers.getFile());
				try {
					Lock lock = index.lock();
					try {
						index.stale();
					} finally {
						lock.release();
					}
				} catch (InterruptedException e) {
					logger.warn(e.getMessage(), e);
					res.sendError(503);
					return;
				}
			}
			chain.doFilter(req, res);
		}
	}

	private synchronized CacheIndex findCacheIndex(File file)
			throws IOException {
		CacheIndex index;
		WeakReference<CacheIndex> ref = cache.get(file);
		if (ref == null) {
			index = new CacheIndex(file);
			ref = new WeakReference<CacheIndex>(index);
			cache.put(file, ref);
		} else {
			index = ref.get();
			if (index == null) {
				index = new CacheIndex(file);
				ref = new WeakReference<CacheIndex>(index);
				cache.put(file, ref);
			}
		}
		return index;
	}

	private boolean isStale(CachedResponse cached, RequestHeader headers,
			long now) throws IOException {
		boolean stale = true;
		if (cached != null && !cached.isStale()) {
			int age = cached.getAge(now);
			int lifeTime = cached.getLifeTime();
			int maxage = headers.getMaxAge();
			int minFresh = headers.getMinFresh();
			int maxStale = 0;
			if (!cached.mustRevalidate()) {
				maxStale = headers.getMaxStale();
			}
			boolean fresh = age - lifeTime + minFresh <= maxStale;
			stale = age > maxage || !fresh;
		}
		return stale;
	}

	private CachedResponse cacheResponse(CacheIndex index,
			RequestHeader headers, FileResponse response, CachedResponse cached)
			throws IOException, InterruptedException {
		if (response.isNotModified()) {
			// TODO lookup based on entity tag
			assert cached != null;
			cached.setResponseHeaders(response);
			return cached;
		}
		CachedResponse fresh = index.find(response);
		fresh.addRequest(headers);
		index.replace(cached, fresh);
		return fresh;
	}

	private void respondWithCache(long now, HttpServletRequest req,
			CachedResponse cached, HttpServletResponse res) throws IOException {
		boolean modifiedSince = modifiedSince(req, cached);
		int status = cached.getStatus();
		String statusText = cached.getStatusText();
		// TODO check for other conditional requests
		if (modifiedSince) {
			if (statusText == null) {
				res.setStatus(status);
			} else {
				res.setStatus(status, statusText);
			}
			// TODO check Accept-Encoding headers
			for (Map.Entry<String, String> e : cached.getHeaders().entrySet()) {
				res.setHeader(e.getKey(), e.getValue());
			}
			res.setIntHeader("Age", cached.getAge(now));
			if (cached.isBodyPresent()) {
				res.setHeader("Content-Length", Long.toString(cached
						.getContentLength()));
				if (!"HEAD".equals(req.getMethod())) {
					// TODO gunzip body if needed
					cached.writeBodyTo(res.getOutputStream());
				}
			}
		} else if ("GET".equals(req.getMethod())
				|| "HEAD".equals(req.getMethod())) {
			res.setStatus(304);
			for (Map.Entry<String, String> e : cached.getHeaders().entrySet()) {
				res.setHeader(e.getKey(), e.getValue());
			}
			res.setIntHeader("Age", cached.getAge(now));
		} else {
			res.setStatus(412);
			for (Map.Entry<String, String> e : cached.getHeaders().entrySet()) {
				res.setHeader(e.getKey(), e.getValue());
			}
			res.setIntHeader("Age", cached.getAge(now));
		}
	}

	private boolean modifiedSince(HttpServletRequest req, CachedResponse cached)
			throws IOException {
		try {
			long since = req.getDateHeader("If-Modified-Since");
			if (since <= 0)
				return true;
			long lastModified = cached.getDateHeader("Last-Modified");
			if (lastModified <= since)
				return false;
		} catch (IllegalArgumentException e) {
			// invalid date header
		}
		return true;
	}
}
