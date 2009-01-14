package org.openrdf.elmo.sesame;

import java.util.Iterator;

import junit.framework.Test;

import org.openrdf.elmo.sesame.base.ElmoManagerTestCase;
import org.openrdf.model.URI;
import org.openrdf.repository.object.annotations.oneOf;
import org.openrdf.repository.object.annotations.rdf;

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
		Iterator<MyClass> iter = manager.findAll(MyClass.class).iterator();
		assertTrue(iter.hasNext());
		URI myClass = manager.getValueFactory().createURI(BASE, "my-class");
		assertEquals(myClass, manager.valueOf(iter.next()));
		assertFalse(iter.hasNext());
	}

	public void testOneOf() throws Exception {
		Iterator<MyIndividual> iter = manager.findAll(MyIndividual.class).iterator();
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
