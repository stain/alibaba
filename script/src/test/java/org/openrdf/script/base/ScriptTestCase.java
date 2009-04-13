package org.openrdf.script.base;


import junit.framework.TestCase;

import org.openrdf.model.Value;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectRepository;
import org.openrdf.repository.object.config.ObjectRepositoryFactory;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.memory.MemoryStore;
import org.openrdf.script.ScriptEngine;
import org.openrdf.script.ScriptParser;
import org.openrdf.script.ast.ParseException;
import org.openrdf.script.ast.SyntaxTreeBuilder;
import org.openrdf.script.ast.TokenMgrError;
import org.openrdf.script.model.Body;
import org.openrdf.store.StoreException;

public abstract class ScriptTestCase extends TestCase {

	private ObjectConnection con;
	private ObjectRepository repository;

	@Override
	public void setUp() throws Exception {
		super.setUp();
		ObjectRepositoryFactory factory = new ObjectRepositoryFactory();
		SailRepository delegate = new SailRepository(new MemoryStore());
		delegate.initialize();
		repository = factory.createRepository(delegate);
		con = repository.getConnection();
	}

	@Override
	public void tearDown() throws Exception {
		con.close();
		repository.shutDown();
		super.tearDown();
	}

	public Object evaluateSingleObject(String code) throws TokenMgrError,
			ParseException, StoreException {
		return con.getObject(evaluateSingleValue(code));
	}

	public Value evaluateSingleValue(String code) throws TokenMgrError,
			ParseException, StoreException {
		return new ScriptEngine(repository).evalSingleValue(code);
	}

	public void evaluate(String code) throws TokenMgrError, ParseException,
			StoreException {
		SyntaxTreeBuilder.parse(code).dump("");
		Body body = new ScriptParser().parse(code);
		System.out.println(body.toString());
	}

}
