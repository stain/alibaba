package org.openrdf.alibaba.decor;

import java.io.IOException;

import javax.xml.namespace.QName;

import org.openrdf.alibaba.exceptions.AlibabaException;
import org.openrdf.alibaba.pov.Intent;
import org.openrdf.concepts.rdfs.Class;
import org.openrdf.elmo.Entity;

/**
 * Basic object management methods.
 * 
 * @author James Leigh
 *
 */
public interface PresentationServiceBehaviour {
	/**
	 * 
	 * @parm manager
	 * @param resource
	 *            name of new resource or null
	 * @param type
	 * @param source
	 * @param intent
	 *            intention of source or null for default
	 * @return name of created resource
	 * @throws AlibabaException
	 * @throws IOException
	 */
	public abstract QName create(Entity target, Class type, Content source,
			Intent intent) throws AlibabaException, IOException;

	/**
	 * 
	 * @parm manager
	 * @param resource
	 * @param resp
	 * @param intent
	 *            intention of response or null for default
	 * @throws AlibabaException
	 * @throws IOException
	 */
	public abstract void retrieve(Entity target, Response resp, Intent intent)
			throws AlibabaException, IOException;

	/**
	 * 
	 * @parm manager
	 * @param resource
	 * @param source
	 * @param intent
	 *            intention of source or null for default
	 * @throws AlibabaException
	 * @throws IOException
	 */
	public abstract void save(Entity target, Content source, Intent intent)
			throws AlibabaException, IOException;

	public abstract void remove(Entity target) throws AlibabaException;

	public abstract long getLastModified(Entity target, Intent intent);
}
