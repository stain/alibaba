package org.openrdf.elmo.sesame;

import junit.framework.Test;

import org.openrdf.elmo.sesame.base.ElmoManagerTestCase;
import org.openrdf.elmo.sesame.concepts.Agent;

public class FunctionalTest extends ElmoManagerTestCase {
	public static Test suite() throws Exception {
		return ElmoManagerTestCase.suite(FunctionalTest.class);
	}

	public void testGender() throws Exception {
		Agent a = manager.create(Agent.class);
		a.setFoafGender("male");
		Object item = manager.createQuery("SELECT DISTINCT ?item WHERE {?item ?p ?o}").getSingleResult();
		assertTrue(((Agent)item).getFoafGender().equals("male"));
	}

	@Override
	protected void setUp() throws Exception {
		module.addConcept(Agent.class);
		super.setUp();
	}
}
