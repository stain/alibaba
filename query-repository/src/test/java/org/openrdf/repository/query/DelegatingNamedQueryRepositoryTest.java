package org.openrdf.repository.query;

import java.util.Iterator;

import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.query.QueryLanguage;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.event.NotifyingRepository;
import org.openrdf.repository.event.base.NotifyingRepositoryWrapper;
import org.openrdf.repository.query.NamedQueryRepository.NamedQuery;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.memory.MemoryStore;

import junit.framework.TestCase;

public class DelegatingNamedQueryRepositoryTest extends TestCase {
	
	private NamedQueryRepository repo, repo1 ;
	private NotifyingRepository repo2 ;
	private String NS = "http://rdf.example.org/";
	private RepositoryConnection a;

	private URI PAINTER;
	private URI PAINTS;
	private URI PICASSO;
	private URI GUERNICA;

	private URI QUERY1, QUERY2;

	@Override
	public void setUp() throws Exception {
		SailRepository sail = new SailRepository(new MemoryStore()) ;
		repo2 = new NotifyingRepositoryWrapper(sail) ;
		repo1 = new NamedQueryRepositoryWrapper(repo2) ;
		repo = new DelegatingNamedQueryRepository(repo1) ;
		repo.initialize();
		ValueFactory vf = repo.getValueFactory();
		PAINTER = vf.createURI(NS, "Painter");
		PAINTS = vf.createURI(NS, "paints");
		PICASSO = vf.createURI(NS, "picasso");
		GUERNICA = vf.createURI(NS, "guernica");
		QUERY1 = vf.createURI(NS, "query1");
		QUERY2 = vf.createURI(NS, "query2");
		a = repo.getConnection();		
	}

	@Override
	public void tearDown() throws Exception {
		a.close();
		repo.shutDown();
	}

	public void test_NamedQueryRepository() throws Exception {
		String rq1 = "SELECT ?painting WHERE { [a <Painter>] <paints> ?painting }";
		NamedQuery nq1 = repo.createNamedQuery(QUERY1, QueryLanguage.SPARQL, rq1, NS);
		assertEquals(nq1.getQueryString(), rq1);
		
		NamedQuery nq2 = repo.createNamedQuery(QUERY2, QueryLanguage.SPARQL, rq1, NS);
		assertEquals(nq2, repo.getNamedQuery(QUERY2)) ;

		Iterator<URI> i = repo.getNamedQueryURIs();
		assertEquals(QUERY1, i.next());
		assertEquals(QUERY2, i.next());
	}
	
	/* In the (non-optimistic) repository any change causes an update */
	
	public void test_addCausesChange() throws Exception {
		String rq1 = "SELECT ?painting WHERE { [a <Painter>] <paints> ?painting }";
		NamedQuery nq1 = repo.createNamedQuery(QUERY1, QueryLanguage.SPARQL, rq1, NS);
		long lastModified = nq1.getResultLastModified() ;
		String eTag = nq1.getResultETag() ;
		Thread.sleep(1000) ;
		
		// Adding just the type has no effect on the query results, but causes an update nevertheless
		a.add(PICASSO, RDF.TYPE , PAINTER);
		
		assertTrue(lastModified < nq1.getResultLastModified());
		assertTrue(!eTag.equals(nq1.getResultETag()));
	}

	public void test_removeCausesChange() throws Exception {
		a.add(PICASSO, RDF.TYPE , PAINTER);
		a.add(PICASSO, PAINTS, GUERNICA);

		String rq1 = "SELECT ?painting WHERE { [a <Painter>] <paints> ?painting }";
		NamedQuery nq1 = repo.createNamedQuery(QUERY1, QueryLanguage.SPARQL, rq1, NS);
		long lastModified = nq1.getResultLastModified() ;
		String eTag = nq1.getResultETag() ;
		Thread.sleep(1000) ;
		
		// Remove triple that has no effect on the results, but causes update
		a.remove(PICASSO, PAINTS, GUERNICA);

		assertTrue(!eTag.equals(nq1.getResultETag()));
		assertTrue(lastModified < nq1.getResultLastModified());
	}

	public void test_addCausesNoChangeUntilCommit() throws Exception {
		a.setAutoCommit(false) ;
		a.add(PICASSO, RDF.TYPE , PAINTER);

		String rq1 = "SELECT ?painting WHERE { [a <Painter>] <paints> ?painting }";
		NamedQuery nq1 = repo.createNamedQuery(QUERY1, QueryLanguage.SPARQL, rq1, NS);
		long lastModified = nq1.getResultLastModified() ;
		String eTag = nq1.getResultETag() ;
		Thread.sleep(1000) ;

		a.add(PICASSO, PAINTS, GUERNICA);

		assertEquals(lastModified, nq1.getResultLastModified());
		assertEquals(eTag,nq1.getResultETag());
		
		a.commit() ;
		
		assertTrue(lastModified < nq1.getResultLastModified());
		assertTrue(!eTag.equals(nq1.getResultETag()));
	}
	
	public void test_rollbackCausesNoChange() throws Exception {
		a.setAutoCommit(false) ;
		a.add(PICASSO, RDF.TYPE , PAINTER);

		String rq1 = "SELECT ?painting WHERE { [a <Painter>] <paints> ?painting }";
		NamedQuery nq1 = repo.createNamedQuery(QUERY1, QueryLanguage.SPARQL, rq1, NS);
		long lastModified = nq1.getResultLastModified() ;
		String eTag = nq1.getResultETag() ;

		a.add(PICASSO, PAINTS, GUERNICA);

		assertEquals(lastModified, nq1.getResultLastModified());
		assertEquals(eTag,nq1.getResultETag());
		
		a.rollback() ;
		
		assertEquals(lastModified, nq1.getResultLastModified());
		assertEquals(eTag,nq1.getResultETag());
	}

}
