package org.openrdf.repository.object.codegen;

import java.io.File;

import org.openrdf.repository.object.base.CodeGenTestCase;

public class EquivalentTest extends CodeGenTestCase {

	public void testEquivalent() throws Exception {
		addRdfSource("/ontologies/xsd-datatypes.rdf");
		addRdfSource("/ontologies/rdfs-schema.rdf");
		addRdfSource("/ontologies/owl-schema.rdf");
		addRdfSource("/ontologies/equivalent-ontology.owl");
		File jar = createJar("equivalent.jar");
		assertTrue(jar.isFile());
		assertEquals(7, countClasses(jar, "equivalent", ".java"));
		assertEquals(7, countClasses(jar, "equivalent", ".class"));
	}
}
