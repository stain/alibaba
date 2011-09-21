package org.openrdf.repository.query;

import java.io.File;

import junit.framework.TestCase;

import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.query.QueryLanguage;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.base.RepositoryWrapper;
import org.openrdf.repository.event.NotifyingRepository;
import org.openrdf.repository.event.base.NotifyingRepositoryWrapper;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.memory.MemoryStore;

public class DelegatingNamedQueryRepositoryTest extends TestCase {
	
	private DelegatingNamedQueryRepository repo ;
	private NamedQueryRepository nestedRepo ;
	private String NS = "http://rdf.example.org/";
	private File dataDir ;
	private URI PAINTER;
	private URI PAINTS;
	private URI PICASSO;
	private URI GUERNICA;
	private URI QUERY1, QUERY2;

	@Override
	public void setUp() throws Exception {
		dataDir = new File("/tmp/test/") ;
		deleteDir(dataDir) ;
		dataDir.mkdir() ;
	}
		
	private void init(File dataDir) throws RepositoryException {
		MemoryStore store = dataDir==null ? new MemoryStore() : new MemoryStore(dataDir) ;
		SailRepository sail = new SailRepository(store) ;
		NotifyingRepository notifier = new NotifyingRepositoryWrapper(sail) ;
		NamedQueryRepository named = new NamedQueryRepositoryWrapper(notifier) ;
		repo = new DelegatingNamedQueryRepository(named) ;
		
		ValueFactory vf = repo.getValueFactory();
		PAINTER = vf.createURI(NS, "Painter");
		PAINTS = vf.createURI(NS, "paints");
		PICASSO = vf.createURI(NS, "picasso");
		GUERNICA = vf.createURI(NS, "guernica");
		QUERY1 = vf.createURI(NS, "query1");
		QUERY2 = vf.createURI(NS, "query2");
	}
	
	/* Nested init wraps the named query repository in a dummy wrapper.
	 * The named query repository must be set before initialization.
	 */
	
	private void nestedInit(File dataDir) throws RepositoryException {
		MemoryStore store = dataDir==null ? new MemoryStore() : new MemoryStore(dataDir) ;
		SailRepository sail = new SailRepository(store) ;
		NotifyingRepository notifier = new NotifyingRepositoryWrapper(sail) ;
		nestedRepo = new NamedQueryRepositoryWrapper(notifier) ;
		// nest the named query repository within a dummy repository wrapper
		RepositoryWrapper dummy = new RepositoryWrapper(nestedRepo) ;
		repo = new DelegatingNamedQueryRepository(dummy) ;
		
		ValueFactory vf = repo.getValueFactory();
		PAINTER = vf.createURI(NS, "Painter");
		PAINTS = vf.createURI(NS, "paints");
		PICASSO = vf.createURI(NS, "picasso");
		GUERNICA = vf.createURI(NS, "guernica");
		QUERY1 = vf.createURI(NS, "query1");
		QUERY2 = vf.createURI(NS, "query2");
	}

	public void test_NamedQueryRepository() throws Exception {
		init(null) ;
		repo.initialize();
		
		String rq1 = "SELECT ?painting WHERE { [a <Painter>] <paints> ?painting }";
		NamedQuery nq1 = repo.createNamedQuery(QUERY1, QueryLanguage.SPARQL, rq1, NS);
		assertEquals(nq1.getQueryString(), rq1);
		
		NamedQuery nq2 = repo.createNamedQuery(QUERY2, QueryLanguage.SPARQL, rq1, NS);
		assertEquals(nq2, repo.getNamedQuery(QUERY2)) ;

		assertTrue(repo.getNamedQueryIDs().length==2) ;
		
		repo.shutDown();
	}
	
	/* In the (non-optimistic) repository any change causes an update */
	
	public void test_addCausesChange() throws Exception {
		init(null) ;
		repo.initialize();
		RepositoryConnection a = repo.getConnection() ;

		String rq1 = "SELECT ?painting WHERE { [a <Painter>] <paints> ?painting }";
		NamedQuery nq1 = repo.createNamedQuery(QUERY1, QueryLanguage.SPARQL, rq1, NS);
		long lastModified = nq1.getResultLastModified() ;
		String eTag = nq1.getResultTag() ;
		Thread.sleep(1) ;
		
		// Adding just the type has no effect on the query results, but causes an update nevertheless
		a.add(PICASSO, RDF.TYPE , PAINTER);
		
		assertTrue(lastModified < nq1.getResultLastModified());
		assertTrue(!eTag.equals(nq1.getResultTag()));
		
		a.close() ;
		repo.shutDown();
	}

	public void test_removeCausesChange() throws Exception {
		init(null) ;
		repo.initialize();
		RepositoryConnection a = repo.getConnection() ;

		a.add(PICASSO, RDF.TYPE , PAINTER);
		a.add(PICASSO, PAINTS, GUERNICA);

		String rq1 = "SELECT ?painting WHERE { [a <Painter>] <paints> ?painting }";
		NamedQuery nq1 = repo.createNamedQuery(QUERY1, QueryLanguage.SPARQL, rq1, NS);
		long lastModified = nq1.getResultLastModified() ;
		String eTag = nq1.getResultTag() ;
		Thread.sleep(1) ;
		
		// Remove triple that has no effect on the results, but causes update
		a.remove(PICASSO, PAINTS, GUERNICA);

		assertTrue(!eTag.equals(nq1.getResultTag()));
		assertTrue(lastModified < nq1.getResultLastModified());
		
		a.close();
		repo.shutDown();
	}

	public void test_addCausesNoChangeUntilCommit() throws Exception {
		init(null) ;
		repo.initialize();
		RepositoryConnection a = repo.getConnection() ;

		a.setAutoCommit(false) ;
		a.add(PICASSO, RDF.TYPE , PAINTER);

		String rq1 = "SELECT ?painting WHERE { [a <Painter>] <paints> ?painting }";
		NamedQuery nq1 = repo.createNamedQuery(QUERY1, QueryLanguage.SPARQL, rq1, NS);
		long lastModified = nq1.getResultLastModified() ;
		String eTag = nq1.getResultTag() ;
		Thread.sleep(1) ;

		a.add(PICASSO, PAINTS, GUERNICA);

		assertEquals(lastModified, nq1.getResultLastModified());
		assertEquals(eTag,nq1.getResultTag());
		
		a.commit() ;
		
		assertTrue(lastModified < nq1.getResultLastModified());
		assertTrue(!eTag.equals(nq1.getResultTag()));
		
		a.close();
		repo.shutDown();
	}
	
	public void test_rollbackCausesNoChange() throws Exception {
		init(null) ;
		repo.initialize();
		RepositoryConnection a = repo.getConnection() ;

		a.setAutoCommit(false) ;
		a.add(PICASSO, RDF.TYPE , PAINTER);

		String rq1 = "SELECT ?painting WHERE { [a <Painter>] <paints> ?painting }";
		NamedQuery nq1 = repo.createNamedQuery(QUERY1, QueryLanguage.SPARQL, rq1, NS);
		long lastModified = nq1.getResultLastModified() ;
		String eTag = nq1.getResultTag() ;

		a.add(PICASSO, PAINTS, GUERNICA);

		assertEquals(lastModified, nq1.getResultLastModified());
		assertEquals(eTag,nq1.getResultTag());
		
		a.rollback() ;
		
		assertEquals(lastModified, nq1.getResultLastModified());
		assertEquals(eTag,nq1.getResultTag());
		
		a.close();
		repo.shutDown();
	}
	
	public void test_persistence() throws Exception {
		init(dataDir);
		repo.initialize();
		
		String rq1 = "SELECT ?painting WHERE { [a <Painter>] <paints> ?painting }";
		repo.createNamedQuery(QUERY1, QueryLanguage.SPARQL, rq1, NS);
		
		// shut-down (desist named query) then restart the persistent repository
		repo.shutDown();		
		init(dataDir) ;
		repo.initialize();
		
		NamedQuery nq2 = repo.getNamedQuery(QUERY1) ;
		assertEquals(nq2.getQueryString(), rq1);
	
		repo.shutDown() ;
	}

	public void test_setNestedDelegate() throws Exception {
		// use setter to set the nested named query delegate
		nestedInit(null) ;
		repo.setNamedQueryDelegate(nestedRepo) ;
		repo.initialize() ;
		RepositoryConnection a = repo.getConnection() ;
		
		String rq1 = "SELECT ?painting WHERE { [a <Painter>] <paints> ?painting }";
		NamedQuery nq1 = repo.createNamedQuery(QUERY1, QueryLanguage.SPARQL, rq1, NS);
		long lastModified = nq1.getResultLastModified() ;
		String eTag = nq1.getResultTag() ;
		Thread.sleep(1) ;
		
		// Adding just the type has no effect on the query results, but causes an update nevertheless
		a.add(PICASSO, RDF.TYPE , PAINTER);
		
		assertTrue(lastModified < nq1.getResultLastModified());
		assertTrue(!eTag.equals(nq1.getResultTag()));

		a.close() ;
		repo.shutDown() ;
	}
	
	
	public void test_setNestedDelegatePersistence() throws Exception {
		// use setter to (re)set the nested named query delegate
		nestedInit(dataDir);
		repo.setNamedQueryDelegate(nestedRepo) ;	
		repo.initialize();
		
		String rq1 = "SELECT ?painting WHERE { [a <Painter>] <paints> ?painting }";
		repo.createNamedQuery(QUERY1, QueryLanguage.SPARQL, rq1, NS);
		
		// shut-down (desist named query) then restart the persistent repository
		repo.shutDown();		
		init(dataDir) ;
		repo.initialize();
		
		NamedQuery nq2 = repo.getNamedQuery(QUERY1) ;
		assertEquals(nq2.getQueryString(), rq1);
	
		repo.shutDown() ;
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
