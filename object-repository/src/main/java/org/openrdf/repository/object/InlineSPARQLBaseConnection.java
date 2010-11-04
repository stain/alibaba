package org.openrdf.repository.object;

import java.util.regex.Pattern;

import org.openrdf.query.BooleanQuery;
import org.openrdf.query.GraphQuery;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.Query;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.base.RepositoryConnectionWrapper;

/**
 * This is a work around for http://www.openrdf.org/issues/browse/SES-721
 * 
 * @author James Leigh
 * 
 */
public class InlineSPARQLBaseConnection extends RepositoryConnectionWrapper {

	private static final Pattern BASE = Pattern.compile("\\bBASE\\b",
			Pattern.CASE_INSENSITIVE);

	public InlineSPARQLBaseConnection(Repository repository,
			RepositoryConnection delegate) {
		super(repository, delegate);
	}

	@Override
	public BooleanQuery prepareBooleanQuery(QueryLanguage ql, String query,
			String b) throws MalformedQueryException, RepositoryException {
		return super.prepareBooleanQuery(ql, inlineBase(ql, query, b), b);
	}

	@Override
	public GraphQuery prepareGraphQuery(QueryLanguage ql, String query, String b)
			throws MalformedQueryException, RepositoryException {
		return super.prepareGraphQuery(ql, inlineBase(ql, query, b), b);
	}

	@Override
	public Query prepareQuery(QueryLanguage ql, String query, String b)
			throws MalformedQueryException, RepositoryException {
		return super.prepareQuery(ql, inlineBase(ql, query, b), b);
	}

	@Override
	public TupleQuery prepareTupleQuery(QueryLanguage ql, String query, String b)
			throws MalformedQueryException, RepositoryException {
		return super.prepareTupleQuery(ql, inlineBase(ql, query, b), b);
	}

	private String inlineBase(QueryLanguage ql, String query, String b) {
		if (ql == QueryLanguage.SPARQL && b != null && b.contains(":")) {
			if (!BASE.matcher(query).find())
				return "BASE <" + b + "> " + query;
		}
		return query;
	}

}
