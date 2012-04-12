package org.openrdf.sail.optimistic;

import junit.framework.TestCase;

import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.query.QueryLanguage;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.sail.memory.MemoryStore;

public class ModificationTest extends TestCase {
	private OptimisticRepository repo;
	private RepositoryConnection con;
	private String NS = "http://rdf.example.org/";
	private ValueFactory lf;
	private URI PAINTER;
	private URI PAINTS;
	private URI PAINTING;
	private URI YEAR;
	private URI PERIOD;
	private URI PICASSO;
	private URI REMBRANDT;
	private URI GUERNICA;
	private URI JACQUELINE;
	private URI NIGHTWATCH;
	private URI ARTEMISIA;
	private URI DANAE;
	private URI JACOB;
	private URI ANATOMY;
	private URI BELSHAZZAR;

	@Override
	public void setUp() throws Exception {
		repo = new OptimisticRepository(new MemoryStore());
		repo.setSnapshot(false);
		repo.setSerializable(false);
		repo.initialize();
		lf = repo.getValueFactory();
		ValueFactory uf = repo.getValueFactory();
		PAINTER = uf.createURI(NS, "Painter");
		PAINTS = uf.createURI(NS, "paints");
		PAINTING = uf.createURI(NS, "Painting");
		YEAR = uf.createURI(NS, "year");
		PERIOD = uf.createURI(NS, "period");
		PICASSO = uf.createURI(NS, "picasso");
		REMBRANDT = uf.createURI(NS, "rembrandt");
		GUERNICA = uf.createURI(NS, "guernica");
		JACQUELINE = uf.createURI(NS, "jacqueline");
		NIGHTWATCH = uf.createURI(NS, "nightwatch");
		ARTEMISIA = uf.createURI(NS, "artemisia");
		DANAE = uf.createURI(NS, "danaë");
		JACOB = uf.createURI(NS, "jacob");
		ANATOMY = uf.createURI(NS, "anatomy");
		BELSHAZZAR = uf.createURI(NS, "belshazzar");
		con = repo.getConnection();
	}

	@Override
	public void tearDown() throws Exception {
		con.close();
		repo.shutDown();
	}

	public void testAdd() throws Exception {
		con.setAutoCommit(false);
		con.add(PICASSO, RDF.TYPE, PAINTER);
		con.setAutoCommit(true);
		assertTrue(con.hasStatement(PICASSO, RDF.TYPE, PAINTER, false));
	}

	public void testAutoCommit() throws Exception {
		con.add(PICASSO, RDF.TYPE, PAINTER);
		con.close();
		con = repo.getConnection();
		assertTrue(con.hasStatement(PICASSO, RDF.TYPE, PAINTER, false));
	}

	public void testInsertData() throws Exception {
		con.setAutoCommit(false);
		con.prepareUpdate(QueryLanguage.SPARQL, "INSERT DATA { <picasso> a <Painter> }", NS).execute();
		con.setAutoCommit(true);
		assertTrue(con.hasStatement(PICASSO, RDF.TYPE, PAINTER, false));
	}

	public void testInsertDataAutoCommit() throws Exception {
		con.prepareUpdate(QueryLanguage.SPARQL, "INSERT DATA { <picasso> a <Painter> }", NS).execute();
		con.close();
		con = repo.getConnection();
		assertTrue(con.hasStatement(PICASSO, RDF.TYPE, PAINTER, false));
	}

	public void testRemove() throws Exception {
		con.setAutoCommit(false);
		con.add(PICASSO, RDF.TYPE, PAINTER);
		con.setAutoCommit(true);
		con.setAutoCommit(false);
		con.remove(PICASSO, RDF.TYPE, PAINTER);
		con.setAutoCommit(true);
		assertFalse(con.hasStatement(PICASSO, RDF.TYPE, PAINTER, false));
	}

	public void testAddIn() throws Exception {
		con.setAutoCommit(false);
		con.add(PICASSO, RDF.TYPE, PAINTER, PICASSO);
		con.setAutoCommit(true);
		assertTrue(con.hasStatement(PICASSO, RDF.TYPE, PAINTER, false, PICASSO));
	}

	public void testRemoveFrom() throws Exception {
		con.setAutoCommit(false);
		con.add(PICASSO, RDF.TYPE, PAINTER, PICASSO);
		con.setAutoCommit(true);
		con.setAutoCommit(false);
		con.remove(PICASSO, RDF.TYPE, PAINTER, PICASSO);
		con.setAutoCommit(true);
		assertFalse(con.hasStatement(PICASSO, RDF.TYPE, PAINTER, false, PICASSO));
	}

	public void testMove() throws Exception {
		con.setAutoCommit(false);
		con.add(PICASSO, RDF.TYPE, PAINTER, PICASSO);
		con.setAutoCommit(true);
		con.setAutoCommit(false);
		con.remove(PICASSO, RDF.TYPE, PAINTER, PICASSO);
		con.add(PICASSO, RDF.TYPE, PAINTER, PAINTER);
		con.setAutoCommit(true);
		assertFalse(con.hasStatement(PICASSO, RDF.TYPE, PAINTER, false, PICASSO));
		assertTrue(con.hasStatement(PICASSO, RDF.TYPE, PAINTER, false, PAINTER));
	}

	public void testMoveOut() throws Exception {
		con.setAutoCommit(false);
		con.add(PICASSO, RDF.TYPE, PAINTER, PICASSO);
		con.setAutoCommit(true);
		con.setAutoCommit(false);
		con.remove(PICASSO, RDF.TYPE, PAINTER, PICASSO);
		con.add(PICASSO, RDF.TYPE, PAINTER);
		con.setAutoCommit(true);
		assertFalse(con.hasStatement(PICASSO, RDF.TYPE, PAINTER, false, PICASSO));
		assertTrue(con.hasStatement(PICASSO, RDF.TYPE, PAINTER, false));
	}

	public void testCancel() throws Exception {
		con.setAutoCommit(false);
		con.add(PICASSO, RDF.TYPE, PAINTER, PICASSO);
		con.remove(PICASSO, RDF.TYPE, PAINTER, PICASSO);
		con.setAutoCommit(true);
		assertFalse(con.hasStatement(PICASSO, RDF.TYPE, PAINTER, false, PICASSO));
	}

	public void testRemoveDuplicate() throws Exception {
		con.add(PICASSO, RDF.TYPE, PAINTER, PICASSO, PAINTER);
		assertTrue(con.hasStatement(PICASSO, RDF.TYPE, PAINTER, false, PAINTER));
		assertTrue(con.hasStatement(PICASSO, RDF.TYPE, PAINTER, false, PICASSO));
		con.setAutoCommit(false);
		con.remove(PICASSO, RDF.TYPE, PAINTER, PAINTER);
		assertFalse(con.hasStatement(PICASSO, RDF.TYPE, PAINTER, false, PAINTER));
		assertTrue(con.hasStatement(PICASSO, RDF.TYPE, PAINTER, false, PICASSO));
		assertTrue(con.prepareBooleanQuery(QueryLanguage.SPARQL, "ASK {<"+PICASSO+"> a <"+PAINTER+">}").evaluate());
		con.setAutoCommit(true);
		assertFalse(con.hasStatement(PICASSO, RDF.TYPE, PAINTER, false, PAINTER));
		assertTrue(con.hasStatement(PICASSO, RDF.TYPE, PAINTER, false, PICASSO));
		assertTrue(con.prepareBooleanQuery(QueryLanguage.SPARQL, "ASK {<"+PICASSO+"> a <"+PAINTER+">}").evaluate());
	}

}
