package org.openrdf.sail.optimistic;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.sail.memory.MemoryStore;
import org.openrdf.sail.optimistic.exceptions.ConcurrencyException;

public class SnapshotTest extends TestCase {
	private OptimisticRepository sail;
	private RepositoryConnection a;
	private RepositoryConnection b;
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
		sail = new OptimisticRepository(new MemoryStore());
		sail.setSnapshot(true);
		sail.setSerializable(false);
		sail.initialize();
		lf = sail.getValueFactory();
		ValueFactory uf = sail.getValueFactory();
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
		a = sail.getConnection();
		b = sail.getConnection();
	}

	@Override
	public void tearDown() throws Exception {
		a.close();
		b.close();
		sail.shutDown();
	}

	public void test_independentPattern() throws Exception {
		a.setAutoCommit(false);
		b.setAutoCommit(false);
		a.add(PICASSO, RDF.TYPE, PAINTER);
		b.add(REMBRANDT, RDF.TYPE, PAINTER);
		assertEquals(1, size(a, PICASSO, RDF.TYPE, PAINTER, false));
		assertEquals(1, size(b, REMBRANDT, RDF.TYPE, PAINTER, false));
		a.setAutoCommit(true);
		b.setAutoCommit(true);
		assertEquals(2, size(a, null, RDF.TYPE, PAINTER, false));
		assertEquals(2, size(b, null, RDF.TYPE, PAINTER, false));
	}

	public void test_safePattern() throws Exception {
		a.setAutoCommit(false);
		b.setAutoCommit(false);
		a.add(PICASSO, RDF.TYPE, PAINTER);
		b.add(REMBRANDT, RDF.TYPE, PAINTER);
		assertEquals(1, size(a, null, RDF.TYPE, PAINTER, false));
		a.setAutoCommit(true);
		b.setAutoCommit(true);
	}

	public void test_afterPattern() throws Exception {
		a.setAutoCommit(false);
		b.setAutoCommit(false);
		a.add(PICASSO, RDF.TYPE, PAINTER);
		b.add(REMBRANDT, RDF.TYPE, PAINTER);
		assertEquals(1, size(a, null, RDF.TYPE, PAINTER, false));
		a.setAutoCommit(true);
		assertEquals(2, size(b, null, RDF.TYPE, PAINTER, false));
		b.setAutoCommit(true);
	}

	public void test_afterInsertDataPattern() throws Exception {
		a.setAutoCommit(false);
		b.setAutoCommit(false);
		a.prepareUpdate(QueryLanguage.SPARQL, "INSERT DATA { <picasso> a <Painter> }", NS).execute();
		b.prepareUpdate(QueryLanguage.SPARQL, "INSERT DATA { <rembrandt> a <Painter> }", NS).execute();
		assertEquals(1, size(a, null, RDF.TYPE, PAINTER, false));
		a.setAutoCommit(true);
		assertEquals(2, size(b, null, RDF.TYPE, PAINTER, false));
		b.setAutoCommit(true);
	}

	public void test_conflictPattern() throws Exception {
		a.setAutoCommit(false);
		b.setAutoCommit(false);
		a.add(PICASSO, RDF.TYPE, PAINTER);
		b.add(REMBRANDT, RDF.TYPE, PAINTER);
		assertEquals(1, size(b, null, RDF.TYPE, PAINTER, false));
		a.setAutoCommit(true);
		try {
			size(b, null, RDF.TYPE, PAINTER, false);
			b.setAutoCommit(true);
			fail();
		} catch (ConcurrencyException e) {
			e.printStackTrace();
		}
	}

	public void test_safeQuery() throws Exception {
		b.add(REMBRANDT, RDF.TYPE, PAINTER);
		b.add(REMBRANDT, PAINTS, NIGHTWATCH);
		b.add(REMBRANDT, PAINTS, ARTEMISIA);
		b.add(REMBRANDT, PAINTS, DANAE);
		a.setAutoCommit(false);
		b.setAutoCommit(false);
		// PICASSO is *not* a known PAINTER
		a.add(PICASSO, PAINTS, GUERNICA);
		a.add(PICASSO, PAINTS, JACQUELINE);
		List<Value> result = eval("painting", b, "SELECT ?painting "
				+ "WHERE { [a <Painter>] <paints> ?painting }");
		for (Value painting : result) {
			b.add((Resource) painting, RDF.TYPE, PAINTING);
		}
		a.setAutoCommit(true);
		b.setAutoCommit(true);
		assertEquals(9, size(a, null, null, null, false));
		assertEquals(9, size(b, null, null, null, false));
	}

	public void test_safeInsert() throws Exception {
		b.add(REMBRANDT, RDF.TYPE, PAINTER);
		b.add(REMBRANDT, PAINTS, NIGHTWATCH);
		b.add(REMBRANDT, PAINTS, ARTEMISIA);
		b.add(REMBRANDT, PAINTS, DANAE);
		a.setAutoCommit(false);
		b.setAutoCommit(false);
		// PICASSO is *not* a known PAINTER
		a.add(PICASSO, PAINTS, GUERNICA);
		a.add(PICASSO, PAINTS, JACQUELINE);
		b.prepareUpdate(QueryLanguage.SPARQL, "INSERT { ?painting a <Painting> }\n"
				+ "WHERE { [a <Painter>] <paints> ?painting }", NS).execute();
		a.setAutoCommit(true);
		b.setAutoCommit(true);
		assertEquals(9, size(a, null, null, null, false));
		assertEquals(9, size(b, null, null, null, false));
	}

	public void test_mergeQuery() throws Exception {
		a.add(PICASSO, RDF.TYPE, PAINTER);
		b.add(REMBRANDT, RDF.TYPE, PAINTER);
		b.add(REMBRANDT, PAINTS, NIGHTWATCH);
		b.add(REMBRANDT, PAINTS, ARTEMISIA);
		b.add(REMBRANDT, PAINTS, DANAE);
		a.setAutoCommit(false);
		b.setAutoCommit(false);
		// PICASSO *is* a known PAINTER
		a.add(PICASSO, PAINTS, GUERNICA);
		a.add(PICASSO, PAINTS, JACQUELINE);
		List<Value> result = eval("painting", b, "SELECT ?painting "
				+ "WHERE { [a <Painter>] <paints> ?painting }");
		for (Value painting : result) {
			b.add((Resource) painting, RDF.TYPE, PAINTING);
		}
		a.setAutoCommit(true);
		assertEquals(3, size(b, REMBRANDT, PAINTS, null, false));
		b.setAutoCommit(true);
		assertEquals(3, size(a, null, RDF.TYPE, PAINTING, false));
	}

	public void test_mergeInsert() throws Exception {
		a.add(PICASSO, RDF.TYPE, PAINTER);
		b.add(REMBRANDT, RDF.TYPE, PAINTER);
		b.add(REMBRANDT, PAINTS, NIGHTWATCH);
		b.add(REMBRANDT, PAINTS, ARTEMISIA);
		b.add(REMBRANDT, PAINTS, DANAE);
		a.setAutoCommit(false);
		b.setAutoCommit(false);
		// PICASSO *is* a known PAINTER
		a.add(PICASSO, PAINTS, GUERNICA);
		a.add(PICASSO, PAINTS, JACQUELINE);
		b.prepareUpdate(QueryLanguage.SPARQL, "INSERT { ?painting a <Painting> }\n"
				+ "WHERE { [a <Painter>] <paints> ?painting }", NS).execute();
		a.setAutoCommit(true);
		assertEquals(3, size(b, REMBRANDT, PAINTS, null, false));
		b.setAutoCommit(true);
		assertEquals(3, size(a, null, RDF.TYPE, PAINTING, false));
	}

	public void test_conflictQuery() throws Exception {
		a.add(PICASSO, RDF.TYPE, PAINTER);
		b.add(REMBRANDT, RDF.TYPE, PAINTER);
		b.add(REMBRANDT, PAINTS, NIGHTWATCH);
		b.add(REMBRANDT, PAINTS, ARTEMISIA);
		b.add(REMBRANDT, PAINTS, DANAE);
		a.setAutoCommit(false);
		b.setAutoCommit(false);
		// PICASSO *is* a known PAINTER
		a.add(PICASSO, PAINTS, GUERNICA);
		a.add(PICASSO, PAINTS, JACQUELINE);
		List<Value> result = eval("painting", b, "SELECT ?painting "
				+ "WHERE { [a <Painter>] <paints> ?painting }");
		for (Value painting : result) {
			b.add((Resource) painting, RDF.TYPE, PAINTING);
		}
		a.setAutoCommit(true);
		try {
			size(b, null, PAINTS, null, false);
			b.setAutoCommit(true);
			fail();
		} catch (ConcurrencyException e) {
			e.printStackTrace();
		}
		assertEquals(0, size(a, null, RDF.TYPE, PAINTING, false));
	}

	public void test_conflictInsert() throws Exception {
		a.add(PICASSO, RDF.TYPE, PAINTER);
		b.add(REMBRANDT, RDF.TYPE, PAINTER);
		b.add(REMBRANDT, PAINTS, NIGHTWATCH);
		b.add(REMBRANDT, PAINTS, ARTEMISIA);
		b.add(REMBRANDT, PAINTS, DANAE);
		a.setAutoCommit(false);
		b.setAutoCommit(false);
		// PICASSO *is* a known PAINTER
		a.add(PICASSO, PAINTS, GUERNICA);
		a.add(PICASSO, PAINTS, JACQUELINE);
		b.prepareUpdate(QueryLanguage.SPARQL, "INSERT { ?painting a <Painting> }\n"
				+ "WHERE { [a <Painter>] <paints> ?painting }", NS).execute();
		a.setAutoCommit(true);
		try {
			size(b, null, PAINTS, null, false);
			b.setAutoCommit(true);
			fail();
		} catch (ConcurrencyException e) {
			e.printStackTrace();
		}
		assertEquals(0, size(a, null, RDF.TYPE, PAINTING, false));
	}

	public void test_safeOptionalQuery() throws Exception {
		b.add(REMBRANDT, RDF.TYPE, PAINTER);
		b.add(REMBRANDT, PAINTS, NIGHTWATCH);
		b.add(REMBRANDT, PAINTS, ARTEMISIA);
		b.add(REMBRANDT, PAINTS, DANAE);
		a.setAutoCommit(false);
		b.setAutoCommit(false);
		// PICASSO is *not* a known PAINTER
		a.add(PICASSO, PAINTS, GUERNICA);
		a.add(PICASSO, PAINTS, JACQUELINE);
		List<Value> result = eval("painting", b, "SELECT ?painting "
				+ "WHERE { ?painter a <Painter> "
				+ "OPTIONAL { ?painter <paints> ?painting } }");
		for (Value painting : result) {
			if (painting != null) {
				b.add((Resource) painting, RDF.TYPE, PAINTING);
			}
		}
		a.setAutoCommit(true);
		b.setAutoCommit(true);
		assertEquals(9, size(a, null, null, null, false));
		assertEquals(9, size(b, null, null, null, false));
	}

	public void test_safeOptionalInsert() throws Exception {
		b.add(REMBRANDT, RDF.TYPE, PAINTER);
		b.add(REMBRANDT, PAINTS, NIGHTWATCH);
		b.add(REMBRANDT, PAINTS, ARTEMISIA);
		b.add(REMBRANDT, PAINTS, DANAE);
		a.setAutoCommit(false);
		b.setAutoCommit(false);
		// PICASSO is *not* a known PAINTER
		a.add(PICASSO, PAINTS, GUERNICA);
		a.add(PICASSO, PAINTS, JACQUELINE);
		b.prepareUpdate(QueryLanguage.SPARQL, "INSERT { ?painting a <Painting> }\n"
				+ "WHERE { ?painter a <Painter> "
				+ "OPTIONAL { ?painter <paints> ?painting } }", NS).execute();
		a.setAutoCommit(true);
		b.setAutoCommit(true);
		assertEquals(9, size(a, null, null, null, false));
		assertEquals(9, size(b, null, null, null, false));
	}

	public void test_mergeOptionalQuery() throws Exception {
		a.add(PICASSO, RDF.TYPE, PAINTER);
		b.add(REMBRANDT, RDF.TYPE, PAINTER);
		b.add(REMBRANDT, PAINTS, NIGHTWATCH);
		b.add(REMBRANDT, PAINTS, ARTEMISIA);
		b.add(REMBRANDT, PAINTS, DANAE);
		a.setAutoCommit(false);
		b.setAutoCommit(false);
		// PICASSO *is* a known PAINTER
		a.add(PICASSO, PAINTS, GUERNICA);
		a.add(PICASSO, PAINTS, JACQUELINE);
		List<Value> result = eval("painting", b, "SELECT ?painting "
				+ "WHERE { ?painter a <Painter> "
				+ "OPTIONAL { ?painter <paints> ?painting } }");
		for (Value painting : result) {
			if (painting != null) {
				b.add((Resource) painting, RDF.TYPE, PAINTING);
			}
		}
		a.setAutoCommit(true);
		assertEquals(3, size(b, REMBRANDT, PAINTS, null, false));
		b.setAutoCommit(true);
		assertEquals(10, size(a, null, null, null, false));
	}

	public void test_mergeOptionalInsert() throws Exception {
		a.add(PICASSO, RDF.TYPE, PAINTER);
		b.add(REMBRANDT, RDF.TYPE, PAINTER);
		b.add(REMBRANDT, PAINTS, NIGHTWATCH);
		b.add(REMBRANDT, PAINTS, ARTEMISIA);
		b.add(REMBRANDT, PAINTS, DANAE);
		a.setAutoCommit(false);
		b.setAutoCommit(false);
		// PICASSO *is* a known PAINTER
		a.add(PICASSO, PAINTS, GUERNICA);
		a.add(PICASSO, PAINTS, JACQUELINE);
		b.prepareUpdate(QueryLanguage.SPARQL, "INSERT { ?painting a <Painting> }\n"
				+ "WHERE { ?painter a <Painter> "
				+ "OPTIONAL { ?painter <paints> ?painting } }", NS).execute();
		a.setAutoCommit(true);
		assertEquals(3, size(b, REMBRANDT, PAINTS, null, false));
		b.setAutoCommit(true);
		assertEquals(10, size(a, null, null, null, false));
	}

	public void test_conflictOptionalQuery() throws Exception {
		a.add(PICASSO, RDF.TYPE, PAINTER);
		b.add(REMBRANDT, RDF.TYPE, PAINTER);
		b.add(REMBRANDT, PAINTS, NIGHTWATCH);
		b.add(REMBRANDT, PAINTS, ARTEMISIA);
		b.add(REMBRANDT, PAINTS, DANAE);
		a.setAutoCommit(false);
		b.setAutoCommit(false);
		// PICASSO *is* a known PAINTER
		a.add(PICASSO, PAINTS, GUERNICA);
		a.add(PICASSO, PAINTS, JACQUELINE);
		List<Value> result = eval("painting", b, "SELECT ?painting "
				+ "WHERE { ?painter a <Painter> "
				+ "OPTIONAL { ?painter <paints> ?painting } }");
		for (Value painting : result) {
			if (painting != null) {
				b.add((Resource) painting, RDF.TYPE, PAINTING);
			}
		}
		a.setAutoCommit(true);
		try {
			size(b, null, PAINTS, null, false);
			b.setAutoCommit(true);
			fail();
		} catch (ConcurrencyException e) {
			e.printStackTrace();
		}
		assertEquals(7, size(a, null, null, null, false));
	}

	public void test_conflictOptionalInsert() throws Exception {
		a.add(PICASSO, RDF.TYPE, PAINTER);
		b.add(REMBRANDT, RDF.TYPE, PAINTER);
		b.add(REMBRANDT, PAINTS, NIGHTWATCH);
		b.add(REMBRANDT, PAINTS, ARTEMISIA);
		b.add(REMBRANDT, PAINTS, DANAE);
		a.setAutoCommit(false);
		b.setAutoCommit(false);
		// PICASSO *is* a known PAINTER
		a.add(PICASSO, PAINTS, GUERNICA);
		a.add(PICASSO, PAINTS, JACQUELINE);
		b.prepareUpdate(QueryLanguage.SPARQL, "INSERT { ?painting a <Painting> }\n"
				+ "WHERE { ?painter a <Painter> "
				+ "OPTIONAL { ?painter <paints> ?painting } }", NS).execute();
		a.setAutoCommit(true);
		try {
			size(b, null, PAINTS, null, false);
			b.setAutoCommit(true);
			fail();
		} catch (ConcurrencyException e) {
			e.printStackTrace();
		}
		assertEquals(7, size(a, null, null, null, false));
	}

	public void test_safeFilterQuery() throws Exception {
		b.add(REMBRANDT, RDF.TYPE, PAINTER);
		b.add(REMBRANDT, PAINTS, NIGHTWATCH);
		b.add(REMBRANDT, PAINTS, ARTEMISIA);
		b.add(REMBRANDT, PAINTS, DANAE);
		a.setAutoCommit(false);
		b.setAutoCommit(false);
		a.add(PICASSO, RDF.TYPE, PAINTER);
		a.add(PICASSO, PAINTS, GUERNICA);
		a.add(PICASSO, PAINTS, JACQUELINE);
		List<Value> result = eval("painting", b, "SELECT ?painting "
				+ "WHERE { ?painter a <Painter>; <paints> ?painting "
				+ "FILTER  regex(str(?painter), \"rem\", \"i\") }");
		for (Value painting : result) {
			b.add((Resource) painting, RDF.TYPE, PAINTING);
		}
		a.setAutoCommit(true);
		b.setAutoCommit(true);
		assertEquals(10, size(a, null, null, null, false));
	}

	public void test_safeFilterInsert() throws Exception {
		b.add(REMBRANDT, RDF.TYPE, PAINTER);
		b.add(REMBRANDT, PAINTS, NIGHTWATCH);
		b.add(REMBRANDT, PAINTS, ARTEMISIA);
		b.add(REMBRANDT, PAINTS, DANAE);
		a.setAutoCommit(false);
		b.setAutoCommit(false);
		a.add(PICASSO, RDF.TYPE, PAINTER);
		a.add(PICASSO, PAINTS, GUERNICA);
		a.add(PICASSO, PAINTS, JACQUELINE);
		b.prepareUpdate(QueryLanguage.SPARQL, "INSERT { ?painting a <Painting> }\n"
				+ "WHERE { ?painter a <Painter>; <paints> ?painting "
				+ "FILTER  regex(str(?painter), \"rem\", \"i\") }", NS).execute();
		a.setAutoCommit(true);
		b.setAutoCommit(true);
		assertEquals(10, size(a, null, null, null, false));
	}

	public void test_mergeOptionalFilterQuery() throws Exception {
		a.add(PICASSO, RDF.TYPE, PAINTER);
		a.add(PICASSO, PAINTS, GUERNICA);
		a.add(PICASSO, PAINTS, JACQUELINE);
		b.add(REMBRANDT, RDF.TYPE, PAINTER);
		b.add(REMBRANDT, PAINTS, NIGHTWATCH);
		b.add(REMBRANDT, PAINTS, ARTEMISIA);
		b.add(REMBRANDT, PAINTS, DANAE);
		a.setAutoCommit(false);
		b.setAutoCommit(false);
		a.add(GUERNICA, RDF.TYPE, PAINTING);
		a.add(JACQUELINE, RDF.TYPE, PAINTING);
		List<Value> result = eval("painting", b, "SELECT ?painting "
				+ "WHERE { [a <Painter>] <paints> ?painting "
				+ "OPTIONAL { ?painting a ?type  } FILTER (!bound(?type)) }");
		for (Value painting : result) {
			if (painting != null) {
				b.add((Resource) painting, RDF.TYPE, PAINTING);
			}
		}
		a.setAutoCommit(true);
		assertEquals(5, size(b, null, PAINTS, null, false));
		b.setAutoCommit(true);
		assertEquals(12, size(a, null, null, null, false));
	}

	public void test_mergeOptionalFilterInsert() throws Exception {
		a.add(PICASSO, RDF.TYPE, PAINTER);
		a.add(PICASSO, PAINTS, GUERNICA);
		a.add(PICASSO, PAINTS, JACQUELINE);
		b.add(REMBRANDT, RDF.TYPE, PAINTER);
		b.add(REMBRANDT, PAINTS, NIGHTWATCH);
		b.add(REMBRANDT, PAINTS, ARTEMISIA);
		b.add(REMBRANDT, PAINTS, DANAE);
		a.setAutoCommit(false);
		b.setAutoCommit(false);
		a.add(GUERNICA, RDF.TYPE, PAINTING);
		a.add(JACQUELINE, RDF.TYPE, PAINTING);
		b.prepareUpdate(QueryLanguage.SPARQL, "INSERT { ?painting a <Painting> }\n"
				+ "WHERE { [a <Painter>] <paints> ?painting "
				+ "OPTIONAL { ?painting a ?type  } FILTER (!bound(?type)) }", NS).execute();
		a.setAutoCommit(true);
		assertEquals(5, size(b, null, PAINTS, null, false));
		b.setAutoCommit(true);
		assertEquals(12, size(a, null, null, null, false));
	}

	public void test_conflictOptionalFilterQuery() throws Exception {
		a.add(PICASSO, RDF.TYPE, PAINTER);
		a.add(PICASSO, PAINTS, GUERNICA);
		a.add(PICASSO, PAINTS, JACQUELINE);
		b.add(REMBRANDT, RDF.TYPE, PAINTER);
		b.add(REMBRANDT, PAINTS, NIGHTWATCH);
		b.add(REMBRANDT, PAINTS, ARTEMISIA);
		b.add(REMBRANDT, PAINTS, DANAE);
		a.setAutoCommit(false);
		b.setAutoCommit(false);
		a.add(GUERNICA, RDF.TYPE, PAINTING);
		a.add(JACQUELINE, RDF.TYPE, PAINTING);
		List<Value> result = eval("painting", b, "SELECT ?painting "
				+ "WHERE { [a <Painter>] <paints> ?painting "
				+ "OPTIONAL { ?painting a ?type  } FILTER (!bound(?type)) }");
		for (Value painting : result) {
			if (painting != null) {
				b.add((Resource) painting, RDF.TYPE, PAINTING);
			}
		}
		a.setAutoCommit(true);
		try {
			size(b, null, RDF.TYPE, PAINTING, false);
			b.setAutoCommit(true);
			fail();
		} catch (ConcurrencyException e) {
			e.printStackTrace();
		}
		assertEquals(9, size(a, null, null, null, false));
	}

	public void test_conflictOptionalFilterInsert() throws Exception {
		a.add(PICASSO, RDF.TYPE, PAINTER);
		a.add(PICASSO, PAINTS, GUERNICA);
		a.add(PICASSO, PAINTS, JACQUELINE);
		b.add(REMBRANDT, RDF.TYPE, PAINTER);
		b.add(REMBRANDT, PAINTS, NIGHTWATCH);
		b.add(REMBRANDT, PAINTS, ARTEMISIA);
		b.add(REMBRANDT, PAINTS, DANAE);
		a.setAutoCommit(false);
		b.setAutoCommit(false);
		a.add(GUERNICA, RDF.TYPE, PAINTING);
		a.add(JACQUELINE, RDF.TYPE, PAINTING);
		b.prepareUpdate(QueryLanguage.SPARQL, "INSERT { ?painting a <Painting> }\n"
				+ "WHERE { [a <Painter>] <paints> ?painting "
				+ "OPTIONAL { ?painting a ?type  } FILTER (!bound(?type)) }", NS).execute();
		a.setAutoCommit(true);
		try {
			size(b, null, RDF.TYPE, PAINTING, false);
			b.setAutoCommit(true);
			fail();
		} catch (ConcurrencyException e) {
			e.printStackTrace();
		}
		assertEquals(9, size(a, null, null, null, false));
	}

	public void test_safeRangeQuery() throws Exception {
		a.add(REMBRANDT, RDF.TYPE, PAINTER);
		a.add(REMBRANDT, PAINTS, ARTEMISIA);
		a.add(REMBRANDT, PAINTS, DANAE);
		a.add(REMBRANDT, PAINTS, JACOB);
		a.add(REMBRANDT, PAINTS, ANATOMY);
		a.add(REMBRANDT, PAINTS, BELSHAZZAR);
		a.add(BELSHAZZAR, YEAR, lf.createLiteral(1635));
		a.add(ARTEMISIA, YEAR, lf.createLiteral(1634));
		a.add(DANAE, YEAR, lf.createLiteral(1636));
		a.add(JACOB, YEAR, lf.createLiteral(1632));
		a.add(ANATOMY, YEAR, lf.createLiteral(1632));
		a.setAutoCommit(false);
		b.setAutoCommit(false);
		List<Value> result = eval("painting", b, "SELECT ?painting "
				+ "WHERE { <rembrandt> <paints> ?painting . ?painting <year> ?year "
				+ "FILTER  (1631 <= ?year && ?year <= 1635) }");
		for (Value painting : result) {
			b.add((Resource) painting, PERIOD, lf.createLiteral("First Amsterdam period"));
		}
		a.add(REMBRANDT, PAINTS, NIGHTWATCH);
		a.add(NIGHTWATCH, YEAR, lf.createLiteral(1642));
		a.setAutoCommit(true);
		b.setAutoCommit(true);
		assertEquals(17, size(a, null, null, null, false));
	}

	public void test_safeRangeInsert() throws Exception {
		a.add(REMBRANDT, RDF.TYPE, PAINTER);
		a.add(REMBRANDT, PAINTS, ARTEMISIA);
		a.add(REMBRANDT, PAINTS, DANAE);
		a.add(REMBRANDT, PAINTS, JACOB);
		a.add(REMBRANDT, PAINTS, ANATOMY);
		a.add(REMBRANDT, PAINTS, BELSHAZZAR);
		a.add(BELSHAZZAR, YEAR, lf.createLiteral(1635));
		a.add(ARTEMISIA, YEAR, lf.createLiteral(1634));
		a.add(DANAE, YEAR, lf.createLiteral(1636));
		a.add(JACOB, YEAR, lf.createLiteral(1632));
		a.add(ANATOMY, YEAR, lf.createLiteral(1632));
		a.setAutoCommit(false);
		b.setAutoCommit(false);
		b.prepareUpdate(QueryLanguage.SPARQL, "INSERT { ?painting <period> \"First Amsterdam period\" }\n"
				+ "WHERE { <rembrandt> <paints> ?painting . ?painting <year> ?year "
				+ "FILTER  (1631 <= ?year && ?year <= 1635) }", NS).execute();
		a.add(REMBRANDT, PAINTS, NIGHTWATCH);
		a.add(NIGHTWATCH, YEAR, lf.createLiteral(1642));
		a.setAutoCommit(true);
		b.setAutoCommit(true);
		assertEquals(17, size(a, null, null, null, false));
	}

	public void test_mergeRangeQuery() throws Exception {
		a.add(REMBRANDT, RDF.TYPE, PAINTER);
		a.add(REMBRANDT, PAINTS, NIGHTWATCH);
		a.add(REMBRANDT, PAINTS, ARTEMISIA);
		a.add(REMBRANDT, PAINTS, DANAE);
		a.add(REMBRANDT, PAINTS, JACOB);
		a.add(REMBRANDT, PAINTS, ANATOMY);
		a.add(ARTEMISIA, YEAR, lf.createLiteral(1634));
		a.add(NIGHTWATCH, YEAR, lf.createLiteral(1642));
		a.add(DANAE, YEAR, lf.createLiteral(1636));
		a.add(JACOB, YEAR, lf.createLiteral(1632));
		a.add(ANATOMY, YEAR, lf.createLiteral(1632));
		a.setAutoCommit(false);
		b.setAutoCommit(false);
		List<Value> result = eval("painting", b, "SELECT ?painting "
				+ "WHERE { <rembrandt> <paints> ?painting . ?painting <year> ?year "
				+ "FILTER  (1631 <= ?year && ?year <= 1635) }");
		for (Value painting : result) {
			b.add((Resource) painting, PERIOD, lf.createLiteral("First Amsterdam period"));
		}
		a.add(REMBRANDT, PAINTS, BELSHAZZAR);
		a.add(BELSHAZZAR, YEAR, lf.createLiteral(1635));
		a.setAutoCommit(true);
		assertEquals(2, size(b, ARTEMISIA, null, null, false));
		b.setAutoCommit(true);
		assertEquals(16, size(a, null, null, null, false));
	}

	public void test_mergeRangeInsert() throws Exception {
		a.add(REMBRANDT, RDF.TYPE, PAINTER);
		a.add(REMBRANDT, PAINTS, NIGHTWATCH);
		a.add(REMBRANDT, PAINTS, ARTEMISIA);
		a.add(REMBRANDT, PAINTS, DANAE);
		a.add(REMBRANDT, PAINTS, JACOB);
		a.add(REMBRANDT, PAINTS, ANATOMY);
		a.add(ARTEMISIA, YEAR, lf.createLiteral(1634));
		a.add(NIGHTWATCH, YEAR, lf.createLiteral(1642));
		a.add(DANAE, YEAR, lf.createLiteral(1636));
		a.add(JACOB, YEAR, lf.createLiteral(1632));
		a.add(ANATOMY, YEAR, lf.createLiteral(1632));
		a.setAutoCommit(false);
		b.setAutoCommit(false);
		b.prepareUpdate(QueryLanguage.SPARQL, "INSERT { ?painting <period> \"First Amsterdam period\" }\n"
				+ "WHERE { <rembrandt> <paints> ?painting . ?painting <year> ?year "
				+ "FILTER  (1631 <= ?year && ?year <= 1635) }", NS).execute();
		a.add(REMBRANDT, PAINTS, BELSHAZZAR);
		a.add(BELSHAZZAR, YEAR, lf.createLiteral(1635));
		a.setAutoCommit(true);
		assertEquals(2, size(b, ARTEMISIA, null, null, false));
		b.setAutoCommit(true);
		assertEquals(16, size(a, null, null, null, false));
	}

	public void test_conflictRangeQuery() throws Exception {
		a.add(REMBRANDT, RDF.TYPE, PAINTER);
		a.add(REMBRANDT, PAINTS, NIGHTWATCH);
		a.add(REMBRANDT, PAINTS, ARTEMISIA);
		a.add(REMBRANDT, PAINTS, DANAE);
		a.add(REMBRANDT, PAINTS, JACOB);
		a.add(REMBRANDT, PAINTS, ANATOMY);
		a.add(ARTEMISIA, YEAR, lf.createLiteral(1634));
		a.add(NIGHTWATCH, YEAR, lf.createLiteral(1642));
		a.add(DANAE, YEAR, lf.createLiteral(1636));
		a.add(JACOB, YEAR, lf.createLiteral(1632));
		a.add(ANATOMY, YEAR, lf.createLiteral(1632));
		a.setAutoCommit(false);
		b.setAutoCommit(false);
		List<Value> result = eval("painting", b, "SELECT ?painting "
				+ "WHERE { <rembrandt> <paints> ?painting . ?painting <year> ?year "
				+ "FILTER  (1631 <= ?year && ?year <= 1635) }");
		for (Value painting : result) {
			b.add((Resource) painting, PERIOD, lf.createLiteral("First Amsterdam period"));
		}
		a.add(REMBRANDT, PAINTS, BELSHAZZAR);
		a.add(BELSHAZZAR, YEAR, lf.createLiteral(1635));
		a.setAutoCommit(true);
		try {
			size(b, REMBRANDT, PAINTS, null, false);
			b.setAutoCommit(true);
			fail();
		} catch (ConcurrencyException e) {
			e.printStackTrace();
		}
		assertEquals(13, size(a, null, null, null, false));
	}

	public void test_conflictRangeInsert() throws Exception {
		a.add(REMBRANDT, RDF.TYPE, PAINTER);
		a.add(REMBRANDT, PAINTS, NIGHTWATCH);
		a.add(REMBRANDT, PAINTS, ARTEMISIA);
		a.add(REMBRANDT, PAINTS, DANAE);
		a.add(REMBRANDT, PAINTS, JACOB);
		a.add(REMBRANDT, PAINTS, ANATOMY);
		a.add(ARTEMISIA, YEAR, lf.createLiteral(1634));
		a.add(NIGHTWATCH, YEAR, lf.createLiteral(1642));
		a.add(DANAE, YEAR, lf.createLiteral(1636));
		a.add(JACOB, YEAR, lf.createLiteral(1632));
		a.add(ANATOMY, YEAR, lf.createLiteral(1632));
		a.setAutoCommit(false);
		b.setAutoCommit(false);
		b.prepareUpdate(QueryLanguage.SPARQL, "INSERT { ?painting <period> \"First Amsterdam period\" }\n"
				+ "WHERE { <rembrandt> <paints> ?painting . ?painting <year> ?year "
				+ "FILTER  (1631 <= ?year && ?year <= 1635) }", NS).execute();
		a.add(REMBRANDT, PAINTS, BELSHAZZAR);
		a.add(BELSHAZZAR, YEAR, lf.createLiteral(1635));
		a.setAutoCommit(true);
		try {
			size(b, REMBRANDT, PAINTS, null, false);
			b.setAutoCommit(true);
			fail();
		} catch (ConcurrencyException e) {
			e.printStackTrace();
		}
		assertEquals(13, size(a, null, null, null, false));
	}

	private int size(RepositoryConnection con, Resource subj, URI pred,
			Value obj, boolean inf, Resource... ctx) throws Exception {
		return con.getStatements(subj, pred, obj, inf, ctx).asList().size();
	}

	private List<Value> eval(String var, RepositoryConnection con, String qry)
			throws Exception {
		TupleQueryResult result;
		result = con.prepareTupleQuery(QueryLanguage.SPARQL, qry, NS).evaluate();
		try {
			List<Value> list = new ArrayList<Value>();
			while (result.hasNext()) {
				list.add(result.next().getValue(var));
			}
			return list;
		} finally {
			result.close();
		}
	}

}
