/*
 * Copyright 2010, Zepheira LLC Some rights reserved.
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
package org.openrdf.http.object.handlers;

import static org.openrdf.sail.auditing.vocabulary.Audit.CURRENT_TRX;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.nio.entity.NByteArrayEntity;
import org.openrdf.http.object.annotations.realm;
import org.openrdf.http.object.concepts.Transaction;
import org.openrdf.http.object.filters.HttpEntityWrapper;
import org.openrdf.http.object.filters.MD5ValidationEntity;
import org.openrdf.http.object.model.Handler;
import org.openrdf.http.object.model.ResourceOperation;
import org.openrdf.http.object.model.Response;
import org.openrdf.http.object.traits.Realm;
import org.openrdf.http.object.util.ChannelUtil;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectFactory;
import org.openrdf.repository.object.RDFObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ensures the request is authentic if protected.
 * 
 * @author James Leigh
 * 
 */
public class AuthenticationHandler implements Handler {
	private static final String EMPTY_CONTENT_MD5 = "1B2M2Y8AsgTpgAmY7PhCfg==";
	private static final String REQUEST_METHOD = "Access-Control-Request-Method";
	private static final String ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";
	private static final BasicStatusLine _403 = new BasicStatusLine(
			new ProtocolVersion("HTTP", 1, 1), 403, "Forbidden");
	private final Logger logger = LoggerFactory
			.getLogger(AuthenticationHandler.class);
	private final Handler delegate;
	private final String basic;

	public AuthenticationHandler(Handler delegate, String basic) {
		this.delegate = delegate;
		this.basic = basic;
	}

	public Response verify(ResourceOperation request) throws Exception {
		String method = request.getMethod();
		if (request.isAuthenticating()
				&& !isBoot(method, request.getHeader("Authorization"))) {
			HttpResponse unauthorized = authorize(request);
			if (unauthorized != null) {
				return new Response().unauthorized(unauthorized);
			}
		}
		return allow(request, delegate.verify(request));
	}

	public Response handle(ResourceOperation request) throws Exception {
		return allow(request, delegate.handle(request));
	}

	private Response allow(ResourceOperation request, Response rb)
			throws QueryEvaluationException, RepositoryException {
		if (rb == null)
			return null;
		if (!rb.containsHeader("Access-Control-Allow-Origin")) {
			String origins = allowOrigin(request);
			if (origins != null) {
				rb = rb.header("Access-Control-Allow-Origin", origins);
			}
		}
		if (!rb.containsHeader(ALLOW_CREDENTIALS)) {
			String origin = request.getVaryHeader("Origin");
			if (origin != null) {
				if (withAgentCredentials(request, origin)) {
					rb = rb.header(ALLOW_CREDENTIALS, "true");
				} else {
					rb = rb.header(ALLOW_CREDENTIALS, "false");
				}
			}
		}
		return rb;
	}

	private boolean withAgentCredentials(ResourceOperation request, String origin)
			throws QueryEvaluationException, RepositoryException {
		for (Realm realm : request.getRealms()) {
			if (realm.withAgentCredentials(origin)) {
				return true;
			}
		}
		if ("OPTIONS".equals(request.getMethod())) {
			String m = request.getVaryHeader(REQUEST_METHOD);
			RDFObject target = request.getRequestedResource();
			for (Method method : request.findMethodHandlers(m)) {
				if (method.isAnnotationPresent(realm.class)) {
					if (withAgenCredentials(request, method, target)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private boolean withAgenCredentials(ResourceOperation request, Method method,
			RDFObject target) throws QueryEvaluationException,
			RepositoryException {
		String origin = request.getVaryHeader("Origin");
		String[] values = method.getAnnotation(realm.class).value();
		ObjectConnection con = target.getObjectConnection();
		List<?> list = con.getObjects(Realm.class, values).asList();
		for (Object r : list) {
			if (request.isRealm(r)) {
				Realm realm = (Realm) r;
				if (realm.withAgentCredentials(origin)) {
					return true;
				}
			}
		}
		return false;
	}

	private HttpResponse authorize(ResourceOperation request)
			throws QueryEvaluationException, RepositoryException, IOException {
		HttpResponse unauth = null;
		boolean noRealm = true;
		boolean wrongOrigin = true;
		String m = request.getMethod();
		RDFObject target = request.getRequestedResource();
		String qs = request.getQueryString();
		String or = request.getVaryHeader("Origin");
		Map<String, String[]> map = getAuthorizationMap(request);
		for (Realm realm : request.getRealms()) {
			noRealm = false;
			try {
				String allowed = realm.allowOrigin();
				if (or != null && !isOriginAllowed(allowed, or)) {
					try {
						unauth = choose(unauth, realm.forbidden(target));
					} catch (Exception exc) {
						logger.error(exc.toString(), exc);
					}
					continue;
				}
				wrongOrigin = false;
				Object cred = realm.authenticateRequest(m, target, map);
				if (cred != null
						&& realm.authorizeCredential(cred, m, target, qs)) {
					ObjectConnection con = request.getObjectConnection();
					ObjectFactory of = con.getObjectFactory();
					Transaction trans = of.createObject(CURRENT_TRX,
							Transaction.class);
					trans.setHttpAuthorized(cred);
					request.setCredential(cred);
					return null;
				} else {
					try {
						if (cred == null) {
							unauth = choose(unauth, realm.unauthorized(target));
						} else {
							unauth = choose(unauth, realm.forbidden(target));
						}
					} catch (Exception exc) {
						logger.error(exc.toString(), exc);
					}
				}
			} catch (AbstractMethodError ame) {
				logger.error(ame.toString() + " in " + realm, ame);
			}
		}
		if (noRealm) {
			logger.info("No active realm for {}", request);
		} else if (wrongOrigin) {
			logger.info("Origin {} not allowed for {}", or, request);
		}
		if (unauth != null)
			return unauth;
		StringEntity body = new StringEntity("Forbidden", "UTF-8");
		body.setContentType("text/plain");
		HttpResponse resp = new BasicHttpResponse(_403);
		resp.setHeader("Content-Type", "text/plain;charset=UTF-8");
		resp.setEntity(body);
		return resp;
	}

	private HttpResponse choose(HttpResponse unauthorized, HttpResponse auth)
			throws IOException {
		if (unauthorized == null)
			return auth;
		if (auth == null)
			return unauthorized;
		int code = unauthorized.getStatusLine().getStatusCode();
		if (auth.getStatusLine().getStatusCode() < code) {
			HttpEntity entity = unauthorized.getEntity();
			if (entity != null) {
				entity.consumeContent();
			}
			return auth;
		} else {
			HttpEntity entity = auth.getEntity();
			if (entity != null) {
				entity.consumeContent();
			}
			return unauthorized;
		}
	}

	private Map<String, String[]> getAuthorizationMap(ResourceOperation request)
			throws IOException {
		Map<String, String[]> map = new HashMap<String, String[]>();
		map.put("request-target", new String[] { request.getRequestTarget() });
		String au = request.getVaryHeader("Authorization");
		if (au != null) {
			map.put("authorization", new String[] { au });
		}
		String via = getRequestSource(request);
		map.put("via", via.split("\\s*,\\s*"));
		X509Certificate cret = request.getX509Certificate();
		if (cret != null) {
			Set<String> names = getSubjectNames(cret);
			if (names != null && !names.isEmpty()) {
				map.put("name", names.toArray(new String[names.size()]));
			}
			PublicKey pk = cret.getPublicKey();
			map.put("algorithm", new String[] { pk.getAlgorithm() });
			byte[] hash = Base64.encodeBase64(pk.getEncoded());
			map.put("encoded", new String[] { new String(hash, "UTF-8") });
		}
		String md5 = request.getHeader("Content-MD5");
		if (md5 == null) {
			md5 = computeMD5(request);
		}
		if (md5 != null) {
			map.put("content-md5", new String[] { md5 });
		}
		return Collections.unmodifiableMap(map);
	}

	private Set<String> getSubjectNames(X509Certificate cret) {
		Set<String> names = new LinkedHashSet<String>();
		try {
			for (List<?> pair : cret.getSubjectAlternativeNames()) {
				if (pair.size() == 2 && pair.get(1) instanceof String) {
					names.add((String) pair.get(1));
				}
			}
		} catch (CertificateParsingException e1) {
			logger.warn(e1.toString());
		}
		return names;
	}

	private String getRequestSource(ResourceOperation request) {
		StringBuilder via = new StringBuilder();
		for (String hd : request.getVaryHeaders("Via", "X-Forwarded-For")) {
			if (via.length() > 0) {
				via.append(",");
			}
			via.append(hd);
		}
		InetAddress remoteAddr = request.getRemoteAddr();
		if (via.length() > 0) {
			via.append(",");
		}
		via.append("1.1 " + remoteAddr.getCanonicalHostName());
		return via.toString();
	}

	private String computeMD5(ResourceOperation request) throws IOException {
		HttpEntity entity = request.getEntity();
		if (entity == null)
			return EMPTY_CONTENT_MD5;
		String md5 = findContentMD5(entity);
		if (md5 != null)
			return md5;
		try {
			MessageDigest digest = MessageDigest.getInstance("MD5");
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			InputStream in = entity.getContent();
			try {
				ChannelUtil.transfer(in, out, digest);
				byte[] bar = out.toByteArray();
				NByteArrayEntity replacement = new NByteArrayEntity(bar);
				replacement.setChunked(entity.isChunked());
				replacement.setContentEncoding(entity.getContentEncoding());
				replacement.setContentType(entity.getContentType());
				request.setEntity(replacement);
			} finally {
				in.close();
			}
			md5 = findContentMD5(entity);
			if (md5 != null)
				return md5;
			byte[] hash = Base64.encodeBase64(digest.digest());
			return new String(hash, "UTF-8");
		} catch (NoSuchAlgorithmException e) {
			logger.error(e.toString(), e);
			return null;
		}
	}

	private String findContentMD5(HttpEntity entity) {
		if (entity instanceof MD5ValidationEntity)
			return ((MD5ValidationEntity) entity).getContentMD5();
		if (entity instanceof HttpEntityWrapper)
			return findContentMD5(((HttpEntityWrapper) entity)
					.getEntityDelegate());
		return null;
	}

	private boolean isOriginAllowed(String allowed, String o) {
		if (allowed == null)
			return false;
		for (String ao : allowed.split("\\s*,\\s*")) {
			if (o.startsWith(ao) || ao.startsWith(o)
					&& ao.charAt(o.length()) == '/')
				return true;
		}
		return false;
	}

	private String allowOrigin(ResourceOperation request)
			throws QueryEvaluationException, RepositoryException {
		StringBuilder sb = new StringBuilder();
		List<Realm> realms = request.getRealms();
		if (realms.isEmpty())
			return "*";
		for (Realm realm : realms) {
			if (sb.length() > 0) {
				sb.append(", ");
			}
			String origin = realm.allowOrigin();
			if ("*".equals(origin))
				return origin;
			if (origin != null && origin.length() > 0) {
				sb.append(origin);
			}
		}
		if ("OPTIONS".equals(request.getMethod())) {
			String m = request.getVaryHeader(REQUEST_METHOD);
			RDFObject target = request.getRequestedResource();
			for (Method method : request.findMethodHandlers(m)) {
				if (method.isAnnotationPresent(realm.class)) {
					String[] values = method.getAnnotation(realm.class).value();
					ObjectConnection con = target.getObjectConnection();
					List<?> list = con.getObjects(Realm.class, values).asList();
					for (Object r : list) {
						if (request.isRealm(r)) {
							Realm realm = (Realm) r;
							if (sb.length() > 0) {
								sb.append(", ");
							}
							String origin = realm.allowOrigin();
							if ("*".equals(origin))
								return origin;
							if (origin != null && origin.length() > 0) {
								sb.append(origin);
							}
						}
					}
				}
			}
		}
		if (sb.length() < 1)
			return null;
		return sb.toString();
	}

	private boolean isBoot(String method, String auth)
			throws UnsupportedEncodingException {
		if (basic == null || auth == null)
			return false;
		if (auth.startsWith("Basic")) {
			byte[] bytes = basic.getBytes("UTF-8");
			String encoded = new String(Base64.encodeBase64(bytes));
			return auth.equals("Basic " + encoded);
		} else if (auth.startsWith("Digest")) {
			String string = auth.substring("Digest ".length());
			Map<String, String> options = parseOptions(string);
			if (options == null)
				return false;
			if (!basic.startsWith(options.get("username") + ":"))
				return false;
			String realm = options.get("realm");
			String nonce = options.get("nonce");
			String response = options.get("response");
			String a1 = basic.replaceFirst(":", ":" + realm + ":");
			String a2 = method + ":" + options.get("uri");
			String legacy = md5(a1) + ":" + nonce + ":" + md5(a2);
			if (md5(legacy).equals(response))
				return true;
			String digest = md5(a1) + ":" + nonce + ":" + options.get("nc")
					+ ":" + options.get("cnonce") + ":" + options.get("qop")
					+ ":" + md5(a2);
			return md5(digest).equals(response);
		}
		return false;
	}

	private String md5(String a2) {
		return DigestUtils.md5Hex(a2);
	}

	private Map<String, String> parseOptions(String options) {
		Map<String, String> result = new HashMap<String, String>();
		for (String keyvalue : options.split("\\s*,\\s*")) {
			int idx = keyvalue.indexOf('=');
			if (idx < 0)
				return null;
			String key = keyvalue.substring(0, idx);
			if (idx == keyvalue.length() - 1) {
				result.put(key, "");
			} else if (keyvalue.charAt(idx + 1) == '"') {
				int eq = keyvalue.lastIndexOf('"');
				if (eq <= idx + 2)
					return null;
				String value = keyvalue.substring(idx + 2, eq);
				result.put(key, value);
			} else {
				String value = keyvalue.substring(idx + 1);
				result.put(key, value);
			}
		}
		return result;
	}

}
