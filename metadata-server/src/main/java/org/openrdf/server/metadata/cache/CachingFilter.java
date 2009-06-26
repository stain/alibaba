package org.openrdf.server.metadata.cache;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Enumeration;
import java.util.LinkedHashMap;
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

public class CachingFilter implements Filter {
	private File dataDir;
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
		if (storable) {
			Enumeration cc = headers.getHeaders("Cache-Control");
			while (cc.hasMoreElements()) {
				if (cc.nextElement().toString().contains("no-store")) {
					storable = false;
				}
			}
		}
		if (storable) {
			CacheIndex index = findCacheIndex(headers.getFile());
			CachedResponse cached = null;
			try {
				boolean stale = true;
				long now = System.currentTimeMillis();
				if (index.exists()) {
					cached = index.findCacheFor(headers);
					if (cached != null) {
						int age = cached.getAge(now);
						int lifeTime = cached.getLifeTime();
						int maxage = headers.getMaxAge();
						int minFresh = headers.getMinFresh();
						int maxStale = headers.getMaxStale();
						boolean fresh = age - lifeTime - minFresh <= maxStale;
						stale = age > maxage || !fresh;
					}
				}
				if (stale) {
					cached = storeNewResponse(headers, req, cached, res, chain,
							index);
				}
				if (cached != null) {
					respondWithCache(now, req, cached, res);
				}
			} finally {
				if (cached != null) {
					cached.close();
				}
			}
		} else {
			if (!safe && !method.equals("TRACE") && !method.equals("COPY")
					&& !method.equals("LOCK") && !method.equals("UNLOCK")) {
				CacheIndex index = findCacheIndex(headers.getFile());
				index.stale();
			}
			chain.doFilter(req, res);
		}
	}

	private synchronized CacheIndex findCacheIndex(File file) {
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

	private CachedResponse storeNewResponse(RequestHeader headers,
			HttpServletRequest req, CachedResponse stale,
			HttpServletResponse res, FilterChain chain, CacheIndex index)
			throws IOException, ServletException {
		CachedResponse cached = index.createCachedResponse();
		try {
			FileResponse store = new FileResponse(cached, res);
			CachableRequest cachable = new CachableRequest(req, stale);
			chain.doFilter(cachable, store);
			store.flushBuffer();
			if (!store.isCachable()) {
				return null;
			}
			if (store.isNotModified()) {
				assert stale != null;
				stale.setHeaders(store.getHeaders());
				return stale;
			}
			cached.setMethod(cachable.getMethod());
			cached.setURL(headers.getRequestURL());
			Enumeration names = req.getHeaderNames();
			Map<String, String> map = new LinkedHashMap<String, String>();
			while (names.hasMoreElements()) {
				String name = (String) names.nextElement();
				Enumeration values = req.getHeaders(name);
				while (values.hasMoreElements()) {
					String value = (String) values.nextElement();
					String existing = map.get(name);
					if (existing == null) {
						map.put(name, value);
					} else {
						map.put(name, existing + "," + value);
					}
				}
			}
			for (Map.Entry<String, String> e : map.entrySet()) {
				cached.setRequestHeader(e.getKey(), e.getValue());
			}
			index.add(cached);
			index.remove(stale);
			return cached;
		} finally {
			cached.close();
		}
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
			String header;
			while ((header = cached.getHeaderName()) != null) {
				res.addHeader(header, cached.getHeaderValue());
			}
			res.setIntHeader("Age", cached.getAge(now));
			if (cached.isBodyPresent()) {
				res.setHeader("Content-Length", Long.toString(cached
						.getContentLength()));
				if (!"HEAD".equals(req.getMethod())) {
					cached.writeBodyTo(res.getOutputStream());
				}
			}
		} else if ("GET".equals(req.getMethod())
				|| "HEAD".equals(req.getMethod())) {
			res.setStatus(304);
			String header;
			while ((header = cached.getHeaderName()) != null) {
				res.addHeader(header, cached.getHeaderValue());
			}
			res.setIntHeader("Age", cached.getAge(now));
		} else {
			res.setStatus(412);
			String header;
			while ((header = cached.getHeaderName()) != null) {
				res.addHeader(header, cached.getHeaderValue());
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
