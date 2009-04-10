package org.openrdf.server.metadata.behaviours;

import static java.util.Collections.singleton;
import static org.openrdf.query.QueryLanguage.SPARQL;

import java.util.Collections;
import java.util.Map;

import org.openrdf.model.Model;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.StatementImpl;
import org.openrdf.model.util.ModelOrganizer;
import org.openrdf.query.Dataset;
import org.openrdf.query.GraphQuery;
import org.openrdf.query.impl.DatasetImpl;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.result.GraphResult;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.helpers.RDFHandlerWrapper;
import org.openrdf.rio.helpers.StatementCollector;
import org.openrdf.server.metadata.annotations.purpose;
import org.openrdf.store.StoreException;

public abstract class DescribeSupport implements RDFObject {

	private static final String CONSTRUCT_ALL = "CONSTRUCT {?subj ?pred ?obj}\n"
			+ "WHERE {?subj ?pred ?obj}";

	@purpose("describe")
	public Model metaDescribe() throws StoreException, RDFHandlerException {
		final URI self = (URI) getResource();
		Dataset dataset = new DatasetImpl(singleton(self), Collections
				.<URI> emptySet());

		StatementCollector rdf = new StatementCollector();
		RepositoryConnection con = getObjectConnection();
		GraphQuery query = con.prepareGraphQuery(SPARQL, CONSTRUCT_ALL);
		query.setDataset(dataset);
		query.evaluate(new RDFHandlerWrapper(rdf) {

			@Override
			public void handleStatement(Statement st)
					throws RDFHandlerException {
				Resource s = st.getSubject();
				URI p = st.getPredicate();
				Value o = st.getObject();
				super.handleStatement(new StatementImpl(s, p, o, self));
			}
		});
		con.exportMatch(self, null, null, true, rdf);

		ModelOrganizer organizer = new ModelOrganizer(rdf.getModel());
		organizer.setSubjectOrder(self);
		return organizer.organize();
	}

	@purpose("describe")
	public void metaDescribed(GraphResult graph) throws StoreException {
		RepositoryConnection con = getObjectConnection();
		URI uri = (URI) getResource();
		con.begin();
		con.clear(uri);
		for (Map.Entry<String, String> e : graph.getNamespaces().entrySet()) {
			con.setNamespace(e.getKey(), e.getValue());
		}
		Statement st;
		while ((st = graph.next()) != null) {
			con.add(st, uri);
		}
		con.commit();
	}
}
