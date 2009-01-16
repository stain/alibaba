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
package org.openrdf.repository.object.managers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.openrdf.cursor.CollectionCursor;
import org.openrdf.cursor.ConvertingCursor;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.BindingSet;
import org.openrdf.repository.contextaware.ContextAwareConnection;
import org.openrdf.repository.object.annotations.complementOf;
import org.openrdf.repository.object.annotations.intersectionOf;
import org.openrdf.repository.object.annotations.oneOf;
import org.openrdf.repository.object.composition.ClassResolver;
import org.openrdf.repository.object.exceptions.ObjectPersistException;
import org.openrdf.repository.object.exceptions.ObjectStoreException;
import org.openrdf.result.ModelResult;
import org.openrdf.result.Result;
import org.openrdf.result.TupleResult;
import org.openrdf.result.impl.ResultImpl;
import org.openrdf.store.StoreException;

/**
 * Determine the rdf:types of a Sesame Resource.
 * 
 * @author James Leigh
 * 
 */
public class ResourceManager {

	private ContextAwareConnection conn;

	private ValueFactory vf;

	private TypeManager types;

	private RoleMapper mapper;

	private ClassResolver resolver;

	public void setConnection(ContextAwareConnection conn) {
		this.conn = conn;
		this.vf = conn.getValueFactory();
	}

	public void setSesameTypeRepository(TypeManager types) {
		this.types = types;
	}

	public void setRoleMapper(RoleMapper mapper) {
		this.mapper = mapper;
	}

	public void setClassResolver(ClassResolver resolver) {
		this.resolver = resolver;
	}

	public Result<Resource> createRoleQuery(Class<?> concept) {
		if (concept.isAnnotationPresent(oneOf.class)) {
			oneOf ann = concept.getAnnotation(oneOf.class);
			Iterator<String> list = Arrays.asList(ann.value()).iterator();
			CollectionCursor<String> cursor = new CollectionCursor<String>(list);
			return new ResultImpl(new ConvertingCursor<String, Resource>(cursor) {
				@Override
				protected Resource convert(String uri) {
					return vf.createURI(uri);
				}
			});
		}
		StringBuilder query = new StringBuilder();
		query.append("SELECT DISTINCT ?subj WHERE {");
		appendFilter(concept, query);
		query.append("}");
		String qry = query.toString();
		try {
			TupleResult result = types.evaluateTypeQuery(qry);
			final String binding = result.getBindingNames().get(0);
			return new ResultImpl(new ConvertingCursor<BindingSet, Resource>(result) {
				@Override
				protected Resource convert(BindingSet sol) {
					Value value = sol.getValue(binding);
					assert value instanceof Resource : value;
					return (Resource) value;
				}
			});
		} catch (StoreException e) {
			throw new ObjectStoreException(e);
		}
	}

	@SuppressWarnings("unchecked")
	public Class<?> getEntityClass(Resource res) {
		URI uri = null;
		if (res instanceof URI) {
			uri = (URI) res;
		}
		try {
			ModelResult stmts = null;
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
		} catch (StoreException e) {
			throw new ObjectStoreException(e);
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
		} catch (StoreException e) {
			throw new ObjectPersistException(e);
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
		} catch (StoreException e) {
			throw new ObjectPersistException(e);
		}
	}

	public Class<?> removeRole(Resource resource, Class<?>... roles) {
		try {
			for (Class<?> role : roles) {
				removeType(resource, role);
			}
			return getEntityClass(resource);
		} catch (StoreException e) {
			throw new ObjectPersistException(e);
		}
	}

	public void removeResource(Resource resource) {
		try {
			boolean autoCommit = conn.isAutoCommit();
			conn.setAutoCommit(false);
			conn.removeMatch(resource, (URI) null, null);
			conn.removeMatch((URI) null, (URI) null, resource);
			conn.setAutoCommit(autoCommit);
			types.removeResource(resource);
		} catch (StoreException e) {
			throw new ObjectPersistException(e);
		}
	}

	public void renameResource(Resource before, Resource after) {
		try {
			ModelResult stmts;
			boolean autoCommit = conn.isAutoCommit();
			conn.setAutoCommit(false);
			stmts = conn.match(before, null, null, false);
			try {
				while (stmts.hasNext()) {
					Statement stmt = stmts.next();
					URI pred = stmt.getPredicate();
					Value obj = stmt.getObject();
					conn.removeMatch(before, pred, obj);
					conn.add(after, pred, obj);
				}
			} finally {
				stmts.close();
			}
			stmts = conn.match(null, null, before);
			try {
				while (stmts.hasNext()) {
					Statement stmt = stmts.next();
					Resource subj = stmt.getSubject();
					URI pred = stmt.getPredicate();
					conn.removeMatch(subj, pred, before);
					conn.add(subj, pred, after);
				}
			} finally {
				stmts.close();
			}
			conn.setAutoCommit(autoCommit);
			types.renameResource(before, after);
		} catch (StoreException e) {
			throw new ObjectPersistException(e);
		}
	}

	public void removeType(Resource resource, Class<?> role)
			throws StoreException {
		URI type = mapper.findType(role);
		if (type != null) {
			types.removeTypeStatement(resource, type);
		} else if (role.isAnnotationPresent(complementOf.class)) {
			addType(resource, role.getAnnotation(complementOf.class).value(),
					true);
		} else if (role.isAnnotationPresent(intersectionOf.class)) {
			// FIXME ignore ??
		} else {
			throw new ObjectPersistException("Concept not registered: "
					+ role.getSimpleName());
		}
	}

	private Set<URI> addType(Resource resource, Class<?> role, boolean required)
			throws StoreException {
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
				throw new ObjectPersistException("Concept not registered: "
						+ role.getSimpleName());
			return types;
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
			throw new ObjectPersistException("Concept not registered: "
					+ concept.getSimpleName());
		}
	}

}
