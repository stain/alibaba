package org.openrdf.elmo.sesame;

import junit.framework.Test;

import org.openrdf.elmo.sesame.base.ElmoManagerTestCase;
import org.openrdf.elmo.sesame.concepts.Person;
import org.openrdf.repository.object.ObjectQuery;

public class ElmoQueryTest extends ElmoManagerTestCase {

	public static Test suite() throws Exception {
		return ElmoManagerTestCase.suite(ElmoQueryTest.class);
	}

	private static final String QUERY_PERSON_SMITH = "PREFIX foaf: <http://xmlns.com/foaf/0.1/> SELECT ?person WHERE { ?person foaf:family_name \"Smith\" }";

	private static final String QUERY_PERSON_NAME_SMITH = "PREFIX foaf: <http://xmlns.com/foaf/0.1/> SELECT ?person ?name WHERE { ?person foaf:family_name \"Smith\" ; foaf:name ?name }";

	private static final String QUERY_NAME_SMITH = "PREFIX foaf: <http://xmlns.com/foaf/0.1/> SELECT ?name WHERE { ?person foaf:family_name \"Smith\" ; foaf:name ?name }";

	public void testBeanQuery() throws Exception {
		ObjectQuery query = manager.createQuery(QUERY_PERSON_SMITH);
		int count = 0;
		for (Object bean : query.evaluate().asList()) {
			Person person = (Person) bean;
			count++;
			assertTrue(person.getFoafNames().contains("Bob")
					|| person.getFoafNames().contains("John"));
		}
		assertEquals(2, count);
	}

	public void testTupleQuery() throws Exception {
		ObjectQuery query = manager.createQuery(QUERY_PERSON_NAME_SMITH);
		int count = 0;
		for (Object row : query.evaluate().asList()) {
			Person person = (Person) ((Object[]) row)[0];
			String name = (String) ((Object[]) row)[1];
			count++;
			assertTrue(person.getFoafNames().contains("Bob")
					|| person.getFoafNames().contains("John"));
			assertTrue(name.equals("Bob") || name.equals("John"));
		}
		assertEquals(2, count);
	}

	public void testLiteralQuery() throws Exception {
		ObjectQuery query = manager.createQuery(QUERY_NAME_SMITH);
		int count = 0;
		for (Object result : query.evaluate().asList()) {
			String name = (String) result;
			count++;
			assertTrue(name.equals("Bob") || name.equals("John"));
		}
		assertEquals(2, count);
	}

	public void testResourceQuery() throws Exception {
		ObjectQuery query = manager.createQuery(QUERY_PERSON_SMITH);
		int count = 0;
		for (Object bean : query.evaluate().asList()) {
			Person person = (Person) bean;
			count++;
			assertTrue(person.getFoafNames().contains("Bob")
					|| person.getFoafNames().contains("John"));
		}
		assertEquals(2, count);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		Person bob = manager.create(Person.class);
		bob.getFoafNames().add("Bob");
		bob.getFoafFamily_names().add("Smith");
		Person john = manager.create(Person.class);
		john.getFoafNames().add("John");
		john.getFoafFamily_names().add("Smith");
	}
}
