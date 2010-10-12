package org.openrdf.repository.object.codegen;

import junit.framework.TestSuite;

import org.openrdf.repository.object.base.ScriptTestCase;

public class RestrictionTest extends ScriptTestCase {

	public static TestSuite suite() throws Exception {
		return ScriptTestCase.suite(RestrictionTest.class, "/ontologies/restriction.ttl", "#test");
	}

}
