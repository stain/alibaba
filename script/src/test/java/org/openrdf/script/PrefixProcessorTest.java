package org.openrdf.script;

import org.openrdf.model.Value;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.script.base.ScriptTestCase;

public class PrefixProcessorTest extends ScriptTestCase {

	public void test_nobase() throws Exception {
		assertEquals(new URIImpl("urn:root"), eval("prefix u:<urn:>. u:root."));
	}

	private Value eval(String code) throws Exception {
		return evaluateSingleValue(code);
	}

}
