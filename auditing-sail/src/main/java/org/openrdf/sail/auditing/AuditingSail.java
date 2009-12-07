package org.openrdf.sail.auditing;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.concurrent.atomic.AtomicLong;

import javax.xml.datatype.DatatypeConfigurationException;

import org.openrdf.model.URI;
import org.openrdf.sail.Sail;
import org.openrdf.sail.SailConnection;
import org.openrdf.sail.SailException;
import org.openrdf.sail.helpers.SailWrapper;

public class AuditingSail extends SailWrapper {
	private static final String prefix = "t"
			+ Long.toHexString(System.currentTimeMillis()) + "x";
	private static final AtomicLong seq = new AtomicLong(0);
	private String ns;
	private boolean archiving;

	public AuditingSail() {
		super();
	}

	public AuditingSail(Sail baseSail) {
		super(baseSail);
	}

	public String getNamespace() {
		return ns;
	}

	public void setNamespace(String ns) {
		this.ns = ns;
	}

	public boolean isArchiving() {
		return archiving;
	}

	public void setArchiving(boolean archiving) {
		this.archiving = archiving;
	}

	@Override
	public void initialize() throws SailException {
		super.initialize();
		if (ns == null) {
			RuntimeMXBean bean = ManagementFactory.getRuntimeMXBean();
			ns = "urn:trx:" + bean.getName() + ":";
		}
	}

	@Override
	public SailConnection getConnection() throws SailException {
		try {
			return new AuditingConnection(this, super.getConnection());
		} catch (DatatypeConfigurationException e) {
			throw new SailException(e);
		}
	}

	public URI nextTransaction() {
		return getValueFactory().createURI(ns, prefix + seq.getAndIncrement());
	}

	public String toString() {
		return String.valueOf(getDataDir());
	}
}
