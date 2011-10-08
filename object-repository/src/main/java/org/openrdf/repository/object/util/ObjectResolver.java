/*
 * Copyright (c) 2011 Talis Inc., Some rights reserved.
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
package org.openrdf.repository.object.util;

import static java.lang.Integer.parseInt;
import static java.lang.System.currentTimeMillis;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Using the supplied factory, caches a Java object from URL.
 *
 * @author James Leigh
 **/
public class ObjectResolver<T> {
	public interface ObjectFactory<T> {

		String[] getContentTypes();

		boolean isReusable();

		T create(String systemId, InputStream in) throws Exception;

		T create(String systemId, Reader in) throws Exception;
	}

	public static ObjectResolver<?> newInstance() {
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		Object inst = findProvider(cl, OBJECT_RESOLVER_SRV);
		if (inst == null)
			return new ObjectResolver<Object>();
		return (ObjectResolver<?>) inst;
	}

	public static ObjectResolver<?> newInstance(ClassLoader cl) {
		Object inst = findProvider(cl, OBJECT_RESOLVER_SRV);
		if (inst == null)
			return new ObjectResolver<Object>();
		return (ObjectResolver<?>) inst;
	}

	public static <N> ObjectResolver<N> newInstance(ObjectFactory<N> factory) {
		ObjectResolver<N> ret = (ObjectResolver<N>) newInstance();
		ret.setObjectFactory(factory);
		return ret;
	}

	public static <N> ObjectResolver<N> newInstance(ClassLoader cl,
			ObjectFactory<N> factory) {
		ObjectResolver<N> ret = (ObjectResolver<N>) newInstance(cl);
		ret.setObjectFactory(factory);
		return ret;
	}

	private static final String OBJECT_RESOLVER_SRV = "META-INF/services/"
			+ ObjectResolver.class.getName();
	private static Logger logger = LoggerFactory.getLogger(ObjectResolver.class);
	private static final Pattern SMAXAGE = Pattern
			.compile("\\bs-maxage\\s*=\\s*(\\d+)");
	private static final Pattern MAXAGE = Pattern
			.compile("\\bmax-age\\s*=\\s*(\\d+)");
	private static final Pattern CHARSET = Pattern
			.compile("\\bcharset\\s*=\\s*([\\w-:]+)");

	private static Object findProvider(ClassLoader cl, String resource) {
		try {
			Enumeration<URL> resources = cl.getResources(resource);
			while (resources.hasMoreElements()) {
				try {
					InputStream in = resources.nextElement().openStream();
					try {
						Properties properties = new Properties();
						properties.load(in);
						Enumeration<?> names = properties.propertyNames();
						while (names.hasMoreElements()) {
							String name = (String) names.nextElement();
							try {
								Class<?> c = forName(name, true, cl);
								return c.newInstance();
							} catch (ClassNotFoundException e) {
								logger.warn(e.toString());
							} catch (InstantiationException e) {
								logger.warn(e.toString());
							} catch (IllegalAccessException e) {
								logger.warn(e.toString());
							}
						}
					} finally {
						in.close();
					}
				} catch (IOException e) {
					logger.warn(e.toString());
				}
			}
		} catch (IOException e) {
			logger.warn(e.toString());
		}
		return null;
	}

	private static Class<?> forName(String name, boolean init, ClassLoader cl)
			throws ClassNotFoundException {
		synchronized (cl) {
			return Class.forName(name, init, cl);
		}
	}

	private ObjectFactory<T> factory;
	private String uri;
	private String tag;
	private Integer maxage;
	private long expires;
	private T object;

	public ObjectFactory<T> getObjectFactory() {
		return factory;
	}

	public void setObjectFactory(ObjectFactory<T> factory) {
		this.factory = factory;
	}

	/**
	 * returns null for 404 resources.
	 */
	public synchronized T resolve(String systemId) throws Exception {
		if (uri == null || !uri.equals(systemId)) {
			uri = systemId;
			object = null;
			tag = null;
			expires = 0;
			maxage = null;
		} else if (object != null
				&& (expires == 0 || expires > currentTimeMillis())) {
			return object;
		}
		URLConnection con = new URL(systemId).openConnection();
		con.addRequestProperty("Accept", join(getObjectFactory()
				.getContentTypes()));
		con.addRequestProperty("Accept-Encoding", "gzip");
		if (tag != null && object != null) {
			con.addRequestProperty("If-None-Match", tag);
		}
		try {
			if (isStorable(con.getHeaderField("Cache-Control"))) {
				return object = createObject(con);
			} else {
				object = null;
				tag = null;
				expires = 0;
				maxage = 0;
				return createObject(con);
			}
		} catch (FileNotFoundException e) {
			object = null;
			tag = null;
			expires = 0;
			maxage = 0;
			return null;
		}
	}

	private String join(String[] contentTypes) {
		if (contentTypes == null)
			return "*/*";
		int iMax = contentTypes.length - 1;
		if (iMax == -1)
			return "*/*";

		StringBuilder b = new StringBuilder();
		for (int i = 0;; i++) {
			b.append(String.valueOf(contentTypes[i]));
			if (i == iMax)
				return b.toString();
			b.append(", ");
		}
	}

	private boolean isStorable(String cc) {
		if (!getObjectFactory().isReusable())
			return false;
		return cc == null || !cc.contains("no-store")
				&& (!cc.contains("private") || cc.contains("public"));
	}

	private T createObject(URLConnection con) throws Exception {
		String cacheControl = con.getHeaderField("Cache-Control");
		long date = con.getHeaderFieldDate("Expires", expires);
		expires = getExpires(cacheControl, date);
		if (con instanceof HttpURLConnection) {
			int status = ((HttpURLConnection) con).getResponseCode();
			if (status == 304 || status == 412) {
				return object; // Not Modified
			}
		}
		if (getObjectFactory().isReusable()) {
			logger.info("Compiling {}", con.getURL());
		}
		tag = con.getHeaderField("ETag");
		String base = con.getURL().toExternalForm();
		String type = con.getContentType();
		String encoding = con.getHeaderField("Content-Encoding");
		InputStream in = con.getInputStream();
		if (encoding != null && encoding.contains("gzip")) {
			in = new GZIPInputStream(in);
		}
		Matcher m = CHARSET.matcher(type);
		if (m.find()) {
			Reader reader = new InputStreamReader(in, m.group(1));
			return getObjectFactory().create(base, reader);
		}
		return getObjectFactory().create(base, in);
	}

	private long getExpires(String cacheControl, long defaultValue) {
		if (cacheControl != null && cacheControl.contains("s-maxage")) {
			try {
				Matcher m = SMAXAGE.matcher(cacheControl);
				if (m.find()) {
					maxage = parseInt(m.group(1));
				}
			} catch (NumberFormatException e) {
				// skip
			}
		} else if (cacheControl != null && cacheControl.contains("max-age")) {
			try {
				Matcher m = MAXAGE.matcher(cacheControl);
				if (m.find()) {
					maxage = parseInt(m.group(1));
				}
			} catch (NumberFormatException e) {
				// skip
			}
		}
		if (maxage != null)
			return currentTimeMillis() + maxage * 1000;
		return defaultValue;
	}

}
