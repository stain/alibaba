package org.openrdf.alibaba.helpers;

import javax.xml.namespace.QName;

import org.openrdf.alibaba.servlet.Content;
import org.openrdf.alibaba.servlet.Response;
import org.openrdf.alibaba.servlet.StateManager;

public class MockStateManager implements StateManager {
	public QName _type, _resource, _intention;

	public Content _source;

	public Response _response;

	long _lastModified;

	public QName create(QName type, Content source) {
		_type = type;
		_source = source;
		return _resource;
	}

	public QName create(QName resource, QName type, Content source) {
		QName old_resource = _resource;
		_resource = resource;
		_type = type;
		_source = source;
		return old_resource;
	}

	public QName create(QName resource, QName type, Content source,
			QName intention) {
		QName old_resource = _resource;
		_resource = resource;
		_type = type;
		_source = source;
		_intention = intention;
		return old_resource;
	}

	public long getLastModified(QName resource) {
		_resource = resource;
		return _lastModified;
	}

	public void remove(QName resource) {
		_resource = resource;
	}

	public void retrieve(QName resource, Response resp) {
		_resource = resource;
		_response = resp;
	}

	public void retrieve(QName resource, Response resp, QName intention) {
		_resource = resource;
		_response = resp;
		_intention = intention;
	}

	public void save(QName resource, Content source) {
		_resource = resource;
		_source = source;
	}

	public void save(QName resource, Content source, QName intention) {
		_resource = resource;
		_source = source;
		_intention = intention;
	}
}
