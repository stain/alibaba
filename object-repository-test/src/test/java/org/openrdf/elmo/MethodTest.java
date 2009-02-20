package org.openrdf.elmo.codegen;

import java.io.File;

import javax.xml.namespace.QName;

import org.openrdf.elmo.ElmoManager;
import org.openrdf.elmo.ElmoModule;
import org.openrdf.elmo.sesame.SesameManagerFactory;

public class MethodTest extends CodeGenTestCase {

	private static final String NS = "urn:test:candy#";

	public void testCandy() throws Exception {
		File jar = createFile("candy.jar");
		OntologyConverter converter = createConventer();
		converter.addRdfSource(find("/META-INF/ontologies/elmo-ontology.owl"));
		converter.addRdfSource(find("/ontologies/candy-ontology.owl"));
		converter.bindPackageToNamespace("candy", NS);
		converter.init();
		converter.createClasses(jar);
		assertTrue(jar.isFile());
		assertEquals(9, countClasses(jar, ".java"));
		assertEquals(9, countClasses(jar, ".class"));
		testCandyJar(jar);
	}

	private void testCandyJar(File jar) throws Exception {
		ElmoModule module = new ElmoModule();
		module.addJarFileUrl(jar.toURI().toURL());
		SesameManagerFactory factory = new SesameManagerFactory(module);
		ElmoManager manager = factory.createElmoManager();
		Class<?> Candy = Class.forName("candy.Candy", false, module.getClassLoader());
		Class<?> Person = Class.forName("candy.Person", false, module.getClassLoader());
		Class<?> John = Class.forName("candy.John", false, module.getClassLoader());
		Object candy = manager.create(Candy);
		Object person = manager.create(Person);
		Object john = manager.designate(new QName(NS, "john"), Person);
		Candy.getMethod("setGood", boolean.class).invoke(candy, true);
		assertEquals(Boolean.TRUE, Person.getMethod("taste", Candy).invoke(person, candy));
		Candy.getMethod("setGood", boolean.class).invoke(candy, false);
		assertEquals(Boolean.FALSE, Person.getMethod("taste", Candy).invoke(person, candy));
		John.getMethod("setGoodDay", boolean.class).invoke(john, true);
		assertEquals(Boolean.TRUE, Person.getMethod("taste", Candy).invoke(john, candy));
		John.getMethod("setGoodDay", boolean.class).invoke(john, false);
		assertEquals(Boolean.FALSE, Person.getMethod("taste", Candy).invoke(john, candy));
	}
}
