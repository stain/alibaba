package org.openrdf.repository.auditing;

import org.openrdf.model.URI;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;

public interface ActivityFactory {

	URI assignActivityURI(AuditingRepositoryConnection con);

	void activityStarted(URI activityGraph, RepositoryConnection con) throws RepositoryException;

	void activityEnded(URI activityGraph, RepositoryConnection con) throws RepositoryException;
}
