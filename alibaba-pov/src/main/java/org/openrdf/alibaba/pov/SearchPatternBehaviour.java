package org.openrdf.alibaba.pov;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openrdf.alibaba.exceptions.AlibabaException;
import org.openrdf.elmo.ElmoQuery;

public interface SearchPatternBehaviour {
	public abstract List<Display> getBindings(Set<String> filters,
			String orderBy) throws AlibabaException;

	public abstract String getJpqlQueryString(Set<String> filters,
			String orderBy) throws AlibabaException;

	public abstract String getSerqlQueryString(Set<String> filters,
			String orderBy) throws AlibabaException;

	public abstract String getSparqlQueryString(Set<String> filters,
			String orderBy) throws AlibabaException;

	public abstract String getSqlQueryString(Set<String> filters, String orderBy)
			throws AlibabaException;

	public abstract ElmoQuery createElmoQuery(Map<String, String> filter,
			String orderBy) throws AlibabaException;
}
