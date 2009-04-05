package org.openrdf.repository.object.codegen;

import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectRepository;
import org.openrdf.repository.object.base.CodeGenTestCase;
import org.openrdf.repository.object.config.ObjectRepositoryFactory;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.memory.MemoryStore;

public class NamedTest extends CodeGenTestCase {

	public void testNamed() throws Exception {
		addRdfSource("/ontologies/named-ontology.owl");
		ObjectRepositoryFactory ofm = new ObjectRepositoryFactory();
		ObjectRepository repo = ofm.createRepository(converter, new SailRepository(new MemoryStore()));
		repo.setDataDir(targetDir);
		repo.initialize();
		ObjectConnection manager = repo.getConnection();
		ClassLoader cl = manager.getObjectFactory().getClassLoader();
		Class<?> Document = Class.forName("somepack.age.Doc", true, cl);
		Document.getMethod("getNameTitle");
		Document.getMethod("getSubjects");
		manager.close();
		repo.shutDown();
	}
}
