package org.openrdf.http.object.behaviours;

import org.openrdf.http.object.concepts.Transaction;
import org.openrdf.http.object.concepts.VersionedObject;
import org.openrdf.repository.object.ObjectFactory;
import org.openrdf.sail.auditing.vocabulary.Audit;

public abstract class VersionedObjectSupport implements VersionedObject {

	public void touchRevision() {
		ObjectFactory of = getObjectConnection().getObjectFactory();
		setRevision(of.createObject(Audit.CURRENT_TRX, Transaction.class));
	}

}
