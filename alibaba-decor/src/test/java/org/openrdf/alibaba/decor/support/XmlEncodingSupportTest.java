package org.openrdf.alibaba.decor.support;

import junit.framework.TestCase;

public class XmlEncodingSupportTest extends TestCase {
	public void testEncode() throws Exception {
		String before = "<input type='text' value=\"&lt;tagname&gt;\"/>";
		String after = "&lt;input type=&apos;text&apos; value=&quot;&amp;lt;tagname&amp;gt;&quot;/&gt;";
		assertEquals(after, new XmlEncodingSupport().encode(before));
	}
	public void testDencode() throws Exception {
		String before = "&lt;input type=&apos;text&apos; value=&quot;&amp;lt;tagname&amp;gt;&quot;/&gt;";
		String after = "<input type='text' value=\"&lt;tagname&gt;\"/>";
		assertEquals(after, new XmlEncodingSupport().decode(before));
	}
}
