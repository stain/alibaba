package org.openrdf.script;

import org.openrdf.model.Value;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.script.base.ScriptTestCase;

public class BaseIriTest extends ScriptTestCase {

	public void test_nobase() throws Exception {
		assertEquals(new URIImpl("urn:root"), eval("<urn:root>."));
	}

	private Value eval(String code) throws Exception {
		return evaluateSingleValue(code);
	}
}
