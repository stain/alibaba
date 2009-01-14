/*
 * Copyright (c) 2007, James Leigh All rights reserved.
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
package org.openrdf.elmo.sesame;

import info.aduna.iteration.CloseableIteration;
import info.aduna.iteration.CloseableIteratorIteration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;

import org.openrdf.elmo.ElmoEntityResolver;
import org.openrdf.elmo.ResourceManager;
import org.openrdf.elmo.RoleMapper;
import org.openrdf.elmo.annotations.complementOf;
import org.openrdf.elmo.annotations.disjointWith;
import org.openrdf.elmo.annotations.intersectionOf;
import org.openrdf.elmo.annotations.oneOf;
import org.openrdf.elmo.exceptions.ElmoIOException;
import org.openrdf.elmo.exceptions.ElmoPersistException;
import org.openrdf.elmo.sesame.iterators.ElmoIteration;
import org.openrdf.model.Namespace;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.contextaware.ContextAwareConnection;

/**
 * Determine the rdf:types of a Sesame Resource.
 * 
 * @author James Leigh
 * 
 */
public class SesameResourceManager implements ResourceManager<Resource> {

	private static final String DEFAULT_PREFIX = XMLConstants.DEFAULT_NS_PREFIX;

	private ContextAwareConnection conn;

	private ValueFactory vf;

	private SesameTypeManager types;

	private RoleMapper<URI> mapper;

	private ElmoEntityResolver<URI> resolver;

	public void setConnection(ContextAwareConnection conn) {
		this.conn = conn;
		this.vf = conn.getRepository().getValueFactory();
	}

	public void setSesameTypeRepository(SesameTypeManager types) {
		this.types = types;
	}

	public void setRoleMapper(RoleMapper<URI> mapper) {
		this.mapper = mapper;
	}

	public void setElmoEntityResolver(ElmoEntityResolver<URI> resolver) {
		this.resolver = resolver;
	}

	public Resource createResource(QName qname) {
		if (qname == null)
			return vf.createBNode();
		String ns = qname.getNamespaceURI();
		String name = qname.getLocalPart();
		if (ns.equals(XMLConstants.NULL_NS_URI)) {
			String prefix = qname.getPrefix();
			if (prefix.equals(DEFAULT_PREFIX))
				return vf.createURI(name);
			try {
				ns = conn.getNamespace(prefix);
				return vf.createURI(ns, name);
			} catch (RepositoryException e) {
				throw new ElmoIOException(e);
			}
		}
		return vf.createURI(ns, name);
	}

	public QName createQName(Resource res) {
		if (res instanceof URI) {
			URI uri = (URI) res;
			String prefix = getPrefix(uri.getNamespace());
			return new QName(uri.getNamespace(), uri.getLocalName(), prefix);
		}
		return null;
	}

	public Iterator<Resource> createRoleQuery(Class<?> concept) {
		if (concept.isAnnotationPresent(oneOf.class)) {
			oneOf ann = concept.getAnnotation(oneOf.class);
			Iterator<String> list = Arrays.asList(ann.value()).iterator();
			CloseableIteration<String, RuntimeException> iter;
			iter = new CloseableIteratorIteration(list);
			return new ElmoIteration<String, Resource>(iter) {
				@Override
				protected Resource convert(String uri) {
					return vf.createURI(uri);
				}
			};
		}
		StringBuilder query = new StringBuilder();
		query.append("SELECT DISTINCT ?subj WHERE {");
		appendFilter(concept, query);
		query.append("}");
		String qry = query.toString();
		try {
			TupleQueryResult result = types.evaluateTypeQuery(qry);
			final String binding = result.getBindingNames().get(0);
			return new ElmoIteration<BindingSet, Resource>(result) {
				@Override
				protected Resource convert(BindingSet sol) {
					Value value = sol.getValue(binding);
					assert value instanceof Resource : value;
					return (Resource) value;
				}
			};
		} catch (QueryEvaluationException e) {
			throw new ElmoIOException(e);
		} catch (MalformedQueryException e) {
			throw new ElmoIOException(e);
		} catch (RepositoryException e) {
			throw new ElmoIOException(e);
		}
	}

	@SuppressWarnings("unchecked")
	public Class<?> getEntityClass(Resource res) {
		URI uri = null;
		if (res instanceof URI) {
			uri = (URI) res;
		}
		try {
			RepositoryResult<Statement> stmts = null;
			try {
				stmts = types.getTypeStatements(res);
				if (stmts.hasNext()) {
					Value obj = stmts.next().getObject();
					if (obj instanceof URI && !stmts.hasNext())
						return resolver.resolveEntity(uri, (URI) obj);
					List<URI> types = new ArrayList<URI>();
					if (obj instanceof URI) {
						types.add((URI) obj);
					}
					while (stmts.hasNext()) {
						obj = stmts.next().getObject();
						if (obj instanceof URI) {
							types.add((URI) obj);
						}
					}
					return resolver.resolveEntity(uri, types);
				}
				return resolver.resolveEntity(uri);
			} finally {
				if (stmts != null)
					stmts.close();
			}
		} catch (RepositoryException e) {
			throw new ElmoIOException(e);
		}
	}

	public Class<?> mergeRole(Resource resource, Class<?> role,
			Class<?>... roles) {
		try {
			addType(resource, role, true);
			if (roles != null) {
				for (Class<?> r : roles) {
					addType(resource, r, true);
				}
			}
			Class<?> c = getEntityClass(resource);
			assert isDisjointWith(c, role, roles);
			return c;
		} catch (RepositoryException e) {
			throw new ElmoPersistException(e);
		}
	}

	public Class<?> persistRole(Resource resource, Class<?> role,
			Class<?>... roles) {
		try {
			Set<URI> types = addType(resource, role, true);
			if (roles != null) {
				if (types != null) {
					types = new HashSet<URI>(types);
				}
				for (Class<?> r : roles) {
					Set<URI> set = addType(resource, r, true);
					if (set == null) {
						types = null;
					} else if (types != null) {
						types.addAll(set);
					}
				}
			}
			Class<?> c;
			if (types == null) {
				c = getEntityClass(resource);
			} else {
				URI uri = null;
				if (resource instanceof URI) {
					uri = (URI) resource;
				}
				c = resolver.resolveEntity(uri, types);
			}
			assert isDisjointWith(c, role);
			return c;
		} catch (RepositoryException e) {
			throw new ElmoPersistException(e);
		}
	}

	public Class<?> removeRole(Resource resource, Class<?>... roles) {
		try {
			for (Class<?> role : roles) {
				removeType(resource, role);
			}
			return getEntityClass(resource);
		} catch (RepositoryException e) {
			throw new ElmoPersistException(e);
		}
	}

	public void removeResource(Resource resource) {
		try {
			boolean autoCommit = conn.isAutoCommit();
			conn.setAutoCommit(false);
			conn.remove(resource, (URI) null, null);
			conn.remove((URI) null, (URI) null, resource);
			conn.setAutoCommit(autoCommit);
			types.removeResource(resource);
		} catch (Exception e) {
			throw new ElmoPersistException(e);
		}
	}

	public void renameResource(Resource before, Resource after) {
		try {
			RepositoryResult<Statement> stmts;
			boolean autoCommit = conn.isAutoCommit();
			conn.setAutoCommit(false);
			stmts = conn.getStatements(before, null, null, false);
			try {
				while (stmts.hasNext()) {
					Statement stmt = stmts.next();
					URI pred = stmt.getPredicate();
					Value obj = stmt.getObject();
					conn.remove(before, pred, obj);
					conn.add(after, pred, obj);
				}
			} finally {
				stmts.close();
			}
			stmts = conn.getStatements(null, null, before);
			try {
				while (stmts.hasNext()) {
					Statement stmt = stmts.next();
					Resource subj = stmt.getSubject();
					URI pred = stmt.getPredicate();
					conn.remove(subj, pred, before);
					conn.add(subj, pred, after);
				}
			} finally {
				stmts.close();
			}
			conn.setAutoCommit(autoCommit);
			types.renameResource(before, after);
		} catch (Exception e) {
			throw new ElmoPersistException(e);
		}
	}

	public void removeType(Resource resource, Class<?> role)
			throws RepositoryException {
		URI type = mapper.findType(role);
		if (type != null) {
			types.removeTypeStatement(resource, type);
		} else if (role.isAnnotationPresent(complementOf.class)) {
			addType(resource, role.getAnnotation(complementOf.class).value(), true);
		} else if (role.isAnnotationPresent(intersectionOf.class)) {
			for (Class<?> of : role.getAnnotation(intersectionOf.class).value()) {
				removeType(resource, of);
			}
		} else {
			throw new ElmoPersistException("Concept not registered: "
					+ role.getSimpleName());
		}
	}

	private Set<URI> addType(Resource resource, Class<?> role, boolean required)
			throws RepositoryException {
		URI type = mapper.findType(role);
		if (type != null) {
			types.addTypeStatement(resource, type);
			return Collections.singleton(type);
		} else if (role.isAnnotationPresent(complementOf.class)) {
			removeType(resource, role.getAnnotation(complementOf.class).value());
			return null;
		} else if (role.isAnnotationPresent(intersectionOf.class)) {
			for (Class<?> of : role.getAnnotation(intersectionOf.class).value()) {
				addType(resource, of, true);
			}
			return null;
		} else {
			Set<URI> types = new HashSet<URI>();
			Class<?> superclass = role.getSuperclass();
			if (superclass != null) {
				Set<URI> addedTypes = addType(resource, superclass, false);
				if (addedTypes == null) {
					types = null;
				} else if (types != null) {
					types.addAll(addedTypes);
				}
			}
			Class<?>[] interfaces = role.getInterfaces();
			for (int i = 0, n = interfaces.length; i < n; i++) {
				Set<URI> addedTypes = addType(resource, interfaces[i], false);
				if (addedTypes == null) {
					types = null;
				} else if (types != null) {
					types.addAll(addedTypes);
				}
			}
			if (required && types != null && types.isEmpty())
				throw new ElmoPersistException("Concept not registered: "
						+ role.getSimpleName());
			return types;
		}
	}

	private String getPrefix(String namespace) {
		CloseableIteration<? extends Namespace, RepositoryException> namespaces = null;
		try {
			try {
				namespaces = conn.getNamespaces();
				while (namespaces.hasNext()) {
					Namespace ns = namespaces.next();
					if (namespace.equals(ns.getName()))
						return ns.getPrefix();
				}
				return DEFAULT_PREFIX;
			} finally {
				if (namespaces != null)
					namespaces.close();
			}
		} catch (RepositoryException e) {
			throw new ElmoIOException(e);
		}
	}

	private boolean isDisjointWith(Class<?> type, Class<?> role,
			Class<?>... roles) {
		isDisjointWith(type, role);
		for (Class<?> r : roles) {
			isDisjointWith(type, r);
		}
		return true;
	}

	private boolean isDisjointWith(Class<?> type, Class<?> role) {
		disjointWith dist = role.getAnnotation(disjointWith.class);
		if (dist != null) {
			for (Class<?> c : dist.value()) {
				assert !c.isAssignableFrom(type) : role.getSimpleName()
						+ " cannot be assigned to a " + type.getSimpleName();
			}
		}
		return true;
	}

	private void appendFilter(Class<?> concept, StringBuilder query) {
		Collection<URI> types = new HashSet<URI>();
		mapper.findSubTypes(concept, types);
		Iterator<URI> iter = types.iterator();
		if (iter.hasNext()) {
			while (iter.hasNext()) {
				query.append("{ ?subj a <");
				query.append(iter.next()).append(">}\n");
				if (iter.hasNext()) {
					query.append(" UNION ");
				}
			}
		} else if (concept.isAnnotationPresent(intersectionOf.class)) {
			throw new IllegalArgumentException("Intersections not supported");
		} else if (concept.isAnnotationPresent(complementOf.class)) {
			throw new IllegalArgumentException("Complements not supported");
		} else {
			throw new ElmoPersistException("Concept not registered: "
					+ concept.getSimpleName());
		}
	}

}
