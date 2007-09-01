package org.openrdf.alibaba.decor;

public interface EncodingBehaviour {
	public abstract String encode(String value);
	public abstract String decode(String value);
}
