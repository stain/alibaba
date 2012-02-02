package org.openrdf.sail.optimistic;

import info.aduna.io.IOUtil;
import junit.framework.TestCase;

import org.openrdf.query.QueryLanguage;
import org.openrdf.repository.sail.SailRepositoryConnection;
import org.openrdf.sail.memory.MemoryStore;

public class DeleteInsertTest extends TestCase {
	private OptimisticRepository repo;
	private String NS = "http://example.org/";
	private SailRepositoryConnection con;
	private ClassLoader cl = getClass().getClassLoader();

	@Override
	public void setUp() throws Exception {
		repo = new OptimisticRepository(new MemoryStore());
		repo.setSnapshot(false);
		repo.setSerializable(false);
		repo.initialize();
		con = repo.getConnection();
	}

	@Override
	public void tearDown() throws Exception {
		con.close();
		repo.shutDown();
	}

	public void test() throws Exception {
		String load = IOUtil.readString(cl.getResource("test/insert-data.ru"));
		con.prepareUpdate(QueryLanguage.SPARQL, load, NS).execute();
		con.setAutoCommit(false);
		String modify = IOUtil.readString(cl.getResource("test/delete-insert.ru"));
		con.prepareUpdate(QueryLanguage.SPARQL, modify, NS).execute();
		con.setAutoCommit(true);
		String ask = IOUtil.readString(cl.getResource("test/ask.rq"));
		assertTrue(con.prepareBooleanQuery(QueryLanguage.SPARQL, ask, NS).evaluate());
	}
}
