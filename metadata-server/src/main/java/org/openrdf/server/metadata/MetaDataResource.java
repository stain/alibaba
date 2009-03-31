package org.openrdf.server.metadata;

import static java.util.Collections.singleton;
import static org.openrdf.query.QueryLanguage.SPARQL;

import java.util.Collections;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.openrdf.cursor.CollectionCursor;
import org.openrdf.cursor.EmptyCursor;
import org.openrdf.http.protocol.exceptions.NotFound;
import org.openrdf.model.Model;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.util.ModelOrganizer;
import org.openrdf.query.Dataset;
import org.openrdf.query.GraphQuery;
import org.openrdf.query.impl.DatasetImpl;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.result.GraphResult;
import org.openrdf.result.ModelResult;
import org.openrdf.result.impl.ModelResultImpl;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.helpers.StatementCollector;
import org.openrdf.store.StoreException;

@Path("{path:.*}")
public class MetaDataResource {

	private static final String CONSTRUCT_ALL = "CONSTRUCT {?subj ?pred ?obj}\n"
			+ "WHERE {?subj ?pred ?obj}";

	private Repository repository;

	public MetaDataResource(Repository repository) {
		this.repository = repository;
	}

	@HEAD
	public ModelResult head(@Context UriInfo info) throws StoreException,
			RDFHandlerException, NotFound {
		URI uri = new URIImpl(info.getAbsolutePath().toASCIIString());
		Dataset dataset = new DatasetImpl(singleton(uri), Collections
				.<URI> emptySet());

		RepositoryConnection con = repository.getConnection();
		try {
			if (con.hasMatch(uri, null, null, true)) {
				return new ModelResultImpl(new EmptyCursor<Statement>());
			} else {
				GraphQuery query = con.prepareGraphQuery(SPARQL, CONSTRUCT_ALL
						+ "\nLIMIT 1");
				query.setDataset(dataset);
				if (!query.evaluate().asList().isEmpty()) {
					return new ModelResultImpl(new EmptyCursor<Statement>());
				}
			}
		} finally {
			con.close();
		}

		throw new NotFound("Not Found <" + uri.stringValue() + ">");
	}

	@GET
	public ModelResult get(@Context UriInfo info) throws StoreException,
			RDFHandlerException, NotFound {
		URI uri = new URIImpl(info.getAbsolutePath().toASCIIString());
		Dataset dataset = new DatasetImpl(singleton(uri), Collections
				.<URI> emptySet());

		StatementCollector rdf = new StatementCollector();
		RepositoryConnection con = repository.getConnection();
		try {
			GraphQuery query = con.prepareGraphQuery(SPARQL, CONSTRUCT_ALL);
			query.setDataset(dataset);
			query.evaluate(rdf);
			con.exportMatch(uri, null, null, true, rdf);
		} finally {
			con.close();
		}

		if (rdf.isEmpty()) {
			throw new NotFound("Not Found <" + uri.stringValue() + ">");
		}

		ModelOrganizer organizer = new ModelOrganizer(rdf.getModel());
		organizer.setSubjectOrder(uri);
		Model organized = organizer.organize();
		return new ModelResultImpl(new CollectionCursor<Statement>(organized));
	}

	@PUT
	public void put(@Context UriInfo info, GraphResult graph)
			throws StoreException {
		URI uri = new URIImpl(info.getAbsolutePath().toASCIIString());

		RepositoryConnection con = repository.getConnection();
		try {
			con.begin();
			con.clear(uri);
			Statement st;
			while ((st = graph.next()) != null) {
				con.add(st, uri);
			}
			con.commit();
		} finally {
			con.close();
		}
	}

	@DELETE
	public void delete(@Context UriInfo info) throws StoreException {
		URI uri = new URIImpl(info.getAbsolutePath().toASCIIString());

		RepositoryConnection con = repository.getConnection();
		try {
			con.clear(uri);
		} finally {
			con.close();
		}
	}
}
