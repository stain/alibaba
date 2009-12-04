package org.openrdf.http.object.concepts;

import java.io.IOException;
import java.io.InputStream;

import org.openrdf.http.object.annotations.cacheControl;
import org.openrdf.http.object.annotations.operation;
import org.openrdf.http.object.annotations.parameter;
import org.openrdf.http.object.annotations.type;
import org.openrdf.repository.object.annotations.iri;

public interface Realm {

	@operation("allow-origin")
	@iri("http://www.openrdf.org/rdf/2009/metadata#allow-origin")
	String allowOrigin();

	@cacheControl("no-store")
	@type("message/http")
	@operation("unauthorized")
	@iri("http://www.openrdf.org/rdf/2009/metadata#unauthorized")
	InputStream unauthorized() throws IOException;

	@operation("authorize")
	@iri("http://www.openrdf.org/rdf/2009/metadata#authorized")
	boolean authorize(@parameter("addr") String addr,
			@parameter("method") String method,
			@parameter("format") String format,
			@parameter("algorithm") String algorithm,
			@parameter("encoded") byte[] encoded);

	@operation("authorize")
	@iri("http://www.openrdf.org/rdf/2009/metadata#authorized")
	boolean authorize(@parameter("addr") String addr,
			@parameter("method") String method, @parameter("uri") String url,
			String authorization, @parameter("format") String format,
			@parameter("algorithm") String algorithm,
			@parameter("encoded") byte[] encoded);

}
