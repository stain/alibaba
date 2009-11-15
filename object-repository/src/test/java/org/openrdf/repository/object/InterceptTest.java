package org.openrdf.repository.object;

import junit.framework.Test;

import org.openrdf.model.impl.URIImpl;
import org.openrdf.repository.object.annotations.parameterTypes;
import org.openrdf.repository.object.base.ObjectRepositoryTestCase;
import org.openrdf.repository.object.concepts.Message;

public class InterceptTest extends ObjectRepositoryTestCase {
	public static Test suite() throws Exception {
		return ObjectRepositoryTestCase.suite(InterceptTest.class);
	}

	public static class CConcept {
		public static int count;

		public void increment1() {
			count++;
		}

		public void increment2() {
			count++;
		}
	}

	public static class Behaviour {
		public static int count;

		@parameterTypes( {})
		public void increment1(Message msg) {
			count++;
			msg.proceed();
		}

		@parameterTypes( {})
		public void increment2(Message msg) {
			count++;
		}
	}

	public void setUp() throws Exception {
		config.addConcept(CConcept.class, new URIImpl("urn:test:Concept"));
		config.addBehaviour(Behaviour.class, new URIImpl("urn:test:Concept"));
		super.setUp();
	}

	public void testInterceptBaseMethod() throws Exception {
		CConcept.count = 0;
		Behaviour.count = 0;
		CConcept concept = con.addDesignation(
				con.getObject("urn:test:concept"), CConcept.class);
		concept.increment1();
		assertEquals(1, CConcept.count);
		assertEquals(1, Behaviour.count);
	}

	public void testOverrideBaseMethod() throws Exception {
		CConcept.count = 0;
		Behaviour.count = 0;
		CConcept concept = con.addDesignation(
				con.getObject("urn:test:concept"), CConcept.class);
		concept.increment2();
		assertEquals(1, Behaviour.count);
		assertEquals(0, CConcept.count);
	}
}
