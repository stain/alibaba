package org.openrdf.sail.auditing.config;

import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;

public class AuditingSchema {

	/** http://www.openrdf.org/config/sail/auditing# */
	public static final String NAMESPACE = "http://www.openrdf.org/config/sail/auditing#";

	public static final URI TRX_NAMESPACE = new URIImpl(NAMESPACE
			+ "trxNamespace");
	public static final URI ARCHIVING = new URIImpl(NAMESPACE + "archiving");

	private AuditingSchema() {
		// no constructor
	}
}
