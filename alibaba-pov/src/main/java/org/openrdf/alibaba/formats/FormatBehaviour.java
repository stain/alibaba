package org.openrdf.alibaba.formats;

import org.openrdf.alibaba.exceptions.AlibabaException;

/** Methods used for formating and parsing. */
public interface FormatBehaviour {
	public abstract String format(Object value) throws AlibabaException;

	public abstract Object parse(String source) throws AlibabaException;
}
