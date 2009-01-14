package org.openrdf.sail.optimistic;

import static org.openrdf.query.parser.QueryParserUtil.parseTupleQuery;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.openrdf.cursor.Cursor;
import org.openrdf.model.LiteralFactory;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.URIFactory;
import org.openrdf.model.Value;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.impl.EmptyBindingSet;
import org.openrdf.query.parser.TupleQueryModel;
import org.openrdf.sail.Sail;
import org.openrdf.sail.SailConnection;
import org.openrdf.sail.memory.MemoryStore;
import org.openrdf.sail.optimistic.exceptions.ConcurrencyException;
import org.openrdf.store.StoreException;

public class ConcurrencyTest extends TestCase {
	private Sail sail;
	private SailConnection a;
	private SailConnection b;
	private String NS = "http://rdf.example.org/";
	private LiteralFactory lf;
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
		sail = new MemoryStore();
		sail = new OptimisticSail(sail);
		sail.initialize();
		lf = sail.getLiteralFactory();
		URIFactory uf = sail.getURIFactory();
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
		a.begin();
		b.begin();
		a.addStatement(PICASSO, RDF.TYPE, PAINTER);
		b.addStatement(REMBRANDT, RDF.TYPE, PAINTER);
		assertEquals(1, a.size(PICASSO, RDF.TYPE, PAINTER, false));
		assertEquals(1, b.size(REMBRANDT, RDF.TYPE, PAINTER, false));
		a.commit();
		b.commit();
		assertEquals(2, a.size(null, RDF.TYPE, PAINTER, false));
		assertEquals(2, b.size(null, RDF.TYPE, PAINTER, false));
	}

	public void test_safePattern() throws Exception {
		a.begin();
		b.begin();
		a.addStatement(PICASSO, RDF.TYPE, PAINTER);
		b.addStatement(REMBRANDT, RDF.TYPE, PAINTER);
		assertEquals(1, a.size(null, RDF.TYPE, PAINTER, false));
		a.commit();
		b.commit();
	}

	public void test_conflictPattern() throws Exception {
		a.begin();
		b.begin();
		a.addStatement(PICASSO, RDF.TYPE, PAINTER);
		b.addStatement(REMBRANDT, RDF.TYPE, PAINTER);
		assertEquals(1, b.size(null, RDF.TYPE, PAINTER, false));
		a.commit();
		try {
			b.commit();
			fail();
		} catch (ConcurrencyException e) {
			e.printStackTrace();
		}
	}

	public void test_safeQuery() throws Exception {
		b.addStatement(REMBRANDT, RDF.TYPE, PAINTER);
		b.addStatement(REMBRANDT, PAINTS, NIGHTWATCH);
		b.addStatement(REMBRANDT, PAINTS, ARTEMISIA);
		b.addStatement(REMBRANDT, PAINTS, DANAE);
		a.begin();
		b.begin();
		// PICASSO is *not* a known PAINTER
		a.addStatement(PICASSO, PAINTS, GUERNICA);
		a.addStatement(PICASSO, PAINTS, JACQUELINE);
		List<Value> result = eval("painting", b, "SELECT ?painting "
				+ "WHERE { [a <Painter>] <paints> ?painting }");
		for (Value painting : result) {
			b.addStatement((Resource) painting, RDF.TYPE, PAINTING);
		}
		a.commit();
		b.commit();
		assertEquals(9, a.size(null, null, null, false));
		assertEquals(9, b.size(null, null, null, false));
	}

	public void test_conflictQuery() throws Exception {
		a.addStatement(PICASSO, RDF.TYPE, PAINTER);
		b.addStatement(REMBRANDT, RDF.TYPE, PAINTER);
		b.addStatement(REMBRANDT, PAINTS, NIGHTWATCH);
		b.addStatement(REMBRANDT, PAINTS, ARTEMISIA);
		b.addStatement(REMBRANDT, PAINTS, DANAE);
		a.begin();
		b.begin();
		// PICASSO *is* a known PAINTER
		a.addStatement(PICASSO, PAINTS, GUERNICA);
		a.addStatement(PICASSO, PAINTS, JACQUELINE);
		List<Value> result = eval("painting", b, "SELECT ?painting "
				+ "WHERE { [a <Painter>] <paints> ?painting }");
		for (Value painting : result) {
			b.addStatement((Resource) painting, RDF.TYPE, PAINTING);
		}
		a.commit();
		try {
			b.commit();
			fail();
		} catch (ConcurrencyException e) {
			e.printStackTrace();
		}
		assertEquals(7, a.size(null, null, null, false));
	}

	public void test_safeOptionalQuery() throws Exception {
		b.addStatement(REMBRANDT, RDF.TYPE, PAINTER);
		b.addStatement(REMBRANDT, PAINTS, NIGHTWATCH);
		b.addStatement(REMBRANDT, PAINTS, ARTEMISIA);
		b.addStatement(REMBRANDT, PAINTS, DANAE);
		a.begin();
		b.begin();
		// PICASSO is *not* a known PAINTER
		a.addStatement(PICASSO, PAINTS, GUERNICA);
		a.addStatement(PICASSO, PAINTS, JACQUELINE);
		List<Value> result = eval("painting", b, "SELECT ?painting "
				+ "WHERE { ?painter a <Painter> "
				+ "OPTIONAL { ?painter <paints> ?painting } }");
		for (Value painting : result) {
			if (painting != null) {
				b.addStatement((Resource) painting, RDF.TYPE, PAINTING);
			}
		}
		a.commit();
		b.commit();
		assertEquals(9, a.size(null, null, null, false));
		assertEquals(9, b.size(null, null, null, false));
	}

	public void test_conflictOptionalQuery() throws Exception {
		a.addStatement(PICASSO, RDF.TYPE, PAINTER);
		b.addStatement(REMBRANDT, RDF.TYPE, PAINTER);
		b.addStatement(REMBRANDT, PAINTS, NIGHTWATCH);
		b.addStatement(REMBRANDT, PAINTS, ARTEMISIA);
		b.addStatement(REMBRANDT, PAINTS, DANAE);
		a.begin();
		b.begin();
		// PICASSO *is* a known PAINTER
		a.addStatement(PICASSO, PAINTS, GUERNICA);
		a.addStatement(PICASSO, PAINTS, JACQUELINE);
		List<Value> result = eval("painting", b, "SELECT ?painting "
				+ "WHERE { ?painter a <Painter> "
				+ "OPTIONAL { ?painter <paints> ?painting } }");
		for (Value painting : result) {
			if (painting != null) {
				b.addStatement((Resource) painting, RDF.TYPE, PAINTING);
			}
		}
		a.commit();
		try {
			b.commit();
			fail();
		} catch (ConcurrencyException e) {
			e.printStackTrace();
		}
		assertEquals(7, a.size(null, null, null, false));
	}

	public void test_safeFilterQuery() throws Exception {
		b.addStatement(REMBRANDT, RDF.TYPE, PAINTER);
		b.addStatement(REMBRANDT, PAINTS, NIGHTWATCH);
		b.addStatement(REMBRANDT, PAINTS, ARTEMISIA);
		b.addStatement(REMBRANDT, PAINTS, DANAE);
		a.begin();
		b.begin();
		a.addStatement(PICASSO, RDF.TYPE, PAINTER);
		a.addStatement(PICASSO, PAINTS, GUERNICA);
		a.addStatement(PICASSO, PAINTS, JACQUELINE);
		List<Value> result = eval("painting", b, "SELECT ?painting "
				+ "WHERE { ?painter a <Painter>; <paints> ?painting "
				+ "FILTER  regex(str(?painter), \"rem\", \"i\") }");
		for (Value painting : result) {
			b.addStatement((Resource) painting, RDF.TYPE, PAINTING);
		}
		a.commit();
		b.commit();
		assertEquals(10, a.size(null, null, null, false));
	}

	public void test_conflictOptionalFilterQuery() throws Exception {
		a.addStatement(PICASSO, RDF.TYPE, PAINTER);
		a.addStatement(PICASSO, PAINTS, GUERNICA);
		a.addStatement(PICASSO, PAINTS, JACQUELINE);
		b.addStatement(REMBRANDT, RDF.TYPE, PAINTER);
		b.addStatement(REMBRANDT, PAINTS, NIGHTWATCH);
		b.addStatement(REMBRANDT, PAINTS, ARTEMISIA);
		b.addStatement(REMBRANDT, PAINTS, DANAE);
		a.begin();
		b.begin();
		a.addStatement(GUERNICA, RDF.TYPE, PAINTING);
		a.addStatement(JACQUELINE, RDF.TYPE, PAINTING);
		List<Value> result = eval("painting", b, "SELECT ?painting "
				+ "WHERE { [a <Painter>] <paints> ?painting "
				+ "OPTIONAL { ?painting a ?type  } FILTER (!bound(?type)) }");
		for (Value painting : result) {
			if (painting != null) {
				b.addStatement((Resource) painting, RDF.TYPE, PAINTING);
			}
		}
		a.commit();
		try {
			b.commit();
			fail();
		} catch (ConcurrencyException e) {
			e.printStackTrace();
		}
		assertEquals(9, a.size(null, null, null, false));
	}

	public void test_safeRangeQuery() throws Exception {
		a.addStatement(REMBRANDT, RDF.TYPE, PAINTER);
		a.addStatement(REMBRANDT, PAINTS, ARTEMISIA);
		a.addStatement(REMBRANDT, PAINTS, DANAE);
		a.addStatement(REMBRANDT, PAINTS, JACOB);
		a.addStatement(REMBRANDT, PAINTS, ANATOMY);
		a.addStatement(REMBRANDT, PAINTS, BELSHAZZAR);
		a.addStatement(BELSHAZZAR, YEAR, lf.createLiteral(1635));
		a.addStatement(ARTEMISIA, YEAR, lf.createLiteral(1634));
		a.addStatement(DANAE, YEAR, lf.createLiteral(1636));
		a.addStatement(JACOB, YEAR, lf.createLiteral(1632));
		a.addStatement(ANATOMY, YEAR, lf.createLiteral(1632));
		a.begin();
		b.begin();
		List<Value> result = eval("painting", b, "SELECT ?painting "
				+ "WHERE { <rembrandt> <paints> ?painting . ?painting <year> ?year "
				+ "FILTER  (1631 <= ?year && ?year <= 1635) }");
		for (Value painting : result) {
			b.addStatement((Resource) painting, PERIOD, lf.createLiteral("First Amsterdam period"));
		}
		a.addStatement(REMBRANDT, PAINTS, NIGHTWATCH);
		a.addStatement(NIGHTWATCH, YEAR, lf.createLiteral(1642));
		a.commit();
		b.commit();
		assertEquals(17, a.size(null, null, null, false));
	}

	public void test_conflictRangeQuery() throws Exception {
		a.addStatement(REMBRANDT, RDF.TYPE, PAINTER);
		a.addStatement(REMBRANDT, PAINTS, NIGHTWATCH);
		a.addStatement(REMBRANDT, PAINTS, ARTEMISIA);
		a.addStatement(REMBRANDT, PAINTS, DANAE);
		a.addStatement(REMBRANDT, PAINTS, JACOB);
		a.addStatement(REMBRANDT, PAINTS, ANATOMY);
		a.addStatement(ARTEMISIA, YEAR, lf.createLiteral(1634));
		a.addStatement(NIGHTWATCH, YEAR, lf.createLiteral(1642));
		a.addStatement(DANAE, YEAR, lf.createLiteral(1636));
		a.addStatement(JACOB, YEAR, lf.createLiteral(1632));
		a.addStatement(ANATOMY, YEAR, lf.createLiteral(1632));
		a.begin();
		b.begin();
		List<Value> result = eval("painting", b, "SELECT ?painting "
				+ "WHERE { <rembrandt> <paints> ?painting . ?painting <year> ?year "
				+ "FILTER  (1631 <= ?year && ?year <= 1635) }");
		for (Value painting : result) {
			b.addStatement((Resource) painting, PERIOD, lf.createLiteral("First Amsterdam period"));
		}
		a.addStatement(REMBRANDT, PAINTS, BELSHAZZAR);
		a.addStatement(BELSHAZZAR, YEAR, lf.createLiteral(1635));
		a.commit();
		try {
			b.commit();
			fail();
		} catch (ConcurrencyException e) {
			e.printStackTrace();
		}
		assertEquals(13, a.size(null, null, null, false));
	}

	private List<Value> eval(String var, SailConnection con, String qry)
			throws StoreException {
		TupleQueryModel query = parseTupleQuery(QueryLanguage.SPARQL, qry, NS);
		Cursor<? extends BindingSet> result;
		result = con.evaluate(query, EmptyBindingSet.getInstance(), false);
		try {
			List<Value> list = new ArrayList<Value>();
			BindingSet bs;
			while ((bs = result.next()) != null) {
				list.add(bs.getValue(var));
			}
			return list;
		} finally {
			result.close();
		}
	}

}
