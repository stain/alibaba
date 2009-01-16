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
		Result<MyClass> iter = manager.findAll(MyClass.class);
		assertTrue(iter.hasNext());
		URI myClass = manager.getValueFactory().createURI(BASE, "my-class");
		assertEquals(myClass, manager.valueOf(iter.next()));
		assertFalse(iter.hasNext());
	}

	public void testOneOf() throws Exception {
		Result<MyIndividual> iter = manager.findAll(MyIndividual.class);
		assertTrue(iter.hasNext());
		URI myIndividual = manager.getValueFactory().createURI("urn:test:my-individual");
		assertEquals(myIndividual, manager.valueOf(iter.next()));
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
		manager.designate(manager.find(myClass), MyClass.class);
		manager.designate(manager.find(myOtherClass), MyOtherClass.class);
	}
}
