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
	public static final URI INVOKE = new URIImpl(NAMESPACE + "invoke");
	public static final URI TARGET = new URIImpl(NAMESPACE + "target");
	public static final URI IMPORTS = new URIImpl(NAMESPACE + "imports");
	public static final URI GROOVY = new URIImpl("http://www.openrdf.org/rdf/2008/08/elmo#groovy");
	public static final URI JAVA = new URIImpl("http://www.openrdf.org/rdf/2008/08/elmo#java");

	private ELMO() {
		// prevent instantiation
	}

}
