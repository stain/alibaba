package org.openrdf.repository.object;

import java.util.Set;

import junit.framework.Test;

import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.object.annotations.name;
import org.openrdf.repository.object.annotations.rdf;
import org.openrdf.repository.object.annotations.sparql;
import org.openrdf.repository.object.base.ObjectRepositoryTestCase;
import org.openrdf.repository.object.base.RepositoryTestCase;
import org.openrdf.result.Result;

public class NamedQueryTest extends ObjectRepositoryTestCase {
	private static final String NS = "urn:test:";
	private static final String PREFIX = "PREFIX :<" + NS + ">\n";
	private Person me;
	private Person john;

	public static Test suite() throws Exception {
		return RepositoryTestCase.suite(NamedQueryTest.class);
	}

	@rdf(NS + "Person")
	public interface Person {
		@rdf(NS + "name")
		String getName();

		void setName(String name);

		@rdf(NS + "age")
		int getAge();

		void setAge(int age);

		@rdf(NS + "friend")
		Set<Person> getFriends();

		void setFriends(Set<Person> friends);

		@sparql(PREFIX + "SELECT ?friend WHERE { $this :friend ?friend . "
				+ "?friend :name $name }")
		Person findFriendByName(@name("name") String arg1);

		@sparql(PREFIX + "ASK { $this :friend $friend }")
		boolean isFriend(@name("friend") Person arg1);

		@sparql(PREFIX + "SELECT ?person WHERE { ?person a :Person }")
		Result<Person> findAllPeople();

		@sparql(PREFIX + "SELECT ?person ?name "
				+ "WHERE { ?person :name ?name } ORDER BY ?name")
		TupleQueryResult findAllPeopleName();

		@sparql(PREFIX + "CONSTRUCT { ?person a :Person; :name ?name } "
				+ "WHERE { ?person :name ?name } ORDER BY ?name")
		GraphQueryResult loadAllPeople();

		@sparql(PREFIX + "SELECT ?person WHERE { ?person a :Person }")
		Set<Person> findFriends();

		@sparql(PREFIX + "SELECT $age WHERE { $this :age $age }")
		int findAge(@name("age") int age);

		@sparql(PREFIX + "SELECT ?nothing WHERE { $this :age $bool }")
		Object findNull(@name("bool") boolean bool);
	}

	@rdf(NS + "Employee")
	public static class Employee {
		@rdf(NS + "name") String name;
		public String getName() { return name; }
		public void setName(String name) { this.name = name; }

		@sparql(PREFIX + "SELECT ?name WHERE { $this :name ?name }")
		public String findName() {
			return "not overriden";
		}
	}

	@Override
	public void setUp() throws Exception {
		config.addConcept(Person.class);
		config.addConcept(Employee.class);
		super.setUp();
		me = con.addDesignation(con.getObject(NS + "me"), Person.class);
		me.setName("james");
		me.setAge(102);
		john = con.addDesignation(con.getObject(NS + "john"), Person.class);
		john.setName("john");
		me.getFriends().add(john);
	}

	public void testFriendByName() throws Exception {
		assertEquals(john, me.findFriendByName("john"));
	}

	public void testIsFriend() throws Exception {
		assertTrue(me.isFriend(john));
	}

	public void testFindAllPeople() throws Exception {
		Result<Person> result = me.findAllPeople();
		assertTrue(result.hasNext());
		Set<Person> set = result.asSet();
		assertTrue(set.contains(me));
		assertTrue(set.contains(john));
	}

	public void testTupleResult() throws Exception {
		TupleQueryResult result = me.findAllPeopleName();
		assertTrue(result.hasNext());
		assertEquals("james", result.next().getValue("name").stringValue());
		result.close();
	}

	public void testConstruct() throws Exception {
		GraphQueryResult result = me.loadAllPeople();
		assertTrue(result.hasNext());
		result.close();
	}

	public void testSet() throws Exception {
		Set<Person> set = me.findFriends();
		assertEquals(2, set.size());
		assertTrue(set.contains(me));
		assertTrue(set.contains(john));
	}

	public void testInt() throws Exception {
		int age = me.getAge();
		assertEquals(age, me.findAge(age));
	}

	public void testBool() throws Exception {
		me.findNull(true);
	}

	public void testOveride() throws Exception {
		Employee e = con.addDesignation(con.getObject(NS+"e"), Employee.class);
		e.setName("employee");
		assertEquals("employee", e.findName());
	}
}
