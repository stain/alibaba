package org.openrdf.alibaba;


import java.util.Collections;
import java.util.Set;

import junit.framework.TestCase;

import org.openrdf.alibaba.concepts.Display;
import org.openrdf.alibaba.concepts.Expression;
import org.openrdf.alibaba.concepts.ExpressionRepository;
import org.openrdf.alibaba.concepts.Format;
import org.openrdf.alibaba.concepts.LiteralDisplay;
import org.openrdf.alibaba.concepts.OrderByExpression;
import org.openrdf.alibaba.concepts.OrderByRepository;
import org.openrdf.alibaba.concepts.SearchPattern;
import org.openrdf.alibaba.vocabulary.ALI;
import org.openrdf.concepts.rdf.Seq;
import org.openrdf.elmo.ElmoManager;
import org.openrdf.elmo.sesame.SesameManagerFactory;
import org.openrdf.repository.Repository;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.memory.MemoryStore;

public class ExpressionTest extends TestCase {
	private static final Set EMPTY_SET = Collections.EMPTY_SET;

	private static final String SELECT_TYPE = "SELECT ?label, ?comment, ?type WHERE { ?type a owl:Class ; rdfs:label ?label ; rdfs:comment ?comment .";

	private static final String TYPE_SUPER = "?type rdfs:subClassOf ?super .";

	private static final String QUERY_EMPTY = "SELECT ?label, ?comment, ?type WHERE { ?type a owl:Class ; rdfs:label ?label ; rdfs:comment ?comment . } ORDER BY ?label ";

	private static final String QUERY_COMMENT = "SELECT ?label, ?comment, ?type WHERE { ?type a owl:Class ; rdfs:label ?label ; rdfs:comment ?comment . } ORDER BY ?comment ";

	private static final String QUERY_SUPER = "SELECT ?label, ?comment, ?type WHERE { ?type a owl:Class ; rdfs:label ?label ; rdfs:comment ?comment . ?type rdfs:subClassOf ?super . } ORDER BY ?label ";

	private Repository repository;

	private ElmoManager manager;

	public void testOrderByExpressionRepository() throws Exception {
		SearchPattern pattern = createSearchPattern();
		assertEquals(QUERY_EMPTY, pattern.getSparqlQueryString(EMPTY_SET, null));
		assertEquals(QUERY_COMMENT, pattern.getSparqlQueryString(EMPTY_SET,
				"comment"));
		assertEquals(QUERY_SUPER, pattern.getSparqlQueryString(Collections
				.singleton("super"), null));
		for (Display display : pattern.getPovDisplays()) {
			assertNotNull(pattern.getPovOrderByExpressions().findAscending(
					display));
		}
	}

	private SearchPattern createSearchPattern() {
		SearchPattern p = manager.create(SearchPattern.class);
		p.setPovSelectExpression(sparql(SELECT_TYPE));
		p.setPovFilterExpressions(createRepository(TYPE_SUPER));
		p.setPovGroupByExpression(sparql("}"));
		p.setPovOrderByExpressions(createOrderByRepository("ORDER BY ?label",
				"ORDER BY ?comment", "ORDER BY ?type"));
		Seq<Display> seq = manager.create(Seq.class);
		boolean autoFlush = manager.isAutoFlush();
		manager.setAutoFlush(false);
		for (Expression expr : p.getPovOrderByExpressions()) {
			OrderByExpression order = (OrderByExpression) expr;
			Display display = createDisplay();
			order.getPovAscendings().add(display);
			seq.add(display);
		}
		manager.setAutoFlush(autoFlush);
		p.setPovDisplays(seq);
		return p;
	}

	private Display createDisplay() {
		LiteralDisplay display = manager.create(LiteralDisplay.class);
		display.setPovFormat(manager.create(Format.class, ALI.NONE));
		return display;
	}

	private ExpressionRepository createRepository(String... filters) {
		ExpressionRepository repo = manager.create(ExpressionRepository.class);
		for (String filter : filters) {
			repo.getPovRegisteredExpressions().add(sparql(filter));
		}
		return repo;
	}

	private OrderByRepository createOrderByRepository(
			String... orderBys) {
		OrderByRepository repo = manager
				.create(OrderByRepository.class);
		for (String orderBy : orderBys) {
			OrderByExpression expr = manager.create(OrderByExpression.class);
			expr.setPovInSparql(orderBy);
			String[] split = orderBy.split("\\W+");
			expr.setPovName(split[split.length - 1]);
			repo.getPovRegisteredExpressions().add(expr);
			if (repo.getPovDefaultExpression() == null) {
				repo.setPovDefaultExpression(expr);
			}
		}
		return repo;
	}

	private Expression sparql(String sparql) {
		Expression expr = manager.create(Expression.class);
		expr.setPovInSparql(sparql);
		String[] split = sparql.split("\\W+");
		if (split.length > 0) {
			expr.setPovName(split[split.length - 1]);
		}
		return expr;
	}

	@Override
	protected void setUp() throws Exception {
		repository = new SailRepository(new MemoryStore());
		repository.initialize();
		manager = new SesameManagerFactory(repository).createElmoManager();
	}

	@Override
	protected void tearDown() throws Exception {
		manager.close();
		repository.shutDown();
	}
}
