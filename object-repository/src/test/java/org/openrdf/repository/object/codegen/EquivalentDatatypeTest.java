package org.openrdf.repository.object.codegen;

import junit.framework.TestSuite;

import org.openrdf.repository.object.base.ScriptTestCase;

public class EquivalentDatatypeTest extends ScriptTestCase {

	public static TestSuite suite() throws Exception {
		return ScriptTestCase.suite(EquivalentDatatypeTest.class, "/ontologies/equivalent.ttl", "#test");
	}

}
