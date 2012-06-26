package org.openrdf.model;

import junit.framework.Test;

import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.model.impl.UnionModel;

public class TestUnionModel extends TestModel {

	public static Test suite() throws Exception {
		return TestModel.suite(TestUnionModel.class);
	}

	public TestUnionModel(String name) {
		super(name);
	}

	public Model makeEmptyModel() {
		return new UnionModel(new LinkedHashModel());
	}
}
