package org.openrdf.repository.auditing.helpers;

import java.util.concurrent.atomic.AtomicLong;

import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.auditing.ActivityFactory;

public class ActivitySequenceFactory implements ActivityFactory {
	private static final String ACTIVITY = "http://www.w3.org/ns/prov#Activity";
	private final String uid = "t"
			+ Long.toHexString(System.currentTimeMillis()) + "x";
	private final AtomicLong seq = new AtomicLong(0);
	private final String ns;

	public ActivitySequenceFactory(String ns) {
		this.ns = ns;
	}

	public URI createActivityURI(ValueFactory vf) {
		return vf.createURI(ns, uid + seq.getAndIncrement());
	}

	public void activityStarted(URI activityGraph, RepositoryConnection con)
			throws RepositoryException {
		ValueFactory vf = con.getValueFactory();
		con.add(activityGraph, RDF.TYPE, vf.createURI(ACTIVITY), activityGraph);
	}

	public void activityEnded(URI activityGraph, RepositoryConnection con) {
		// don't care
	}

}
