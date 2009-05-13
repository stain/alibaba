package org.openrdf.server.metadata.behaviours;

import static org.openrdf.query.QueryLanguage.SPARQL;

import java.util.HashMap;
import java.util.Map;

import org.openrdf.model.Namespace;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.query.GraphQuery;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.impl.DatasetImpl;
import org.openrdf.query.impl.GraphQueryResultImpl;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.server.metadata.annotations.operation;
import org.openrdf.server.metadata.annotations.rel;
import org.openrdf.server.metadata.annotations.title;
import org.openrdf.server.metadata.annotations.type;
import org.openrdf.server.metadata.concepts.WebResource;

public abstract class MetadataSupport implements WebResource {

	private static final String CONSTRUCT_ALL = "CONSTRUCT {?subj ?pred ?obj}\n"
			+ "WHERE {?subj ?pred ?obj}";

	@rel("alternate")
	@title("RDF Metadata")
	@operation("metadata")
	@type( { "application/rdf+xml", "application/x-turtle", "text/rdf+n3",
			"application/trix", "application/x-trig" })
	public GraphQueryResult metadata() throws RepositoryException,
			RDFHandlerException, QueryEvaluationException,
			MalformedQueryException {
		Resource self = getResource();
		if (self instanceof URI) {
			DatasetImpl dataset = new DatasetImpl();
			dataset.addDefaultGraph((URI) self);

			RepositoryConnection con = getObjectConnection();
			GraphQuery query = con.prepareGraphQuery(SPARQL, CONSTRUCT_ALL);
			query.setDataset(dataset);

			// Use the namespaces of the repository (not the query)
			RepositoryResult<Namespace> namespaces = con.getNamespaces();
			Map<String, String> map = new HashMap<String, String>();
			while (namespaces.hasNext()) {
				Namespace ns = namespaces.next();
				map.put(ns.getPrefix(), ns.getName());
			}
			return new GraphQueryResultImpl(map, query.evaluate());
		} else {
			return null;
		}
	}
}
