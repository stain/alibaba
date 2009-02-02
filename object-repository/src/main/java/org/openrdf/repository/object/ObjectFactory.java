package org.openrdf.repository.object;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.repository.object.annotations.complementOf;
import org.openrdf.repository.object.annotations.intersectionOf;
import org.openrdf.repository.object.annotations.rdf;
import org.openrdf.repository.object.composition.ClassResolver;
import org.openrdf.repository.object.composition.helpers.ObjectQueryFactory;
import org.openrdf.repository.object.exceptions.ObjectCompositionException;
import org.openrdf.repository.object.managers.LiteralManager;
import org.openrdf.repository.object.managers.PropertyMapper;
import org.openrdf.repository.object.managers.RoleMapper;
import org.openrdf.repository.object.traits.InternalRDFObject;
import org.openrdf.store.StoreException;

public class ObjectFactory {

	private LiteralManager lm;

	private ClassResolver resolver;

	private RoleMapper mapper;

	private PropertyMapper properties;

	private ObjectConnection connection;

	private Map<Class<?>, ObjectQueryFactory> factories;

	public ObjectFactory(LiteralManager lm, RoleMapper mapper,
			ClassResolver resolver, PropertyMapper properties) {
		this.lm = lm;
		this.mapper = mapper;
		this.properties = properties;
		this.resolver = resolver;
	}

	public void setObjectConnection(ObjectConnection connection)
			throws StoreException {
		this.connection = connection;
		if (!connection.getRepository().getMetaData().isEmbedded()) {
			factories = new HashMap<Class<?>, ObjectQueryFactory>();
		}
	}

	public Object createObject(Literal literal) {
		return lm.createObject(literal);
	}

	public Literal createLiteral(Object object) {
		return lm.createLiteral(object);
	}

	public RDFObject createBlankObject() {
		BNode node = connection.getValueFactory().createBNode();
		return createBean(node, resolver.resolveBlankEntity());

	}

	public RDFObject createRDFObject(Resource resource) {
		if (resource instanceof URI)
			return createBean(resource, resolver.resolveEntity((URI) resource));
		return createBean(resource, resolver.resolveBlankEntity());
	}

	public RDFObject createRDFObject(Resource resource, Collection<URI> types) {
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

	protected boolean isDatatype(Class<?> type) {
		return lm.isDatatype(type);
	}

	protected boolean isConcept(Class<?> type) {
		if (type.isAnnotationPresent(rdf.class))
			return true;
		if (type.isAnnotationPresent(complementOf.class))
			return true;
		if (type.isAnnotationPresent(intersectionOf.class))
			return true;
		if (mapper.findType(type) != null)
			return true;
		return false;
	}

	protected URI getType(Class<?> concept) {
		return mapper.findType(concept);
	}

	protected PropertyMapper getPropertyMapper() {
		return properties;
	}

	private RDFObject createBean(Resource resource, Class<?> proxy) {
		try {
			ObjectQueryFactory factory = createObjectQueryFactory(proxy);
			Object obj = proxy.newInstance();
			InternalRDFObject bean = (InternalRDFObject) obj;
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
