package org.openrdf.server.metadata.behaviours;

import static org.openrdf.query.QueryLanguage.SPARQL;

import org.openrdf.query.GraphQuery;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.server.metadata.annotations.purpose;

public abstract class DescribeSupport implements RDFObject {

	private static final String DESCRIBE_SELF = "CONSTRUCT {$self ?pred ?obj}\n"
			+ "WHERE {$self ?pred ?obj}";

	@purpose("describe")
	public GraphQueryResult metaDescribe() throws RepositoryException,
			RDFHandlerException, QueryEvaluationException,
			MalformedQueryException {
		RepositoryConnection con = getObjectConnection();
		GraphQuery query = con.prepareGraphQuery(SPARQL, DESCRIBE_SELF);
		query.setBinding("self", getResource());
		return query.evaluate();
	}
}
