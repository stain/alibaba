package org.openrdf.alibaba.pov;


import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import junit.framework.TestCase;

import org.openrdf.alibaba.vocabulary.POV;
import org.openrdf.elmo.codegen.CodeGenerator;
import org.openrdf.elmo.codegen.JavaCompiler;
import org.openrdf.elmo.codegen.OntologyConverter;
import org.openrdf.elmo.codegen.OwlGenerator;
import org.openrdf.elmo.sesame.SesameManager;
import org.openrdf.model.impl.URIImpl;

public class PovOntologyTest extends TestCase {

	private static final String POV_PKG = "org.openrdf.alibaba.concepts";

	private static final String POINT_OF_VIEW_ONTOLOGY = "META-INF/ontologies/point-of-view-ontology.owl";

	public void testPov() throws Exception {
		OntologyConverter converter = new OntologyConverter();
		File jar = File.createTempFile("pov-", ".jar");
		converter.addOntology(new URIImpl(POV.NS), POV_PKG);
		converter.setNamespace("pov", POV.NS);
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		converter.addRdfSource(cl.getResource(POINT_OF_VIEW_ONTOLOGY));
		converter.init();
		converter.createClasses(jar);
		assertEquals(40, countClasses(jar, ".java"), 20);
		assertEquals(40, countClasses(jar, ".class"), 20);
		jar.delete();
	}

	@Override
	protected void setUp() throws Exception {
		enableLogging(OntologyConverter.class);
		// enableLogging(OwlNormalizer.class);
		enableLogging(CodeGenerator.class);
		enableLogging(OwlGenerator.class);
		enableLogging(JavaCompiler.class);
		enableLogging(SesameManager.class);
		super.setUp();
	}

	private void enableLogging(Class<?> clazz) {
		Logger logger = Logger.getLogger(clazz.getName());
		ConsoleHandler handler = new ConsoleHandler();
		logger.addHandler(handler);
		handler.setLevel(Level.FINE);
		logger.setLevel(Level.FINE);
	}

	private int countClasses(File jar, String suffix) throws IOException {
		int count = 0;
		JarFile file = new JarFile(jar);
		Enumeration<JarEntry> entries = file.entries();
		while (entries.hasMoreElements()) {
			String name = entries.nextElement().getName();
			if (name.endsWith(suffix) && !name.contains("-"))
				count++;
		}
		return count;
	}
}
