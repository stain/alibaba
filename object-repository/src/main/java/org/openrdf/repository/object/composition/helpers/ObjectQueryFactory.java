/*
 * Copyright (c) 2009, James Leigh All rights reserved.
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
package org.openrdf.repository.object.composition.helpers;

import static org.openrdf.query.QueryLanguage.SPARQL;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.query.TupleQuery;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectQuery;
import org.openrdf.repository.object.annotations.complementOf;
import org.openrdf.repository.object.annotations.intersectionOf;
import org.openrdf.repository.object.annotations.rdf;
import org.openrdf.repository.object.managers.PropertyMapper;
import org.openrdf.store.StoreException;

public class ObjectQueryFactory {

	private PropertyMapper mapper;

	private ObjectConnection connection;

	private ValueFactory vf;

	private Map<PropertySetFactory, ObjectQuery> queries = new HashMap<PropertySetFactory, ObjectQuery>();

	public ObjectQueryFactory(ObjectConnection connection, PropertyMapper mapper) {
		this.connection = connection;
		this.mapper = mapper;
		this.vf = connection.getValueFactory();
	}

	public ObjectQuery createQuery(PropertySetFactory factory)
			throws StoreException {
		synchronized (queries) {
			ObjectQuery query = queries.remove(factory);
			if (query != null)
				return query;
		}
		Class<?> type = factory.getPropertyType();
		Map<String, URI> properties = findEagerProperties(type);
		if (properties == null)
			return null;
		String sparql = buildQuery(properties, factory);
		TupleQuery tuples = connection.prepareTupleQuery(SPARQL, sparql);
		return new ObjectQuery(connection, tuples);
	}

	public void returnQuery(PropertySetFactory factory, ObjectQuery query) {
		synchronized (queries) {
			// will we need to close old query?
			queries.put(factory, query);
		}
	}

	private Map<String, URI> findEagerProperties(Class<?> type) {
		Map<String, URI> properties = new HashMap<String, URI>();
		findEagerProperties(type, properties);
		if (properties.isEmpty())
			return null;
		properties.put("class", RDF.TYPE);
		return properties;
	}

	private Map<String, URI> findEagerProperties(Class<?> concept,
			Map<String, URI> properties) {
		for (PropertyDescriptor pd : mapper.findProperties(concept)) {
			Class<?> type = pd.getPropertyType();
			Type generic = pd.getReadMethod().getGenericReturnType();
			if (!isEagerPropertyType(generic, type))
				continue;
			String pred = mapper.findPredicate(pd);
			properties.put(pd.getName(), vf.createURI(pred));
		}
		for (Field field : mapper.findFields(concept)) {
			Class<?> type = field.getType();
			if (!isEagerPropertyType(field.getGenericType(), type))
				continue;
			String pred = mapper.findPredicate(field);
			properties.put(field.getName(), vf.createURI(pred));
		}
		for (Class<?> face : concept.getInterfaces()) {
			findEagerProperties(face, properties);
		}
		if (concept.getSuperclass() == null)
			return properties;
		return findEagerProperties(concept.getSuperclass(), properties);
	}

	private boolean isEagerPropertyType(Type t, Class<?> type) {
		if (type.isInterface())
			return false;
		if (Object.class.equals(type))
			return false;
		if (type.isAnnotationPresent(rdf.class))
			return false;
		if (type.isAnnotationPresent(complementOf.class))
			return false;
		if (type.isAnnotationPresent(intersectionOf.class))
			return false;
		return true;
	}

	private String buildQuery(Map<String, URI> properties,
			PropertySetFactory factory) {
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT ?_ ");
		for (String name : properties.keySet()) {
			sb.append(" ?__").append(name);
		}
		sb.append("\nWHERE { ");
		String uri = factory.getPredicate().stringValue();
		if (factory.isInversed()) {
			sb.append(" ?_ <").append(uri).append("> $self ");
		} else {
			sb.append(" $self <").append(uri).append("> ?_ ");
		}
		for (String name : properties.keySet()) {
			URI pred = properties.get(name);
			sb.append("\nOPTIONAL {").append(" ?_ <");
			sb.append(pred.stringValue());
			sb.append("> ?__").append(name).append(" } ");
		}
		sb.append(" } ");
		return sb.toString();
	}
}
