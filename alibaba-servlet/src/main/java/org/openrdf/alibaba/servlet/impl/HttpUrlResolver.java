package org.openrdf.alibaba.servlet.impl;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.xml.namespace.QName;

import org.openrdf.alibaba.decor.UrlResolver;
import org.openrdf.elmo.Entity;

public class HttpUrlResolver implements UrlResolver {
	private boolean useRdfProtocol;
	
	private String baseUrl;

	private QName intent;

	private boolean intentPrefix;

	public HttpUrlResolver(boolean useRdfProtocol, String baseUrl, QName intent) {
		this.useRdfProtocol = useRdfProtocol;
		this.baseUrl = baseUrl;
		this.intent = intent;
		intentPrefix = intent == null || encPrefix(intent).length() > 0;
	}

	public String resolve(Entity entity) {
		QName name = entity.getQName();
		if (name == null)
			return "";
		StringBuilder sb = new StringBuilder();
		if (useRdfProtocol) {
			sb.append("rdf:");
			sb.append(name.getNamespaceURI());
			sb.append(name.getLocalPart());
			return sb.toString();
		}
		sb.append(baseUrl);
		if (encPrefix(name).length() > 0 && intentPrefix) {
			if (intent != null) {
				sb.append("/").append(encPrefix(intent));
				sb.append("/").append(encLocal(intent));
			}
			sb.append("/").append(encPrefix(name));
			sb.append("/").append(encLocal(name));
			return sb.toString();
		}
		sb.append("?");
		sb.append("uri=").append(encNS(name)).append(encLocal(name));
		if (intent != null) {
			sb.append("intent=").append(encNS(intent)).append(encLocal(intent));
		}
		return sb.toString();
	}

	public String resolve(Entity entity, String format) {
		QName name = entity.getQName();
		if (name == null)
			return "";
		StringBuilder sb = new StringBuilder();
		sb.append(baseUrl);
		sb.append("?");
		sb.append("uri=").append(encNS(name)).append(encLocal(name));
		if (intent != null) {
			sb.append("intent=").append(encNS(intent)).append(encLocal(intent));
		}
		sb.append("format=").append(enc(format));
		return sb.toString();
	}

	private String encPrefix(QName name) {
		return enc(name.getPrefix());
	}

	private String encNS(QName name) {
		return enc(name.getNamespaceURI());
	}

	private String encLocal(QName name) {
		return enc(name.getLocalPart());
	}

	private String enc(String str) {
		try {
			return URLEncoder.encode(str, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new AssertionError(e);
		}
	}

}
