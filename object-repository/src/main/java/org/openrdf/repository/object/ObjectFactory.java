package org.openrdf.repository.object;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.repository.object.annotations.rdf;
import org.openrdf.repository.object.composition.ClassResolver;
import org.openrdf.repository.object.composition.helpers.ObjectQueryFactory;
import org.openrdf.repository.object.exceptions.ObjectCompositionException;
import org.openrdf.repository.object.managers.LiteralManager;
import org.openrdf.repository.object.managers.PropertyMapper;
import org.openrdf.repository.object.managers.RoleMapper;
import org.openrdf.repository.object.traits.ManagedRDFObject;

public class ObjectFactory {

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

	public ClassLoader getClassLoader() {
		return cl;
	}

	public Object createObject(Literal literal) {
		return lm.createObject(literal);
	}

	public Literal createLiteral(Object object) {
		return lm.createLiteral(object);
	}

	public RDFObject createObject() {
		BNode node = connection.getValueFactory().createBNode();
		return createBean(node, resolver.resolveBlankEntity());

	}

	public RDFObject createObject(String uri) {
		ValueFactory vf = connection.getValueFactory();
		return createObject(vf.createURI(uri));
	}

	public RDFObject createObject(Resource resource) {
		if (resource instanceof URI)
			return createBean(resource, resolver.resolveEntity((URI) resource));
		return createBean(resource, resolver.resolveBlankEntity());
	}

	public <T> T createObject(Resource resource, Class<T> type) {
		Set<URI> types = Collections.singleton(getType(type));
		return type.cast(createObject(resource, types));
	}

	public RDFObject createObject(Resource resource, URI... types) {
		assert types != null && types.length > 0;
		List<URI> list = Arrays.asList(types);
		return createObject(resource, list);
	}

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

	public boolean isNamedConcept(Class<?> type) {
		if (type.isAnnotationPresent(rdf.class))
			return true;
		if (mapper.findType(type) != null)
			return true;
		return false;
	}

	protected boolean isDatatype(Class<?> type) {
		return lm.isDatatype(type);
	}

	protected URI getType(Class<?> concept) {
		return mapper.findType(concept);
	}

	protected PropertyMapper getPropertyMapper() {
		return properties;
	}

	protected void setObjectConnection(ObjectConnection connection) {
		this.connection = connection;
		factories = new HashMap<Class<?>, ObjectQueryFactory>();
	}

	private RDFObject createBean(Resource resource, Class<?> proxy) {
		try {
			ObjectQueryFactory factory = createObjectQueryFactory(proxy);
			Object obj = proxy.newInstance();
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
}
