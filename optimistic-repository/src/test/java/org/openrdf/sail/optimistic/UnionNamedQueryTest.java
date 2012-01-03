package org.openrdf.sail.optimistic;

import junit.framework.TestCase;

import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.query.QueryLanguage;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.query.NamedQuery;
import org.openrdf.sail.memory.MemoryStore;

public class UnionNamedQueryTest extends TestCase {
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

}
