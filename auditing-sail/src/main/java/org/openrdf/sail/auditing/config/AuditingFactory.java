package org.openrdf.sail.auditing.config;

import org.openrdf.sail.Sail;
import org.openrdf.sail.auditing.AuditingSail;
import org.openrdf.sail.config.SailConfigException;
import org.openrdf.sail.config.SailFactory;
import org.openrdf.sail.config.SailImplConfig;

public class AuditingFactory implements SailFactory {

	/**
	 * The type of repositories that are created by this factory.
	 * 
	 * @see SailFactory#getSailType()
	 */
	public static final String SAIL_TYPE = "openrdf:AuditingSail";

	/**
	 * Returns the Sail's type: <tt>openrdf:AuditingSail</tt>.
	 */
	public String getSailType() {
		return SAIL_TYPE;
	}

	public SailImplConfig getConfig() {
		return new AuditingConfig();
	}

	public Sail getSail(SailImplConfig config) throws SailConfigException {
		if (!SAIL_TYPE.equals(config.getType())) {
			throw new SailConfigException("Invalid Sail type: "
					+ config.getType());
		}
		assert config instanceof AuditingConfig;
		AuditingConfig cfg = (AuditingConfig) config;
		AuditingSail sail = new AuditingSail();
		sail.setNamespace(cfg.getNamespace());
		sail.setArchiving(cfg.isArchiving());
		return sail;
	}
}
