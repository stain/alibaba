package org.openrdf.sail.optimistic;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;

import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.query.QueryLanguage;
import org.openrdf.repository.RepositoryConnection;
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
	
	public void test_addCausesBindChange() throws Exception {
		RepositoryConnection a = repo.getConnection();
		try {
			a.add(PICASSO, RDF.TYPE , PAINTER);
	
			String rq1 = "SELECT ?painting WHERE { ?painter a <Painter> BIND (?painter as ?artist) ?artist <paints> ?painting }";
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
	
	public void test_addCausesBindNoChange() throws Exception {
		RepositoryConnection a = repo.getConnection();
		// if an assertion fails the connection must be closed
		try {
			String rq1 = "SELECT ?painting WHERE { ?painter a <Painter> BIND (?painter as ?artist) ?artist <paints> ?painting }";
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

	/* rdf type */
	
	public void test_persistence() throws Exception {
		File dataDir = createTempDir();
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
		deleteDir(dataDir);
	}
	
	public void test_activePostPersistence() throws Exception {
		RepositoryConnection a = null ;
		try {	
			File dataDir = createTempDir();
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
		File dataDir = createTempDir();
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
		deleteDir(dataDir);
	}

	private File createTempDir() throws IOException {
		String tmpDirStr = System.getProperty("java.io.tmpdir");
		if (tmpDirStr != null) {
			File tmpDir = new File(tmpDirStr);
			if (!tmpDir.exists()) {
				tmpDir.mkdirs();
			}
		}
		File dataDir = File.createTempFile(getClass().getSimpleName(), "");
		deleteDir(dataDir) ;
		dataDir.mkdir() ;
		return dataDir;
	}
	
	private static void deleteDir(File dir) {
		if (dir.isDirectory()) {
			File[] files = dir.listFiles() ;
			for (int i=0; i<files.length; i++) {
				deleteDir(files[i]) ;
			}
		}
		dir.delete() ;
	}

}
