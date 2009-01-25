package org.openrdf.repository.object;

import junit.framework.Test;

import org.openrdf.repository.object.base.ElmoManagerTestCase;
import org.openrdf.repository.object.concepts.Agent;

public class FunctionalTest extends ElmoManagerTestCase {
	public static Test suite() throws Exception {
		return ElmoManagerTestCase.suite(FunctionalTest.class);
	}

	public void testGender() throws Exception {
		Agent a = manager.addType(manager.getObjectFactory().createBlankObject(), Agent.class);
		a.setFoafGender("male");
		Object item = manager.prepareObjectQuery("SELECT DISTINCT ?item WHERE {?item ?p ?o}").evaluate().singleResult();
		assertTrue(((Agent)item).getFoafGender().equals("male"));
	}

	@Override
	protected void setUp() throws Exception {
		module.addConcept(Agent.class);
		super.setUp();
	}
}
