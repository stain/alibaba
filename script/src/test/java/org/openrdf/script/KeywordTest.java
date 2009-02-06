package org.openrdf.script;

import org.openrdf.model.Value;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.script.base.ScriptTestCase;
import org.openrdf.script.vocabulary.Script;

public class KeywordTest extends ScriptTestCase {

	private static final String NS = Script.NAMESPACE;

	public void test_notKeyword() throws Exception {
		assertEquals(new URIImpl("urn:root"), eval("prefix :<urn:>. root."));
	}

	public void test_keyword() throws Exception {
		assertEquals(new URIImpl(NS + "begin"), eval("prefix :<urn:>. begin."));
	}

	public void test_excludeKeyword() throws Exception {
		assertEquals(new URIImpl("urn:begin"), eval("keywords while. prefix :<urn:>. begin."));
	}

	private Value eval(String code) throws Exception {
		return evaluateSingleValue(code);
	}

}
