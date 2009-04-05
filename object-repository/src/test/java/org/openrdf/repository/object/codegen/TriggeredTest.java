package org.openrdf.repository.object.codegen;

import java.util.List;

import junit.framework.Test;

import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.query.BindingSet;
import org.openrdf.query.TupleQuery;
import org.openrdf.repository.object.base.ObjectRepositoryTestCase;

public class TriggeredTest extends ObjectRepositoryTestCase {

	private static final String NS = "http://example.org/trigger#";

	public static Test suite() throws Exception {
		return ObjectRepositoryTestCase.suite(TriggeredTest.class);
	}

	@Override
	public void setUp() throws Exception {
		config.addImports(getClass().getResource(
				"/ontologies/triggered-ontology.owl"));
		super.setUp();
	}

	public void testLocalized() throws Exception {
		ValueFactory vf = con.getValueFactory();
		URI Document = vf.createURI(NS, "Document");
		URI body = vf.createURI(NS, "body");
		URI author = vf.createURI(NS, "author");
		URI doc = vf.createURI("urn:test:doc");
		Literal name = vf.createLiteral("Thomas Buchanan Read");
		Literal text = vf
				.createLiteral("Within the sober realm of leafless trees,\n"
						+ "The russet year inhaled the dreamy air;\n"
						+ "Like some tanned reaper, in his hour of ease,\n"
						+ "When all the fields are lying brown and bare.");
		con.add(doc, RDF.TYPE, Document);
		con.add(doc, author, name);
		con.add(doc, body, text);
		TupleQuery qry = con.prepareTupleQuery("PREFIX :<" + NS + "> "
				+ "SELECT ?doc WHERE { ?doc :keyword \"trees\"}");
		List<BindingSet> list = qry.evaluate().asList();
		assertFalse(list.isEmpty());
		Value result = list.get(0).getValue("doc");
		assertEquals(doc, result);
	}
}
