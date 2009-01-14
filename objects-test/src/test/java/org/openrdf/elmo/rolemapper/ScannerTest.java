package org.openrdf.elmo.rolemapper;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

import junit.framework.TestCase;

public class ScannerTest extends TestCase {
	private ClassLoader ccl;

	@Override
	protected void setUp() throws Exception {
		ccl = Thread.currentThread().getContextClassLoader();
	}

	public void testJar() throws Exception {
		URL jar = ccl.getResource("testcases/resources.jar");
		ClassLoader cl = new URLClassLoader(new URL[]{jar}, null);
		URL resource = cl.getResource("META-INF/scanning");
		assertNotNull(resource);
		Scanner scanner = new Scanner(cl, "META-INF/scanning");
		List<String> list = scanner.scan(resource);
		assertFalse(list.isEmpty());
		assertEquals(3, list.size());
	}
}
