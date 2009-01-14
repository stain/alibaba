package org.openrdf.elmo.sesame;

import java.util.Iterator;

import javax.xml.namespace.QName;

import junit.framework.Test;

import org.openrdf.elmo.Entity;
import org.openrdf.elmo.annotations.oneOf;
import org.openrdf.elmo.annotations.rdf;
import org.openrdf.elmo.sesame.base.ElmoManagerTestCase;

public class FindAllTest extends ElmoManagerTestCase {
	private static final String BASE = "urn:test:";

	public static Test suite() throws Exception {
		return ElmoManagerTestCase.suite(FindAllTest.class);
	}

	@rdf("urn:test:MyClass")
	public interface MyClass extends Entity {}

	@rdf("urn:test:MyOtherClass")
	public interface MyOtherClass extends Entity {}

	@oneOf("urn:test:my-individual")
	public interface MyIndividual extends Entity {}

	public void testClass() throws Exception {
		Iterator<MyClass> iter = manager.findAll(MyClass.class).iterator();
		assertTrue(iter.hasNext());
		assertEquals("my-class", iter.next().getQName().getLocalPart());
		assertFalse(iter.hasNext());
	}

	public void testOneOf() throws Exception {
		Iterator<MyIndividual> iter = manager.findAll(MyIndividual.class).iterator();
		assertTrue(iter.hasNext());
		assertEquals("my-individual", iter.next().getQName().getLocalPart());
		assertFalse(iter.hasNext());
	}

	@Override
	protected void setUp() throws Exception {
		module.addConcept(MyClass.class);
		module.addConcept(MyOtherClass.class);
		module.addConcept(MyIndividual.class);
		super.setUp();
		manager.designate(new QName(BASE, "my-class"), MyClass.class);
		manager.designate(new QName(BASE, "my-other-class"), MyOtherClass.class);
	}
}
