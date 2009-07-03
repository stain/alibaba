package org.openrdf.server.metadata.cache;

import info.aduna.concurrent.locks.Lock;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.WeakHashMap;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openrdf.server.metadata.http.RequestHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CachingFilter implements Filter {
	private static String hostname;
	static {
		try {
			hostname = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			hostname = "AliBaba";
		}
	}
	private static String WARN_110 = "110 " + hostname
			+ " \"Response is stale\"";
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
		if (headers.isStorable()) {
			try {
				useCache(headers, req, res, chain);
			} catch (InterruptedException e) {
				logger.warn(e.getMessage(), e);
				res.sendError(504); // Gateway Timeout
				return;
			}
		} else {
			if (headers.invalidatesCache()) {
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

	private void useCache(RequestHeader headers, HttpServletRequest req,
			HttpServletResponse res, FilterChain chain) throws IOException,
			InterruptedException, ServletException {
		Lock used = null;
		try {
			long now = System.currentTimeMillis();
			CachedResponse cached = null;
			CacheIndex index = findCacheIndex(headers.getFile());
			Lock lock = index.lock();
			try {
				cached = index.find(headers);
				boolean stale = isStale(cached, headers, now);
				if (stale && !headers.isOnlyIfCache()) {
					File dir = index.getDirectory();
					String url = headers.getRequestURL();
					CachableRequest cachable;
					FileResponse body;
					String match = index.findCachedETags(headers);
					cachable = new CachableRequest(req, cached, match);
					body = new FileResponse(url, cachable, res, dir, lock);
					chain.doFilter(cachable, body);
					body.flushBuffer();
					if (body.isCachable()) {
						cached = cacheResponse(index, headers, body, cached);
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
		if (cached == null || headers.isNoCache() || cached.isStale())
			return true;
		int age = cached.getAge(now);
		int lifeTime = cached.getLifeTime();
		int maxage = headers.getMaxAge();
		int minFresh = headers.getMinFresh();
		int maxStale = 0;
		if (!cached.mustRevalidate()) {
			maxStale = headers.getMaxStale();
		}
		boolean fresh = age - lifeTime + minFresh <= maxStale;
		return age > maxage || !fresh;
	}

	private CachedResponse cacheResponse(CacheIndex index,
			RequestHeader headers, FileResponse response, CachedResponse cached)
			throws IOException, InterruptedException {
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
		int age = cached.getAge(now);
		if (modifiedSince) {
			if (statusText == null) {
				res.setStatus(status);
			} else {
				res.setStatus(status, statusText);
			}
			for (Map.Entry<String, String> e : cached.getHeaders().entrySet()) {
				res.setHeader(e.getKey(), e.getValue());
			}
			res.setIntHeader("Age", age);
			if (age > cached.getLifeTime()) {
				res.addHeader("Warning", WARN_110);
			}
			if (cached.isBodyPresent()) {
				res.setHeader("Content-Length", Long.toString(cached
						.getContentLength()));
				if (!"HEAD".equals(req.getMethod())) {
					ServletOutputStream out = res.getOutputStream();
					try {
						cached.writeBodyTo(out);
					} finally {
						out.close();
					}
				}
			}
		} else if ("GET".equals(req.getMethod())
				|| "HEAD".equals(req.getMethod())) {
			res.setStatus(304);
			for (Map.Entry<String, String> e : cached.getHeaders().entrySet()) {
				res.setHeader(e.getKey(), e.getValue());
			}
			res.setIntHeader("Age", age);
		} else {
			res.setStatus(412);
			for (Map.Entry<String, String> e : cached.getHeaders().entrySet()) {
				res.setHeader(e.getKey(), e.getValue());
			}
			res.setIntHeader("Age", age);
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
