package org.openrdf.repository.object.compiler;

import junit.framework.TestCase;

import org.openrdf.repository.object.managers.LiteralManager;
import org.openrdf.repository.object.managers.RoleMapper;

public class TestOWLCompiler extends TestCase {

	private OWLCompiler owlCompiler;
	private LiteralManager literalManager;
	private RoleMapper roleMapper;

	public TestOWLCompiler() {
		makeOwlCompiler();
	}
	
	// Annotation missing for Junit < 4.0 
	// @Before
	public void makeOwlCompiler() {
		roleMapper = new RoleMapper();
		literalManager = new LiteralManager();
		owlCompiler = new OWLCompiler(roleMapper, literalManager);
	}
	
	public void testPackageName() throws Exception {
		assertEquals("fred", owlCompiler.packageName("fred"));
		assertEquals("_123abc", owlCompiler.packageName("123abc"));
		assertEquals("f._3red", owlCompiler.packageName("f.3red"));			
		// FIXME: Is this double __ necessary?
		assertEquals("f.__red", owlCompiler.packageName("f._red"));
	}
	
	/**
	 * Test for http://www.openrdf.org/issues/browse/ALI-18
	 */
	public void testInvalidWindowsPackageNames() throws Exception {
		
		// Remember that old COPY CON FILE
		// .. well, those device names are still invalid on Windows
		// http://msdn.microsoft.com/en-us/library/aa561308.aspx
		// 
		// Thus our JAR would not be unzippable on Windows
		
		assertEquals("_con", owlCompiler.packageName("con"));		
		assertEquals("_lpt1", owlCompiler.packageName("lpt1"));

		

		// But not replaced if not surrounded by .
		assertEquals("conx", owlCompiler.packageName("conx"));
		assertEquals("xcon", owlCompiler.packageName("xcon"));
		
		// Case insensitive
		assertEquals("_CoM2", owlCompiler.packageName("CoM2"));
		
		// Also if there's a prefix
		assertEquals("fred._con", owlCompiler.packageName("fred.con"));
		assertEquals("fred._lpt1", owlCompiler.packageName("fred.lpt1"));
		
		// or anywhere earlier
		assertEquals("_con.fred", owlCompiler.packageName("con.fred"));
		assertEquals("_lpt1.soup", owlCompiler.packageName("lpt1.soup"));		
		assertEquals("fred._lpt1.soup", owlCompiler.packageName("fred.lpt1.soup"));

		// This is silly, I know, but the $ could screw up the regex 
		assertEquals("a._clock$.b", owlCompiler.packageName("a.clock$.b"));

		
	}
	
	// TODO: Test actual OWL compilation
}

