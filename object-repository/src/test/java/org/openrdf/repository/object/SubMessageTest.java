package org.openrdf.repository.object;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collections;
import java.util.Set;

import junit.framework.Test;

import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.repository.object.annotations.parameterTypes;
import org.openrdf.repository.object.annotations.rdf;
import org.openrdf.repository.object.base.ObjectRepositoryTestCase;
import org.openrdf.repository.object.concepts.Message;

public class SubMessageTest extends ObjectRepositoryTestCase {

	public static Test suite() throws Exception {
		return ObjectRepositoryTestCase.suite(SubMessageTest.class);
	}

	@rdf(RDFS.NAMESPACE + "subClassOf")
	@Retention(RetentionPolicy.RUNTIME)
	public @interface subMessageOf {
		public String[] value();
	}

	@rdf("urn:test:Concept")
	public interface Concept {
		void msg1();
		void msg2();
		void msg3();
		void msg4();
		void msg5();
		void msg6();
		void msg7();
		Object msg8();
		String msg9();
		Set<?> msg10();
		@subMessageOf("urn:test:msg10")
		String msg11();
	}

	@rdf("urn:test:msg6")
	public interface Msg6 extends Message {
	}

	public static abstract class Behaviour implements Concept {
		public static int base;
		public static int message;

		@rdf("urn:test:base")
		public void base() {
			base++;
		}

		@subMessageOf("urn:test:base")
		@parameterTypes({})
		public void msg1(Message msg) throws Exception {
			message++;
			msg.proceed();
		}

		@subMessageOf("urn:test:base")
		public void msg2() {
			message++;
		}

		@subMessageOf("urn:test:base")
		@rdf("urn:test:msg3")
		public void msg3() {
			message++;
		}

		@subMessageOf("urn:test:msg3")
		public void msg4() {
			message++;
		}

		@rdf("urn:test:msg5")
		public void msg5(Message msg) {
			if (msg instanceof Msg6) {
				message++;
			}
		}

		@subMessageOf("urn:test:msg5")
		@rdf("urn:test:msg6")
		public void msg6() {
			message++;
		}

		@rdf("urn:test:msg8")
		public Object msg8() {
			message++;
			return "msg8";
		}

		@subMessageOf("urn:test:msg8")
		@rdf("urn:test:msg9")
		public String msg9() {
			message++;
			return null;
		}

		@rdf("urn:test:msg10")
		public Set<?> msg10() {
			message++;
			return Collections.singleton("msg10");
		}
	}

	public static abstract class Behaviour2 implements Concept {
		public static int message;

		@subMessageOf("urn:test:base")
		public void msg7() {
			message++;
		}
	}

	public void setUp() throws Exception {
		Behaviour.base = 0;
		Behaviour.message = 0;
		Behaviour2.message = 0;
		config.addAnnotation(subMessageOf.class);
		config.addConcept(Concept.class);
		config.addConcept(Msg6.class);
		config.addBehaviour(Behaviour.class);
		config.addBehaviour(Behaviour2.class);
		super.setUp();
	}

	public void testBaseMessage() throws Exception {
		Concept o = con.addDesignation(con.getObject("urn:test:obj"), Concept.class);
		o.msg1();
		assertEquals(1, Behaviour.message);
		assertEquals(1, Behaviour.base);
	}

	public void testMsg2() throws Exception {
		Concept o = con.addDesignation(con.getObject("urn:test:obj"), Concept.class);
		o.msg2();
		assertEquals(1, Behaviour.message);
		assertEquals(1, Behaviour.base);
	}

	public void testMsg4() throws Exception {
		Concept o = con.addDesignation(con.getObject("urn:test:obj"), Concept.class);
		o.msg4();
		assertEquals(2, Behaviour.message);
		assertEquals(1, Behaviour.base);
	}

	public void testMsg6() throws Exception {
		Concept o = con.addDesignation(con.getObject("urn:test:obj"), Concept.class);
		o.msg6();
		assertEquals(2, Behaviour.message);
	}

	public void testMsg7() throws Exception {
		Concept o = con.addDesignation(con.getObject("urn:test:obj"), Concept.class);
		o.msg7();
		assertEquals(1, Behaviour2.message);
		assertEquals(1, Behaviour.base);
	}

	public void testMsg9() throws Exception {
		Concept o = con.addDesignation(con.getObject("urn:test:obj"), Concept.class);
		assertEquals("msg8", o.msg9());
		assertEquals(2, Behaviour.message);
	}

	public void testMsg10() throws Exception {
		Concept o = con.addDesignation(con.getObject("urn:test:obj"), Concept.class);
		assertEquals("msg10", o.msg11());
		assertEquals(1, Behaviour.message);
	}

}
