package org.openrdf.repository.object;

import java.util.Set;

import junit.framework.Test;

import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.repository.object.annotations.iri;
import org.openrdf.repository.object.annotations.triggeredBy;
import org.openrdf.repository.object.base.ObjectRepositoryTestCase;
import org.openrdf.repository.object.base.RepositoryTestCase;

public class TriggerTest extends ObjectRepositoryTestCase {

	public static Test suite() throws Exception {
		return RepositoryTestCase.suite(TriggerTest.class);
	}

	@iri("urn:test:Person1")
	public static class Person1 {
		@iri("urn:test:name1")
		private String name;
		@iri("urn:test:firstName1")
		private String firstName;
		@iri("urn:test:lastName1")
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
		@triggeredBy("urn:test:name1")
		public void nameChangedTo(String name) {
			firstName = name.split(" ")[0];
		}
		@triggeredBy("urn:test:name1")
		public void updateLastName(String name) {
			lastName = name.split(" ")[1];
		}
	}

	public void testConceptTrigger() throws Exception {
		URI id = new URIImpl("urn:test:person");
		Person1 person = con.addDesignation(con.getObject(id), Person1.class);
		person.setName("James Leigh");
		assertEquals("James", person.getFirstName());
		assertEquals("Leigh", person.getLastName());
	}

	@iri("urn:test:Person2")
	public interface Person2 {
		@iri("urn:test:name2")
		public String getName();
		public void setName(String name);
		@iri("urn:test:firstName2")
		public String getFirstName();
		public void setFirstName(String name);
		@iri("urn:test:lastName2")
		public String getLastName();
		public void setLastName(String name);
		@triggeredBy("urn:test:name2")
		public void nameChangedTo(String name);
	}

	public static abstract class UpdateFirstName2 implements Person2 {
		public void nameChangedTo(String name) {
			setFirstName(name.split(" ")[0]);
		}
	}

	public static abstract class UpdateLastName2 implements Person2 {
		public void nameChangedTo(String name) {
			setLastName(name.split(" ")[1]);
		}
	}

	public void testInterfaceTrigger() throws Exception {
		URI id = new URIImpl("urn:test:person");
		Person2 person = con.addDesignation(con.getObject(id), Person2.class);
		person.setName("James Leigh");
		assertEquals("James", person.getFirstName());
		assertEquals("Leigh", person.getLastName());
	}

	@iri("urn:test:Person3")
	public interface Person3 {
		@iri("urn:test:name3")
		public String getName();
		public void setName(String name);
		@iri("urn:test:firstname3")
		public String getFirstName();
		public void setFirstName(String name);
		@iri("urn:test:lastName3")
		public String getLastName();
		public void setLastName(String name);
	}

	public static abstract class UpdateFirstName3 implements Person3 {
		@triggeredBy("urn:test:name3")
		public void nameChangedTo(String name) {
			setFirstName(name.split(" ")[0]);
		}
	}

	public static abstract class UpdateLastName3 implements Person3 {
		@triggeredBy("urn:test:name3")
		public void nameChangedTo(String name) {
			setLastName(name.split(" ")[1]);
		}
	}

	public void testBehaviourTrigger() throws Exception {
		URI id = new URIImpl("urn:test:person");
		Person3 person = con.addDesignation(con.getObject(id), Person3.class);
		person.setName("James Leigh");
		assertEquals("James", person.getFirstName());
		assertEquals("Leigh", person.getLastName());
	}

	@iri("urn:test:Person4")
	public interface Person4 {
		@iri("urn:test:name4")
		public String getName();
		public void setName(String name);
		@iri("urn:test:lastName4")
		public String getLastName();
		public void setLastName(String name);
	}

	public static abstract class UpdateLastName4 implements Person4 {
		@triggeredBy("urn:test:name4")
		public void nameChangedTo() {
			setLastName(getName().split(" ")[1]);
		}
	}

	public void testTriggerFailure() throws Exception {
		URI id = new URIImpl("urn:test:person");
		Person4 person = con.addDesignation(con.getObject(id), Person4.class);
		try {
			person.setName("James");
			fail();
		} catch (Exception e) {
		}
		assertNull(person.getName());
	}

	@iri("urn:test:Person5")
	public static abstract class Person5 {
		public static boolean nameChanged;
		public static boolean friendChanged;
		public static boolean bestFriendChanged;
		@iri("urn:test:name5")
		public abstract String getName();
		public abstract void setName(String name);
		@iri("urn:test:friend5")
		public abstract Set<Person5> getFriends();
		public abstract void setFriends(Set<Person5> friends);
		@iri("urn:test:bestFriend5")
		public abstract Person5 getBestFriend();
		public abstract void setBestFriend(Person5 bestFriend);
		@triggeredBy({"urn:test:name5", "urn:test:friend5", "urn:test:bestFriend5"})
		public void changing(
				@iri("urn:test:name5") String name,
				@iri("urn:test:friend5") Set<Person5> friends,
				@iri("urn:test:bestFriend5") Person5 bestFriend) {
			if (name != null) {
				nameChanged = true;
			}
			if (friends != null) {
				friendChanged = true;
			}
			if (bestFriend != null) {
				bestFriendChanged = true;
			}
		}
	}

	public void testMultiplePredicates() throws Exception {
		Person5.nameChanged = false;
		Person5.friendChanged = false;
		Person5.bestFriendChanged = false;
		Person5 person = con.addDesignation(con.getObject("urn:test:person"), Person5.class);
		Person5 friend = con.addDesignation(con.getObject("urn:test:friend"), Person5.class);
		person.setName("James Leigh");
		person.getFriends().add(friend);
		person.setBestFriend(friend);
		assertTrue(Person5.nameChanged);
		assertTrue(Person5.friendChanged);
		assertTrue(Person5.bestFriendChanged);
	}

	@Override
	public void setUp() throws Exception {
		config.addConcept(Person1.class);
		config.addConcept(Person2.class);
		config.addBehaviour(UpdateFirstName2.class);
		config.addBehaviour(UpdateLastName2.class);
		config.addConcept(Person3.class);
		config.addBehaviour(UpdateFirstName3.class);
		config.addBehaviour(UpdateLastName3.class);
		config.addConcept(Person4.class);
		config.addBehaviour(UpdateLastName4.class);
		config.addConcept(Person5.class);
		super.setUp();
	}
}
