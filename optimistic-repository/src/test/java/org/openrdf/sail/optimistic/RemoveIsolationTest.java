package org.openrdf.sail.optimistic;

import java.util.Collections;

import junit.framework.TestCase;

import org.openrdf.model.Statement;
import org.openrdf.model.ValueFactory;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.memory.MemoryStore;

public class RemoveIsolationTest extends TestCase {

	public void testRemoveOptimisticIsolation() throws Exception {
		Repository myRepository = new OptimisticRepository(new MemoryStore());
		try {
			myRepository.initialize();
			ValueFactory f = myRepository.getValueFactory();
			RepositoryConnection con = myRepository.getConnection();
			try {
				con.setAutoCommit(false);

				con.add(f.createURI("http://example.org/people/alice"), f
						.createURI("http://example.org/ontology/name"), f
						.createLiteral("Alice"));

				con.remove(con.getStatements(null, null, null, true));

				RepositoryResult<Statement> stats = con.getStatements(null,
						null, null, true);
				assertEquals(Collections.emptyList(), stats.asList());
				con.rollback();
			} finally {
				con.close();
			}
		} finally {
			myRepository.shutDown();
		}
	}

	public void testRemoveIsolation() throws Exception {
		Repository myRepository = new SailRepository(new MemoryStore());
		try {
			myRepository.initialize();
			ValueFactory f = myRepository.getValueFactory();
			RepositoryConnection con = myRepository.getConnection();
			try {
				con.setAutoCommit(false);

				con.add(f.createURI("http://example.org/people/alice"), f
						.createURI("http://example.org/ontology/name"), f
						.createLiteral("Alice"));

				con.remove(con.getStatements(null, null, null, true));

				RepositoryResult<Statement> stats = con.getStatements(null,
						null, null, true);
				assertEquals(Collections.emptyList(), stats.asList());
				con.rollback();
			} finally {
				con.close();
			}
		} finally {
			myRepository.shutDown();
		}
	}
}
