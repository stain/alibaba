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

	@matches("**")
	public interface AnyHierarchy {
	}

	@matches("*/*")
	public interface AnyPath2 {
	}

	@matches("/path")
	public interface Path {
	}

	@matches("/path/*")
	public interface AnySubPath {
	}

	@matches("*/")
	public interface AnyRootPath {
	}

	@matches("*.com/*")
	public interface AnyDotCom {
	}

	@matches("*.com")
	public interface AnyDotComOrigin {
	}

	@matches("*localhost")
	public interface LocalhostNoPath {
	}

	@matches("*localhost*")
	public interface Localhost {
	}

	@matches("*localhost/*")
	public interface LocalhostWithPath {
	}

	@iri("urn:test:Something")
	public interface Something {
	}

	public void setUp() throws Exception {
		config.addConcept(TestResource.class);
		config.addConcept(TestSomething.class);
		config.addConcept(Anything.class);
		config.addConcept(AnyHierarchy.class);
		config.addConcept(AnyPath.class);
		config.addConcept(AnyPath2.class);
		config.addConcept(Path.class);
		config.addConcept(AnySubPath.class);
		config.addConcept(AnyRootPath.class);
		config.addConcept(AnyDotCom.class);
		config.addConcept(AnyDotComOrigin.class);
		config.addConcept(LocalhostNoPath.class);
		config.addConcept(Localhost.class);
		config.addConcept(LocalhostWithPath.class);
		config.addConcept(Something.class);
		super.setUp();
	}

	public void testOthers() throws Exception {
		Object o = con.getObject("urn:nothing");
		assertFalse(o instanceof TestResource);
		assertFalse(o instanceof TestSomething);
		assertTrue(o instanceof Anything);
		assertFalse(o instanceof AnyHierarchy);
		assertFalse(o instanceof AnyPath);
		assertFalse(o instanceof AnyPath2);
		assertFalse(o instanceof Path);
		assertFalse(o instanceof AnySubPath);
		assertFalse(o instanceof AnyRootPath);
		assertFalse(o instanceof AnyDotCom);
		assertFalse(o instanceof AnyDotComOrigin);
		assertFalse(o instanceof LocalhostNoPath);
		assertFalse(o instanceof Localhost);
		assertFalse(o instanceof LocalhostWithPath);
	}

	public void testMatch() throws Exception {
		Object o = con.getObject("urn:test:something");
		assertTrue(o instanceof TestResource);
		assertTrue(o instanceof TestSomething);
		assertTrue(o instanceof Anything);
		assertFalse(o instanceof AnyHierarchy);
		assertFalse(o instanceof AnyPath);
		assertFalse(o instanceof AnyPath2);
		assertFalse(o instanceof Path);
		assertFalse(o instanceof AnySubPath);
		assertFalse(o instanceof AnyRootPath);
		assertFalse(o instanceof AnyDotCom);
		assertFalse(o instanceof AnyDotComOrigin);
		assertFalse(o instanceof LocalhostNoPath);
		assertFalse(o instanceof Localhost);
		assertFalse(o instanceof LocalhostWithPath);
	}

	public void testMatchWithTypes() throws Exception {
		Object o = con.getObject("urn:test:something");
		o = con.addDesignation(o, Something.class);
		assertTrue(o instanceof TestResource);
		assertTrue(o instanceof TestSomething);
		assertTrue(o instanceof Anything);
		assertFalse(o instanceof AnyHierarchy);
		assertFalse(o instanceof AnyPath);
		assertFalse(o instanceof AnyPath2);
		assertFalse(o instanceof Path);
		assertFalse(o instanceof AnySubPath);
		assertFalse(o instanceof AnyRootPath);
		assertFalse(o instanceof AnyDotCom);
		assertFalse(o instanceof AnyDotComOrigin);
		assertFalse(o instanceof LocalhostNoPath);
		assertFalse(o instanceof Localhost);
		assertFalse(o instanceof LocalhostWithPath);
	}

	public void testMatchPath() throws Exception {
		Object o = con.getObject("file:///path");
		assertFalse(o instanceof TestResource);
		assertFalse(o instanceof TestSomething);
		assertTrue(o instanceof Anything);
		assertTrue(o instanceof AnyHierarchy);
		assertTrue(o instanceof AnyPath);
		assertTrue(o instanceof AnyPath2);
		assertTrue(o instanceof Path);
		assertFalse(o instanceof AnySubPath);
		assertFalse(o instanceof AnyRootPath);
		assertFalse(o instanceof AnyDotCom);
		assertFalse(o instanceof AnyDotComOrigin);
		assertFalse(o instanceof LocalhostNoPath);
		assertFalse(o instanceof Localhost);
		assertFalse(o instanceof LocalhostWithPath);
	}

	public void testMatchPathSlash() throws Exception {
		Object o = con.getObject("file:///path/");
		assertFalse(o instanceof TestResource);
		assertFalse(o instanceof TestSomething);
		assertTrue(o instanceof Anything);
		assertTrue(o instanceof AnyHierarchy);
		assertTrue(o instanceof AnyPath);
		assertTrue(o instanceof AnyPath2);
		assertFalse(o instanceof Path);
		assertTrue(o instanceof AnySubPath);
		assertFalse(o instanceof AnyRootPath);
		assertFalse(o instanceof AnyDotCom);
		assertFalse(o instanceof AnyDotComOrigin);
		assertFalse(o instanceof LocalhostNoPath);
		assertFalse(o instanceof Localhost);
		assertFalse(o instanceof LocalhostWithPath);
	}

	public void testMatchSubPath() throws Exception {
		Object o = con.getObject("file:///path/sub");
		assertFalse(o instanceof TestResource);
		assertFalse(o instanceof TestSomething);
		assertTrue(o instanceof Anything);
		assertTrue(o instanceof AnyHierarchy);
		assertTrue(o instanceof AnyPath);
		assertTrue(o instanceof AnyPath2);
		assertFalse(o instanceof Path);
		assertTrue(o instanceof AnySubPath);
		assertFalse(o instanceof AnyRootPath);
		assertFalse(o instanceof AnyDotCom);
		assertFalse(o instanceof AnyDotComOrigin);
		assertFalse(o instanceof LocalhostNoPath);
		assertFalse(o instanceof Localhost);
		assertFalse(o instanceof LocalhostWithPath);
	}

	public void testMatchRootPath() throws Exception {
		Object o = con.getObject("file://localhost/");
		assertFalse(o instanceof TestResource);
		assertFalse(o instanceof TestSomething);
		assertTrue(o instanceof Anything);
		assertTrue(o instanceof AnyHierarchy);
		assertTrue(o instanceof AnyPath);
		assertTrue(o instanceof AnyPath2);
		assertFalse(o instanceof Path);
		assertFalse(o instanceof AnySubPath);
		assertTrue(o instanceof AnyRootPath);
		assertFalse(o instanceof AnyDotCom);
		assertFalse(o instanceof AnyDotComOrigin);
		assertFalse(o instanceof LocalhostNoPath);
		assertTrue(o instanceof Localhost);
		assertTrue(o instanceof LocalhostWithPath);
	}

	public void testMatchLocalhostNoPath() throws Exception {
		Object o = con.getObject("file://localhost");
		assertFalse(o instanceof TestResource);
		assertFalse(o instanceof TestSomething);
		assertTrue(o instanceof Anything);
		assertTrue(o instanceof AnyHierarchy);
		assertFalse(o instanceof AnyPath);
		assertFalse(o instanceof AnyPath2);
		assertFalse(o instanceof Path);
		assertFalse(o instanceof AnySubPath);
		assertFalse(o instanceof AnyRootPath);
		assertFalse(o instanceof AnyDotCom);
		assertFalse(o instanceof AnyDotComOrigin);
		assertTrue(o instanceof LocalhostNoPath);
		assertTrue(o instanceof Localhost);
		assertFalse(o instanceof LocalhostWithPath);
	}

	public void testMatchDotComRootPath() throws Exception {
		Object o = con.getObject("file://example.com/");
		assertFalse(o instanceof TestResource);
		assertFalse(o instanceof TestSomething);
		assertTrue(o instanceof Anything);
		assertTrue(o instanceof AnyHierarchy);
		assertTrue(o instanceof AnyPath);
		assertTrue(o instanceof AnyPath2);
		assertFalse(o instanceof Path);
		assertFalse(o instanceof AnySubPath);
		assertTrue(o instanceof AnyRootPath);
		assertTrue(o instanceof AnyDotCom);
		assertFalse(o instanceof AnyDotComOrigin);
		assertFalse(o instanceof LocalhostNoPath);
		assertFalse(o instanceof Localhost);
		assertFalse(o instanceof LocalhostWithPath);
	}

	public void testMatchDotComOrigin() throws Exception {
		Object o = con.getObject("file://example.com");
		assertFalse(o instanceof TestResource);
		assertFalse(o instanceof TestSomething);
		assertTrue(o instanceof Anything);
		assertTrue(o instanceof AnyHierarchy);
		assertFalse(o instanceof AnyPath);
		assertFalse(o instanceof AnyPath2);
		assertFalse(o instanceof Path);
		assertFalse(o instanceof AnySubPath);
		assertFalse(o instanceof AnyRootPath);
		assertFalse(o instanceof AnyDotCom);
		assertTrue(o instanceof AnyDotComOrigin);
		assertFalse(o instanceof LocalhostNoPath);
		assertFalse(o instanceof Localhost);
		assertFalse(o instanceof LocalhostWithPath);
	}

	public void testMatchDotComPath() throws Exception {
		Object o = con.getObject("file://example.com/path");
		assertFalse(o instanceof TestResource);
		assertFalse(o instanceof TestSomething);
		assertTrue(o instanceof Anything);
		assertTrue(o instanceof AnyHierarchy);
		assertTrue(o instanceof AnyPath);
		assertTrue(o instanceof AnyPath2);
		assertTrue(o instanceof Path);
		assertFalse(o instanceof AnySubPath);
		assertFalse(o instanceof AnyRootPath);
		assertTrue(o instanceof AnyDotCom);
		assertFalse(o instanceof AnyDotComOrigin);
		assertFalse(o instanceof LocalhostNoPath);
		assertFalse(o instanceof Localhost);
		assertFalse(o instanceof LocalhostWithPath);
	}

}
