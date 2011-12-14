package org.openrdf.repository.object;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.Test;

import org.openrdf.annotations.Iri;
import org.openrdf.annotations.Bind;
import org.openrdf.annotations.Sparql;
import org.openrdf.model.Model;
import org.openrdf.model.Statement;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.BindingSet;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.TupleQueryResult;
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

	@Iri(NS + "Person")
	public interface Person {
		@Iri(NS + "name")
		String getName();

		void setName(String name);

		@Iri(NS + "age")
		int getAge();

		void setAge(int age);

		@Sparql(PREFIX + "INSERT { $this :friend $friend } WHERE { $friend a :Person }")
		void addFriend(@Bind("friend") Person friend);

		@Sparql(PREFIX + "SELECT ?friend WHERE { $this :friend ?friend . "
				+ "?friend :name $name }")
		Person findFriendByName(@Bind("name") String arg1);

		@Sparql(PREFIX + "SELECT ?friend WHERE { $this :friend ?friend . "
				+ "?friend :name $name }")
		BindingSet findBindingSetByName(@Bind("name") String arg1);

		@Sparql(PREFIX
				+ "CONSTRUCT { ?friend :name $name } WHERE { $this :friend ?friend . "
				+ "?friend :name $name }")
		Statement findStatementByName(@Bind("name") String arg1);

		@Sparql(PREFIX + "ASK { $this :friend $friend }")
		boolean isFriend(@Bind("friend") Person arg1);

		@Sparql(PREFIX + "SELECT ?person WHERE { ?person a :Person }")
		Result<Person> findAllPeople();

		@Sparql(PREFIX + "SELECT ?person WHERE { ?person a $type }")
		<T> Result<T> findAll(@Bind("type") Class<T> type);

		@Sparql(PREFIX + "SELECT ?person ?name "
				+ "WHERE { ?person :name ?name } ORDER BY ?name")
		TupleQueryResult findAllPeopleName();

		@Sparql(PREFIX + "CONSTRUCT { ?person a :Person; :name ?name } "
				+ "WHERE { ?person :name ?name } ORDER BY ?name")
		GraphQueryResult loadAllPeople();

		@Sparql(PREFIX + "CONSTRUCT { ?person a :Person; :name ?name } "
				+ "WHERE { ?person :name ?name } ORDER BY ?name")
		Model loadAllPeopleInModel();

		@Sparql(PREFIX + "SELECT ?person WHERE { ?person a :Person }")
		Set<Person> findFriends();

		@Sparql(PREFIX + "SELECT ?person\n"
				+ "WHERE { ?person a :Person; :name ?name }\n"
				+ "ORDER BY ?name")
		List<Person> findFriendByNames();

		@Sparql(PREFIX + "SELECT $age WHERE { $this :age $age }")
		int findAge(@Bind("age") int age);

		@Sparql(PREFIX + "SELECT ?nothing WHERE { $this :age $bool }")
		Object findNull(@Bind("bool") boolean bool);
	}

	@Iri(NS + "Employee")
	public static class Employee {
		@Iri(NS + "name")
		String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@Sparql(PREFIX + "SELECT ?name WHERE { $this :name ?name }")
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
		me.addFriend(john);
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
