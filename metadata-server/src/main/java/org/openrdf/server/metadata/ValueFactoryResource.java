package org.openrdf.server.metadata;

import info.aduna.net.ParsedURI;

import java.io.File;
import java.net.URISyntaxException;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;

public class ValueFactoryResource {
	private UriInfo info;
	private HttpHeaders headers;
	private File dataDir;
	private ValueFactory vf;

	public ValueFactoryResource(@Context UriInfo info, @Context HttpHeaders headers) {
		this.info = info;
		this.headers = headers;
	}

	public void setDataDir(File dataDir) {
		this.dataDir = dataDir;
	}

	public void setValueFactory(ValueFactory vf) {
		this.vf = vf;
	}

	public MultivaluedMap<String, String> getQueryParameters() {
		return info.getQueryParameters();
	}

	@GET
	public String toString() {
		return getURI().toString();
	}

	public Literal createLiteral(String label) {
		return vf.createLiteral(label);
	}

	public Literal createLiteral(String label, URI datatype) {
		return vf.createLiteral(label, datatype);
	}

	public URI createURI(String namespace, String localName) {
		return vf.createURI(namespace, localName);
	}

	public URI createURI(String uriSpec) {
		ParsedURI base = new ParsedURI(getURI().stringValue());
		base.normalize();
		ParsedURI uri = new ParsedURI(uriSpec);
		return vf.createURI(base.resolve(uri).toString());
	}

	public URI getURI() {
		java.net.URI uri = info.getAbsolutePath();
		String host = getHost();
		try {
			String scheme = uri.getScheme();
			String path = uri.getPath();
			uri = new java.net.URI(scheme, host, path, null);
		} catch (URISyntaxException e) {
			// bad Host header
		}
		return vf.createURI(uri.toASCIIString());
	}

	public File getFile() {
		String host = getHost();
		File base = new File(dataDir, safe(host));
		File file = new File(base, safe(getPath()));
		if (file.isFile())
			return file;
		return new File(file, Integer.toHexString(getURI().hashCode()));
	}

	private String getHost() {
		List<String> host = headers.getRequestHeader("Host");
		if (host.isEmpty()) {
			java.net.URI uri = info.getAbsolutePath();
			return uri.getAuthority();
		}
		return host.get(0);
	}

	private String getPath() {
		return info.getAbsolutePath().getPath();
	}

	private String safe(String path) {
		path = path.replace('/', File.separatorChar);
		path = path.replace('\\', File.separatorChar);
		path = path.replace('*', '_');
		path = path.replace('"', '_');
		path = path.replace('[', '_');
		path = path.replace(']', '_');
		path = path.replace(':', '_');
		path = path.replace(';', '_');
		path = path.replace('|', '_');
		path = path.replace('=', '_');
		return path;
	}

}
