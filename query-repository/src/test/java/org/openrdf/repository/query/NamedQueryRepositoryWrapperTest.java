package org.openrdf.repository.query;

import junit.framework.TestCase;

import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.parser.ParsedBooleanQuery;
import org.openrdf.query.parser.ParsedGraphQuery;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.event.base.NotifyingRepositoryWrapper;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.memory.MemoryStore;

public class NamedQueryRepositoryWrapperTest extends TestCase {
	private NamedQueryRepositoryWrapper repo;
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
		repo = new NamedQueryRepositoryWrapper(new NotifyingRepositoryWrapper(sail));
		//repo.setSnapshot(false);
		//repo.setSerializable(false);
		repo.initialize();
		ValueFactory uf = repo.getValueFactory();
		PAINTER = uf.createURI(NS, "Painter");
		PAINTS = uf.createURI(NS, "paints");
		PICASSO = uf.createURI(NS, "picasso");
		GUERNICA = uf.createURI(NS, "guernica");
		QUERY1 = uf.createURI(NS, "query1");
		QUERY2 = uf.createURI(NS, "query2");
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

		assertTrue(repo.getNamedQueryIDs().length==2) ;
	}
	
	/* In the (non-optimistic) repository any change causes an update */
	
	public void test_addCausesChange() throws Exception {
		String rq1 = "SELECT ?painting WHERE { [a <Painter>] <paints> ?painting }";
		NamedQuery nq1 = repo.createNamedQuery(QUERY1, QueryLanguage.SPARQL, rq1, NS);
		long lastModified = nq1.getResultLastModified() ;
		String eTag = nq1.getResultTag() ;
		Thread.sleep(1000) ;
		
		// Adding just the type has no effect on the query results, but causes an update nevertheless
		a.add(PICASSO, RDF.TYPE , PAINTER);
		
		assertTrue(lastModified < nq1.getResultLastModified());
		assertTrue(!eTag.equals(nq1.getResultTag()));
	}

	public void test_removeCausesChange() throws Exception {
		a.add(PICASSO, RDF.TYPE , PAINTER);
		a.add(PICASSO, PAINTS, GUERNICA);

		String rq1 = "SELECT ?painting WHERE { [a <Painter>] <paints> ?painting }";
		NamedQuery nq1 = repo.createNamedQuery(QUERY1, QueryLanguage.SPARQL, rq1, NS);
		long lastModified = nq1.getResultLastModified() ;
		String eTag = nq1.getResultTag() ;
		Thread.sleep(1000) ;
		
		// Remove triple that has no effect on the results, but causes update
		a.remove(PICASSO, PAINTS, GUERNICA);

		assertTrue(!eTag.equals(nq1.getResultTag()));
		assertTrue(lastModified < nq1.getResultLastModified());
	}

	public void test_addCausesNoChangeUntilCommit() throws Exception {
		a.setAutoCommit(false) ;
		a.add(PICASSO, RDF.TYPE , PAINTER);

		String rq1 = "SELECT ?painting WHERE { [a <Painter>] <paints> ?painting }";
		NamedQuery nq1 = repo.createNamedQuery(QUERY1, QueryLanguage.SPARQL, rq1, NS);
		long lastModified = nq1.getResultLastModified() ;
		String eTag = nq1.getResultTag() ;
		Thread.sleep(1000) ;

		a.add(PICASSO, PAINTS, GUERNICA);

		assertEquals(lastModified, nq1.getResultLastModified());
		assertEquals(eTag,nq1.getResultTag());
		
		a.commit() ;
		
		assertTrue(lastModified < nq1.getResultLastModified());
		assertTrue(!eTag.equals(nq1.getResultTag()));
	}
	
	public void test_rollbackCausesNoChange() throws Exception {
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
	}
	
	public void test_Ask() throws Exception {
		String rq1 = "ASK { [a <Painter>] <paints> ?painting }";
		NamedQuery nq1 = repo.createNamedQuery(QUERY1, QueryLanguage.SPARQL, rq1, NS);
		assertEquals(nq1.getQueryString(), rq1);
		assertTrue(repo.getNamedQueryIDs().length==1) ;
		assertTrue(nq1.getParsedQuery() instanceof ParsedBooleanQuery) ;
	}
	
	
	public void test_Construct() throws Exception {
		String rq1 = "CONSTRUCT { ?painter a <Painter> } WHERE { ?painter <paints> ?painting }";
		NamedQuery nq1 = repo.createNamedQuery(QUERY1, QueryLanguage.SPARQL, rq1, NS);
		assertEquals(nq1.getQueryString(), rq1);
		assertTrue(repo.getNamedQueryIDs().length==1) ;
		assertTrue(nq1.getParsedQuery() instanceof ParsedGraphQuery) ;
	}
	
	public void test_addChangesAll() throws Exception {
		String rq1 = "SELECT ?painting WHERE { [a <Painter>] <paints> ?painting }";
		NamedQuery nq1 = repo.createNamedQuery(QUERY1, QueryLanguage.SPARQL, rq1, NS);
		String et1 = nq1.getResultTag() ;
		
		String rq2 = "SELECT ?painting "
			+ "WHERE { ?painter a <Painter> "
			+ "OPTIONAL { ?painter <paints> ?painting } }" ;
		NamedQuery nq2 = repo.createNamedQuery(QUERY2, QueryLanguage.SPARQL, rq2, NS);
		String et2 = nq2.getResultTag() ;
		
		// This test is non-optimistic, any change affects all
		a.add(PICASSO, RDF.TYPE , PAINTER);
		
		assertFalse(et1.equals(nq1.getResultTag()));
		assertFalse(et2.equals(nq2.getResultTag()));
	}

}

