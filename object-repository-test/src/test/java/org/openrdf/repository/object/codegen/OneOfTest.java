package org.openrdf.repository.object.codegen;

import org.openrdf.repository.object.base.CodeGenTestCase;


public class OneOfTest extends CodeGenTestCase {

	public void testOneOf() throws Exception {
		addRdfSource("/ontologies/xsd-datatypes.rdf");
		addRdfSource("/ontologies/rdfs-schema.rdf");
		addRdfSource("/ontologies/owl-schema.rdf");
		addRdfSource("/ontologies/oneof-ontology.owl");
		createJar("oneOf.jar");
	}
}
