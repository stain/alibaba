package org.openrdf.repository.object.codegen;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectRepository;
import org.openrdf.repository.object.behaviours.AssertSupport;
import org.openrdf.repository.object.config.ObjectRepositoryConfig;
import org.openrdf.repository.object.config.ObjectRepositoryFactory;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.sail.memory.MemoryStore;

public class ScriptTest extends TestCase {
	public static ObjectRepository repository;

	public static Test suite() throws Exception {
		init("/ontologies/script.ttl");
		TestSuite suite = new TestSuite(ScriptTest.class.getName());
		ObjectConnection con = repository.getConnection();
		try {
			Class<? extends Object> c = con.getObject("urn:test").getClass();
			for (Method method : c.getMethods()) {
				if (method.getName().startsWith("test")
						&& method.getParameterTypes().length == 0
						&& method.getReturnType().equals(Void.TYPE)) {
					suite.addTest(new ScriptTest(method.getName()));
				}
			}
		} finally {
			con.close();
		}
		if (suite.countTestCases() == 0) {
			suite.addTest(TestSuite
					.warning("Individual urn:test has no public test methods"));
		}
		return suite;
	}

	private static void init(String ontology) throws Exception {
		if (repository == null) {
			URL url = ScriptTest.class.getResource(ontology);
			assertNotNull(url);
			ObjectRepositoryFactory ofm = new ObjectRepositoryFactory();
			ObjectRepositoryConfig config = ofm.getConfig();
			config.addBehaviour(AssertSupport.class, RDFS.RESOURCE);
			config.addImports(url);
			repository = ofm.getRepository(config);
			repository.setDelegate(new SailRepository(new MemoryStore()));
			repository.initialize();
			ObjectConnection con = repository.getConnection();
			try {
				ValueFactory vf = con.getValueFactory();
				String base = url.toExternalForm();
				RDFFormat format = RDFFormat.forFileName(url.getFile());
				con.add(url, base, format, vf.createURI(base));
			} finally {
				con.close();
			}
		}
	}

	private ObjectConnection con;
	private Object self;

	public ScriptTest(String name) {
		super(name);
	}

	public void setUp() throws Exception {
		init("/ontologies/script.ttl");
		con = repository.getConnection();
		self = con.getObject("urn:test");
		try {
			self.getClass().getMethod("setUp").invoke(self);
		} catch (NoSuchMethodException exc) {
			// skip
		}
	}

	public void tearDown() throws Exception {
		try {
			self.getClass().getMethod("tearDown").invoke(self);
		} catch (NoSuchMethodException exc) {
			// skip
		} finally {
			con.close();
		}
	}

	@Override
	protected void runTest() throws Throwable {
		assertNotNull(getName());
		Method runMethod = null;
		try {
			runMethod = self.getClass().getMethod(getName(), (Class[]) null);
		} catch (NoSuchMethodException e) {
			fail("Method \"" + getName() + "\" not found");
		}
		if (!Modifier.isPublic(runMethod.getModifiers())) {
			fail("Method \"" + getName() + "\" should be public");
		}

		try {
			runMethod.invoke(self, (Object[]) new Class[0]);
		} catch (InvocationTargetException e) {
			e.fillInStackTrace();
			throw e.getTargetException();
		} catch (IllegalAccessException e) {
			e.fillInStackTrace();
			throw e;
		}

	}

}
