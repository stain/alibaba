package org.openrdf.server.metadata.cache;

import info.aduna.concurrent.locks.Lock;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Enumeration;
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
				invalidate(headers, req, chain, res);
			} else {
				chain.doFilter(req, res);
			}
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
						CachedResponse fresh = index.find(body);
						fresh.addRequest(headers);
						index.replace(cached, fresh);
						cached = fresh;
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

	private void respondWithCache(long now, HttpServletRequest req,
			CachedResponse cached, HttpServletResponse res) throws IOException {
		boolean unmodifiedSince = unmodifiedSince(req, cached);
		boolean modifiedSince = modifiedSince(req, cached);
		String statusText = cached.getStatusText();
		if (!unmodifiedSince) {
			res.setStatus(412);
		} else if (modifiedSince && statusText == null) {
			res.setStatus(cached.getStatus());
		} else if (modifiedSince) {
			res.setStatus(cached.getStatus(), statusText);
		} else if ("GET".equals(req.getMethod())
				|| "HEAD".equals(req.getMethod())) {
			res.setStatus(304);
		} else {
			res.setStatus(412);
		}
		int age = cached.getAge(now);
		res.setIntHeader("Age", age);
		if (age > cached.getLifeTime()) {
			res.addHeader("Warning", WARN_110);
		}
		if (cached.getWarning() != null) {
			res.addHeader("Warning", cached.getWarning());
		}
		if (cached.getETag() != null) {
			res.setHeader("ETag", cached.getETag());
		}
		if (cached.lastModified() > 0) {
			res.setHeader("Last-Modified", cached.getLastModified());
		}
		if (cached.date() > 0) {
			res.setHeader("Date", cached.getDate());
		}
		if (cached.getContentType() != null) {
			res.setHeader("Content-Type", cached.getContentType());
		}
		if (unmodifiedSince && modifiedSince) {
			if (cached.getVary() != null) {
				res.setHeader("Vary", cached.getVary());
			}
			if (cached.getLink() != null) {
				res.setHeader("Link", cached.getLink());
			}
			if (cached.getContentEncoding() != null) {
				res.setHeader("Content-Encoding", cached.getContentEncoding());
			}
			if (cached.getContentMD5() != null) {
				res.setHeader("Content-MD5", cached.getContentMD5());
			}
			if (cached.getContentLocation() != null) {
				res.setHeader("Content-Location", cached.getContentLocation());
			}
			if (cached.getLocation() != null) {
				res.setHeader("Location", cached.getLocation());
			}
			if (cached.getContentLanguage() != null) {
				res.setHeader("Content-Language", cached.getContentLanguage());
			}
			if (cached.getCacheControl() != null) {
				res.setHeader("Cache-Control", cached.getCacheControl());
			}
			if (cached.getAllow() != null) {
				res.setHeader("Allow", cached.getAllow());
			}
			if (cached.getContentLength() != null) {
				res.setHeader("Content-Length", cached.getContentLength());
			}
			if (!"HEAD".equals(req.getMethod()) && cached.isBodyPresent()) {
				ServletOutputStream out = res.getOutputStream();
				try {
					cached.writeBodyTo(out);
				} finally {
					out.close();
				}
			}
		}
	}

	private boolean unmodifiedSince(HttpServletRequest req,
			CachedResponse cached) {
		try {
			long unmodified = req.getDateHeader("If-Unmodified-Since");
			long lastModified = cached.lastModified();
			if (unmodified > 0 && lastModified > unmodified)
				return false;
		} catch (IllegalArgumentException e) {
			// invalid date header
		}
		Enumeration matchs = req.getHeaders("If-Match");
		boolean mustMatch = matchs.hasMoreElements();
		if (mustMatch) {
			String entityTag = cached.getETag();
			while (matchs.hasMoreElements()) {
				String match = (String) matchs.nextElement();
				if (match(entityTag, match))
					return true;
			}
		}
		return !mustMatch;
	}

	private boolean modifiedSince(HttpServletRequest req, CachedResponse cached)
			throws IOException {
		boolean notModified = false;
		try {
			long modified = req.getDateHeader("If-Modified-Since");
			long lastModified = cached.lastModified();
			notModified = lastModified > 0 && modified > 0;
			if (notModified && modified < lastModified)
				return true;
		} catch (IllegalArgumentException e) {
			// invalid date header
		}
		Enumeration matchs = req.getHeaders("If-None-Match");
		boolean mustMatch = matchs.hasMoreElements();
		if (mustMatch) {
			String entityTag = cached.getETag();
			while (matchs.hasMoreElements()) {
				String match = (String) matchs.nextElement();
				if (match(entityTag, match))
					return false;
			}
		}
		return !notModified || mustMatch;
	}

	private boolean match(String tag, String match) {
		if (tag == null)
			return false;
		if ("*".equals(match))
			return true;
		return match.equals(tag);
	}

	private void invalidate(RequestHeader headers, HttpServletRequest req,
			FilterChain chain, HttpServletResponse res) throws IOException,
			ServletException {
		try {
			invalidate(headers.getFile());
			invalidate(headers.getFile(headers.getHeader("Location")));
			invalidate(headers.getFile(headers.getHeader("Content-Location")));
			ReadableResponse resp = new ReadableResponse(res);
			chain.doFilter(req, resp);
			invalidate(headers.getFile(resp.getHeader("Location")));
			invalidate(headers.getFile(resp.getHeader("Content-Location")));
		} catch (InterruptedException e) {
			logger.warn(e.getMessage(), e);
			res.sendError(503); // Service Unavailable
		}
	}

	private void invalidate(File file) throws IOException, InterruptedException {
		if (file == null)
			return;
		CacheIndex index = findCacheIndex(file);
		Lock lock = index.lock();
		try {
			index.stale();
		} finally {
			lock.release();
		}
	}
}
