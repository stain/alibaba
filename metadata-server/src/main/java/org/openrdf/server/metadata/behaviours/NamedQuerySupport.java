package org.openrdf.server.metadata.behaviours;

import static org.openrdf.query.QueryLanguage.SPARQL;

import java.net.URISyntaxException;

import javax.ws.rs.core.MultivaluedMap;

import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.BooleanQuery;
import org.openrdf.query.GraphQuery;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.Query;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQuery;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.server.metadata.annotations.parameter;
import org.openrdf.server.metadata.annotations.purpose;
import org.openrdf.server.metadata.concepts.NamedQuery;
import org.openrdf.server.metadata.concepts.Parameter;

public abstract class NamedQuerySupport implements NamedQuery, RDFObject {

	@purpose("evaluate")
	public Object metaEvaluate(
			@parameter MultivaluedMap<String, String> parameters)
			throws RepositoryException, URISyntaxException,
			QueryEvaluationException, MalformedQueryException {
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
				if (baseURI.endsWith("#") || baseURI.endsWith("/")) {
					query.setBinding(name, vf.createURI(baseURI, value));
				} else if (getResource() instanceof URI) {
					java.net.URI uri = new java.net.URI(baseURI);
					uri = uri.resolve(value);
					query.setBinding(name, vf.createURI(uri.toASCIIString()));
				} else {
					query.setBinding(name, vf.createURI(value));
				}
			} else {
				query.setBinding(name, vf.createLiteral(value));
			}
		}
		if (query instanceof TupleQuery) {
			return ((TupleQuery) query).evaluate();
		} else if (query instanceof GraphQuery) {
			return ((GraphQuery) query).evaluate();
		} else if (query instanceof BooleanQuery) {
			return ((BooleanQuery) query).evaluate();
		} else {
			throw new IllegalStateException();
		}
	}
}
