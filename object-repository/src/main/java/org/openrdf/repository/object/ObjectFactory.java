package org.openrdf.repository.object;

import java.util.Collection;

import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.repository.object.composition.ClassResolver;
import org.openrdf.repository.object.exceptions.ObjectCompositionException;
import org.openrdf.repository.object.managers.LiteralManager;
import org.openrdf.repository.object.managers.RoleMapper;
import org.openrdf.repository.object.traits.InitializableRDFObject;

public class ObjectFactory {

	private LiteralManager lm;

	private ClassResolver resolver;

	private RoleMapper mapper;

	private ObjectConnection connection;

	public ObjectFactory(LiteralManager lm, RoleMapper mapper,
			ClassResolver resolver) {
		this.lm = lm;
		this.mapper = mapper;
		this.resolver = resolver;
	}

	public void setObjectConnection(ObjectConnection connection) {
		this.connection = connection;
	}

	public Object createObject(Literal literal) {
		return lm.createObject(literal);
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

	public boolean isDatatype(Class<?> type) {
		return lm.isDatatype(type);
	}

	public Literal getLiteral(Object object) {
		return lm.createLiteral(object);
	}

	public boolean isConcept(Class<?> type) {
		return mapper.findType(type) != null;
	}

	public URI getType(Class<?> concept) {
		return mapper.findType(concept);
	}

	private RDFObject createBean(Resource resource, Class<?> proxy) {
		try {
			Object obj = proxy.newInstance();
			InitializableRDFObject bean = (InitializableRDFObject) obj;
			bean.initObjectConnection(connection, resource);
			return (RDFObject) bean;
		} catch (InstantiationException e) {
			throw new ObjectCompositionException(e);
		} catch (IllegalAccessException e) {
			throw new ObjectCompositionException(e);
		}
	}
}
