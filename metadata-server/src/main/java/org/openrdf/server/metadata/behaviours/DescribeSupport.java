package org.openrdf.server.metadata.behaviours;

import static java.util.Collections.singleton;
import static org.openrdf.query.QueryLanguage.SPARQL;

import java.util.Collections;

import org.openrdf.model.Model;
import org.openrdf.model.URI;
import org.openrdf.model.util.ModelOrganizer;
import org.openrdf.query.Dataset;
import org.openrdf.query.GraphQuery;
import org.openrdf.query.impl.DatasetImpl;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.helpers.StatementCollector;
import org.openrdf.server.metadata.annotations.purpose;
import org.openrdf.store.StoreException;

public abstract class DescribeSupport implements RDFObject {

	private static final String CONSTRUCT_ALL = "CONSTRUCT {?subj ?pred ?obj}\n"
			+ "WHERE {?subj ?pred ?obj}";

	@purpose("describe")
	public Model describe() throws StoreException, RDFHandlerException {
		URI self = (URI) getResource();
		Dataset dataset = new DatasetImpl(singleton(self), Collections
				.<URI> emptySet());

		StatementCollector rdf = new StatementCollector();
		RepositoryConnection con = getObjectConnection();
		GraphQuery query = con.prepareGraphQuery(SPARQL, CONSTRUCT_ALL);
		query.setDataset(dataset);
		query.evaluate(rdf);
		con.exportMatch(self, null, null, true, rdf);

		ModelOrganizer organizer = new ModelOrganizer(rdf.getModel());
		organizer.setSubjectOrder(self);
		return organizer.organize();
	}
}
