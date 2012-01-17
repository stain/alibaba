package org.openrdf.model;

import junit.framework.Test;

import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.model.impl.MemoryOverflowModel;
import org.openrdf.model.impl.StatementImpl;
import org.openrdf.model.impl.ValueFactoryImpl;

public class TestMemoryOverflowModel extends TestModel {

	public static Test suite() throws Exception {
		return TestModel.suite(TestMemoryOverflowModel.class);
	}

	public TestMemoryOverflowModel(String name) {
		super(name);
	}

	public MemoryOverflowModel makeEmptyModel() {
		return new MemoryOverflowModel();
	}

	public void testIncreaseTotalMemory() throws Exception {
		ValueFactory vf = ValueFactoryImpl.getInstance();
		Model model = new LinkedHashModel();
		for (int i = 0; i < 100000; i++) {
			model.add(new StatementImpl(vf.createBNode(), vf
					.createURI("urn:test:pred"), vf.createBNode()));
		}
	}

	public void testOverflow() throws Exception {
		ValueFactory vf = ValueFactoryImpl.getInstance();
		MemoryOverflowModel model = makeEmptyModel();
		for (int i = 0; i < 100000; i++) {
			model.add(new StatementImpl(vf.createBNode(), vf
					.createURI("urn:test:pred"), vf.createBNode()));
		}
		model.release();
	}
}
