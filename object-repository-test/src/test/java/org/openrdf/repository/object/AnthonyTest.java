package org.openrdf.repository.object;

import junit.framework.Test;

import org.openrdf.repository.object.annotations.rdf;
import org.openrdf.repository.object.base.ElmoManagerTestCase;

public class AnthonyTest extends ElmoManagerTestCase {
	public static Test suite() throws Exception {
		return ElmoManagerTestCase.suite(AnthonyTest.class);
	}

	@rdf("http://www.test.com#Person")
	public interface IPerson {

		@rdf("http://www.test.com#name")
		public String getName();

		public void setName(String name);

		public String getOtherProperty();
	}

	@rdf("http://www.test.com#Person")
	public static class Person implements IPerson {

		private String name;
		private IPerson person;

		public Person() {
		}

		public Person(IPerson person) {
			this.person = person;
		}

		public String getName() {
			if (person != null)
				return person.getName();
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getOtherProperty() {
			return getName().substring(0, 1);
		}
	}

	public void testMerge() throws Exception {
		IPerson p = new Person();
		p.setName("testName");
		manager.merge(p);

		IPerson f = manager.findAll(IPerson.class).iterator().next();
		assertEquals("testName", f.getName());
		assertEquals("t", f.getOtherProperty());
	}

	@Override
	protected void setUp() throws Exception {
		module.addConcept(IPerson.class);
		module.addBehaviour(Person.class);
		super.setUp();
	}
}
