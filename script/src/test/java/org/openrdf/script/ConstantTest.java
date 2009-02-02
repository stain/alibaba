package org.openrdf.script;

import java.math.BigInteger;

import org.openrdf.script.base.ScriptTestCase;

public class ConstantTest extends ScriptTestCase {

	public void test_string() throws Exception {
		assertEquals("string", eval("'string'"));
	}

	public void test_bool() throws Exception {
		assertEquals(Boolean.TRUE, eval("true"));
	}

	public void test_numeric() throws Exception {
		assertEquals(new BigInteger("1"), eval("1"));
	}

	private Object eval(String code) throws Exception {
		return evaluateSingleObject(code);
	}
}
