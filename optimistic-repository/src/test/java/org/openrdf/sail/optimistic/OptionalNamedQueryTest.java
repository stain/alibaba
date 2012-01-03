package org.openrdf.sail.optimistic;

import junit.framework.TestCase;

import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.query.QueryLanguage;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.query.NamedQuery;
import org.openrdf.sail.memory.MemoryStore;

public class OptionalNamedQueryTest extends TestCase {
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

}
