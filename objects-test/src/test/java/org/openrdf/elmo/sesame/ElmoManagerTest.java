package org.openrdf.elmo.sesame;

import javax.xml.namespace.QName;

import junit.framework.Test;

import org.openrdf.elmo.ElmoModule;
import org.openrdf.elmo.sesame.base.RepositoryTestCase;
import org.openrdf.elmo.sesame.concepts.Person;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.contextaware.ContextAwareConnection;

public class ElmoManagerTest extends RepositoryTestCase {

	public static Test suite() throws Exception {
		return RepositoryTestCase.suite(ElmoManagerTest.class);
	}

	private SesameManagerFactory factory;

	private SesameManager manager;

	private ContextAwareConnection conn;

	private ValueFactory vf;

	public void testCreateBean() throws Exception {
		assertEquals(0, conn.size());
		manager.create(Person.class);
		assertEquals(1, conn.size());
		assertTrue(conn.hasStatement((URI) null, RDF.TYPE, vf
				.createURI("http://xmlns.com/foaf/0.1/Person")));
	}

	public void testContainsBean() throws Exception {
		Object bean = manager.designate(new QName("urn:me"), Person.class);
		assertTrue(manager.contains(bean));
	}

	public void testRenameBean() throws Exception {
		assertEquals(0, conn.size());
		Person me = manager.create(Person.class);
		Person friend = manager.create(Person.class);
		friend.getFoafKnows().add(me);
		assertEquals(3, conn.size());
		manager.getTransaction().begin();
		me = manager.rename(me, new QName("urn:me"));
		manager.getTransaction().commit();
		assertEquals(3, conn.size());
		assertTrue(conn.hasStatement((URI) null, vf
				.createURI("http://xmlns.com/foaf/0.1/knows"), vf
				.createURI("urn:me")));
	}

	public void testRemoveBean() throws Exception {
		assertEquals(0, conn.size());
		Person me = manager.create(Person.class);
		Person friend = manager.create(Person.class);
		friend.getFoafKnows().add(me);
		assertEquals(3, conn.size());
		manager.remove(me);
		assertEquals(1, conn.size());
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		vf = repository.getValueFactory();
		factory = new SesameManagerFactory(new ElmoModule(), repository);
		manager = factory.createElmoManager();
		conn = manager.getConnection();
	}

	@Override
	protected void tearDown() throws Exception {
		manager.close();
		super.tearDown();
	}
}
