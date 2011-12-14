package org.openrdf.repository.object.codegen;

import junit.framework.Test;

import org.openrdf.model.Resource;
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

	public void testValid() throws Exception {
		ValueFactory vf = con.getValueFactory();
		con.setNamespace("", "urn:dynamic:");
		URI property = vf.createURI("urn:dynamic:property");
		con.add(property, RDF.TYPE, OWL.FUNCTIONALPROPERTY);
		con.add(property, RDFS.RANGE, XMLSchema.STRING);
		con.recompileAfterClose();
		con.close();
		con = con.getRepository().getConnection();
		Object obj = con.getObject("urn:test:resource");
		obj.getClass().getMethod("getProperty");
	}

	public void testInvalid() throws Exception {
		ValueFactory vf = con.getValueFactory();
		con.setNamespace("", "urn:dynamic:");
		URI property = vf.createURI("urn:dynamic:property");
		con.add(property, RDF.TYPE, OWL.FUNCTIONALPROPERTY);
		con.add(property, RDFS.RANGE, XMLSchema.BASE64BINARY);
		con.recompileAfterClose();
		con.close();
		con = con.getRepository().getConnection();
		con.remove(property, RDFS.RANGE, null);
		con.add(property, RDFS.RANGE, XMLSchema.STRING);
		con.recompileAfterClose();
		con.close();
		con = con.getRepository().getConnection();
		Object obj = con.getObject("urn:test:resource");
		obj.getClass().getMethod("getProperty");
	}

	public void testUnionOf() throws Exception {
		ValueFactory vf = con.getValueFactory();
		con.setNamespace("", "urn:dynamic:");
		URI property = vf.createURI("urn:dynamic:property");
		con.add(property, RDF.TYPE, OWL.FUNCTIONALPROPERTY);
		con.add(property, RDFS.RANGE, XMLSchema.STRING);
		Resource node = vf.createBNode();
		con.add(property, RDFS.DOMAIN, node);
		Resource list = vf.createBNode();
		con.add(node, OWL.UNIONOF, list);
		con.add(list, RDF.FIRST, vf.createURI("urn:mimetype:text/html"));
		Resource rest = vf.createBNode();
		con.add(list, RDF.REST, rest);
		con.add(rest, RDF.FIRST, vf.createURI("urn:mimetype:image/gif"));
		con.add(rest, RDF.REST, RDF.NIL);
		con.recompileAfterClose();
		con.close();
		con = con.getRepository().getConnection();
		con.add(vf.createURI("urn:test:resource"), RDF.TYPE, vf.createURI("urn:mimetype:text/html"));
		Object obj = con.getObject("urn:test:resource");
		obj.getClass().getMethod("getProperty");
	}
}
