package org.openrdf.script;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.script.base.ScriptTestCase;

public class ValueExprTest extends ScriptTestCase {

	private ValueFactory vf = ValueFactoryImpl.getInstance();

	public void test_not() throws Exception {
		assertEquals(vf.createLiteral(Boolean.TRUE), eval("!false"));
	}

	public void test_multiply() throws Exception {
		assertEquals(vf.createLiteral(new BigInteger("4")), eval("2 * 2"));
	}

	public void test_divide() throws Exception {
		assertEquals(vf.createLiteral(new BigDecimal("2")), eval("4 / 2"));
	}

	public void test_add() throws Exception {
		assertEquals(vf.createLiteral(new BigInteger("4")), eval("2 + 2"));
	}

	public void test_subtract() throws Exception {
		assertEquals(vf.createLiteral(new BigInteger("4")), eval("6 - 2"));
	}

	public void test_eq() throws Exception {
		assertEquals(vf.createLiteral(Boolean.TRUE), eval("1 = 1"));
	}

	public void test_neq() throws Exception {
		assertEquals(vf.createLiteral(Boolean.TRUE), eval("1 != 2"));
	}

	public void test_lt() throws Exception {
		assertEquals(vf.createLiteral(Boolean.TRUE), eval("1 < 2"));
	}

	public void test_le() throws Exception {
		assertEquals(vf.createLiteral(Boolean.TRUE), eval("1 <= 1"));
	}

	public void test_gt() throws Exception {
		assertEquals(vf.createLiteral(Boolean.TRUE), eval("2 > 1"));
	}

	public void test_ge() throws Exception {
		assertEquals(vf.createLiteral(Boolean.TRUE), eval("1 >= 1"));
	}

	public void test_and() throws Exception {
		assertEquals(vf.createLiteral(Boolean.TRUE), eval("true && !false"));
	}

	public void test_or() throws Exception {
		assertEquals(vf.createLiteral(Boolean.TRUE), eval("true || false"));
	}

	private Value eval(String code) throws Exception {
		return evaluateSingleValue(code);
	}
}
