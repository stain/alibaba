package org.openrdf.server.metadata.cache;

import info.aduna.concurrent.locks.Lock;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
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
		List<Long> range = range(req, cached);
		int status = cached.getStatus();
		String statusText = cached.getStatusText();
		String method = req.getMethod();
		if (!unmodifiedSince) {
			res.setStatus(412); // Precondition Failed
		} else if (!modifiedSince
				&& ("GET".equals(method) || "HEAD".equals(method))) {
			res.setStatus(304); // Not Modified
		} else if (!modifiedSince) {
			res.setStatus(412); // Precondition Failed
		} else if (status == 200 && range != null && range.isEmpty()) {
			res.setStatus(416); // Requested Range Not Satisfiable
		} else if (status == 200 && range != null) {
			res.setStatus(206); // Partial Content
		} else if (statusText == null) {
			res.setStatus(status);
		} else {
			res.setStatus(status, statusText);
		}
		sendEntityHeaders(now, cached, res);
		if (unmodifiedSince && modifiedSince) {
			sendContentHeaders(cached, res);
			if (range != null) {
				sendRangeBody(method, range, cached, res);
			} else {
				sendMessageBody(method, cached, res);
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

	private boolean modifiedSince(HttpServletRequest req, CachedResponse cached) {
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

	/**
	 * None range request return null.
	 * Not satisfiable requests return an empty list.
	 * Satisfiable requests return a list of start and length pairs.
	 */
	private List<Long> range(HttpServletRequest req, CachedResponse cached) {
		if (!cached.isBodyPresent())
			return null;
		String tag = req.getHeader("If-Range");
		if (tag != null) {
			if (tag.startsWith("W/") || tag.charAt(0) == '"') {
				if (!match(cached.getETag(), tag))
					return null;
			} else {
				try {
					long date = req.getDateHeader("If-Range");
					if (cached.lastModified() > date)
						return null;
				} catch (IllegalArgumentException e) {
					// invalid date header
					return null;
				}
			}
		}
		try {
			String range = req.getHeader("Range");
			if (range == null || !range.startsWith("bytes="))
				return null;
			Long length = cached.contentLength();
			List<Long> ranges = new ArrayList<Long>();
			for (String r : range.substring(6).split("\\s*,\\s*")) {
				int idx = r.indexOf('-');
				if (idx == 0) {
					long l = Long.parseLong(r.substring(1));
					if (l > length)
						return null;
					ranges.add(length - l);
					ranges.add(l);
				} else if (idx < 0) {
					long l = Long.parseLong(r);
					if (l >= length)
						return Collections.emptyList();
					ranges.add(l);
					ranges.add(length - l);
				} else {
					long b = Long.parseLong(r.substring(0, idx));
					long e = Long.parseLong(r.substring(idx + 1));
					if (b > e)
						return null;
					if (b >= length)
						return Collections.emptyList();
					if (b == 0 && e + 1 >= length)
						return null;
					ranges.add(b);
					if (e < length) {
						ranges.add(e + 1 - b);
					} else {
						ranges.add(length - b);
					}
				}
			}
			return ranges;
		} catch (Exception e) {
			return null;
		}
	}

	private boolean match(String tag, String match) {
		if (tag == null)
			return false;
		if ("*".equals(match))
			return true;
		return match.equals(tag);
	}

	private void sendEntityHeaders(long now, CachedResponse cached,
			HttpServletResponse res) throws IOException {
		int age = cached.getAge(now);
		res.setIntHeader("Age", age);
		if (age > cached.getLifeTime()) {
			res.addHeader("Warning", WARN_110);
		}
		String warning = cached.getWarning();
		if (warning != null) {
			res.addHeader("Warning", warning);
		}
		String tag = cached.getETag();
		if (tag != null) {
			res.setHeader("ETag", tag);
		}
		if (cached.lastModified() > 0) {
			res.setHeader("Last-Modified", cached.getLastModified());
		}
		if (cached.date() > 0) {
			res.setHeader("Date", cached.getDate());
		}
		String type = cached.getContentType();
		if (type != null) {
			res.setHeader("Content-Type", type);
		}
	}

	private void sendContentHeaders(CachedResponse cached,
			HttpServletResponse res) {
		String vary = cached.getVary();
		if (vary != null) {
			res.setHeader("Vary", vary);
		}
		String link = cached.getLink();
		if (link != null) {
			res.setHeader("Link", link);
		}
		String encoding = cached.getContentEncoding();
		if (encoding != null) {
			res.setHeader("Content-Encoding", encoding);
		}
		String md5 = cached.getContentMD5();
		if (md5 != null) {
			res.setHeader("Content-MD5", md5);
		}
		String contentLocation = cached.getContentLocation();
		if (contentLocation != null) {
			res.setHeader("Content-Location", contentLocation);
		}
		String location = cached.getLocation();
		if (location != null) {
			res.setHeader("Location", location);
		}
		String language = cached.getContentLanguage();
		if (language != null) {
			res.setHeader("Content-Language", language);
		}
		String control = cached.getCacheControl();
		if (control != null) {
			res.setHeader("Cache-Control", control);
		}
		String allow = cached.getAllow();
		if (allow != null) {
			res.setHeader("Allow", allow);
		}
	}

	private void sendRangeBody(String method, List<Long> range,
			CachedResponse cached, HttpServletResponse res) throws IOException {
		if (range.size() == 0)
			return;
		long contentLength = cached.contentLength();
		if (range.size() == 2) {
			long start = range.get(0);
			long length = range.get(1);
			long end = start + length - 1;
			String contentRange = "bytes " + start + "-" + end + "/"
					+ contentLength;
			res.setHeader("Content-Range", contentRange);
			res.setHeader("Content-Length", Long.toString(length));
			if (!"HEAD".equals(method)) {
				ServletOutputStream out = res.getOutputStream();
				try {
					res.flushBuffer();
					cached.writeBodyTo(out, res.getBufferSize(), start, length);
				} finally {
					out.close();
				}
			}
		} else {
			String boundary = "THIS_STRING_SEPARATES";
			res.setContentType("multipart/byteranges; boundary=" + boundary);
			if (!"HEAD".equals(method)) {
				ServletOutputStream out = res.getOutputStream();
				try {
					out.print("--");
					out.println(boundary);
					for (int i=0,n=range.size();i<n;i+=2) {
						long start = range.get(i);
						long length = range.get(i+1);
						long end = start + length - 1;
						String type = cached.getContentType();
						if (type != null) {
							out.print("Content-Type: ");
							out.println(type);
						}
						out.print("Content-Length: ");
						out.println(Long.toString(length));
						out.print("Content-Range: bytes ");
						out.print(start);
						out.print("-");
						out.print(end);
						out.print("/");
						out.println(contentLength);
						out.println();
						res.flushBuffer();
						cached.writeBodyTo(out, res.getBufferSize(), start, length);
						out.println();
						out.print("--");
						out.println(boundary);
					}
				} finally {
					out.close();
				}
			}
		}
	}

	private void sendMessageBody(String method, CachedResponse cached,
			HttpServletResponse res) throws IOException {
		res.setHeader("Accept-Ranges", "bytes");
		if (cached.getContentLength() != null) {
			res.setHeader("Content-Length", cached.getContentLength());
		}
		if (!"HEAD".equals(method) && cached.isBodyPresent()) {
			ServletOutputStream out = res.getOutputStream();
			try {
				res.flushBuffer();
				cached.writeBodyTo(out, res.getBufferSize());
			} finally {
				out.close();
			}
		}
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
