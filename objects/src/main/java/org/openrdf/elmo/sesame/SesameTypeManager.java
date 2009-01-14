package org.openrdf.elmo.sesame;

import static org.openrdf.query.QueryLanguage.SPARQL;

import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.contextaware.ContextAwareConnection;

public class SesameTypeManager {

	private ContextAwareConnection conn;

	public SesameTypeManager(ContextAwareConnection conn) {
		setConnection(conn);
	}

	public void setConnection(ContextAwareConnection conn) {
		this.conn = conn;
	}

	public RepositoryResult<Statement> getTypeStatements(Resource res)
			throws RepositoryException {
		return conn.getStatements(res, RDF.TYPE, null);
	}

	public TupleQueryResult evaluateTypeQuery(String qry)
			throws MalformedQueryException, RepositoryException,
			QueryEvaluationException {
		TupleQuery q;
		q = conn.prepareTupleQuery(SPARQL, qry, null);
		return q.evaluate();
	}

	public void addTypeStatement(Resource resource, URI type)
			throws RepositoryException {
		if (!RDFS.RESOURCE.equals(type)) {
			conn.add(resource, RDF.TYPE, type);
		}
	}

	public void removeTypeStatement(Resource resource, URI type)
			throws RepositoryException {
		conn.remove(resource, RDF.TYPE, type);
	}

	public void removeResource(Resource resource) {
		// types are removed with other properties
	}

	public void renameResource(Resource before, Resource after) {
		// types are renamed with other properties
	}
}
