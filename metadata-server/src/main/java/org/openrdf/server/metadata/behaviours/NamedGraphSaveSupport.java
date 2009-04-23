package org.openrdf.server.metadata.behaviours;

import java.util.Map;

import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.server.metadata.annotations.operation;
import org.openrdf.server.metadata.concepts.NamedGraph;

public abstract class NamedGraphSaveSupport implements RDFObject {

	@operation("named-graph")
	public void metaSaveNamedGraph(GraphQueryResult graph)
			throws RepositoryException, QueryEvaluationException {
		ObjectConnection con = getObjectConnection();
		URI uri = (URI) getResource();
		con.clear(uri);
		if (graph == null) {
			con.removeType(this, NamedGraph.class);
		} else {
			con.addType(this, NamedGraph.class);
			for (Map.Entry<String, String> e : graph.getNamespaces().entrySet()) {
				con.setNamespace(e.getKey(), e.getValue());
			}
			while (graph.hasNext()) {
				Statement st = graph.next();
				con.add(st, uri);
			}
		}
	}
}
