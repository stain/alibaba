package org.openrdf.server.metadata;

import java.io.File;
import java.net.URISyntaxException;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;

import com.sun.jersey.spi.resource.PerRequest;

@PerRequest
public class URIResolver {
	private UriInfo info;
	private HttpHeaders headers;
	private File dataDir;
	private ValueFactory vf;

	public URIResolver(@Context UriInfo info, @Context HttpHeaders headers) {
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
		File base = new File(dataDir, host);
		File file = new File(base, getPath());
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

}
