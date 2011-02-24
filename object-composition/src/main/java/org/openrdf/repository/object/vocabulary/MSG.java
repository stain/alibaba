package org.openrdf.repository.object.vocabulary;

import java.util.Arrays;
import java.util.Collection;

import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;

public class MSG {
	public static final String NAMESPACE = "http://www.openrdf.org/rdf/2011/messaging#";
	public static final URI IMPORTS = new URIImpl(NAMESPACE + "imports");
	public static final URI LITERAL = new URIImpl(NAMESPACE + "literal");
	public static final URI LITERAL_SET = new URIImpl(NAMESPACE + "literalSet");
	public static final URI MATCHING = new URIImpl(NAMESPACE + "matching");
	public static final URI MESSAGE = new URIImpl(NAMESPACE + "Message");
	public static final URI OBJECT = new URIImpl(NAMESPACE + "object");
	public static final URI OBJECT_SET = new URIImpl(NAMESPACE + "objectSet");
	public static final URI PRECEDES = new URIImpl(NAMESPACE + "precedes");
	public static final URI SCRIPT = new URIImpl(NAMESPACE + "script");
	public static final URI SPARQL = new URIImpl(NAMESPACE + "sparql");
	public static final URI TARGET = new URIImpl(NAMESPACE + "target");
	public static final URI TYPE = new URIImpl(NAMESPACE + "type");
	public static final URI XSLT = new URIImpl(NAMESPACE + "xslt");
	public static final Collection<URI> MESSAGE_IMPLS = Arrays
			.asList(new URI[] { SPARQL, XSLT, SCRIPT });

	private MSG() {
		// prevent instantiation
	}

}
