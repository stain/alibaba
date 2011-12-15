package org.openrdf.sail.auditing;


import junit.framework.TestCase;

import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.query.QueryLanguage;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.auditing.vocabulary.Audit;
import org.openrdf.sail.memory.MemoryStore;

public class AuditingTest extends TestCase {
	private ValueFactory vf = ValueFactoryImpl.getInstance();
	private String NS = "http://example.com/";
	private URI carmichael = vf.createURI(NS, "carmichael");
	private URI harris = vf.createURI(NS, "harris");
	private URI jackson = vf.createURI(NS, "jackson");
	private URI johnston = vf.createURI(NS, "johnston");
	private URI lismer = vf.createURI(NS, "lismer");
	private URI macDonald = vf.createURI(NS, "macDonald");
	private URI varley = vf.createURI(NS, "varley");
	private URI thomson = vf.createURI(NS, "thomson");
	private URI knows = vf.createURI("http://xmlns.com/foaf/0.1/knows");
	private RepositoryConnection con;
	private Repository repo;

	private RepositoryConnection reopen(Repository repo,
			RepositoryConnection con) throws RepositoryException {
		con.setAutoCommit(true);
		con.close();
		con = repo.getConnection();
		con.setAutoCommit(false);
		return con;
	}

	public void setUp() throws Exception {
		AuditingSail sail = new AuditingSail(new MemoryStore());
		repo = new SailRepository(sail);
		repo.initialize();
		con = repo.getConnection();
	}

	public void tearDown() throws Exception {
		con.close();
		repo.shutDown();
	}

	public void testAdd() throws Exception {
		con.setAutoCommit(false);
		assertTrue(con.isEmpty());
		con.add(carmichael, knows, harris);
		con.setAutoCommit(true);
		assertTrue(con.hasStatement(carmichael, knows, harris, false));
		assertTrue(con.hasStatement(carmichael, Audit.REVISION, null, false));
		assertFalse(con.hasStatement(null, null, null, false, new Resource[]{null}));
		assertEquals(1, con.getContextIDs().asList().size());
		assertTrue(con.hasStatement(null, RDF.TYPE, Audit.TRANSACTION, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, Audit.RECENT, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, Audit.OBSOLETE, false));
		assertTrue(con.hasStatement(null, Audit.COMMITTED_ON, null, false));
		assertFalse(con.hasStatement(null, Audit.MODIFIED, null, false));
		assertFalse(con.hasStatement(null, Audit.PREDECESSOR, null, false));
		assertFalse(con.hasStatement(null, Audit.CONTAINED, null, false));
	}

	public void testAddMany() throws Exception {
		con.setAutoCommit(false);
		assertTrue(con.isEmpty());
		con.add(carmichael, knows, harris);
		con = reopen(repo, con);
		con.add(harris, knows, jackson);
		con = reopen(repo, con);
		con.add(jackson, knows, johnston);
		con = reopen(repo, con);
		con.add(johnston, knows, lismer);
		con = reopen(repo, con);
		con.add(lismer, knows, macDonald);
		con = reopen(repo, con);
		con.add(macDonald, knows, varley);
		con = reopen(repo, con);
		con.add(varley, knows, thomson);
		con.setAutoCommit(true);
		assertTrue(con.hasStatement(carmichael, knows, harris, false));
		assertTrue(con.hasStatement(carmichael, Audit.REVISION, null, false));
		assertFalse(con.hasStatement(null, null, null, false, new Resource[]{null}));
		assertEquals(7, con.getContextIDs().asList().size());
		assertTrue(con.hasStatement(null, RDF.TYPE, Audit.TRANSACTION, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, Audit.RECENT, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, Audit.OBSOLETE, false));
		assertTrue(con.hasStatement(null, Audit.COMMITTED_ON, null, false));
		assertFalse(con.hasStatement(null, Audit.MODIFIED, null, false));
		assertTrue(con.hasStatement(null, Audit.PREDECESSOR, null, false));
		assertFalse(con.hasStatement(null, Audit.CONTAINED, null, false));
		assertEquals(0, con.getStatements(null, RDF.TYPE, Audit.RECENT, false).asList().size());
	}

	public void testRemove() throws Exception {
		con.setAutoCommit(false);
		assertTrue(con.isEmpty());
		con.add(carmichael, knows, harris);
		con = reopen(repo, con);
		con.remove(carmichael, knows, harris);
		con.setAutoCommit(true);
		assertFalse(con.hasStatement(carmichael, knows, harris, false));
		assertTrue(con.hasStatement(carmichael, Audit.REVISION, null, false));
		assertEquals(2, con.getContextIDs().asList().size());
		assertTrue(con.hasStatement(null, RDF.TYPE, Audit.TRANSACTION, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, Audit.RECENT, false));
		assertTrue(con.hasStatement(null, RDF.TYPE, Audit.OBSOLETE, false));
		assertTrue(con.hasStatement(null, Audit.COMMITTED_ON, null, false));
		assertTrue(con.hasStatement(null, Audit.MODIFIED, null, false));
		assertTrue(con.hasStatement(null, Audit.PREDECESSOR, null, false));
		assertFalse(con.hasStatement(null, Audit.CONTAINED, null, false));
	}

	public void testRemoveMany() throws Exception {
		con.setAutoCommit(false);
		assertTrue(con.isEmpty());
		con.add(carmichael, knows, harris);
		con.add(harris, knows, jackson);
		con.add(jackson, knows, johnston);
		con.add(johnston, knows, lismer);
		con.add(lismer, knows, macDonald);
		con.add(macDonald, knows, varley);
		con.add(varley, knows, thomson);
		con = reopen(repo, con);
		con.remove((Resource)null, knows, null);
		con.setAutoCommit(true);
		assertFalse(con.hasStatement(carmichael, knows, harris, false));
		assertTrue(con.hasStatement(carmichael, Audit.REVISION, null, false));
		assertFalse(con.hasStatement(null, null, null, false, new Resource[]{null}));
		assertEquals(1, con.getContextIDs().asList().size());
		assertTrue(con.hasStatement(null, RDF.TYPE, Audit.TRANSACTION, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, Audit.RECENT, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, Audit.OBSOLETE, false));
		assertTrue(con.hasStatement(null, Audit.COMMITTED_ON, null, false));
		assertFalse(con.hasStatement(null, Audit.MODIFIED, null, false));
		assertFalse(con.hasStatement(null, Audit.PREDECESSOR, null, false));
		assertFalse(con.hasStatement(null, Audit.CONTAINED, null, false));
		assertEquals(0, con.getStatements(null, RDF.TYPE, Audit.RECENT, false).asList().size());
	}

	public void testRemoveAdd() throws Exception {
		con.setAutoCommit(false);
		assertTrue(con.isEmpty());
		con.add(carmichael, knows, harris);
		con = reopen(repo, con);
		con.remove(carmichael, knows, harris);
		con = reopen(repo, con);
		con.add(carmichael, knows, jackson);
		con.setAutoCommit(true);
		assertFalse(con.hasStatement(carmichael, knows, harris, false));
		assertTrue(con.hasStatement(carmichael, knows, jackson, false));
		assertTrue(con.hasStatement(carmichael, Audit.REVISION, null, false));
		assertEquals(3, con.getContextIDs().asList().size());
		assertTrue(con.hasStatement(null, RDF.TYPE, Audit.TRANSACTION, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, Audit.RECENT, false));
		assertTrue(con.hasStatement(null, RDF.TYPE, Audit.OBSOLETE, false));
		assertTrue(con.hasStatement(null, Audit.COMMITTED_ON, null, false));
		assertTrue(con.hasStatement(null, Audit.MODIFIED, null, false));
		assertTrue(con.hasStatement(null, Audit.PREDECESSOR, null, false));
		assertFalse(con.hasStatement(null, Audit.CONTAINED, null, false));
	}

	public void testReplace() throws Exception {
		con.setAutoCommit(false);
		assertTrue(con.isEmpty());
		con.add(carmichael, knows, harris);
		con = reopen(repo, con);
		con.remove(carmichael, knows, harris);
		con.add(carmichael, knows, jackson);
		con.setAutoCommit(true);
		assertFalse(con.hasStatement(carmichael, knows, harris, false));
		assertTrue(con.hasStatement(carmichael, knows, jackson, false));
		assertTrue(con.hasStatement(carmichael, Audit.REVISION, null, false));
		assertEquals(2, con.getContextIDs().asList().size());
		assertTrue(con.hasStatement(null, RDF.TYPE, Audit.TRANSACTION, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, Audit.RECENT, false));
		assertTrue(con.hasStatement(null, RDF.TYPE, Audit.OBSOLETE, false));
		assertTrue(con.hasStatement(null, Audit.COMMITTED_ON, null, false));
		assertTrue(con.hasStatement(null, Audit.MODIFIED, null, false));
		assertTrue(con.hasStatement(null, Audit.PREDECESSOR, null, false));
		assertFalse(con.hasStatement(null, Audit.CONTAINED, null, false));
	}

	public void testRemoveEach() throws Exception {
		con.setAutoCommit(false);
		assertTrue(con.isEmpty());
		con.add(carmichael, knows, harris);
		con = reopen(repo, con);
		RepositoryResult<Statement> stmts = con.getStatements(carmichael, null, null, false);
		while (stmts.hasNext()) {
			con.remove(stmts.next());
		}
		stmts.close();
		con.setAutoCommit(true);
		assertFalse(con.hasStatement(carmichael, null, null, false));
		assertEquals(1, con.getContextIDs().asList().size());
		assertTrue(con.hasStatement(null, RDF.TYPE, Audit.TRANSACTION, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, Audit.RECENT, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, Audit.OBSOLETE, false));
		assertTrue(con.hasStatement(null, Audit.COMMITTED_ON, null, false));
		assertFalse(con.hasStatement(null, Audit.MODIFIED, null, false));
		assertFalse(con.hasStatement(null, Audit.PREDECESSOR, null, false));
		assertFalse(con.hasStatement(null, Audit.CONTAINED, null, false));
	}

	public void testRemoveRevision() throws Exception {
		con.setAutoCommit(false);
		assertTrue(con.isEmpty());
		con.add(carmichael, knows, harris);
		con = reopen(repo, con);
		con.remove(carmichael, Audit.REVISION, null);
		con.setAutoCommit(true);
		assertTrue(con.hasStatement(carmichael, knows, harris, false));
		assertFalse(con.hasStatement(carmichael, Audit.REVISION, null, false));
		assertEquals(1, con.getContextIDs().asList().size());
		assertTrue(con.hasStatement(null, RDF.TYPE, Audit.TRANSACTION, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, Audit.RECENT, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, Audit.OBSOLETE, false));
		assertTrue(con.hasStatement(null, Audit.COMMITTED_ON, null, false));
		assertFalse(con.hasStatement(null, Audit.MODIFIED, null, false));
		assertFalse(con.hasStatement(null, Audit.PREDECESSOR, null, false));
		assertFalse(con.hasStatement(null, Audit.CONTAINED, null, false));
	}

	public void testRemoveLastRevision() throws Exception {
		con.setAutoCommit(false);
		assertTrue(con.isEmpty());
		con.add(carmichael, knows, harris);
		con = reopen(repo, con);
		RepositoryResult<Statement> stmts = con.getStatements(carmichael, Audit.REVISION, null, false);
		Value revision = stmts.next().getObject();
		stmts.close();
		con.remove(carmichael, Audit.REVISION, revision);
		con.setAutoCommit(true);
		assertTrue(con.hasStatement(carmichael, knows, harris, false));
		assertFalse(con.hasStatement(carmichael, Audit.REVISION, null, false));
		assertEquals(1, con.getContextIDs().asList().size());
		assertTrue(con.hasStatement(null, RDF.TYPE, Audit.TRANSACTION, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, Audit.RECENT, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, Audit.OBSOLETE, false));
		assertTrue(con.hasStatement(null, Audit.COMMITTED_ON, null, false));
		assertFalse(con.hasStatement(null, Audit.MODIFIED, null, false));
		assertFalse(con.hasStatement(null, Audit.PREDECESSOR, null, false));
		assertFalse(con.hasStatement(null, Audit.CONTAINED, null, false));
	}

	public void testTouchRevision() throws Exception {
		con.setAutoCommit(false);
		assertTrue(con.isEmpty());
		con.add(carmichael, knows, harris);
		con = reopen(repo, con);
		con.remove(carmichael, Audit.REVISION, null);
		con.add(carmichael, Audit.REVISION, Audit.CURRENT_TRX);
		con.setAutoCommit(true);
		assertTrue(con.hasStatement(carmichael, knows, harris, false));
		assertTrue(con.hasStatement(carmichael, Audit.REVISION, null, false));
		assertEquals(2, con.getContextIDs().asList().size());
		assertTrue(con.hasStatement(null, RDF.TYPE, Audit.TRANSACTION, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, Audit.RECENT, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, Audit.OBSOLETE, false));
		assertTrue(con.hasStatement(null, Audit.COMMITTED_ON, null, false));
		assertFalse(con.hasStatement(null, Audit.MODIFIED, null, false));
		assertTrue(con.hasStatement(null, Audit.PREDECESSOR, null, false));
		assertFalse(con.hasStatement(null, Audit.CONTAINED, null, false));
	}

	public void testDoubleTouchRevision() throws Exception {
		con.setAutoCommit(false);
		assertTrue(con.isEmpty());
		con.add(carmichael, knows, harris);
		con = reopen(repo, con);
		con.remove(carmichael, Audit.REVISION, null);
		con.add(carmichael, Audit.REVISION, Audit.CURRENT_TRX);
		con = reopen(repo, con);
		con.remove(carmichael, Audit.REVISION, null);
		con.add(carmichael, Audit.REVISION, Audit.CURRENT_TRX);
		con.setAutoCommit(true);
		assertTrue(con.hasStatement(carmichael, knows, harris, false));
		assertTrue(con.hasStatement(carmichael, Audit.REVISION, null, false));
		assertEquals(3, con.getContextIDs().asList().size());
		assertTrue(con.hasStatement(null, RDF.TYPE, Audit.TRANSACTION, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, Audit.RECENT, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, Audit.OBSOLETE, false));
		assertTrue(con.hasStatement(null, Audit.COMMITTED_ON, null, false));
		assertFalse(con.hasStatement(null, Audit.MODIFIED, null, false));
		assertTrue(con.hasStatement(null, Audit.PREDECESSOR, null, false));
		assertFalse(con.hasStatement(null, Audit.CONTAINED, null, false));
	}

	public void testUpgrade() throws Exception {
		con.setAutoCommit(false);
		assertTrue(con.isEmpty());
		con.add(carmichael, knows, harris);
		con = reopen(repo, con);
		con.remove(carmichael, null, null);
		con.add(carmichael, knows, jackson);
		con.setAutoCommit(true);
		assertTrue(con.hasStatement(carmichael, knows, jackson, false));
		assertTrue(con.hasStatement(carmichael, Audit.REVISION, null, false));
		assertEquals(2, con.getContextIDs().asList().size());
		assertTrue(con.hasStatement(null, RDF.TYPE, Audit.TRANSACTION, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, Audit.RECENT, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, Audit.OBSOLETE, false));
		assertTrue(con.hasStatement(null, Audit.COMMITTED_ON, null, false));
		assertFalse(con.hasStatement(null, Audit.MODIFIED, null, false));
		assertTrue(con.hasStatement(null, Audit.PREDECESSOR, null, false));
		assertFalse(con.hasStatement(null, Audit.CONTAINED, null, false));
	}

	public void testClear() throws Exception {
		con.setAutoCommit(false);
		assertTrue(con.isEmpty());
		con.add(carmichael, knows, harris);
		con = reopen(repo, con);
		con.remove(carmichael, null, null);
		con.setAutoCommit(true);
		assertFalse(con.hasStatement(carmichael, null, null, false));
		assertEquals(1, con.getContextIDs().asList().size());
		assertTrue(con.hasStatement(null, RDF.TYPE, Audit.TRANSACTION, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, Audit.RECENT, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, Audit.OBSOLETE, false));
		assertTrue(con.hasStatement(null, Audit.COMMITTED_ON, null, false));
		assertFalse(con.hasStatement(null, Audit.MODIFIED, null, false));
		assertFalse(con.hasStatement(null, Audit.PREDECESSOR, null, false));
		assertFalse(con.hasStatement(null, Audit.CONTAINED, null, false));
	}

	public void testInsertData() throws Exception {
		con.setAutoCommit(false);
		assertTrue(con.isEmpty());
		con.prepareUpdate(QueryLanguage.SPARQL, "INSERT DATA { <carmichael> <http://xmlns.com/foaf/0.1/knows> <harris> } ", "http://example.com/").execute();
		con.setAutoCommit(true);
		assertTrue(con.hasStatement(carmichael, knows, harris, false));
		assertTrue(con.hasStatement(carmichael, Audit.REVISION, null, false));
		assertFalse(con.hasStatement(null, null, null, false, new Resource[]{null}));
		assertEquals(1, con.getContextIDs().asList().size());
		assertTrue(con.hasStatement(null, RDF.TYPE, Audit.TRANSACTION, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, Audit.RECENT, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, Audit.OBSOLETE, false));
		assertTrue(con.hasStatement(null, Audit.COMMITTED_ON, null, false));
		assertFalse(con.hasStatement(null, Audit.MODIFIED, null, false));
		assertFalse(con.hasStatement(null, Audit.PREDECESSOR, null, false));
		assertFalse(con.hasStatement(null, Audit.CONTAINED, null, false));
	}

	public void testDeleteData() throws Exception {
		con.setAutoCommit(false);
		assertTrue(con.isEmpty());
		con.add(carmichael, knows, harris);
		con = reopen(repo, con);
		con.prepareUpdate(QueryLanguage.SPARQL, "DELETE DATA { <carmichael> <http://xmlns.com/foaf/0.1/knows> <harris> } ", "http://example.com/").execute();
		con.setAutoCommit(true);
		assertFalse(con.hasStatement(carmichael, knows, harris, false));
		assertTrue(con.hasStatement(carmichael, Audit.REVISION, null, false));
		assertEquals(2, con.getContextIDs().asList().size());
		assertTrue(con.hasStatement(null, RDF.TYPE, Audit.TRANSACTION, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, Audit.RECENT, false));
		assertTrue(con.hasStatement(null, RDF.TYPE, Audit.OBSOLETE, false));
		assertTrue(con.hasStatement(null, Audit.COMMITTED_ON, null, false));
		assertTrue(con.hasStatement(null, Audit.MODIFIED, null, false));
		assertTrue(con.hasStatement(null, Audit.PREDECESSOR, null, false));
		assertFalse(con.hasStatement(null, Audit.CONTAINED, null, false));
	}

	public void testDelete() throws Exception {
		con.setAutoCommit(false);
		assertTrue(con.isEmpty());
		con.add(carmichael, knows, harris);
		con = reopen(repo, con);
		con.prepareUpdate(QueryLanguage.SPARQL, "DELETE { <carmichael> ?p ?o }\n" +
				"WHERE { <carmichael> ?p ?o } ", "http://example.com/").execute();
		con.setAutoCommit(true);
		assertFalse(con.hasStatement(carmichael, null, null, false));
		assertEquals(1, con.getContextIDs().asList().size());
		assertTrue(con.hasStatement(null, RDF.TYPE, Audit.TRANSACTION, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, Audit.RECENT, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, Audit.OBSOLETE, false));
		assertTrue(con.hasStatement(null, Audit.COMMITTED_ON, null, false));
		assertFalse(con.hasStatement(null, Audit.MODIFIED, null, false));
		assertFalse(con.hasStatement(null, Audit.PREDECESSOR, null, false));
		assertFalse(con.hasStatement(null, Audit.CONTAINED, null, false));
	}

	public void testModify() throws Exception {
		con.setAutoCommit(false);
		assertTrue(con.isEmpty());
		con.add(carmichael, knows, harris);
		con = reopen(repo, con);
		con.prepareUpdate(QueryLanguage.SPARQL, "DELETE { <carmichael> ?p ?o }\n" +
				"INSERT { <carmichael> <http://xmlns.com/foaf/0.1/knows> <jackson> }\n" +
				"WHERE { <carmichael> ?p ?o } ", "http://example.com/").execute();
		con.setAutoCommit(true);
		assertTrue(con.hasStatement(carmichael, knows, jackson, false));
		assertTrue(con.hasStatement(carmichael, Audit.REVISION, null, false));
		assertEquals(2, con.getContextIDs().asList().size());
		assertTrue(con.hasStatement(null, RDF.TYPE, Audit.TRANSACTION, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, Audit.RECENT, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, Audit.OBSOLETE, false));
		assertTrue(con.hasStatement(null, Audit.COMMITTED_ON, null, false));
		assertFalse(con.hasStatement(null, Audit.MODIFIED, null, false));
		assertTrue(con.hasStatement(null, Audit.PREDECESSOR, null, false));
		assertFalse(con.hasStatement(null, Audit.CONTAINED, null, false));
	}
}
