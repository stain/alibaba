package org.openrdf.repository.object.codegen;

import java.io.File;

import org.openrdf.model.ValueFactory;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectFactory;
import org.openrdf.repository.object.ObjectRepository;
import org.openrdf.repository.object.config.ObjectRepositoryFactory;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.memory.MemoryStore;

public class MethodTest extends CodeGenTestCase {

	private static final String NS = "urn:test:candy#";

	public void testCandy() throws Exception {
		addRdfSource("/ontologies/elmo-ontology.owl");
		addRdfSource("/ontologies/candy-ontology.owl");
		File jar = createJar("candy.jar");
		assertTrue(jar.isFile());
		assertEquals(5, countClasses(jar, "candy", ".java"));
		assertEquals(5, countClasses(jar, "candy", ".class"));
	}

	public void testCandyBehaviour() throws Exception {
		addRdfSource("/ontologies/elmo-ontology.owl");
		addRdfSource("/ontologies/candy-ontology.owl");
		File jar = createBehaviourJar("candy-methods.jar");
		assertTrue(jar.isFile());
		assertEquals(3, countClasses(jar, "candy", ".java"));
		assertEquals(3, countClasses(jar, "candy", ".class"));
	}

	public void testCandyJar() throws Exception {
		addRdfSource("/ontologies/elmo-ontology.owl");
		addRdfSource("/ontologies/candy-ontology.owl");
		ObjectRepositoryFactory ofm = new ObjectRepositoryFactory();
		ObjectRepository repo = ofm.createRepository(converter, new SailRepository(new MemoryStore()));
		repo.setDataDir(targetDir);
		repo.initialize();
		ObjectConnection manager = repo.getConnection();
		ClassLoader cl = manager.getObjectFactory().getClassLoader();
		Class<?> Candy = Class.forName("candy.Candy", true, cl);
		Class<?> Person = Class.forName("candy.Person", true, cl);
		Class<?> John = Class.forName("candy.John", true, cl);
		ObjectFactory of = manager.getObjectFactory();
		Object candy = manager.addType(of.createBlankObject(), Candy);
		Object person = manager.addType(of.createBlankObject(), Person);
		ValueFactory vf = manager.getValueFactory();
		Object john = manager.addType(of.createRDFObject(vf.createURI(NS, "john")), Person);
		Candy.getMethod("setCandyGood", boolean.class).invoke(candy, true);
		assertEquals(Boolean.TRUE, Person.getMethod("candyTaste", Candy).invoke(person, candy));
		Candy.getMethod("setCandyGood", boolean.class).invoke(candy, false);
		assertEquals(Boolean.FALSE, Person.getMethod("candyTaste", Candy).invoke(person, candy));
		John.getMethod("setCandyGoodDay", boolean.class).invoke(john, true);
		assertEquals(Boolean.TRUE, Person.getMethod("candyTaste", Candy).invoke(john, candy));
		John.getMethod("setCandyGoodDay", boolean.class).invoke(john, false);
		assertEquals(Boolean.FALSE, Person.getMethod("candyTaste", Candy).invoke(john, candy));
	}
}
