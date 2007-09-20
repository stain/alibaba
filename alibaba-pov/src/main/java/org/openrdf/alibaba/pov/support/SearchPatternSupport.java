package org.openrdf.alibaba.pov.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openrdf.alibaba.exceptions.AlibabaException;
import org.openrdf.alibaba.exceptions.BadRequestException;
import org.openrdf.alibaba.formats.Format;
import org.openrdf.alibaba.pov.Display;
import org.openrdf.alibaba.pov.Expression;
import org.openrdf.alibaba.pov.ExpressionRepository;
import org.openrdf.alibaba.pov.OrderByRepository;
import org.openrdf.alibaba.pov.SearchPattern;
import org.openrdf.alibaba.pov.SearchPatternBehaviour;
import org.openrdf.alibaba.vocabulary.POV;
import org.openrdf.elmo.ElmoQuery;
import org.openrdf.elmo.annotations.rdf;

@rdf(POV.NS + "SearchPattern")
public class SearchPatternSupport implements SearchPatternBehaviour {
	private static final char EXPR_SEP = ' ';

	protected static abstract class QueryBuilder {
		private Set<String> filters;

		private String orderBy;

		public QueryBuilder(Set<String> filters, String orderBy) {
			this.filters = filters;
			this.orderBy = orderBy;
		}

		public abstract void append(Expression expr);

		public Set<String> getFilters() {
			return filters;
		}

		public String getOrderBy() {
			return orderBy;
		}
	}

	private SearchPattern qry;

	public SearchPatternSupport(SearchPattern qry) {
		this.qry = qry;
	}

	public List<Display> getBindings(Set<String> filters, String orderBy)
			throws AlibabaException {
		final List<Display> bindings = new ArrayList<Display>();
		buildQuery(new QueryBuilder(filters, orderBy) {
			@Override
			public void append(Expression expr) {
				for (Display binding : expr.getPovBindings()) {
					bindings.add(binding);
				}
			}
		});
		return bindings;
	}

	public String getJpqlQueryString(Set<String> filters, String orderBy)
			throws AlibabaException {
		final StringBuilder sb = new StringBuilder();
		buildQuery(new QueryBuilder(filters, orderBy) {
			@Override
			public void append(Expression expr) {
				sb.append(expr.getPovInJpql()).append(EXPR_SEP);
			}
		});
		return sb.toString();
	}

	public String getSerqlQueryString(Set<String> filters, String orderBy)
			throws AlibabaException {
		final StringBuilder sb = new StringBuilder();
		buildQuery(new QueryBuilder(filters, orderBy) {
			@Override
			public void append(Expression expr) {
				sb.append(expr.getPovInSerql()).append(EXPR_SEP);
			}
		});
		return sb.toString();
	}

	public String getSparqlQueryString(final Set<String> filters,
			final String orderBy) throws AlibabaException {
		final StringBuilder sb = new StringBuilder();
		buildQuery(new QueryBuilder(filters, orderBy) {
			@Override
			public void append(Expression expr) {
				sb.append(expr.getPovInSparql()).append(EXPR_SEP);
			}
		});
		return sb.toString();
	}

	public String getSqlQueryString(Set<String> filters, String orderBy)
			throws AlibabaException {
		final StringBuilder sb = new StringBuilder();
		buildQuery(new QueryBuilder(filters, orderBy) {
			@Override
			public void append(Expression expr) {
				sb.append(expr.getPovInSql()).append(EXPR_SEP);
			}
		});
		return sb.toString();
	}

	public ElmoQuery<?> createElmoQuery(Map<String, String> filter,
			String orderBy) throws AlibabaException {
		Set<String> filters = filter.keySet();
		String queryString = qry.getSparqlQueryString(filters, orderBy);
		ElmoQuery<?> query = qry.getElmoManager().createQuery(queryString);
		for (Display binding : qry.getBindings(filters, orderBy)) {
			String name = binding.getPovName();
			Format format = binding.getPovFormat();
			if (!filter.containsKey(name))
				throw new BadRequestException(name);
			String value = filter.get(name);
			query.setParameter(name, format.parse(value));
		}
		return query;
	}

	protected void buildQuery(QueryBuilder builder) throws AlibabaException {
		buildSelectExpression(builder);
		buildFilterExpressions(builder);
		buildGroupByExpression(builder);
		buildOrderByExpression(builder);
	}

	private void buildSelectExpression(QueryBuilder builder) {
		Expression expr = qry.getPovSelectExpression();
		if (expr != null) {
			builder.append(expr);
		}
	}

	private void buildFilterExpressions(QueryBuilder builder) {
		Set<String> filters = builder.getFilters();
		ExpressionRepository exprs = qry.getPovFilterExpressions();
		if (exprs != null) {
			for (Expression expr : exprs.findByNames(filters)) {
				builder.append(expr);
			}
		}
	}

	private void buildGroupByExpression(QueryBuilder builder) {
		Expression expr = qry.getPovGroupByExpression();
		if (expr != null) {
			builder.append(expr);
		}
	}

	private void buildOrderByExpression(QueryBuilder builder) {
		OrderByRepository exprs = qry.getPovOrderByExpressions();
		if (exprs != null) {
			Expression expr = exprs.findByName(builder.getOrderBy());
			if (expr != null) {
				builder.append(expr);
			}
		}
	}
}
