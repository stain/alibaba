package org.openrdf.repository.object.codegen;

import org.openrdf.repository.object.base.CodeGenTestCase;

public class PrecedesTest extends CodeGenTestCase {

	public void testCompiler() throws Exception {
		addRdfSource("/ontologies/precedes-ontology.ttl");
		createJar("precedes.jar");
	}
}
