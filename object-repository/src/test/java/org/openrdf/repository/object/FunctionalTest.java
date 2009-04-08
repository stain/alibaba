package org.openrdf.repository.object;

import junit.framework.Test;

import org.openrdf.repository.object.base.ObjectRepositoryTestCase;
import org.openrdf.repository.object.concepts.Agent;

public class FunctionalTest extends ObjectRepositoryTestCase {
	public static Test suite() throws Exception {
		return ObjectRepositoryTestCase.suite(FunctionalTest.class);
	}

	public void testGender() throws Exception {
		Agent a = con.addType(con.getObjectFactory().createObject(), Agent.class);
		a.setFoafGender("male");
		Object item = con.prepareObjectQuery("SELECT DISTINCT ?item WHERE {?item ?p ?o}").evaluate().singleResult();
		assertTrue(((Agent)item).getFoafGender().equals("male"));
	}

	@Override
	protected void setUp() throws Exception {
		config.addConcept(Agent.class);
		super.setUp();
	}
}
