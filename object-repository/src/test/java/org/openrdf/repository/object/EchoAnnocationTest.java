package org.openrdf.repository.object;

import java.lang.reflect.Method;

import junit.framework.Test;

import org.openrdf.repository.object.annotations.iri;
import org.openrdf.repository.object.base.ObjectRepositoryTestCase;

public class EchoAnnocationTest extends ObjectRepositoryTestCase {

	public static Test suite() throws Exception {
		return ObjectRepositoryTestCase.suite(EchoAnnocationTest.class);
	}

	@iri("urn:test:Concept")
	public interface Concept {
		@iri("urn:test:pred")
		String getProperty();

		void setProperty(String property);

		@iri("urn:test:method")
		void method(@iri("urn:test:param") String param);
	}

	public abstract static class Behaviour implements Concept {

		@iri("urn:test:method")
		public void method(@iri("urn:test:param") String param) {
			// do nothing
		}
	}

	@Override
	public void setUp() throws Exception {
		config.addConcept(Concept.class);
		config.addBehaviour(Behaviour.class);
		super.setUp();
	}

	public void testPropertyAnnotation() throws Exception {
		Concept c = con.addDesignation(of.createObject(), Concept.class);
		Method property = c.getClass().getMethod("getProperty");
		assertTrue(property.isAnnotationPresent(iri.class));
	}

	public void testMethodAnnotation() throws Exception {
		Concept c = con.addDesignation(of.createObject(), Concept.class);
		Method method = c.getClass().getMethod("method", String.class);
		assertTrue(method.isAnnotationPresent(iri.class));
	}

	public void testMethodParameterAnnotation() throws Exception {
		Concept c = con.addDesignation(of.createObject(), Concept.class);
		Method method = c.getClass().getMethod("method", String.class);
		assertTrue(method.getParameterAnnotations()[0][0] instanceof iri);
	}

}
