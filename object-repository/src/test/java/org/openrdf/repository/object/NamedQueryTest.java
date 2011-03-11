package org.openrdf.repository.object;

import java.util.List;
import java.util.Set;

import junit.framework.Test;

import org.openrdf.model.Model;
import org.openrdf.model.Statement;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.BindingSet;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.object.annotations.iri;
import org.openrdf.repository.object.annotations.name;
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

	@iri(NS + "Person")
	public interface Person {
		@iri(NS + "name")
		String getName();

		void setName(String name);

		@iri(NS + "age")
		int getAge();

		void setAge(int age);

		@iri(NS + "friend")
		Set<Person> getFriends();

		void setFriends(Set<Person> friends);

		@sparql(PREFIX + "SELECT ?friend WHERE { $this :friend ?friend . "
				+ "?friend :name $name }")
		Person findFriendByName(@name("name") String arg1);

		@sparql(PREFIX + "SELECT ?friend WHERE { $this :friend ?friend . "
				+ "?friend :name $name }")
		BindingSet findBindingSetByName(@name("name") String arg1);

		@sparql(PREFIX
				+ "CONSTRUCT { ?friend :name $name } WHERE { $this :friend ?friend . "
				+ "?friend :name $name }")
		Statement findStatementByName(@name("name") String arg1);

		@sparql(PREFIX + "ASK { $this :friend $friend }")
		boolean isFriend(@name("friend") Person arg1);

		@sparql(PREFIX + "SELECT ?person WHERE { ?person a :Person }")
		Result<Person> findAllPeople();

		@sparql(PREFIX + "SELECT ?person WHERE { ?person a $type }")
		<T> Result<T> findAll(@name("type") Class<T> type);

		@sparql(PREFIX + "SELECT ?person ?name "
				+ "WHERE { ?person :name ?name } ORDER BY ?name")
		TupleQueryResult findAllPeopleName();

		@sparql(PREFIX + "CONSTRUCT { ?person a :Person; :name ?name } "
				+ "WHERE { ?person :name ?name } ORDER BY ?name")
		GraphQueryResult loadAllPeople();

		@sparql(PREFIX + "CONSTRUCT { ?person a :Person; :name ?name } "
				+ "WHERE { ?person :name ?name } ORDER BY ?name")
		Model loadAllPeopleInModel();

		@sparql(PREFIX + "SELECT ?person WHERE { ?person a :Person }")
		Set<Person> findFriends();

		@sparql(PREFIX + "SELECT ?person\n"
				+ "WHERE { ?person a :Person; :name ?name }\n"
				+ "ORDER BY ?name")
		List<Person> findFriendByNames();

		@sparql(PREFIX + "SELECT $age WHERE { $this :age $age }")
		int findAge(@name("age") int age);

		@sparql(PREFIX + "SELECT ?nothing WHERE { $this :age $bool }")
		Object findNull(@name("bool") boolean bool);
	}

	@iri(NS + "Employee")
	public static class Employee {
		@iri(NS + "name")
		String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

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

	public void testBindingSetByName() throws Exception {
		ValueFactory vf = con.getValueFactory();
		BindingSet result = me.findBindingSetByName("john");
		assertEquals(vf.createURI(NS, "john"), result.getValue("friend"));
	}

	public void testStatementByName() throws Exception {
		ValueFactory vf = con.getValueFactory();
		Statement result = me.findStatementByName("john");
		assertEquals(vf.createURI(NS, "john"), result.getSubject());
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
		assertEquals(2, set.size());
	}

	public void testFindAllPerson() throws Exception {
		Result<Person> result = me.findAll(Person.class);
		assertTrue(result.hasNext());
		Set<Person> set = result.asSet();
		assertTrue(set.contains(me));
		assertTrue(set.contains(john));
		assertEquals(2, set.size());
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

	public void testModel() throws Exception {
		Model result = me.loadAllPeopleInModel();
		assertFalse(result.isEmpty());
	}

	public void testSet() throws Exception {
		Set<Person> set = me.findFriends();
		assertEquals(2, set.size());
		assertTrue(set.contains(me));
		assertTrue(set.contains(john));
	}

	public void testList() throws Exception {
		List<Person> list = me.findFriendByNames();
		assertEquals(2, list.size());
		assertEquals(me, list.get(0));
		assertEquals(john, list.get(1));
	}

	public void testInt() throws Exception {
		int age = me.getAge();
		assertEquals(age, me.findAge(age));
	}

	public void testBool() throws Exception {
		me.findNull(true);
	}

	public void testOveride() throws Exception {
		Employee e = con
				.addDesignation(con.getObject(NS + "e"), Employee.class);
		e.setName("employee");
		assertEquals("employee", e.findName());
	}
}
