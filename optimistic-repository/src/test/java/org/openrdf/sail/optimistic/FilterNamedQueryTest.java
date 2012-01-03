package org.openrdf.sail.optimistic;

import junit.framework.TestCase;

import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.query.QueryLanguage;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.query.NamedQuery;
import org.openrdf.sail.memory.MemoryStore;

public class FilterNamedQueryTest extends TestCase {
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
	
	public void test_addCausesChange() throws Exception {
		RepositoryConnection a = repo.getConnection();
		try {
			a.add(PICASSO, RDF.TYPE , PAINTER);
	
			String rq1 = "SELECT ?painting WHERE { ?painter <paints> ?painting FILTER EXISTS { ?painter a <Painter> } }";
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
		try {
	
			String rq1 = "SELECT ?painting WHERE { ?painter <paints> ?painting FILTER EXISTS { ?painter a <Painter> } }";
			NamedQuery nq1 = repo.createNamedQuery(QUERY1, QueryLanguage.SPARQL, rq1, NS);
			long lastModified = nq1.getResultLastModified() ;
			String eTag = nq1.getResultTag() ;
			Thread.sleep(1) ;
			
			// Add a new result
			a.add(PICASSO, PAINTS, GUERNICA);
	
			assertEquals(lastModified, nq1.getResultLastModified());
			assertEquals(eTag, nq1.getResultTag());
		}
		finally { a.close(); }
	}
	
	public void test_addCausesExistsChange() throws Exception {
		RepositoryConnection a = repo.getConnection();
		try {
			a.add(PICASSO, PAINTS, GUERNICA);
	
			String rq1 = "SELECT ?painting WHERE { ?painter <paints> ?painting FILTER EXISTS { ?painter a <Painter> } }";
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
	
	public void test_addCausesNotExistsChange() throws Exception {
		RepositoryConnection a = repo.getConnection();
		try {
			a.add(PICASSO, RDF.TYPE , PAINTER);
	
			String rq1 = "SELECT ?painting WHERE { ?painter a <Painter> FILTER NOT EXISTS { ?painter <paints> ?painting } }";
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

}
