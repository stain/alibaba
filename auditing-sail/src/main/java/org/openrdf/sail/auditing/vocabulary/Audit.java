package org.openrdf.sail.auditing.vocabulary;

import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;

public class Audit {
	public static final String NAMESPACE = "http://www.openrdf.org/rdf/2009/auditing#";
	public static final URI TRANSACTION = new URIImpl(NAMESPACE + "Transaction");
	public static final URI COMMITTED_ON = new URIImpl(NAMESPACE + "committedOn");
	public static final URI REVISION = new URIImpl(NAMESPACE + "revision");
	public static final URI REMOVED = new URIImpl(NAMESPACE + "removed");
	public static final URI PATTERN = new URIImpl(NAMESPACE + "Pattern");
	public static final URI SUBJECT = new URIImpl(NAMESPACE + "subject");
	public static final URI OBJECT = new URIImpl(NAMESPACE + "object");
	public static final URI LITERAL = new URIImpl(NAMESPACE + "literal");
	public static final URI PREDICATE = new URIImpl(NAMESPACE + "predicate");
	public static final URI GRAPH = new URIImpl(NAMESPACE + "graph");
}
