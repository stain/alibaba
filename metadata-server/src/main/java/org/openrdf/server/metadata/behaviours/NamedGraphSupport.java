package org.openrdf.server.metadata.behaviours;

import static org.openrdf.query.QueryLanguage.SPARQL;

import java.util.HashMap;
import java.util.Map;

import org.openrdf.model.Namespace;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
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
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.server.metadata.annotations.operation;

public abstract class NamedGraphSupport implements RDFObject {

	private static final String CONSTRUCT_ALL = "CONSTRUCT {?subj ?pred ?obj}\n"
			+ "WHERE {?subj ?pred ?obj}";

	@operation("named-graph")
	public GraphQueryResult metaLoadNamedGraph() throws RepositoryException,
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

	@operation("named-graph")
	public void metaSaveNamedGraph(GraphQueryResult graph)
			throws RepositoryException, QueryEvaluationException {
		ObjectConnection con = getObjectConnection();
		URI uri = (URI) getResource();
		con.clear(uri);
		for (Map.Entry<String, String> e : graph.getNamespaces().entrySet()) {
			con.setNamespace(e.getKey(), e.getValue());
		}
		while (graph.hasNext()) {
			Statement st = graph.next();
			con.add(st, uri);
		}
	}
}
