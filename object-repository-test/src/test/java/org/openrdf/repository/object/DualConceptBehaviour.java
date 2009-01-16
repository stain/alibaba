package org.openrdf.repository.object;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Test;

import org.openrdf.repository.object.RDFObject;
import org.openrdf.repository.object.annotations.rdf;
import org.openrdf.repository.object.base.ElmoManagerTestCase;

public class DualConceptBehaviour extends ElmoManagerTestCase {

	public static Test suite() throws Exception {
		return ElmoManagerTestCase.suite(DualConceptBehaviour.class);
	}

	@rdf("urn:example:Concept1")
	public interface Concept1 extends RDFObject {
		void addBehaviours(List<String> list);
	}

	@rdf("urn:example:Concept2")
	public interface Concept2 extends RDFObject {
		void addBehaviours(List<String> list);
	}

	public static abstract class AbstractConcept1 implements Concept1 {
		public void addBehaviours(List<String> list) {
			list.add("AbstractConcept1");
		}
	}

	public static abstract class AbstractConcept2 implements Concept2 {
		public void addBehaviours(List<String> list) {
			list.add("AbstractConcept2");
		}
	}

	public static abstract class AbstractConcept3 {
		public void addBehaviours(List<String> list) {
			list.add("AbstractConcept3");
		}
	}

	public void testAbstractConcept1() {
		Concept1 concept = manager.create(Concept1.class);
		List<String> list = new ArrayList<String>();
		concept.addBehaviours(list);
		assertEquals(2, list.size());
	}

	public void testAbstractConcept2() {
		Concept2 concept = manager.create(Concept2.class);
		List<String> list = new ArrayList<String>();
		concept.addBehaviours(list);
		assertEquals(2, list.size());
	}

	protected void setUp() throws Exception {
		module.addConcept(Concept1.class);
		module.addConcept(Concept2.class);
		module.addBehaviour(AbstractConcept1.class);
		module.addBehaviour(AbstractConcept2.class);
		module.addBehaviour(AbstractConcept3.class, "urn:example:Concept1");
		module.addBehaviour(AbstractConcept3.class, "urn:example:Concept2");
		super.setUp();
	}
}
