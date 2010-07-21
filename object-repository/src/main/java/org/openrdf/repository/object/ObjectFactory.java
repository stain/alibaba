/*
 * Copyright (c) 2009-2010, James Leigh and Zepheira LLC Some rights reserved.
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

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.object.composition.ClassResolver;
import org.openrdf.repository.object.composition.helpers.ObjectQueryFactory;
import org.openrdf.repository.object.exceptions.ObjectCompositionException;
import org.openrdf.repository.object.managers.LiteralManager;
import org.openrdf.repository.object.managers.PropertyMapper;
import org.openrdf.repository.object.managers.RoleMapper;
import org.openrdf.repository.object.traits.ManagedRDFObject;
import org.openrdf.repository.object.traits.RDFObjectBehaviour;

/**
 * Converts between {@link Value} and objects without accessing the repository.
 * 
 * @author James Leigh
 * 
 */
public class ObjectFactory {
	static final String VAR_PREFIX = "subj"; 
	private LiteralManager lm;
	private ClassResolver resolver;
	private RoleMapper mapper;
	private PropertyMapper properties;
	private ClassLoader cl;
	private ObjectConnection connection;
	private Map<Class<?>, ObjectQueryFactory> factories;

	public ObjectFactory(RoleMapper mapper, PropertyMapper properties,
			LiteralManager lm, ClassResolver resolver, ClassLoader cl) {
		assert lm != null;
		assert mapper != null;
		assert properties != null;
		assert resolver != null;
		this.lm = lm;
		this.mapper = mapper;
		this.properties = properties;
		this.resolver = resolver;
		this.cl = cl;
	}

	/**
	 * @return The ClassLoader used by this ObjectFactory.
	 */
	public ClassLoader getClassLoader() {
		return cl;
	}

	/**
	 * Converts a literal into an object.
	 */
	public Object createObject(Literal literal) {
		return lm.createObject(literal);
	}

	/**
	 * Converts an object back into a literal.
	 */
	public Literal createLiteral(Object object) {
		return lm.createLiteral(object);
	}

	/**
	 * Converts an object into a literal or resource.
	 */
	public Value createValue(Object instance) {
		if (instance instanceof RDFObjectBehaviour) {
			RDFObjectBehaviour support = (RDFObjectBehaviour) instance;
			Object entity = support.getBehaviourDelegate();
			if (entity != instance)
				return createValue(entity);
		}
		if (instance instanceof RDFObject) {
			return ((RDFObject) instance).getResource();
		} else {
			return lm.createLiteral(instance);
		}
	}

	/**
	 * Creates an anonymous object with no rdf:type.
	 */
	public RDFObject createObject() {
		BNode node = connection.getValueFactory().createBNode();
		return createBean(node, resolver.resolveBlankEntity());

	}

	/**
	 * Creates an object with no rdf:type.
	 */
	public RDFObject createObject(String uri) {
		ValueFactory vf = connection.getValueFactory();
		return createObject(vf.createURI(uri));
	}

	/**
	 * Creates an object with no rdf:type.
	 */
	public RDFObject createObject(Resource resource) {
		if (resource instanceof URI)
			return createBean(resource, resolver.resolveEntity((URI) resource));
		return createBean(resource, resolver.resolveBlankEntity());
	}

	/**
	 * Creates an object with an assumed rdf:type.
	 */
	public <T> T createObject(String uri, Class<T> type) {
		ValueFactory vf = connection.getValueFactory();
		return createObject(vf.createURI(uri), type);
	}

	/**
	 * Creates an object with an assumed rdf:type.
	 */
	public <T> T createObject(Resource resource, Class<T> type) {
		URI rdftype = getNameOf(type);
		if (rdftype == null)
			return type.cast(createObject(resource));
		Set<URI> types = Collections.singleton(rdftype);
		return type.cast(createObject(resource, types));
	}

	/**
	 * Creates an object with assumed rdf:types.
	 */
	public RDFObject createObject(String uri, URI... types) {
		ValueFactory vf = connection.getValueFactory();
		return createObject(vf.createURI(uri), types);
	}

	/**
	 * Creates an object with assumed rdf:types.
	 */
	public RDFObject createObject(Resource resource, URI... types) {
		assert types != null && types.length > 0;
		List<URI> list = Arrays.asList(types);
		return createObject(resource, list);
	}

	/**
	 * Creates an object with assumed rdf:types.
	 */
	public RDFObject createObject(String uri,  Collection<URI> types) {
		ValueFactory vf = connection.getValueFactory();
		return createObject(vf.createURI(uri), types);
	}

	/**
	 * Creates an object with assumed rdf:types.
	 */
	public RDFObject createObject(Resource resource, Collection<URI> types) {
		Class<?> proxy;
		if (resource instanceof URI) {
			if (types.isEmpty()) {
				proxy = resolver.resolveEntity((URI) resource);
			} else {
				proxy = resolver.resolveEntity((URI) resource, types);
			}
		} else {
			if (types.isEmpty()) {
				proxy = resolver.resolveBlankEntity();
			} else {
				proxy = resolver.resolveBlankEntity(types);
			}
		}
		return createBean(resource, proxy);
	}

	/**
	 * @return <code>true</code> If the given type can be used as a concept
	 *         parameter.
	 */
	public boolean isNamedConcept(Class<?> type) {
		return mapper.findType(type) != null;
	}

	public boolean isDatatype(Class<?> type) {
		return lm.isDatatype(type);
	}

	public URI getNameOf(Class<?> concept) {
		return mapper.findType(concept);
	}

	protected void setObjectConnection(ObjectConnection connection) {
		this.connection = connection;
		factories = new HashMap<Class<?>, ObjectQueryFactory>();
	}

	protected String createObjectQuery(Class<?> concept, int bindings) {
		Collection<PropertyDescriptor> subjectProperties = properties
				.findFunctionalProperties(concept);
		Collection<Field> subjectFields = properties
				.findFunctionalFields(concept);
		StringBuilder select = new StringBuilder();
		StringBuilder where = new StringBuilder();
		select.append("SELECT REDUCED ?subj");
		boolean namedTypePresent = mapper.isNamedTypePresent();
		if (namedTypePresent) {
			select.append(" ?subj_class");
		}
		where.append("\nWHERE { ");
		URI uri = getNameOf(concept);
		boolean typed = uri != null && bindings == 0;
		if (typed) {
			Collection<URI> types = new HashSet<URI>();
			mapper.findSubTypes(concept, types);
			Iterator<URI> iter = types.iterator();
			assert iter.hasNext();
			while (iter.hasNext()) {
				where.append("\n{ ?subj a <");
				where.append(iter.next().stringValue()).append(">}");
				if (iter.hasNext()) {
					where.append(" UNION ");
				}
			}
			if (namedTypePresent) {
				where.append("\nOPTIONAL {").append(" ?subj <");
				where.append(RDF.TYPE);
				where.append("> ?subj_class } ");
			}
		} else {
			where.append("\n?subj a ?subj_class .");
		}
		String type = RDF.TYPE.stringValue();
		for (PropertyDescriptor pd : subjectProperties) {
			String name = pd.getName();
			String pred = properties.findPredicate(pd);
			optional(select, name, where.append("\n"), null, pred);
			if (pd.getPropertyType().equals(Object.class)) {
				if (namedTypePresent) {
					String name_class = name + "_class";
					StringBuilder w = where.append("\n\t");
					optional(select, name_class, w, name, type).append("}\n");
				}
			} else if (isNamedConcept(pd.getPropertyType())) {
				Map<String, String> map = findEagerProperties(pd.getPropertyType());
				for (String n : map.keySet()) {
					StringBuilder w = where.append("\n\t");
					optional(select, name + "_" + n, w, name, map.get(n)).append("}");
				}
				where.append("\n");
			}
			where.append("}");
		}
		for (Field f : subjectFields) {
			String name = f.getName();
			String pred = properties.findPredicate(f);
			optional(select, name, where.append("\n"), null, pred);
			if (f.getType().equals(Object.class)) {
				if (namedTypePresent) {
					String name_class = name + "_class";
					StringBuilder w = where.append("\n\t");
					optional(select, name_class, w, name, type).append("}\n");
				}
			} else if (isNamedConcept(f.getType())) {
				Map<String, String> map = findEagerProperties(f.getType());
				for (String n : map.keySet()) {
					StringBuilder w = where.append("\n\t");
					optional(select, name + "_" + n, w, name, map.get(n)).append("}");
				}
				where.append("\n");
			}
			where.append("}");
		}
		if (bindings > 1) {
			where.append("\nFILTER (");
			for (int i = 0; i < bindings; i++) {
				where.append(" ?subj = $subj").append(i).append(" ||");
			}
			where.delete(where.length() - 2, where.length());
			where.append(")");
		}
		where.append(" } ");
		if (bindings > 1) {
			where.append("\nORDER BY ?subj");
		}
		return select.append(where).toString();
	}

	private Map<String, String> findEagerProperties(Class<?> type) {
		Map<String, String> result = properties.findEagerProperties(type);
		if (result == null && mapper.isNamedTypePresent())
			return Collections.singletonMap("class", RDF.TYPE.stringValue());
		if (result == null)
			return Collections.emptyMap();
		return result;
	}

	private StringBuilder optional(StringBuilder select, String name,
			StringBuilder where, String subj, String pred) {
		select.append(" ?subj_").append(name);
		where.append("OPTIONAL {");
		if (subj == null) {
			where.append(" ?subj");
		} else {
			where.append(" ?subj_").append(subj);
		}
		where.append(" <");
		return where.append(pred).append("> ?subj_").append(name);
	}

	private RDFObject createBean(Resource resource, Class<?> proxy) {
		try {
			ObjectQueryFactory factory = createObjectQueryFactory(proxy);
			Object obj = newInstance(proxy);
			ManagedRDFObject bean = (ManagedRDFObject) obj;
			bean.initRDFObject(resource, factory, connection);
			return (RDFObject) obj;
		} catch (InstantiationException e) {
			throw new ObjectCompositionException(e);
		} catch (IllegalAccessException e) {
			throw new ObjectCompositionException(e);
		}
	}

	private ObjectQueryFactory createObjectQueryFactory(Class<?> proxy) {
		if (factories == null)
			return null;
		synchronized (factories) {
			ObjectQueryFactory factory = factories.get(proxy);
			if (factory == null) {
				factory = new ObjectQueryFactory(connection, properties);
				factories.put(proxy, factory);
			}
			return factory;
		}
	}

	private Object newInstance(Class<?> proxy) throws InstantiationException,
			IllegalAccessException {
		ClassLoader pcl = proxy.getClassLoader();
		if (pcl == null)
			return proxy.newInstance();
		synchronized (pcl) {
			return proxy.newInstance();
		}
	}
}
