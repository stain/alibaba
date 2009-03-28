package org.openrdf.repository.object.base;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.repository.Repository;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectFactory;
import org.openrdf.repository.object.config.ObjectRepositoryConfig;
import org.openrdf.repository.object.config.ObjectRepositoryFactory;

public class ObjectRepositoryTestCase extends RepositoryTestCase {

	private static final String DELIM = RepositoryTestCase.DELIM;

	public static Test suite() throws Exception {
		return new TestSuite();
	}

	public static Test suite(Class<? extends TestCase> subclass)
			throws Exception {
		return RepositoryTestCase.suite(subclass);
	}

	protected ObjectRepositoryConfig config = new ObjectRepositoryConfig();

	protected ObjectConnection con;

	protected ObjectFactory of;

	public ObjectRepositoryTestCase() {
		super.setFactory(RepositoryTestCase.DEFAULT);
	}

	public ObjectRepositoryTestCase(String name) {
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
		return factory.createRepository(config, delegate);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		con = (ObjectConnection) repository.getConnection();
		con.setNamespace("rdf", RDF.NAMESPACE);
		con.setNamespace("rdfs", RDFS.NAMESPACE);
		of = con.getObjectFactory();
	}

	@Override
	protected void tearDown() throws Exception {
		try {
			if (con.isOpen()) {
				con.close();
			}
			super.tearDown();
		} catch (Exception e) {
		}
	}

}
