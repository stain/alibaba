package org.openrdf.repository.auditing.helpers;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.concurrent.atomic.AtomicLong;

import org.openrdf.model.URI;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.auditing.ActivityFactory;
import org.openrdf.repository.auditing.AuditingRepositoryConnection;

public class ActivityTagFactory implements ActivityFactory {
	private static final String uid = "t"
			+ Long.toHexString(System.currentTimeMillis()) + "x";
	private static final AtomicLong seq = new AtomicLong(0);
	private final String space;
	private String namespace;
	private GregorianCalendar date;

	public ActivityTagFactory() {
		String username = System.getProperty("user.name");
		String hostName = "localhost";
		try {
			InetAddress localMachine = InetAddress.getLocalHost();
			hostName = localMachine.getHostName();
		} catch (UnknownHostException e) {
			// ignore
		}
		space = "tag:" + username + "@" + hostName + ",";
	}

	public URI assignActivityURI(AuditingRepositoryConnection con) {
		String local = uid + seq.getAndIncrement();
		return con.getValueFactory().createURI(getNamespace(), local);
	}

	public void activityStarted(URI activityGraph, RepositoryConnection con) {
		// don't care
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
