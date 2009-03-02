package org.openrdf.repository.object;

import junit.framework.Test;

import org.openrdf.model.URI;
import org.openrdf.repository.object.annotations.oneOf;
import org.openrdf.repository.object.annotations.rdf;
import org.openrdf.repository.object.base.ObjectRepositoryTestCase;
import org.openrdf.result.Result;

public class FindAllTest extends ObjectRepositoryTestCase {
	private static final String BASE = "urn:test:";

	public static Test suite() throws Exception {
		return ObjectRepositoryTestCase.suite(FindAllTest.class);
	}

	@rdf("urn:test:MyClass")
	public interface MyClass {}

	@rdf("urn:test:MyOtherClass")
	public interface MyOtherClass {}

	@oneOf("urn:test:my-individual")
	public interface MyIndividual {}

	public void testClass() throws Exception {
		ObjectQuery query = con.prepareObjectQuery("SELECT ?o WHERE {?o a ?type}");
		query.setType("type", MyClass.class);
		Result<MyClass> iter = (Result)query.evaluate();
		assertTrue(iter.hasNext());
		URI myClass = con.getValueFactory().createURI(BASE, "my-class");
		assertEquals(myClass, con.addObject(iter.next()));
		assertFalse(iter.hasNext());
	}

	@Override
	protected void setUp() throws Exception {
		config.addConcept(MyClass.class);
		config.addConcept(MyOtherClass.class);
		config.addConcept(MyIndividual.class);
		super.setUp();
		URI myClass = con.getValueFactory().createURI(BASE, "my-class");
		URI myOtherClass = con.getValueFactory().createURI(BASE, "my-other-class");
		con.addType(con.getObject(myClass), MyClass.class);
		con.addType(con.getObject(myOtherClass), MyOtherClass.class);
	}
}
