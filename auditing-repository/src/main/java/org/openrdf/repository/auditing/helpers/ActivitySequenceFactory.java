package org.openrdf.repository.auditing.helpers;

import java.util.concurrent.atomic.AtomicLong;

import org.openrdf.model.URI;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.auditing.ActivityFactory;
import org.openrdf.repository.auditing.AuditingRepositoryConnection;

public class ActivitySequenceFactory implements ActivityFactory {
	private final String uid = "t"
			+ Long.toHexString(System.currentTimeMillis()) + "x";
	private final AtomicLong seq = new AtomicLong(0);
	private final String ns;

	public ActivitySequenceFactory(String ns) {
		this.ns = ns;
	}

	public URI assignActivityURI(AuditingRepositoryConnection con) {
		return con.getValueFactory().createURI(ns, uid + seq.getAndIncrement());
	}

	public void activityStarted(URI activityGraph, RepositoryConnection con) {
		// don't care
	}

	public void activityEnded(URI activityGraph, RepositoryConnection con) {
		// don't care
	}

}
