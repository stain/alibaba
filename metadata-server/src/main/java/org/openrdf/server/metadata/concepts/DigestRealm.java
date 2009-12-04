package org.openrdf.server.metadata.concepts;

import java.util.Set;

import org.openrdf.repository.object.annotations.iri;

public interface DigestRealm extends Realm {

	/**
	 * A string to be displayed to users so they know which username and
	 * password to use. This string should contain at least the name of the host
	 * performing the authentication and might additionally indicate the
	 * collection of users who might have access. An example might be
	 * "registered_users@gotham.news.com".
	 */
	@iri("http://www.openrdf.org/rdf/2009/metadata#realmAuth")
	String getRealmAuth();

	@iri("http://www.openrdf.org/rdf/2009/metadata#realmAuth")
	void setRealmAuth(String realmAuth);

	/**
	 * Identifies the security contexts that caused the user agent to initiate
	 * an HTTP request.
	 */
	@iri("http://www.openrdf.org/rdf/2009/metadata#origin")
	Set<HTTPFileObject> getOrigins();

	@iri("http://www.openrdf.org/rdf/2009/metadata#origin")
	void setOrigins(Set<HTTPFileObject> origins);
}
