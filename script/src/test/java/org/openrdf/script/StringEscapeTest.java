package org.openrdf.script;

import org.openrdf.script.base.ScriptTestCase;

public class StringEscapeTest extends ScriptTestCase {

	public void test_simple1() throws Exception {
		assertEquals("string", eval("$string := 'string'."));
	}

	public void test_simple2() throws Exception {
		assertEquals("string", eval("$string := \"string\"."));
	}

	public void test_long1() throws Exception {
		assertEquals("string", eval("$string := '''string'''."));
	}

	public void test_long2() throws Exception {
		assertEquals("string", eval("$string := \"\"\"string\"\"\"."));
	}

	private Object eval(String code) throws Exception {
		return evaluateSingleObject(code);
	}
}
