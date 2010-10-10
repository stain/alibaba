package org.openrdf.repository.object.codegen;

import junit.framework.TestSuite;

import org.openrdf.repository.object.base.ScriptTestCase;

public class ScriptTest extends ScriptTestCase {

	public static TestSuite suite() throws Exception {
		return ScriptTestCase.suite(ScriptTest.class, "/ontologies/script.ttl", "#test");
	}

}
