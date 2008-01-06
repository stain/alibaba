package org.openrdf.alibaba.core;

import org.openrdf.alibaba.exceptions.AlibabaException;
import org.openrdf.elmo.annotations.rdf;

/**
 * Interface to use rdf:Property reflectively on JavaBeans.
 * 
 * @author James Leigh
 * 
 */
@rdf("http://www.w3.org/1999/02/22-rdf-syntax-ns#Property")
public interface PropertyValueBehaviour {

	public abstract void setValueOf(Object bean, Object value)
			throws AlibabaException;

	public abstract Object getValueOf(Object bean) throws AlibabaException;

}