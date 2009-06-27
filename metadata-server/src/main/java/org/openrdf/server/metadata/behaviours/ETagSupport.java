package org.openrdf.server.metadata.behaviours;

import java.util.Calendar;
import java.util.GregorianCalendar;

import javax.xml.datatype.XMLGregorianCalendar;

import org.openrdf.server.metadata.concepts.RDFResource;
import org.openrdf.server.metadata.concepts.Transaction;

public abstract class ETagSupport implements RDFResource {

	public String variantTag(String mediaType) {
		Transaction trans = getRevision();
		if (trans == null)
			return null;
		String uri = trans.getResource().stringValue();
		String revision = Integer.toHexString(uri.hashCode());
		if (mediaType == null)
			return "W/" + '"' + revision + '"';
		String variant = Integer.toHexString(mediaType.hashCode());
		return "W/" + '"' + revision + '-' + variant + '"';
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
