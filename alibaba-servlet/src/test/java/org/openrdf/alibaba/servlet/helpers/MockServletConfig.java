package org.openrdf.alibaba.servlet.helpers;

import java.util.Enumeration;
import java.util.Hashtable;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

public class MockServletConfig implements ServletConfig {
	private Hashtable<String, String> map = new Hashtable<String, String>();

	public String getInitParameter(String key) {
		return map.get(key);
	}

	public Enumeration getInitParameterNames() {
		return map.keys();
	}

	public void putInitParameter(String key, String value) {
		map.put(key, value);
	}

	public ServletContext getServletContext() {
		return null;
	}

	public String getServletName() {
		return null;
	}

}
