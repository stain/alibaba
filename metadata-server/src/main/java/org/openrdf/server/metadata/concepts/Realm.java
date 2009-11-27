package org.openrdf.server.metadata.concepts;

import java.io.IOException;
import java.io.InputStream;

import org.openrdf.repository.object.annotations.iri;
import org.openrdf.server.metadata.annotations.cacheControl;
import org.openrdf.server.metadata.annotations.method;
import org.openrdf.server.metadata.annotations.operation;
import org.openrdf.server.metadata.annotations.parameter;
import org.openrdf.server.metadata.annotations.type;

public interface Realm {

	@operation("allow-origin")
	@iri("http://www.openrdf.org/rdf/2009/metadata#allow-origin")
	String allowOrigin();

	@cacheControl("no-store")
	@type("message/http")
	@operation("unauthorized")
	@iri("http://www.openrdf.org/rdf/2009/metadata#unauthorized")
	InputStream unauthorized() throws IOException;

	@method("GET")
	@operation("authorize")
	@iri("http://www.openrdf.org/rdf/2009/metadata#authorized")
	boolean authorize(@parameter("addr") String addr,
			@parameter("method") String method,
			@parameter("format") String format,
			@parameter("algorithm") String algorithm,
			@parameter("encoded") byte[] encoded);

	@method("POST")
	@operation("authorize")
	@iri("http://www.openrdf.org/rdf/2009/metadata#authorized")
	boolean authorize(@parameter("addr") String addr,
			@parameter("method") String method, @parameter("uri") String url,
			String authorization, @parameter("format") String format,
			@parameter("algorithm") String algorithm,
			@parameter("encoded") byte[] encoded);

}
