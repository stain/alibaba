package org.openrdf.server.metadata.behaviours;

import static org.openrdf.query.QueryLanguage.SPARQL;

import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.result.Result;
import org.openrdf.server.metadata.annotations.purpose;
import org.openrdf.server.metadata.concepts.NamedQuery;
import org.openrdf.store.StoreException;

public abstract class NamedQuerySupport implements NamedQuery, RDFObject {

	@purpose("evaluate")
	public Result<?> evaluate() throws StoreException {
		String sparql = getInSparql();
		RepositoryConnection con = getObjectConnection();
		return con.prepareQuery(SPARQL, sparql).evaluate();
	}
}
