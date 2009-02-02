package org.openrdf.script.controls;

import org.openrdf.script.base.ScriptTestCase;

public class InsertTest extends ScriptTestCase {

	public void test_insert() throws Exception {
		eval("insert { <urn:s> <urn:p> <urn:o>}");
	}

	private void eval(String code) throws Exception {
		evaluate(code);
	}
}
