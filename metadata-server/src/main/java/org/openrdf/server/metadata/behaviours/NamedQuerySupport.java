package org.openrdf.server.metadata.behaviours;

import static org.openrdf.query.QueryLanguage.SPARQL;

import java.net.URISyntaxException;

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
			throws StoreException, URISyntaxException {
		String sparql = getMetaInSparql();
		RepositoryConnection con = getObjectConnection();
		ValueFactory vf = con.getValueFactory();
		Query query = con.prepareQuery(SPARQL, sparql);
		for (Parameter parameter : getMetaParameters()) {
			String name = parameter.getMetaName();
			String value = parameters.getFirst(name);
			if (value == null)
				continue;
			RDFObject base = (RDFObject) parameter.getMetaBase();
			RDFObject datatype = (RDFObject) parameter.getMetaDatatype();
			if (datatype != null) {
				Resource dt = datatype.getResource();
				query.setBinding(name, vf.createLiteral(value, (URI) dt));
			} else if (base != null) {
				String baseURI = ((RDFObject) base).getResource().stringValue();
				if (baseURI.endsWith("#")) {
					query.setBinding(name, vf.createURI(baseURI, value));
				} else {
					java.net.URI uri = new java.net.URI(baseURI);
					uri = uri.resolve(value);
					query.setBinding(name, vf.createURI(uri.toASCIIString()));
				}
			} else {
				query.setBinding(name, vf.createLiteral(value));
			}
		}
		return query.evaluate();
	}
}