package org.openrdf.repository.object.base;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.repository.Repository;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.config.ObjectRepositoryConfig;
import org.openrdf.repository.object.config.ObjectRepositoryFactory;

public class ElmoManagerTestCase extends RepositoryTestCase {

	private static final String DELIM = RepositoryTestCase.DELIM;

	public static Test suite() throws Exception {
		return new TestSuite();
	}

	public static Test suite(Class<? extends TestCase> subclass)
			throws Exception {
		return RepositoryTestCase.suite(subclass);
	}

	protected ObjectRepositoryConfig module = new ObjectRepositoryConfig();

	protected ObjectConnection manager;

	public ElmoManagerTestCase() {
		super.setFactory(RepositoryTestCase.DEFAULT);
	}

	public ElmoManagerTestCase(String name) {
		setName(name);
	}

	@Override
	public String getName() {
		return super.getName() + DELIM + super.getFactory();
	}

	@Override
	public void setName(String name) {
		int pound = name.indexOf(DELIM);
		if (pound < 0) {
			super.setName(name);
			super.setFactory(RepositoryTestCase.DEFAULT);
		} else {
			super.setName(name.substring(0, pound));
			super.setFactory(name.substring(pound + 1));
		}
	}

	@Override
	protected Repository createRepository() throws Exception {
		Repository delegate = super.createRepository();
		ObjectRepositoryFactory factory = new ObjectRepositoryFactory();
		return factory.createRepository(module, delegate);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		manager = (ObjectConnection) repository.getConnection();
		manager.setNamespace("rdf", RDF.NAMESPACE);
		manager.setNamespace("rdfs", RDFS.NAMESPACE);
	}

	@Override
	protected void tearDown() throws Exception {
		try {
			if (manager.isOpen()) {
				manager.close();
			}
			super.tearDown();
		} catch (Exception e) {
		}
	}

}
