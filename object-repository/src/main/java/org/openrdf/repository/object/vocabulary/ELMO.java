package org.openrdf.repository.object.vocabulary;

import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;

public class ELMO {
	public static final String NAMESPACE = "http://www.openrdf.org/rdf/2008/08/elmo#";
	public static final URI MESSAGE = new URIImpl(NAMESPACE + "Message");
	public static final URI METHOD = new URIImpl(NAMESPACE + "method");
	public static final URI OBJECT_RESPONSE = new URIImpl(NAMESPACE
			+ "objectResponse");
	public static final URI LITERAL_RESPONSE = new URIImpl(NAMESPACE
			+ "literalResponse");
	public static final URI TARGET = new URIImpl(NAMESPACE + "target");
	public static final URI IMPORTS = new URIImpl(NAMESPACE + "imports");
	public static final URI GROOVY = new URIImpl("http://www.openrdf.org/rdf/2008/08/elmo#groovy");
	public static final URI JAVA = new URIImpl("http://www.openrdf.org/rdf/2008/08/elmo#java");
	public static final URI LOCALIZED = new URIImpl(NAMESPACE + "localized");
	public static final URI FUNCTIONAL_LOCALIZED = new URIImpl(NAMESPACE + "functionalLocalized");
	public static final URI NAME = new URIImpl(NAMESPACE + "name");
	public static final URI LITERAL_TRIGGER = new URIImpl(NAMESPACE + "literalTrigger");
	public static final URI OBJECT_TRIGGER = new URIImpl(NAMESPACE + "objectTrigger");

	private ELMO() {
		// prevent instantiation
	}

}
