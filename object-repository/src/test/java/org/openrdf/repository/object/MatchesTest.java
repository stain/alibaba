package org.openrdf.repository.object;

import junit.framework.Test;

import org.openrdf.repository.object.annotations.matches;
import org.openrdf.repository.object.annotations.iri;
import org.openrdf.repository.object.base.ObjectRepositoryTestCase;

public class MatchesTest extends ObjectRepositoryTestCase {
	public static Test suite() throws Exception {
		return ObjectRepositoryTestCase.suite(MatchesTest.class);
	}

	@matches("urn:test:*")
	public interface TestResource {
	}

	@matches("urn:test:something")
	public interface TestSomething {
	}

	@matches("*")
	public interface Anything {
	}

	@matches("/*")
	public interface AnyPath {
	}

	@matches("/path")
	public interface Path {
	}

	@matches("/path/*")
	public interface AnySubPath {
	}

	@iri("urn:test:Something")
	public interface Something {
	}

	public void setUp() throws Exception {
		config.addConcept(TestResource.class);
		config.addConcept(TestSomething.class);
		config.addConcept(Anything.class);
		config.addConcept(AnyPath.class);
		config.addConcept(Path.class);
		config.addConcept(AnySubPath.class);
		config.addConcept(Something.class);
		super.setUp();
	}

	public void testOthers() throws Exception {
		Object o = con.getObject("urn:nothing");
		assertFalse(o instanceof TestResource);
		assertFalse(o instanceof TestSomething);
		assertTrue(o instanceof Anything);
		assertFalse(o instanceof AnyPath);
		assertFalse(o instanceof Path);
		assertFalse(o instanceof AnySubPath);
	}

	public void testMatch() throws Exception {
		Object o = con.getObject("urn:test:something");
		assertTrue(o instanceof TestResource);
		assertTrue(o instanceof TestSomething);
		assertTrue(o instanceof Anything);
		assertFalse(o instanceof AnyPath);
		assertFalse(o instanceof Path);
		assertFalse(o instanceof AnySubPath);
	}

	public void testMatchWithTypes() throws Exception {
		Object o = con.getObject("urn:test:something");
		o = con.addDesignation(o, Something.class);
		assertTrue(o instanceof TestResource);
		assertTrue(o instanceof TestSomething);
		assertTrue(o instanceof Anything);
		assertFalse(o instanceof AnyPath);
		assertFalse(o instanceof Path);
		assertFalse(o instanceof AnySubPath);
	}

	public void testMatchPath() throws Exception {
		Object o = con.getObject("file:///path");
		assertFalse(o instanceof TestResource);
		assertFalse(o instanceof TestSomething);
		assertTrue(o instanceof Anything);
		assertTrue(o instanceof AnyPath);
		assertTrue(o instanceof Path);
		assertFalse(o instanceof AnySubPath);
	}

	public void testMatchPathSlash() throws Exception {
		Object o = con.getObject("file:///path/");
		assertFalse(o instanceof TestResource);
		assertFalse(o instanceof TestSomething);
		assertTrue(o instanceof Anything);
		assertTrue(o instanceof AnyPath);
		assertFalse(o instanceof Path);
		assertTrue(o instanceof AnySubPath);
	}

	public void testMatchSubPath() throws Exception {
		Object o = con.getObject("file:///path/sub");
		assertFalse(o instanceof TestResource);
		assertFalse(o instanceof TestSomething);
		assertTrue(o instanceof Anything);
		assertTrue(o instanceof AnyPath);
		assertFalse(o instanceof Path);
		assertTrue(o instanceof AnySubPath);
	}

}
