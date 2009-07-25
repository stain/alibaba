package org.openrdf.repository.object.codegen;

import junit.framework.Test;

import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.model.vocabulary.XMLSchema;
import org.openrdf.repository.object.base.ObjectRepositoryTestCase;

public class RecompileTest extends ObjectRepositoryTestCase {

	public static Test suite() throws Exception {
		return ObjectRepositoryTestCase.suite(RecompileTest.class);
	}

	public void setUp() throws Exception {
		config.setCompileRepository(true);
		super.setUp();
	}

	public void test() throws Exception {
		ValueFactory vf = con.getValueFactory();
		con.setNamespace("", "urn:dynamic:");
		URI property = vf.createURI("urn:dynamic:property");
		con.add(property, RDF.TYPE, OWL.FUNCTIONALPROPERTY);
		con.add(property, RDFS.RANGE, XMLSchema.STRING);
		con.close();
		con = con.getRepository().getConnection();
		Object obj = con.getObject("urn:test:resource");
		obj.getClass().getMethod("getProperty");
	}
}
