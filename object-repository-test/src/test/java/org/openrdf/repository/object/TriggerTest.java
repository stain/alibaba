package org.openrdf.repository.object;

import junit.framework.Test;

import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.repository.object.annotations.rdf;
import org.openrdf.repository.object.annotations.triggeredBy;
import org.openrdf.repository.object.base.ElmoManagerTestCase;
import org.openrdf.repository.object.base.RepositoryTestCase;

public class TriggerTest extends ElmoManagerTestCase {

	public static Test suite() throws Exception {
		return RepositoryTestCase.suite(TriggerTest.class);
	}

	@rdf("urn:test:Person1")
	public static class Person1 {
		@rdf("urn:test:name")
		private String name;
		@rdf("urn:test:firstName")
		private String firstName;
		@rdf("urn:test:lastName")
		private String lastName;
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public String getFirstName() {
			return firstName;
		}
		public String getLastName() {
			return lastName;
		}
		@triggeredBy("urn:test:name")
		public void nameChangedTo() {
			firstName = name.split(" ")[0];
		}
		@triggeredBy("urn:test:name")
		public void updateLastName() {
			lastName = name.split(" ")[1];
		}
	}

	public void testConceptTrigger() throws Exception {
		URI id = new URIImpl("urn:test:person");
		Person1 person = manager.addType(manager.getObject(id), Person1.class);
		person.setName("James Leigh");
		assertEquals("James", person.getFirstName());
		assertEquals("Leigh", person.getLastName());
	}

	@rdf("urn:test:Person2")
	public interface Person2 {
		@rdf("urn:test:name")
		public String getName();
		public void setName(String name);
		@rdf("urn:test:firstName")
		public String getFirstName();
		public void setFirstName(String name);
		@rdf("urn:test:lastName")
		public String getLastName();
		public void setLastName(String name);
		@triggeredBy("urn:test:name")
		public void nameChangedTo();
	}

	public static abstract class UpdateFirstName2 implements Person2 {
		public void nameChangedTo() {
			setFirstName(getName().split(" ")[0]);
		}
	}

	public static abstract class UpdateLastName2 implements Person2 {
		public void nameChangedTo() {
			setLastName(getName().split(" ")[1]);
		}
	}

	public void testInterfaceTrigger() throws Exception {
		URI id = new URIImpl("urn:test:person");
		Person2 person = manager.addType(manager.getObject(id), Person2.class);
		person.setName("James Leigh");
		assertEquals("James", person.getFirstName());
		assertEquals("Leigh", person.getLastName());
	}

	@rdf("urn:test:Person3")
	public interface Person3 {
		@rdf("urn:test:name")
		public String getName();
		public void setName(String name);
		@rdf("urn:test:firstname")
		public String getFirstName();
		public void setFirstName(String name);
		@rdf("urn:test:lastName")
		public String getLastName();
		public void setLastName(String name);
	}

	public static abstract class UpdateFirstName3 implements Person3 {
		@triggeredBy("urn:test:name")
		public void nameChangedTo() {
			setFirstName(getName().split(" ")[0]);
		}
	}

	public static abstract class UpdateLastName3 implements Person3 {
		@triggeredBy("urn:test:name")
		public void nameChangedTo() {
			setLastName(getName().split(" ")[1]);
		}
	}

	public void testBehaviourTrigger() throws Exception {
		URI id = new URIImpl("urn:test:person");
		Person3 person = manager.addType(manager.getObject(id), Person3.class);
		person.setName("James Leigh");
		assertEquals("James", person.getFirstName());
		assertEquals("Leigh", person.getLastName());
	}

	@Override
	public void setUp() throws Exception {
		module.addConcept(Person1.class);
		module.addConcept(Person2.class);
		module.addConcept(UpdateFirstName2.class);
		module.addConcept(UpdateLastName2.class);
		module.addConcept(Person3.class);
		module.addConcept(UpdateFirstName3.class);
		module.addConcept(UpdateLastName3.class);
		super.setUp();
	}
}
