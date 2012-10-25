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
	
	// TODO: Test actual OWL compilation
}
