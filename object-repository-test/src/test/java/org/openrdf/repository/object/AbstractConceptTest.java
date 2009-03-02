package org.openrdf.repository.object;

import junit.framework.Test;

import org.openrdf.model.impl.URIImpl;
import org.openrdf.repository.object.annotations.rdf;
import org.openrdf.repository.object.base.ElmoManagerTestCase;

public class AbstractConceptTest  extends ElmoManagerTestCase {

	public static Test suite() throws Exception {
		return ElmoManagerTestCase.suite(AbstractConceptTest.class);
	}

	public static abstract class Person implements RDFObject {
		@rdf("urn:test:name")
		private String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public abstract String getFirstName();
	}

	public static abstract class FirstNameSupport {
		public abstract String getName();
		public String getFirstName() {
			return getName().split(" ")[0];
		}
	}

	@Override
	public void setUp() throws Exception {
		module.addConcept(Person.class, new URIImpl("urn:test:Person"));
		module.addBehaviour(FirstNameSupport.class, new URIImpl("urn:test:Person"));
		super.setUp();
	}

	public void testAbstractConcept() throws Exception {
		URIImpl id = new URIImpl("urn:test:me");
		Person me = manager.addType(manager.getObject(id), Person.class);
		me.setName("James Leigh");
		assertEquals("James", me.getFirstName());
	}
}
