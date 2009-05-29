package org.openrdf.repository.object.vocabulary;

import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;

public class OBJ {
	public static final String NAMESPACE = "http://www.openrdf.org/rdf/2009/object#";
	public static final URI COMPONENT_TYPE = new URIImpl(NAMESPACE + "componentType");
	public static final URI DATATYPE_TRIGGER = new URIImpl(NAMESPACE + "DatatypeTrigger");
	public static final URI GROOVY = new URIImpl(NAMESPACE + "groovy");
	public static final URI IMPORTS = new URIImpl(NAMESPACE + "imports");
	public static final URI JAVA = new URIImpl(NAMESPACE + "java");
	public static final URI LITERAL_RESPONSE = new URIImpl(NAMESPACE
			+ "literalResponse");
	public static final URI LOCALIZED = new URIImpl(NAMESPACE + "localized");
	public static final URI MESSAGE = new URIImpl(NAMESPACE + "Message");
	public static final URI METHOD = new URIImpl(NAMESPACE + "Method");
	public static final URI CLASS_NAME = new URIImpl(NAMESPACE + "className");
	public static final URI NAME = new URIImpl(NAMESPACE + "name");
	public static final URI OBJECT_RESPONSE = new URIImpl(NAMESPACE
			+ "objectResponse");
	public static final URI OBJECT_TRIGGER = new URIImpl(NAMESPACE + "ObjectTrigger");
	public static final URI PRECEDES = new URIImpl(NAMESPACE + "precedes");
	public static final URI PROCEED = new URIImpl(NAMESPACE + "proceed");
	public static final URI READ_ONLY = new URIImpl(NAMESPACE + "readOnly");
	public static final URI SPARQL = new URIImpl(NAMESPACE + "sparql");
	public static final URI TARGET = new URIImpl(NAMESPACE + "target");

	private OBJ() {
		// prevent instantiation
	}

}
