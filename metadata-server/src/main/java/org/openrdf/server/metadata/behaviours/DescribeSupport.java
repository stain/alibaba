package org.openrdf.server.metadata.behaviours;

import static org.openrdf.query.QueryLanguage.SPARQL;

import java.util.Map;

import org.openrdf.model.Model;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.ContextStatementImpl;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.query.GraphQuery;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.impl.DatasetImpl;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.helpers.RDFHandlerWrapper;
import org.openrdf.rio.helpers.StatementCollector;
import org.openrdf.server.metadata.annotations.purpose;

public abstract class DescribeSupport implements RDFObject {

	private static final String CONSTRUCT_ALL = "CONSTRUCT {?subj ?pred ?obj}\n"
			+ "WHERE {?subj ?pred ?obj}";

	@purpose("describe")
	public Model metaDescribe() throws RepositoryException, RDFHandlerException, QueryEvaluationException {
		final URI self = (URI) getResource();
		DatasetImpl dataset = new DatasetImpl();
		dataset.addDefaultGraph(self);

		Model model = new LinkedHashModel();
		StatementCollector rdf = new StatementCollector(model, model.getNamespaces());
		RepositoryConnection con = getObjectConnection();
		GraphQuery query;
		try {
			query = con.prepareGraphQuery(SPARQL, CONSTRUCT_ALL);
		} catch (MalformedQueryException e) {
			throw new AssertionError(e);
		}
		query.setDataset(dataset);
		query.evaluate(new RDFHandlerWrapper(rdf) {

			@Override
			public void handleStatement(Statement st)
					throws RDFHandlerException {
				Resource s = st.getSubject();
				URI p = st.getPredicate();
				Value o = st.getObject();
				super.handleStatement(new ContextStatementImpl(s, p, o, self));
			}
		});
		con.exportStatements(self, null, null, true, rdf);

		return model;
	}

	@purpose("describe")
	public void metaDescribed(GraphQueryResult graph) throws RepositoryException, QueryEvaluationException {
		ObjectConnection con = getObjectConnection();
		URI uri = (URI) getResource();
		con.begin();
		con.clear(uri);
		for (Map.Entry<String, String> e : graph.getNamespaces().entrySet()) {
			con.setNamespace(e.getKey(), e.getValue());
		}
		while (graph.hasNext()) {
			Statement st = graph.next();
			con.add(st, uri);
		}
		con.end();
	}
}
