package org.openrdf.script.evaluation;

import org.openrdf.cursor.Cursor;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.algebra.evaluation.TripleSource;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.store.StoreException;

public class RepositoryTripleSource implements TripleSource {

	private RepositoryConnection connection;

	private boolean inf;

	public RepositoryTripleSource(RepositoryConnection connection, boolean inf) {
		this.connection = connection;
		this.inf = inf;
	}

	public Cursor<? extends Statement> getStatements(Resource subj, URI pred,
			Value obj, Resource... contexts) throws StoreException {
		return connection.match(subj, pred, obj, inf, contexts);
	}

	public ValueFactory getValueFactory() {
		return connection.getValueFactory();
	}

}
