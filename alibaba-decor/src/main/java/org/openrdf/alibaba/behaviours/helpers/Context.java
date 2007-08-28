package org.openrdf.alibaba.behaviours.helpers;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class Context {
	private Map<String, String> filter;

	private String orderBy;

	private PrintWriter writer;

	private BufferedReader reader;

	private Map<String, Object> bindings = new HashMap<String, Object>();

	public Context(Map<String, String> filter, String orderBy) {
		this.filter = filter;
		this.orderBy = orderBy;
	}

	public Context copy() {
		Context copy = new Context(filter, orderBy);
		copy.setWriter(writer);
		copy.setReader(reader);
		copy.bindings.putAll(bindings);
		return copy;
	}

	public Map<String, String> getFilter() {
		return filter;
	}

	public String getOrderBy() {
		return orderBy;
	}

	public void setWriter(PrintWriter writer) {
		this.writer = writer;
		if (writer == null) {
			bindings.remove("out");
		} else {
			bindings.put("out", writer);
		}
	}

	public void setReader(BufferedReader reader) {
		this.reader = reader;
		if (reader == null) {
			bindings.remove("in");
		} else {
			bindings.put("in", reader);
		}
	}

	public Map<String, Object> getBindings() {
		return bindings;
	}

	public Object bind(String key, Object value) {
		return bindings.put(key, value);
	}

	public Object remove(String key) {
		return bindings.remove(key);
	}
}
