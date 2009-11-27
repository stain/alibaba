package org.openrdf.server.metadata.behaviours;

import static org.openrdf.query.QueryLanguage.SPARQL;
import info.aduna.net.ParsedURI;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectQuery;
import org.openrdf.server.metadata.WebObject;
import org.openrdf.server.metadata.concepts.DigestRealm;
import org.openrdf.server.metadata.exceptions.BadRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class DigestRealmSupport implements DigestRealm, WebObject {
	private static KeyPair key;
	static {
		try {
			KeyPairGenerator gen = KeyPairGenerator.getInstance("DSA");
			gen.initialize(512);
			key = gen.generateKeyPair();
		} catch (NoSuchAlgorithmException e) {
			key = null;
		}
	}
	private Logger logger = LoggerFactory.getLogger(DigestRealmSupport.class);

	private static final String SELECT_CRED = "PREFIX :<http://www.openrdf.org/rdf/2009/metadata#>\n"
			+ "SELECT ?encoded\n"
			+ "WHERE { $this :credential [:name $name; :algorithm \"MD5\"; :encoded ?encoded] }";

	public String allowOrigin() {
		StringBuilder sb = new StringBuilder();
		for (WebObject origin : getOrigins()) {
			if (sb.length() > 0) {
				sb.append(", ");
			}
			sb.append(origin.toUri());
		}
		return sb.toString();
	}

	public InputStream unauthorized() throws IOException {
		String realm = getRealmAuth();
		String nonce = Long.toString(System.currentTimeMillis(), Character.MAX_RADIX);
		String opaque = getOpaque(nonce);
		String body = "Unauthorized";
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		Writer writer = new OutputStreamWriter(baos, "UTF-8");
		writer.write("HTTP/1.0 401 Unauthorized\r\n");
		writer.write("WWW-Authenticate: Digest realm=\"");
		writer.write(realm);
		writer.write("\", qop=\"auth\", nonce=\"");
		writer.write(nonce);
		writer.write("\", opaque=\"");
		writer.write(opaque);
		writer.write("\"\r\n");
		writer.write("Content-Type: text/plain\r\n");
		writer.write("Content-Length: ");
		writer.write(String.valueOf(body.length()));
		writer.write("\r\n\r\n");
		writer.write(body);
		writer.close();
		return new ByteArrayInputStream(baos.toByteArray());
	}

	public boolean authorize(String addr, String method, String format,
			String algorithm, byte[] encoded) {
		return false;
	}

	public boolean authorize(String addr, String method, String url,
			String auth, String format, String algorithm, byte[] encoded) {
		if (auth == null || !auth.startsWith("Digest"))
			return false;
		try {
			String string = auth.substring("Digest ".length());
			Map<String, String> options = parseOptions(string);
			if (options == null)
				throw new BadRequest("Invalid digest authorization header");
			String realm = options.get("realm");
			String uri = options.get("uri");
			String username = options.get("username");
			String nonce = options.get("nonce");
			String opaque = options.get("opaque");
			ParsedURI parsed = new ParsedURI(url);
			String path = parsed.getPath();
			if (parsed.getQuery() != null) {
				path = path + "?" + parsed.getQuery();
			}
			if (nonce == null || opaque == null || username == null
					|| realm == null || !realm.equals(getRealmAuth())
					|| !url.equals(uri) && !path.equals(uri)
					|| !verify(nonce, opaque)) {
				logger.warn("Bad authorization on {} {} using {}",
						new Object[] { method, url, auth });
				throw new BadRequest("Bad authorization");
			}
			for (byte[] a1 : findDigest(username)) {
				String ha1 = new String(Hex.encodeHex(a1));
				String a2 = method + ":" + uri;
				String legacy = ha1 + ":" + nonce + ":" + md5(a2);
				if (md5(legacy).equals(options.get("response")))
					return true;
				String response = ha1 + ":" + nonce + ":" + options.get("nc")
						+ ":" + options.get("cnonce") + ":"
						+ options.get("qop") + ":" + md5(a2);
				if (md5(response).equals(options.get("response")))
					return true;
			}
			return false;
		} catch (BadRequest e) {
			throw e;
		} catch (Exception e) {
			logger.warn(e.toString(), e);
			return false;
		}
	}

	private String getOpaque(String nonce) {
		if (key != null) {
			try {
				Signature sig = Signature.getInstance("DSA");
				sig.initSign(key.getPrivate());
				sig.update(nonce.getBytes());
				return new String(Base64.encodeBase64(sig.sign()));
			} catch (Exception e) {
				logger.error(e.toString(), e);
			}
		}
		return nonce;
	}

	private boolean verify(String nonce, String opaque) {
		long duration = System.currentTimeMillis() - Long.valueOf(nonce, Character.MAX_RADIX);
		if (duration > 1000 * 60 * 60) {
			logger.warn("Old nonce used in digest authentication");
		}
		if (key != null) {
			try {
				Signature sig = Signature.getInstance("DSA");
				sig.initVerify(key.getPublic());
				sig.update(nonce.getBytes());
				return sig.verify(Base64.decodeBase64(opaque.getBytes()));
			} catch (Exception e) {
				logger.error(e.toString(), e);
			}
		}
		return nonce.equals(opaque);
	}

	private List<byte[]> findDigest(String username)
			throws MalformedQueryException, RepositoryException,
			QueryEvaluationException {
		ObjectConnection con = getObjectConnection();
		ObjectQuery query = con.prepareObjectQuery(SPARQL, SELECT_CRED);
		query.setObject("name", username);
		return query.evaluate(byte[].class).asList();
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
			if (keyvalue.charAt(idx + 1) == '"') {
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
