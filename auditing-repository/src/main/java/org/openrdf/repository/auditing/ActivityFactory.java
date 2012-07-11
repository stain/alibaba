package org.openrdf.repository.auditing;

import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;

public interface ActivityFactory {

	URI createActivityURI(ValueFactory vf);

	void activityStarted(URI activityGraph, RepositoryConnection con) throws RepositoryException;

	void activityEnded(URI activityGraph, RepositoryConnection con) throws RepositoryException;
}
