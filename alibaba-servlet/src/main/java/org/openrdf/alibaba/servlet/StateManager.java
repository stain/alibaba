package org.openrdf.alibaba.servlet;

import java.io.IOException;

import javax.xml.namespace.QName;

import org.openrdf.alibaba.exceptions.AlibabaException;

public interface StateManager {
	/**
	 * 
	 * @param resource name of new resource or null
	 * @param type
	 * @param source
	 * @param intention intention of source or null for default
	 * @return name of created resource
	 * @throws AlibabaException
	 * @throws IOException
	 */
	public abstract QName create(QName resource, QName type, Content source,
			QName intention) throws AlibabaException, IOException;

	/**
	 * 
	 * @param resource
	 * @param resp
	 * @param intention intention of response or null for default
	 * @throws AlibabaException
	 * @throws IOException
	 */
	public abstract void retrieve(QName resource, Response resp, QName intention)
			throws AlibabaException, IOException;

	/**
	 * 
	 * @param resource
	 * @param source
	 * @param intention intention of source or null for default
	 * @throws AlibabaException
	 * @throws IOException
	 */
	public abstract void save(QName resource, Content source, QName intention)
			throws AlibabaException, IOException;

	public abstract void remove(QName resource) throws AlibabaException;

	public abstract long getLastModified(QName resource);
}
