package org.openrdf.repository.object;

import junit.framework.Test;

import org.openrdf.repository.object.RDFObject;
import org.openrdf.repository.object.annotations.rdf;
import org.openrdf.repository.object.base.ElmoManagerTestCase;
import org.openrdf.store.StoreException;

public class AbstractBehaviourTest extends ElmoManagerTestCase {

	public static Test suite() throws Exception {
		return ElmoManagerTestCase.suite(AbstractBehaviourTest.class);
	}

	@rdf("urn:example:Concept")
	public interface Concept extends RDFObject {
		@rdf("urn:example:int")
		int getInt();
		void setInt(int value);
		int test();
		void remove();
		void setOneWay(Concept value);
		@rdf("urn:example:ortheother")
		Concept getOrTheOther();
		void setOrTheOther(Concept value);
		@rdf("urn:example:string")
		String getString();
		void setString(String value);
	}

	public static abstract class AbstractConcept implements Concept {
		public int test() {
			setString("blah");
			if ("blah".equals(getString()))
				return getInt();
			return 0;
		}
		public void setOneWay(Concept value) {
			value.setOrTheOther(this);
		}
	}

	public void testAbstractConcept() throws StoreException {
		Concept concept = manager.addType(manager.getObjectFactory().createBlankObject(), Concept.class);
		concept.setInt(5);
		assertEquals(5, concept.test());
	}

	public void testAssignment() throws StoreException {
		Concept c1 = manager.addType(manager.getObjectFactory().createBlankObject(), Concept.class);
		Concept c2 = manager.addType(manager.getObjectFactory().createBlankObject(), Concept.class);
		c1.setOneWay(c2);
		assertEquals(c1, c2.getOrTheOther());
	}

	protected void setUp() throws Exception {
		module.addConcept(Concept.class);
		module.addBehaviour(AbstractConcept.class);
		super.setUp();
	}
}
