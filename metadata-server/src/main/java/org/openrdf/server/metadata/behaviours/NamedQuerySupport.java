package org.openrdf.server.metadata.behaviours;

import static org.openrdf.query.QueryLanguage.SPARQL;

import javax.ws.rs.core.MultivaluedMap;

import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.Query;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.result.Result;
import org.openrdf.server.metadata.annotations.parameter;
import org.openrdf.server.metadata.annotations.purpose;
import org.openrdf.server.metadata.concepts.NamedQuery;
import org.openrdf.server.metadata.concepts.Parameter;
import org.openrdf.store.StoreException;

public abstract class NamedQuerySupport implements NamedQuery, RDFObject {

	@purpose("evaluate")
	public Result<?> metaEvaluate(
			@parameter MultivaluedMap<String, String> parameters)
			throws StoreException {
		String sparql = getMetaInSparql();
		RepositoryConnection con = getObjectConnection();
		ValueFactory vf = con.getValueFactory();
		Query query = con.prepareQuery(SPARQL, sparql);
		for (Parameter parameter : getMetaParameters()) {
			String name = parameter.getMetaName();
			String value = parameters.getFirst(name);
			Object ns = parameter.getMetaNamespace();
			Object range = parameter.getMetaRange();
			if (ns == null) {
				Resource datatype = ((RDFObject) range).getResource();
				query.setBinding(name, vf.createLiteral(value, (URI) datatype));
			} else {
				query.setBinding(name, vf.createURI(ns.toString(), value));
			}
		}
		return query.evaluate();
	}
}
