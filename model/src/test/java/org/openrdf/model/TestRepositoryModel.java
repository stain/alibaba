package org.openrdf.model;

import junit.framework.Test;

import org.openrdf.model.impl.RepositoryModel;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.memory.MemoryStore;

public class TestRepositoryModel extends TestModel {

	public static Test suite() throws Exception {
		return TestModel.suite(TestRepositoryModel.class);
	}

	public TestRepositoryModel(String name) {
		super(name);
	}

	public Model makeEmptyModel() {
		try {
			SailRepository repo = new SailRepository(new MemoryStore());
			repo.initialize();
			return new RepositoryModel(repo.getConnection());
		} catch (RepositoryException e) {
			throw new AssertionError(e);
		}
	}
}
