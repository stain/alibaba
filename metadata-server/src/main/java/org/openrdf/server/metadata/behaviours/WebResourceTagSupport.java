package org.openrdf.server.metadata.behaviours;

import java.util.Calendar;
import java.util.GregorianCalendar;

import javax.xml.datatype.XMLGregorianCalendar;

import org.openrdf.server.metadata.concepts.RDFResource;
import org.openrdf.server.metadata.concepts.Transaction;

public abstract class WebResourceTagSupport implements RDFResource {

	public String eTag() {
		Transaction trans = getRevision();
		if (trans == null)
			return null;
		String uri = trans.getResource().stringValue();
		return "W/" + '"' + Integer.toHexString(uri.hashCode()) + '"';
	}

	public long lastModified() {
		Transaction trans = getRevision();
		if (trans == null)
			return 0;
		XMLGregorianCalendar xgc = trans.getCommittedOn();
		if (xgc == null)
			return 0;
		GregorianCalendar cal = xgc.toGregorianCalendar();
		cal.set(Calendar.MILLISECOND, 0);
		return cal.getTimeInMillis();
	}

}
