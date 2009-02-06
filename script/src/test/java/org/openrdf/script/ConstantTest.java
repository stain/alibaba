package org.openrdf.script;

import java.math.BigInteger;

import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.script.base.ScriptTestCase;

public class ConstantTest extends ScriptTestCase {

	private ValueFactory vf = ValueFactoryImpl.getInstance();

	public void test_string() throws Exception {
		assertEquals(vf.createLiteral("string"), eval("'string'"));
	}

	public void test_lang() throws Exception {
		assertEquals(vf.createLiteral("string", "en"), eval("'string'@en"));
	}

	public void test_bool() throws Exception {
		assertEquals(vf.createLiteral(Boolean.TRUE), eval("true"));
	}

	public void test_numeric() throws Exception {
		assertEquals(vf.createLiteral(new BigInteger("1")), eval("1"));
	}

	private Value eval(String code) throws Exception {
		return evaluateSingleValue(code);
	}
}
