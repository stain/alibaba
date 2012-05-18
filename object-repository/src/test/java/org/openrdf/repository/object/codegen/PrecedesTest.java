package org.openrdf.repository.object.codegen;

import java.io.File;

import org.openrdf.repository.object.base.CodeGenTestCase;

public class PrecedesTest extends CodeGenTestCase {

	public void testCompiler() throws Exception {
		addRdfSource("/ontologies/precedes-ontology.ttl");
		File jar = createJar("precedes.jar");
		assertTrue(jar.exists());
	}
}
