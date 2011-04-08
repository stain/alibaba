package org.openrdf.repository.object.composition;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Enumeration;
import java.util.InvalidPropertiesFormatException;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;

public class OrderedProperties extends Properties {
	private static final long serialVersionUID = -5067493864922247542L;
	private Vector<Object> propertyNames = new Vector<Object>();
	private boolean loaded;

	public OrderedProperties(InputStream inStream) throws IOException {
		super.load(inStream);
		propertyNames.retainAll(super.keySet());
		loaded = true;
	}

	public Enumeration<?> propertyNames() {
		return keys();
	}

	@Override
	public synchronized Enumeration<Object> keys() {
		return propertyNames.elements();
	}

	@Override
	public Set<Object> keySet() {
		return new LinkedHashSet<Object>(propertyNames);
	}

	public synchronized Object put(Object key, Object value) {
		if (loaded)
			throw new UnsupportedOperationException();
		propertyNames.add(key);
		return super.put(key, value);
	}

	@Override
	public synchronized void load(InputStream inStream) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public synchronized void load(Reader reader) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public synchronized void loadFromXML(InputStream in) throws IOException,
			InvalidPropertiesFormatException {
		throw new UnsupportedOperationException();
	}

	@Override
	public synchronized Object setProperty(String key, String value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public synchronized void putAll(Map<? extends Object, ? extends Object> t) {
		throw new UnsupportedOperationException();
	}

	@Override
	public synchronized Object remove(Object key) {
		throw new UnsupportedOperationException();
	}
}