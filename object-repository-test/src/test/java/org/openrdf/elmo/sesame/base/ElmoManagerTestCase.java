package org.openrdf.elmo.sesame.base;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectRepository;
import org.openrdf.repository.object.config.ObjectConfig;

public class ElmoManagerTestCase extends TestCase {

	private static final String DELIM = RepositoryTestCase.DELIM;

	public static Test suite() throws Exception {
		return new TestSuite();
	}

	public static Test suite(Class<? extends TestCase> subclass)
			throws Exception {
		return RepositoryTestCase.suite(subclass);
	}

	private RepositoryTestCase repoTc = new RepositoryTestCase();

	protected ObjectConfig module = new ObjectConfig();

	protected ObjectConnection manager;

	public ElmoManagerTestCase() {
		repoTc.setFactory(RepositoryTestCase.DEFAULT);
	}

	public ElmoManagerTestCase(String name) {
		setName(name);
	}

	@Override
	public String getName() {
		return super.getName() + DELIM + repoTc.getFactory();
	}

	@Override
	public void setName(String name) {
		int pound = name.indexOf(DELIM);
		if (pound < 0) {
			super.setName(name);
			repoTc.setFactory(RepositoryTestCase.DEFAULT);
		} else {
			super.setName(name.substring(0, pound));
			repoTc.setFactory(name.substring(pound + 1));
		}
	}

	@Override
	protected void setUp() throws Exception {
		repoTc.setUp();
		ObjectRepository managerFactory;
		managerFactory = new ObjectRepository(module, repoTc.repository);
		manager = managerFactory.createElmoManager();
	}

	@Override
	protected void tearDown() throws Exception {
		try {
			if (manager.isOpen()) {
				manager.close();
			}
			repoTc.tearDown();
		} catch (Exception e) {
		}
	}

}
