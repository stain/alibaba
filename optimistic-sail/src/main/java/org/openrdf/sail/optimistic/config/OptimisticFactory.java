package org.openrdf.sail.optimistic.config;

import org.openrdf.sail.config.SailFactory;
import org.openrdf.sail.config.SailImplConfig;
import org.openrdf.sail.config.SailImplConfigBase;
import org.openrdf.sail.optimistic.OptimisticSail;
import org.openrdf.store.StoreConfigException;

/**
 * @see OptimisticConfig
 * @author James Leigh
 */
public class OptimisticFactory implements SailFactory {

	/**
	 * The type of repositories that are created by this factory.
	 * 
	 * @see SailFactory#getSailType()
	 */
	public static final String SAIL_TYPE = "openrdf:Optimistic";

	/**
	 * Returns the Sail's type: <tt>openrdf:Optimistic</tt>.
	 */
	public String getSailType() {
		return SAIL_TYPE;
	}

	public SailImplConfig getConfig() {
		return new SailImplConfigBase();
	}

	public OptimisticSail getSail(SailImplConfig config)
		throws StoreConfigException
	{
		if (!SAIL_TYPE.equals(config.getType())) {
			throw new StoreConfigException("Invalid Sail type: " + config.getType());
		}
		return new OptimisticSail();
	}
}
