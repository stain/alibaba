package org.openrdf.repository.auditing.helpers;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.concurrent.atomic.AtomicLong;

import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.auditing.ActivityFactory;

public class ActivityTagFactory implements ActivityFactory {
	private static final String ACTIVITY = "http://www.w3.org/ns/prov#Activity";
	private static final String uid = "t"
			+ Long.toHexString(System.currentTimeMillis()) + "x";
	private static final AtomicLong seq = new AtomicLong(0);
	private final String space;
	private String namespace;
	private GregorianCalendar date;

	public ActivityTagFactory() {
		String userinfo = System.getProperty("user.name");
		if (userinfo != null) {
			userinfo = userinfo.replaceAll("^[^a-zA-Z0-9.-]+", "");
			userinfo = userinfo.replaceAll("[^a-zA-Z0-9.-]+$", "");
			userinfo = userinfo.replaceAll("[^a-zA-Z0-9.-]", "_");
		}
		if (userinfo == null || userinfo.length() == 0) {
			userinfo = "";
		} else {
			userinfo = userinfo + "@";
		}
		String authority = "localhost";
		try {
			InetAddress localMachine = InetAddress.getLocalHost();
			authority = localMachine.getHostName();
			authority = authority.replaceAll("^[^a-zA-Z0-9.-]+", "");
			authority = authority.replaceAll("[^a-zA-Z0-9.-]+$", "");
			authority = authority.replaceAll("[^a-zA-Z0-9.-]", "_");
			authority = authority.toLowerCase();
		} catch (UnknownHostException e) {
			// ignore
		}
		space = "tag:" + userinfo + authority + ",";
	}

	public URI createActivityURI(ValueFactory vf) {
		String local = uid + seq.getAndIncrement();
		return vf.createURI(getNamespace(), local);
	}

	public void activityStarted(URI activityGraph, RepositoryConnection con)
			throws RepositoryException {
		ValueFactory vf = con.getValueFactory();
		con.add(activityGraph, RDF.TYPE, vf.createURI(ACTIVITY), activityGraph);
	}

	public void activityEnded(URI activityGraph, RepositoryConnection con) {
		// don't care
	}

	private synchronized String getNamespace() {
		GregorianCalendar cal = new GregorianCalendar();
		if (date == null || date.get(Calendar.DATE) != cal.get(Calendar.DATE)
				|| date.get(Calendar.MONTH) != cal.get(Calendar.MONTH)
				|| date.get(Calendar.YEAR) != cal.get(Calendar.YEAR)) {
			date = cal;
			return namespace = space + date.get(Calendar.YEAR) + "-"
					+ zero(date.get(Calendar.MONTH) + 1) + "-"
					+ zero(date.get(Calendar.DATE)) + ":";
		}
		return namespace;
	}

	private String zero(int number) {
		if (number < 10)
			return "0" + number;
		return String.valueOf(number);
	}

}
