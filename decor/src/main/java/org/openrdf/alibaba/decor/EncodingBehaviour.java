package org.openrdf.alibaba.decor;

/**
 * Encoding method interface.
 * 
 * @author James Leigh
 * 
 */
public interface EncodingBehaviour {
	public abstract String encode(String value);

	public abstract String decode(String value);
}
