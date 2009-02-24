package org.openrdf.repository.object;

import junit.framework.Test;

import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.Repository;
import org.openrdf.repository.contextaware.ContextAwareConnection;
import org.openrdf.repository.object.base.RepositoryTestCase;
import org.openrdf.repository.object.concepts.Person;
import org.openrdf.repository.object.config.ObjectRepositoryFactory;

public class ElmoManagerTest extends RepositoryTestCase {

	public static Test suite() throws Exception {
		return RepositoryTestCase.suite(ElmoManagerTest.class);
	}

	private ObjectRepository factory;

	private ObjectConnection manager;

	private ContextAwareConnection conn;

	private ValueFactory vf;

	public void testCreateBean() throws Exception {
		assertEquals(0, conn.size());
		manager.addType(manager.getObjectFactory().createBlankObject(), Person.class);
		assertEquals(1, conn.size());
		assertTrue(conn.hasMatch((URI) null, RDF.TYPE, vf
				.createURI("urn:foaf:Person")));
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		factory = (ObjectRepository) repository;
		manager = factory.getConnection();
		conn = manager;
		vf = conn.getValueFactory();
	}

	@Override
	protected Repository createRepository() throws Exception {
		return new ObjectRepositoryFactory().createRepository(super.createRepository());
	}

	@Override
	protected void tearDown() throws Exception {
		manager.close();
		super.tearDown();
	}
}
