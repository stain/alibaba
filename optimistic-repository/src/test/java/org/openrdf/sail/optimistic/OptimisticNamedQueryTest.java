package org.openrdf.sail.optimistic;

import java.io.File;

import junit.framework.TestCase;

import org.openrdf.model.BNode;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.query.QueryLanguage;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.query.NamedQuery;
import org.openrdf.sail.memory.MemoryStore;

public class OptimisticNamedQueryTest extends TestCase {
	private OptimisticRepository repo;
	private String NS = "http://rdf.example.org/";
	private ValueFactory vf ;

	private URI PAINTER;
	private URI PAINTS, YEAR ;
	private URI PICASSO, REMBRANDT;
	private URI GUERNICA, NIGHTWATCH;

	private URI QUERY1, QUERY2;

	@Override
	public void setUp() throws Exception {
		repo = new OptimisticRepository(new MemoryStore());
		repo.setSnapshot(false);
		repo.setSerializable(false);
		repo.initialize();
		vf = repo.getValueFactory();
		PAINTER = vf.createURI(NS, "Painter");
		PAINTS = vf.createURI(NS, "paints");
		YEAR = vf.createURI(NS, "year");
		PICASSO = vf.createURI(NS, "picasso");
		REMBRANDT = vf.createURI(NS, "rembrandt");
		GUERNICA = vf.createURI(NS, "guernica");
		NIGHTWATCH = vf.createURI(NS, "nightwatch");
		QUERY1 = vf.createURI(NS, "query1");
		QUERY2 = vf.createURI(NS, "query2");
	}

	@Override
	public void tearDown() throws Exception {
		repo.shutDown();
	}

	public void test_addNamedQuery() throws Exception {
		String rq1 = "SELECT ?painting WHERE { [a <Painter>] <paints> ?painting }";
		NamedQuery nq1 = repo.createNamedQuery(QUERY1, QueryLanguage.SPARQL, rq1, NS);
		assertEquals(nq1.getQueryString(), rq1);
		
		NamedQuery nq2 = repo.createNamedQuery(QUERY2, QueryLanguage.SPARQL, rq1, NS);
		assertEquals(nq2, repo.getNamedQuery(QUERY2)) ;

		assertTrue(repo.getNamedQueryIDs().length==2);

	}	
	
	public void test_removeNamedQuery() throws Exception {
		String rq1 = "SELECT ?painting WHERE { [a <Painter>] <paints> ?painting }";
		repo.createNamedQuery(QUERY1, QueryLanguage.SPARQL, rq1, NS);
		repo.removeNamedQuery(QUERY1) ;

		assertTrue(repo.getNamedQueryIDs().length==0);
	}
	
	public void test_removeUnknownNamedQuery() throws Exception {
		String rq1 = "SELECT ?painting WHERE { [a <Painter>] <paints> ?painting }";
		repo.createNamedQuery(QUERY1, QueryLanguage.SPARQL, rq1, NS);
		
		// QUERY2 is undefined
		repo.removeNamedQuery(QUERY2) ;

		assertTrue(repo.getNamedQueryIDs().length==1);
	}
	
	public void test_addCausesChange() throws Exception {
		RepositoryConnection a = repo.getConnection();
		try {
			a.add(PICASSO, RDF.TYPE , PAINTER);
	
			String rq1 = "SELECT ?painting WHERE { [a <Painter>] <paints> ?painting }";
			NamedQuery nq1 = repo.createNamedQuery(QUERY1, QueryLanguage.SPARQL, rq1, NS);
			long lastModified = nq1.getResultLastModified() ;
			String eTag = nq1.getResultTag() ;
			Thread.sleep(1) ;
			
			// Add a new result
			a.add(PICASSO, PAINTS, GUERNICA);
	
			assertTrue(lastModified < nq1.getResultLastModified());
			assertTrue(!eTag.equals(nq1.getResultTag()));
		}
		finally { a.close(); }
	}
	
	public void test_addCausesNoChange() throws Exception {
		RepositoryConnection a = repo.getConnection();
		// if an assertion fails the connection must be closed
		try {
			String rq1 = "SELECT ?painting WHERE { [a <Painter>] <paints> ?painting }";
			NamedQuery nq1 = repo.createNamedQuery(QUERY1, QueryLanguage.SPARQL, rq1, NS);
			long lastModified = nq1.getResultLastModified() ;
			String eTag = nq1.getResultTag() ;
			
			// Adding just the type has no effect on the query results
			a.add(PICASSO, RDF.TYPE , PAINTER);
			
			assertEquals(lastModified, nq1.getResultLastModified());
			assertEquals(eTag,nq1.getResultTag());
		}
		finally { a.close(); }
	}

	public void test_addCausesChangeAfterCommit() throws Exception {
		RepositoryConnection a = repo.getConnection();
		try {
			a.add(PICASSO, RDF.TYPE , PAINTER);
	
			String rq1 = "SELECT ?painting WHERE { [a <Painter>] <paints> ?painting }";
			NamedQuery nq1 = repo.createNamedQuery(QUERY1, QueryLanguage.SPARQL, rq1, NS);
			long lastModified = nq1.getResultLastModified() ;
			String eTag = nq1.getResultTag() ;
			Thread.sleep(1) ;
			
			a.setAutoCommit(false) ;
			
			// Add a new result
			a.add(PICASSO, PAINTS, GUERNICA);
			
			assertEquals(lastModified, nq1.getResultLastModified());
			assertEquals(eTag,nq1.getResultTag());
			
			a.commit();
	
			assertTrue(lastModified < nq1.getResultLastModified());
			assertTrue(!eTag.equals(nq1.getResultTag()));
		}
		finally { a.close(); }
	}

	public void test_addCausesNoChangeAfterRollback() throws Exception {
		RepositoryConnection a = repo.getConnection();
		try {
			a.add(PICASSO, RDF.TYPE , PAINTER);
	
			String rq1 = "SELECT ?painting WHERE { [a <Painter>] <paints> ?painting }";
			NamedQuery nq1 = repo.createNamedQuery(QUERY1, QueryLanguage.SPARQL, rq1, NS);
			long lastModified = nq1.getResultLastModified() ;
			String eTag = nq1.getResultTag() ;
			Thread.sleep(1) ;
			
			a.setAutoCommit(false) ;
			
			// Add a new result
			a.add(PICASSO, PAINTS, GUERNICA);
			
			assertEquals(lastModified, nq1.getResultLastModified());
			assertEquals(eTag,nq1.getResultTag());
			
			a.rollback() ;
			a.commit();
	
			assertEquals(lastModified, nq1.getResultLastModified());
			assertEquals(eTag,nq1.getResultTag());
		}
		finally { a.close(); }
	}

	public void test_removeCausesChange() throws Exception {
		RepositoryConnection a = repo.getConnection();
		try {
			a.add(PICASSO, RDF.TYPE , PAINTER);
			a.add(PICASSO, PAINTS, GUERNICA);
	
			String rq1 = "SELECT ?painting WHERE { [a <Painter>] <paints> ?painting }";
			NamedQuery nq1 = repo.createNamedQuery(QUERY1, QueryLanguage.SPARQL, rq1, NS);
			long lastModified = nq1.getResultLastModified() ;
			String eTag = nq1.getResultTag() ;
			Thread.sleep(1) ;
			
			// Remove an existing result
			a.remove(PICASSO, PAINTS, GUERNICA);
	
			assertTrue(lastModified < nq1.getResultLastModified());
			assertTrue(!eTag.equals(nq1.getResultTag()));
		}
		finally { a.close(); }
	}
	
	public void test_removeCausesNoChange() throws Exception {
		RepositoryConnection a = repo.getConnection();
		try {
			a.add(PICASSO, RDF.TYPE , PAINTER);
	
			String rq1 = "SELECT ?painting WHERE { [a <Painter>] <paints> ?painting }";
			NamedQuery nq1 = repo.createNamedQuery(QUERY1, QueryLanguage.SPARQL, rq1, NS);
			long lastModified = nq1.getResultLastModified() ;
			String eTag = nq1.getResultTag() ;
			
			// Removing the type has no effect on the query results
			a.remove(PICASSO, RDF.TYPE , PAINTER);
			
			assertEquals(lastModified, nq1.getResultLastModified());
			assertEquals(eTag,nq1.getResultTag());
		}
		finally { a.close(); }
	}

	public void test_removeTypeCausesChange() throws Exception {
		RepositoryConnection a = repo.getConnection();
		try {
			a.add(PICASSO, RDF.TYPE , PAINTER);
			a.add(PICASSO, PAINTS, GUERNICA);
	
			String rq1 = "SELECT ?painting WHERE { [a <Painter>] <paints> ?painting }";
			NamedQuery nq1 = repo.createNamedQuery(QUERY1, QueryLanguage.SPARQL, rq1, NS);
			long lastModified = nq1.getResultLastModified() ;
			String eTag = nq1.getResultTag() ;
			Thread.sleep(1) ;
			
			// Removing the type effects the query results
			a.remove(PICASSO, RDF.TYPE , PAINTER);
			
			assertTrue(lastModified < nq1.getResultLastModified());
			assertTrue(!eTag.equals(nq1.getResultTag()));
		}
		finally { a.close(); }
	}

	/* SPARQL OPTIONAL */
	
	
	public void test_addCausesChangeWithOptional() throws Exception {
		RepositoryConnection a = repo.getConnection();
		try {			
			String rq1 = "SELECT ?painting "
				+ "WHERE { ?painter a <Painter> "
				+ "OPTIONAL { ?painter <paints> ?painting } }" ;
			
			NamedQuery nq1 = repo.createNamedQuery(QUERY1, QueryLanguage.SPARQL, rq1, NS);
			long lastModified = nq1.getResultLastModified() ;
			String eTag = nq1.getResultTag() ;
			Thread.sleep(1) ;
			
			// Add a new result
			a.add(PICASSO, RDF.TYPE , PAINTER);
			
			assertTrue(lastModified < nq1.getResultLastModified());
			assertTrue(!eTag.equals(nq1.getResultTag()));
		}
		finally { a.close(); }
	}

	
	public void test_addCausesNoChangeWithOptional() throws Exception {
		RepositoryConnection a = repo.getConnection();
		try {
			a.add(PICASSO, RDF.TYPE , PAINTER);

			String rq1 = "SELECT ?painting "
				+ "WHERE { ?painter a <Painter> "
				+ "OPTIONAL { ?painter <paints> ?painting } }" ;
			
			NamedQuery nq1 = repo.createNamedQuery(QUERY1, QueryLanguage.SPARQL, rq1, NS);
			long lastModified = nq1.getResultLastModified() ;
			String eTag = nq1.getResultTag() ;
			Thread.sleep(1) ;
			
			// Untyped painter is not a result
			a.add(REMBRANDT, PAINTS, NIGHTWATCH);
			
			assertEquals(lastModified, nq1.getResultLastModified());
			assertEquals(eTag,nq1.getResultTag());
		}
		finally { a.close(); }
	}
	
	public void test_removeCausesChangeWithOptional() throws Exception {
		RepositoryConnection a = repo.getConnection();
		try {			
			// Add a new result
			a.add(PICASSO, RDF.TYPE , PAINTER);
			a.add(PICASSO, PAINTS, GUERNICA);
			
			String rq1 = "SELECT ?painting "
				+ "WHERE { ?painter a <Painter> "
				+ "OPTIONAL { ?painter <paints> ?painting } }" ;
			
			NamedQuery nq1 = repo.createNamedQuery(QUERY1, QueryLanguage.SPARQL, rq1, NS);
			long lastModified = nq1.getResultLastModified() ;
			String eTag = nq1.getResultTag() ;
			Thread.sleep(1) ;
			
			// removing the optional causes a change, but no fewer results
			a.remove(PICASSO, PAINTS, GUERNICA);
			
			assertTrue(lastModified < nq1.getResultLastModified());
			assertTrue(!eTag.equals(nq1.getResultTag()));
		}
		finally { a.close(); }
	}
	
	public void test_removeCausesNoChangeWithOptional() throws Exception {
		RepositoryConnection a = repo.getConnection();
		try {			
			// Add a new result
			a.add(REMBRANDT, PAINTS, NIGHTWATCH);
			
			// Add a filtered result
			a.add(PICASSO, RDF.TYPE , PAINTER);
			a.add(PICASSO, PAINTS, GUERNICA);
			
			String rq1 = "SELECT ?painting "
				+ "WHERE { ?painter a <Painter> "
				+ "OPTIONAL { ?painter <paints> ?painting } }" ;
			
			NamedQuery nq1 = repo.createNamedQuery(QUERY1, QueryLanguage.SPARQL, rq1, NS);
			long lastModified = nq1.getResultLastModified() ;
			String eTag = nq1.getResultTag() ;
			Thread.sleep(1) ;
			
			// remove non-result
			a.remove(REMBRANDT, PAINTS, NIGHTWATCH);
			
			assertEquals(lastModified, nq1.getResultLastModified());
			assertEquals(eTag,nq1.getResultTag());
		}
		finally { a.close(); }
	}

	
	/* SPARQL FILTER */
	
	public void test_addCausesChangeWithFilter() throws Exception {
		RepositoryConnection a = repo.getConnection();
		try {			
			String rq1 = "SELECT ?painting "
			+ "WHERE { ?painter a <Painter>; <paints> ?painting "
			+ "FILTER  regex(str(?painter), \"rem\", \"i\") }" ;
			
			NamedQuery nq1 = repo.createNamedQuery(QUERY1, QueryLanguage.SPARQL, rq1, NS);
			long lastModified = nq1.getResultLastModified() ;
			String eTag = nq1.getResultTag() ;
			Thread.sleep(1) ;
			
			// Add a new result
			a.add(REMBRANDT, RDF.TYPE, PAINTER);
			a.add(REMBRANDT, PAINTS, NIGHTWATCH);
			
			assertTrue(lastModified < nq1.getResultLastModified());
			assertTrue(!eTag.equals(nq1.getResultTag()));
		}
		finally { a.close(); }
	}

	
	public void test_addCausesNoChangeWithFilter() throws Exception {
		RepositoryConnection a = repo.getConnection();
		try {			
			String rq1 = "SELECT ?painting "
			+ "WHERE { ?painter a <Painter>; <paints> ?painting "
			+ "FILTER  regex(str(?painter), \"rem\", \"i\") }" ;
			
			NamedQuery nq1 = repo.createNamedQuery(QUERY1, QueryLanguage.SPARQL, rq1, NS);
			long lastModified = nq1.getResultLastModified() ;
			String eTag = nq1.getResultTag() ;
			Thread.sleep(1) ;
			
			// Add a filtered result
			a.add(PICASSO, RDF.TYPE , PAINTER);
			a.add(PICASSO, PAINTS, GUERNICA);
			
			assertEquals(lastModified, nq1.getResultLastModified());
			assertEquals(eTag,nq1.getResultTag());
		}
		finally { a.close(); }
	}
	
	public void test_removeCausesChangeWithFilter() throws Exception {
		RepositoryConnection a = repo.getConnection();
		try {			
			// Add a new result
			a.add(REMBRANDT, RDF.TYPE, PAINTER);
			a.add(REMBRANDT, PAINTS, NIGHTWATCH);
			
			// Add a filtered result
			a.add(PICASSO, RDF.TYPE , PAINTER);
			a.add(PICASSO, PAINTS, GUERNICA);
			
			String rq1 = "SELECT ?painting "
			+ "WHERE { ?painter a <Painter>; <paints> ?painting "
			+ "FILTER  regex(str(?painter), \"rem\", \"i\") }" ;
			
			NamedQuery nq1 = repo.createNamedQuery(QUERY1, QueryLanguage.SPARQL, rq1, NS);
			long lastModified = nq1.getResultLastModified() ;
			String eTag = nq1.getResultTag() ;
			Thread.sleep(1) ;
			
			// remove a result
			a.remove(REMBRANDT, RDF.TYPE, PAINTER);
			a.remove(REMBRANDT, PAINTS, NIGHTWATCH);
			
			assertTrue(lastModified < nq1.getResultLastModified());
			assertTrue(!eTag.equals(nq1.getResultTag()));
		}
		finally { a.close(); }
	}
	
	public void test_removeCausesNoChangeWithFilter() throws Exception {
		RepositoryConnection a = repo.getConnection();
		try {			
			// Add a new result
			a.add(REMBRANDT, RDF.TYPE, PAINTER);
			a.add(REMBRANDT, PAINTS, NIGHTWATCH);
			
			// Add a filtered result
			a.add(PICASSO, RDF.TYPE , PAINTER);
			a.add(PICASSO, PAINTS, GUERNICA);
			
			String rq1 = "SELECT ?painting "
			+ "WHERE { ?painter a <Painter>; <paints> ?painting "
			+ "FILTER  regex(str(?painter), \"rem\", \"i\") }" ;
			
			NamedQuery nq1 = repo.createNamedQuery(QUERY1, QueryLanguage.SPARQL, rq1, NS);
			long lastModified = nq1.getResultLastModified() ;
			String eTag = nq1.getResultTag() ;
			Thread.sleep(1) ;
			
			// remove non-result
			a.remove(PICASSO, RDF.TYPE , PAINTER);
			a.remove(PICASSO, PAINTS, GUERNICA);
			
			assertEquals(lastModified, nq1.getResultLastModified());
			assertEquals(eTag,nq1.getResultTag());
		}
		finally { a.close(); }
	}
	
	/* SPARQL UNION */
	
	public void test_addCausesChangeWithUnion() throws Exception {
		RepositoryConnection a = repo.getConnection();
		try {			
			String rq1 = "SELECT ?painting "
				+ "WHERE { { ?painter a <Painter> }"
				+ "UNION { ?painter <paints> ?painting } }" ;
			
			NamedQuery nq1 = repo.createNamedQuery(QUERY1, QueryLanguage.SPARQL, rq1, NS);
			long lastModified = nq1.getResultLastModified() ;
			String eTag = nq1.getResultTag() ;
			Thread.sleep(1) ;
			
			// Add a new result
			a.add(PICASSO, RDF.TYPE , PAINTER);
			
			assertTrue(lastModified < nq1.getResultLastModified());
			assertTrue(!eTag.equals(nq1.getResultTag()));
		}
		finally { a.close(); }
	}
	
	public void test_addCausesNoChangeWithUnion() throws Exception {
		RepositoryConnection a = repo.getConnection();
		try {
			a.add(REMBRANDT, RDF.TYPE, PAINTER);
			a.add(REMBRANDT, PAINTS, NIGHTWATCH);
			
			String rq1 = "SELECT ?painting "
				+ "WHERE { { ?painter a <Painter> }"
				+ "UNION { ?painter <paints> ?painting } }" ;
			
			NamedQuery nq1 = repo.createNamedQuery(QUERY1, QueryLanguage.SPARQL, rq1, NS);
			long lastModified = nq1.getResultLastModified() ;
			String eTag = nq1.getResultTag() ;
			Thread.sleep(1) ;
			
			// data is not used in query
			a.add(NIGHTWATCH, YEAR, vf.createLiteral(1642));
			
			assertEquals(lastModified, nq1.getResultLastModified());
			assertEquals(eTag,nq1.getResultTag());
		}
		finally { a.close(); }
	}
	
	
	public void test_removeCausesChangeWithUnion() throws Exception {
		RepositoryConnection a = repo.getConnection();
		try {			
			// Add a new result
			a.add(REMBRANDT, RDF.TYPE, PAINTER);
			a.add(REMBRANDT, PAINTS, NIGHTWATCH);
			
			// Add a filtered result
			a.add(PICASSO, RDF.TYPE , PAINTER);
			a.add(PICASSO, PAINTS, GUERNICA);
			
			String rq1 = "SELECT ?painting "
				+ "WHERE { { ?painter a <Painter> }"
				+ "UNION { ?painter <paints> ?painting } }" ;
			
			NamedQuery nq1 = repo.createNamedQuery(QUERY1, QueryLanguage.SPARQL, rq1, NS);
			long lastModified = nq1.getResultLastModified() ;
			String eTag = nq1.getResultTag() ;
			Thread.sleep(1) ;
			
			// remove a result
			a.remove(REMBRANDT, PAINTS, NIGHTWATCH);
			
			assertTrue(lastModified < nq1.getResultLastModified());
			assertTrue(!eTag.equals(nq1.getResultTag()));
		}
		finally { a.close(); }
	}
	
	public void test_removeCausesNoChangeWithUnion() throws Exception {
		RepositoryConnection a = repo.getConnection();
		try {			
			// Add a new result
			a.add(REMBRANDT, RDF.TYPE, PAINTER);
			a.add(REMBRANDT, PAINTS, NIGHTWATCH);
			a.add(NIGHTWATCH, YEAR, vf.createLiteral(1642));
			
			// Add a filtered result
			a.add(PICASSO, RDF.TYPE , PAINTER);
			a.add(PICASSO, PAINTS, GUERNICA);
			
			String rq1 = "SELECT ?painting "
				+ "WHERE { { ?painter a <Painter> }"
				+ "UNION { ?painter <paints> ?painting } }" ;
			
			NamedQuery nq1 = repo.createNamedQuery(QUERY1, QueryLanguage.SPARQL, rq1, NS);
			long lastModified = nq1.getResultLastModified() ;
			String eTag = nq1.getResultTag() ;
			Thread.sleep(1) ;
			
			// remove non-result
			a.remove(NIGHTWATCH, YEAR, vf.createLiteral(1642));
			
			assertEquals(lastModified, nq1.getResultLastModified());
			assertEquals(eTag,nq1.getResultTag());
		}
		finally { a.close(); }
	}

	/* rdf type */
	
	public void test_persistence() throws Exception {
		File dataDir = new File("/tmp/test/") ;
		deleteDir(dataDir) ;
		dataDir.mkdir() ;
		OptimisticRepository persistent = new OptimisticRepository(new MemoryStore(dataDir));
		persistent.initialize() ;

		String rq1 = "SELECT ?painting WHERE { [a <Painter>] <paints> ?painting }";
		persistent.createNamedQuery(QUERY1, QueryLanguage.SPARQL, rq1, NS);
		assertEquals(persistent.getNamedQuery(QUERY1).getQueryString(), rq1);
		
		// shut-down and restart the persistent repository
		persistent.shutDown() ;
		persistent = new OptimisticRepository(new MemoryStore(dataDir));
		persistent.initialize() ;
		
		NamedQuery nq2 = persistent.getNamedQuery(QUERY1) ;
		assertEquals(nq2.getQueryString(), rq1);
	}
	
	public void test_activePostPersistence() throws Exception {
		RepositoryConnection a = null ;
		try {	
			File dataDir = new File("/tmp/test/") ;
			deleteDir(dataDir) ;
			dataDir.mkdir() ;
			OptimisticRepository persistent = new OptimisticRepository(new MemoryStore(dataDir));
			persistent.initialize() ;
	
			String rq1 = "SELECT ?painting WHERE { [a <Painter>] <paints> ?painting }";
			persistent.createNamedQuery(QUERY1, QueryLanguage.SPARQL, rq1, NS);
			
			// shut-down and restart the persistent repository
			persistent.shutDown() ;
			persistent = new OptimisticRepository(new MemoryStore(dataDir));
			persistent.initialize() ;
			
			NamedQuery nq1 = persistent.getNamedQuery(QUERY1);
			String eTag = nq1.getResultTag() ;
			
			a = persistent.getConnection() ;
			a.add(PICASSO, RDF.TYPE , PAINTER);
			a.add(PICASSO, PAINTS, GUERNICA);
			
			assertTrue(!eTag.equals(nq1.getResultTag()));
		}
		finally { if (a!=null) a.close(); }
	}
	
	public void test_nonPersistence() throws Exception {
		File dataDir = new File("/tmp/test/") ;
		deleteDir(dataDir) ;
		dataDir.mkdir() ;
		OptimisticRepository persistent = new OptimisticRepository(new MemoryStore(dataDir));
		persistent.initialize() ;

		String rq1 = "SELECT ?painting WHERE { [a <Painter>] <paints> ?painting }";
		persistent.createNamedQuery(QUERY1, QueryLanguage.SPARQL, rq1, NS);
		assertEquals(persistent.getNamedQuery(QUERY1).getQueryString(), rq1);
		
		// shut-down (desist named query) then restart the persistent repository
		persistent.shutDown() ;
		persistent = new OptimisticRepository(new MemoryStore(dataDir));
		persistent.initialize() ;
		
		NamedQuery nq2 = persistent.getNamedQuery(QUERY1) ;
		assertEquals(nq2.getQueryString(), rq1);
		
		// cease persisting QUERY1
		persistent.removeNamedQuery(QUERY1) ;
		
		persistent.shutDown() ;
		persistent = new OptimisticRepository(new MemoryStore(dataDir));
		persistent.initialize() ;
		// the removed named query should not be persisted
		
		assertTrue(persistent.getNamedQueryIDs().length==0) ;
	}


	public void test_addChangesNone() throws Exception {
		RepositoryConnection a = repo.getConnection();
		try {
			String rq1 = "SELECT ?painting WHERE { [a <Painter>] <paints> ?painting }";
			NamedQuery nq1 = repo.createNamedQuery(QUERY1, QueryLanguage.SPARQL, rq1, NS);
			String et1 = nq1.getResultTag() ;
			
			String rq2 = "SELECT ?painting "
				+ "WHERE { ?painter a <Painter> "
				+ "OPTIONAL { ?painter <paints> ?painting } }" ;
			NamedQuery nq2 = repo.createNamedQuery(QUERY2, QueryLanguage.SPARQL, rq2, NS);
			String et2 = nq2.getResultTag() ;
			
			// this matches only the optional clause of query2
			a.add(PICASSO, PAINTS, GUERNICA);
			
			assertEquals(et1,nq1.getResultTag());
			assertEquals(et2,nq2.getResultTag());
		}
		finally { a.close(); }
	}
	
	public void test_addChangesSome() throws Exception {
		RepositoryConnection a = repo.getConnection();
		try {
			String rq1 = "SELECT ?painting WHERE { [a <Painter>] <paints> ?painting }";
			NamedQuery nq1 = repo.createNamedQuery(QUERY1, QueryLanguage.SPARQL, rq1, NS);
			String et1 = nq1.getResultTag() ;
			
			String rq2 = "SELECT ?painting "
				+ "WHERE { ?painter a <Painter> "
				+ "OPTIONAL { ?painter <paints> ?painting } }" ;
			NamedQuery nq2 = repo.createNamedQuery(QUERY2, QueryLanguage.SPARQL, rq2, NS);
			String et2 = nq2.getResultTag() ;
			
			// Adding just the type has no effect on query1, but adds a new result to query2
			a.add(PICASSO, RDF.TYPE , PAINTER);
			
			assertEquals(et1,nq1.getResultTag());
			assertFalse(et2.equals(nq2.getResultTag()));
		}
		finally { a.close(); }
	}
	
	
	public void test_addChangesAll() throws Exception {
		RepositoryConnection a = repo.getConnection();
		try {
			String rq1 = "SELECT ?painting WHERE { [a <Painter>] <paints> ?painting }";
			NamedQuery nq1 = repo.createNamedQuery(QUERY1, QueryLanguage.SPARQL, rq1, NS);
			String et1 = nq1.getResultTag() ;
			
			String rq2 = "SELECT ?painting "
				+ "WHERE { ?painter a <Painter> "
				+ "OPTIONAL { ?painter <paints> ?painting } }" ;
			NamedQuery nq2 = repo.createNamedQuery(QUERY2, QueryLanguage.SPARQL, rq2, NS);
			String et2 = nq2.getResultTag() ;
			
			// Adding just the type adds a new result to query2
			a.add(PICASSO, RDF.TYPE , PAINTER);
			// this, combined with the above, adds a solution to query1
			a.add(PICASSO, PAINTS, GUERNICA);
			
			assertFalse(et1.equals(nq1.getResultTag()));
			assertFalse(et2.equals(nq2.getResultTag()));
		}
		finally { a.close(); }
	}

	public void test_addExclusive() throws Exception {
		RepositoryConnection a = repo.getConnection();
		try {
			String rq1 = "SELECT ?painting WHERE { [a <Painter>] <paints> ?painting }";
			NamedQuery nq1 = repo.createNamedQuery(QUERY1, QueryLanguage.SPARQL, rq1, NS);
			String eTag = nq1.getResultTag() ;

			a.setAutoCommit(false);
			addSeq(a);
			a.setAutoCommit(true);

			// in exclusive mode any change causes an update
			assertTrue(!eTag.equals(nq1.getResultTag()));
		}
		finally { a.close(); }
	}
	
	public void test_removeExclusive() throws Exception {
		RepositoryConnection a = repo.getConnection();
		try {
			String rq1 = "SELECT ?painting WHERE { [a <Painter>] <paints> ?painting }";
			NamedQuery nq1 = repo.createNamedQuery(QUERY1, QueryLanguage.SPARQL, rq1, NS);

			a.add(PICASSO, RDF.TYPE, PAINTER);
			a.setAutoCommit(false);
			Resource seq = addSeq(a);
			a.setAutoCommit(true);

			String eTag = nq1.getResultTag() ;

			a.setAutoCommit(false);
			removeSeq(seq, a);
			a.setAutoCommit(true);
			
			assertTrue(!eTag.equals(nq1.getResultTag()));
		}
		finally { a.close(); }
	}

	private Resource addSeq(RepositoryConnection con) throws RepositoryException {
		ValueFactory vf = con.getValueFactory();
		BNode seq = vf.createBNode();
		con.add(seq, RDF.TYPE, RDF.SEQ);
		for (int i=0; i<10000; i++) {
			URI pred = vf.createURI(RDF.NAMESPACE + "_" + (i + 1));
			con.add(seq, pred, vf.createLiteral(i));
		}
		return seq;
	}

	private void removeSeq(Resource seq, RepositoryConnection con) throws RepositoryException {
		ValueFactory vf = con.getValueFactory();
		con.remove(seq, RDF.TYPE, RDF.SEQ);
		for (int i=0; i<10000; i++) {
			URI pred = vf.createURI(RDF.NAMESPACE + "_" + (i + 1));
			con.remove(seq, pred, vf.createLiteral(i));
		}
	}

	private static void deleteDir(File dir) {
		if (dir.isDirectory()) {
			File[] files = dir.listFiles() ;
			for (int i=0; i<files.length; i++) {
				if (files[i].isDirectory()) deleteDir(files[i]) ;
				files[i].delete() ;
			}
			dir.delete() ;
		}
	}

}
