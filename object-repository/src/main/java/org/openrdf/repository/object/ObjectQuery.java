/*
 * Copyright (c) 2007-2009, James Leigh All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution. 
 * - Neither the name of the openrdf.org nor the names of its contributors may
 *   be used to endorse or promote products derived from this software without
 *   specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 */
package org.openrdf.repository.object;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.WeakHashMap;

import javax.persistence.FlushModeType;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.Query;
import javax.persistence.TemporalType;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.openrdf.elmo.sesame.ElmoSingleQueryResult;
import org.openrdf.elmo.sesame.ElmoTupleQueryResult;
import org.openrdf.elmo.sesame.iterators.ElmoIteration;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.TupleQuery;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.object.exceptions.ElmoConversionException;
import org.openrdf.repository.object.exceptions.ElmoIOException;
import org.openrdf.result.TupleResult;
import org.openrdf.store.StoreException;

/**
 * Implements {@link ObjectQuery} for use with SesameManager.
 * 
 * @author James Leigh
 */
public class ObjectQuery implements Query {

	protected ObjectConnection manager;

	protected TupleQuery query;

	private WeakHashMap<ElmoIteration, Object> opened;

	private int firstResult;

	private int maxResults;

	public ObjectQuery(ObjectConnection manager, TupleQuery query) {
		this.manager = manager;
		this.opened = new WeakHashMap<ElmoIteration, Object>(4);
		this.query = query;
	}

	public void close() {
		for (ElmoIteration c : opened.keySet()) {
			c.close();
		}
	}

	@SuppressWarnings("unchecked")
	private ElmoIteration evaluateQuery() throws StoreException {
		ElmoIteration iter;
		TupleResult result = query.evaluate();
		int max = maxResults <= 0 ? 0 : maxResults + firstResult;
		if (result.getBindingNames().size() > 1) {
			iter = new ElmoTupleQueryResult(manager, result, max);
		} else {
			iter = new ElmoSingleQueryResult(manager, result, max);
		}
		return iter;
	}

	public Iterator evaluate() {
		try {
			ElmoIteration result = evaluateQuery();
			opened.put(result, Boolean.TRUE);
			if (firstResult > 0) {
				for (int i = 0; i < firstResult && result.hasNext(); i++) {
					result.next();
				}
			}
			return result;
		} catch (StoreException e) {
			throw new ElmoIOException(e);
		}
	}

	public Object getSingleResult() {
		Iterator iter = evaluate();
		try {
			if (!iter.hasNext())
				throw new NoResultException("No results");
			Object result = iter.next();
			if (iter.hasNext())
				throw new NonUniqueResultException("More than one result");
			return result;
		} finally {
			ElmoIteration.close(iter);
		}
	}

	public List getResultList() {
		List result = new ArrayList();
		Iterator iter = evaluate();
		try {
			for (int count = 0; iter.hasNext(); count++) {
				result.add(iter.next());
			}
			return result;
		} finally {
			ElmoIteration.close(iter);
		}
	}

	public ObjectQuery setFirstResult(int startPosition) {
		this.firstResult = startPosition;
		return this;
	}

	public ObjectQuery setMaxResults(int maxResult) {
		this.maxResults = maxResult;
		return this;
	}

	public boolean getIncludeInferred() {
		return query.getIncludeInferred();
	}

	public void setIncludeInferred(boolean include) {
		query.setIncludeInferred(include);
	}

	public ObjectQuery setParameter(String name, String label, Locale locale) {
		RepositoryConnection conn = manager;
		ValueFactory vf = conn.getValueFactory();
		if (label == null) {
			setBinding(name, null);
		} else if (locale == null) {
			setBinding(name, vf.createLiteral(label));
		} else {
			String lang = locale.toString().toLowerCase().replace('_', '-');
			setBinding(name, vf.createLiteral(label, lang));
		}
		return this;
	}

	public ObjectQuery setParameter(String name, Object value) {
		if (value == null) {
			setBinding(name, null);
		} else {
			setBinding(name, manager.getValue(value));
		}
		return this;
	}

	public ObjectQuery setType(String name, Class<?> concept) {
		setBinding(name, manager.getRoleMapper().findType(concept));
		return this;
	}

	public ObjectQuery setQName(String name, QName qname) {
		setBinding(name, manager.getResourceManager().createResource(qname));
		return this;
	}

	public ObjectQuery setValue(String name, Value value) {
		setBinding(name, value);
		return this;
	}

	@Override
	public String toString() {
		if (query == null)
			return super.toString();
		return query.toString();
	}

	private void setBinding(String name, Value value) {
		if (query == null)
			throw new UnsupportedOperationException();
		query.setBinding(name, value);
	}

	public int executeUpdate() {
		throw new UnsupportedOperationException();
	}

	public ObjectQuery setFlushMode(FlushModeType flushMode) {
		if (FlushModeType.AUTO.equals(flushMode)) {
			manager.flush();
		}
		return this;
	}

	public ObjectQuery setHint(String hintName, Object value) {
		return this;
	}

	public ObjectQuery setParameter(String name, Date value,
			TemporalType temporalType) {
		int y, M, d, h, m, s, i, z;
		try {
			z = DatatypeConstants.FIELD_UNDEFINED;
			DatatypeFactory factory = DatatypeFactory.newInstance();
			XMLGregorianCalendar xcal;
			switch (temporalType) {
			case DATE:
				y = value.getYear();
				M = value.getMonth() + 1;
				d = value.getDate();
				xcal = factory.newXMLGregorianCalendarDate(y, M, d, z);
				break;
			case TIME:
				h = value.getHours();
				m = value.getMinutes();
				s = value.getSeconds();
				i = (int) (value.getTime() % 1000);
				xcal = factory.newXMLGregorianCalendarTime(h, m, s, i, z);
				break;
			case TIMESTAMP:
				y = value.getYear();
				M = value.getMonth() + 1;
				d = value.getDate();
				h = value.getHours();
				m = value.getMinutes();
				s = value.getSeconds();
				i = (int) (value.getTime() % 1000);
				xcal = factory.newXMLGregorianCalendar(y, M, d, h, m, s, i, z);
				break;
			default:
				throw new AssertionError();
			}
			return setParameter(name, xcal);
		} catch (DatatypeConfigurationException e) {
			throw new ElmoConversionException(e);
		}
	}

	public ObjectQuery setParameter(String name, Calendar value,
			TemporalType temporalType) {
		assert value instanceof GregorianCalendar : value;
		GregorianCalendar cal = (GregorianCalendar) value;
		try {
			DatatypeFactory factory = DatatypeFactory.newInstance();
			XMLGregorianCalendar xcal = factory.newXMLGregorianCalendar(cal);
			switch (temporalType) {
			case DATE:
				xcal.setHour(DatatypeConstants.FIELD_UNDEFINED);
				xcal.setMinute(DatatypeConstants.FIELD_UNDEFINED);
				xcal.setSecond(DatatypeConstants.FIELD_UNDEFINED);
				xcal.setMillisecond(DatatypeConstants.FIELD_UNDEFINED);
				break;
			case TIME:
				xcal.setYear(DatatypeConstants.FIELD_UNDEFINED);
				xcal.setMonth(DatatypeConstants.FIELD_UNDEFINED);
				xcal.setDay(DatatypeConstants.FIELD_UNDEFINED);
				break;
			case TIMESTAMP:
				break;
			}
			return setParameter(name, xcal);
		} catch (DatatypeConfigurationException e) {
			throw new ElmoConversionException(e);
		}
	}

	public ObjectQuery setParameter(int arg0, Object arg1) {
		throw new UnsupportedOperationException(
				"Parameters by index are not supported");
	}

	public ObjectQuery setParameter(int arg0, Date arg1, TemporalType arg2) {
		throw new UnsupportedOperationException(
				"Parameters by index are not supported");
	}

	public ObjectQuery setParameter(int arg0, Calendar arg1, TemporalType arg2) {
		throw new UnsupportedOperationException(
				"Parameters by index are not supported");
	}

}
