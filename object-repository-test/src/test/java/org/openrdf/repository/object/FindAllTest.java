package org.openrdf.repository.object;

import junit.framework.Test;

import org.openrdf.model.URI;
import org.openrdf.repository.object.annotations.oneOf;
import org.openrdf.repository.object.annotations.rdf;
import org.openrdf.repository.object.base.ElmoManagerTestCase;
import org.openrdf.result.Result;

public class FindAllTest extends ElmoManagerTestCase {
	private static final String BASE = "urn:test:";

	public static Test suite() throws Exception {
		return ElmoManagerTestCase.suite(FindAllTest.class);
	}

	@rdf("urn:test:MyClass")
	public interface MyClass {}

	@rdf("urn:test:MyOtherClass")
	public interface MyOtherClass {}

	@oneOf("urn:test:my-individual")
	public interface MyIndividual {}

	public void testClass() throws Exception {
		ObjectQuery query = manager.prepareObjectQuery("SELECT ?o WHERE {?o a ?type}");
		query.setType("type", MyClass.class);
		Result<MyClass> iter = (Result)query.evaluate();
		assertTrue(iter.hasNext());
		URI myClass = manager.getValueFactory().createURI(BASE, "my-class");
		assertEquals(myClass, manager.addObject(iter.next()));
		assertFalse(iter.hasNext());
	}

	@Override
	protected void setUp() throws Exception {
		module.addConcept(MyClass.class);
		module.addConcept(MyOtherClass.class);
		module.addConcept(MyIndividual.class);
		super.setUp();
		URI myClass = manager.getValueFactory().createURI(BASE, "my-class");
		URI myOtherClass = manager.getValueFactory().createURI(BASE, "my-other-class");
		manager.addType(manager.getObject(myClass), MyClass.class);
		manager.addType(manager.getObject(myOtherClass), MyOtherClass.class);
	}
}
