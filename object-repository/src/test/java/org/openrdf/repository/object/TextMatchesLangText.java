package org.openrdf.repository.object;

import junit.framework.TestCase;

public class TextMatchesLangText extends TestCase {

	public void testRange() throws Exception {
		String range = "de-*-DE";
		assertTrue(new Text("", "de-DE").matchesLang(range));
		assertTrue(new Text("", "de-de").matchesLang(range));
		assertTrue(new Text("", "de-Latn-DE").matchesLang(range));
		assertTrue(new Text("", "de-Latf-DE").matchesLang(range));
		assertTrue(new Text("", "de-DE-x-goethe").matchesLang(range));
		assertTrue(new Text("", "de-Latn-DE-1996").matchesLang(range));
		assertTrue(new Text("", "de-Deva-DE").matchesLang(range));
		assertFalse(new Text("", "de").matchesLang(range));
		assertFalse(new Text("", "de-x-DE").matchesLang(range));
		assertFalse(new Text("", "de-Deva").matchesLang(range));
	}

	public void testSynonym() throws Exception {
		String range = "de-DE";
		assertTrue(new Text("", "de-DE").matchesLang(range));
		assertTrue(new Text("", "de-de").matchesLang(range));
		assertTrue(new Text("", "de-Latn-DE").matchesLang(range));
		assertTrue(new Text("", "de-Latf-DE").matchesLang(range));
		assertTrue(new Text("", "de-DE-x-goethe").matchesLang(range));
		assertTrue(new Text("", "de-Latn-DE-1996").matchesLang(range));
		assertTrue(new Text("", "de-Deva-DE").matchesLang(range));
		assertFalse(new Text("", "de").matchesLang(range));
		assertFalse(new Text("", "de-x-DE").matchesLang(range));
		assertFalse(new Text("", "de-Deva").matchesLang(range));
	}
}
