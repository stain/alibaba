package org.openrdf.repository.object.codegen;

import java.io.File;

import org.openrdf.repository.object.base.CodeGenTestCase;

public class DbpediaTest extends CodeGenTestCase {

	public void testLiterals() throws Exception {
		addRdfSource("/ontologies/dbpedia_3.6.owl");
		File jar = createJar("dbpedia.jar");
		assertTrue(jar.isFile());
		assertEquals(272, countClasses(jar, "dbpedia_owl", ".java"));
		assertEquals(272, countClasses(jar, "dbpedia_owl", ".class"));
	}
}
