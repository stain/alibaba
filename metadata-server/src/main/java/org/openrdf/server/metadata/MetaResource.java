package org.openrdf.server.metadata;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Providers;

import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.repository.object.traits.RDFObjectBehaviour;
import org.openrdf.server.metadata.annotations.parameter;
import org.openrdf.server.metadata.annotations.purpose;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.NotFoundException;
import com.sun.jersey.api.core.ResourceContext;

public class MetaResource extends SubResource {
	private Logger logger = LoggerFactory.getLogger(MetaResource.class);

	public MetaResource(Request request, ResourceContext ctx,
			Providers providers, File file, ObjectConnection con, URI uri,
			MultivaluedMap<String, String> params) {
		super(request, ctx, providers, file, con, uri, params);
	}

	public Response get() throws Throwable {
		String name = getPurpose();
		// get RDFObject
		Object target = con.getObject(uri);
		// lookup method
		Method method = findGetterMethod(name, target);
		if (method == null)
			return methodNotAllowed();
		try {
			// invoke method
			Object entity = invoke(method, target);
			// return result
			if (entity instanceof RDFObject && !target.equals(entity)) {
				Resource resource = ((RDFObject) entity).getResource();
				if (resource instanceof URI) {
					URI uri = (URI) resource;
					java.net.URI net = java.net.URI.create(uri.stringValue());
					return Response.status(307).location(net).build();
				}
			}
			if (entity == null) {
				throw new NotFoundException("Not Found <" + uri.stringValue()
						+ "?" + name + ">");
			}
			return Response.ok().entity(entity).build();
		} catch (InvocationTargetException e) {
			throw e.getCause();
		}
	}

	public Response put(HttpHeaders headers, InputStream in) throws Throwable {
		String name = getPurpose();
		// get RDFObject
		Object target = con.getObject(uri);
		// lookup method
		Method method = findSetterMethod(name, target);
		if (method == null)
			return methodNotAllowed();
		try {
			// invoke method
			invoke(method, target, headers, in);
			// save any changes made
			con.setAutoCommit(true);
			return Response.noContent().build();
		} catch (InvocationTargetException e) {
			throw e.getCause();
		}

	}

	public Response post(HttpHeaders headers, InputStream in) throws Throwable {
		String name = getPurpose();
		// get RDFObject
		Object target = con.getObject(uri);
		// lookup method
		Method method = findOperationMethod(name, target);
		if (method == null)
			return methodNotAllowed();
		try {
			Object entity = invoke(method, target, headers, in);
			// save any changes made
			con.setAutoCommit(true);
			// return result
			if (entity instanceof RDFObject && !target.equals(entity)) {
				Resource resource = ((RDFObject) entity).getResource();
				if (resource instanceof URI) {
					URI uri = (URI) resource;
					java.net.URI net = java.net.URI.create(uri.stringValue());
					return Response.status(307).location(net).build();
				}
			}
			if (entity == null) {
				return Response.noContent().build();
			}
			return Response.ok().entity(entity).build();
		} catch (InvocationTargetException e) {
			throw e.getCause();
		}

	}

	public Response delete() throws RepositoryException {
		con.clear(uri);
		con.remove(uri, null, null);
		con.remove((Resource) null, null, uri);
		con.setAutoCommit(true);
		return Response.noContent().build();
	}

	public Set<String> getAllowedMethods() throws RepositoryException {
		Set<String> set = new LinkedHashSet<String>();
		String name = getPurpose();
		Object target = con.getObject(uri);
		if (findGetterMethod(name, target) != null) {
			set.add("GET");
			set.add("HEAD");
		}
		if (findSetterMethod(name, target) != null) {
			set.add("PUT");
		}
		if (findOperationMethod(name, target) != null) {
			set.add("POST");
		}
		set.add("DELETE");
		return set;
	}

	private String getPurpose() {
		for (String key : params.keySet()) {
			List<String> values = params.get(key);
			if (values == null || values.size() == 0 || values.size() == 1
					&& values.get(0).length() == 0) {
				return key;
			}
		}
		return "";
	}

	private Method findGetterMethod(String name, Object target)
			throws RepositoryException {
		return getPurposeMethod(target, name, false, true);
	}

	private Method findSetterMethod(String name, Object target)
			throws RepositoryException {
		return getPurposeMethod(target, name, true, false);
	}

	private Method findOperationMethod(String name, Object target)
			throws RepositoryException {
		return getPurposeMethod(target, name, true, true);
	}

	private Method getPurposeMethod(Object target, String name,
			boolean isReqBody, boolean isRespBody) throws RepositoryException {
		for (Method m : target.getClass().getMethods()) {
			if (isRespBody != !m.getReturnType().equals(Void.TYPE))
				continue;
			purpose ann = m.getAnnotation(purpose.class);
			if (ann == null)
				continue;
			int bodies = 0;
			Annotation[][] anns = m.getParameterAnnotations();
			for (int i = 0; i < anns.length; i++) {
				if (getParameterNames(anns[i]) == null) {
					bodies++;
				}
			}
			if (bodies > 1 || isReqBody != (bodies == 1))
				continue;
			for (String value : ann.value()) {
				if (name.equals(value))
					return m;
			}
		}
		return null;
	}

	private Object invoke(Method method, Object target)
			throws RepositoryException, IOException, IllegalAccessException,
			InvocationTargetException {
		return invoke(method, target, null, null);
	}

	private Object invoke(Method method, Object target, HttpHeaders headers,
			InputStream in) throws RepositoryException, IOException,
			IllegalAccessException, InvocationTargetException {
		Object[] args = getParameters(method, headers, in);
		Object entity = method.invoke(target, args);
		for (Object arg : args) {
			if (arg instanceof Closeable) {
				try {
					((Closeable) arg).close();
				} catch (Exception e) {
					logger.warn(e.getMessage(), e);
				}
			}
		}
		if (entity instanceof RDFObjectBehaviour) {
			entity = ((RDFObjectBehaviour) entity).getBehaviourDelegate();
		}
		return entity;
	}

	private Object[] getParameters(Method method, HttpHeaders headers,
			InputStream in) throws RepositoryException,
			IOException {
		Class<?>[] ptypes = method.getParameterTypes();
		Annotation[][] anns = method.getParameterAnnotations();
		Type[] gtypes = method.getGenericParameterTypes();
		Object[] args = new Object[ptypes.length];
		for (int i = 0; i < args.length; i++) {
			String[] names = getParameterNames(anns[i]);
			if (names == null && isContext(anns[i])) {
				args[i] = providers.getContextResolver(ptypes[i], null)
						.getContext(ptypes[i]);
			} else if (names == null) {
				MultivaluedMap<String, String> map = headers
						.getRequestHeaders();
				MediaType media = headers.getMediaType();
				MessageBodyReader reader = providers.getMessageBodyReader(
						ptypes[i], gtypes[i], anns[i], media);
				if (reader == null)
					throw new WebApplicationException(Response.status(
							Status.UNSUPPORTED_MEDIA_TYPE).build());
				args[i] = reader.readFrom(ptypes[i], gtypes[i], anns[i], media,
						map, in);
			} else if (names.length == 0
					&& ptypes[i].isAssignableFrom(params.getClass())) {
				args[i] = params;
			} else {
				args[i] = getParameter(names, gtypes[i], ptypes[i]);
			}
		}
		return args;
	}

	private boolean isContext(Annotation[] annotations) {
		for (Annotation ann : annotations) {
			if (ann instanceof Context)
				return true;
		}
		return false;
	}

	private Object getParameter(String[] names, Type type, Class<?> klass) throws RepositoryException {
		Class<?> componentType = Object.class;
		if (klass.equals(Set.class)) {
			if (type instanceof ParameterizedType) {
				ParameterizedType ptype = (ParameterizedType) type;
				Type[] args = ptype.getActualTypeArguments();
				if (args.length == 1) {
					if (args[0] instanceof Class) {
						componentType = (Class<?>) args[0];
					}
				}
			}
		}
		Set<Object> result = new LinkedHashSet<Object>();
		for (String name : names) {
			if (params.containsKey(name)) {
				List<String> values = params.get(name);
				if (klass.equals(Set.class)) {
					for (String value : values) {
						result.add(toObject(value, componentType));
					}
				} else if (values.isEmpty()) {
					return null;
				} else {
					return toObject(values.get(0), klass);
				}
			}
		}
		if (klass.equals(Set.class))
			return result;
		return null;
	}

	private <T> T toObject(String value, Class<T> klass)
			throws RepositoryException {
		if (String.class.equals(klass)) {
			return klass.cast(value);
		} else if (klass.isInterface() || of.isNamedConcept(klass)) {
			try {
				return klass.cast(con.getObject(vf.createURI(value)));
			} catch (ClassCastException e) {
				throw new WebApplicationException(e, 400);
			}
		} else {
			URI datatype = vf.createURI("java:", klass.getName());
			Literal lit = vf.createLiteral(value, datatype);
			return klass.cast(of.createObject(lit));
		}
	}

	private String[] getParameterNames(Annotation[] annotations) {
		for (int i = 0; i < annotations.length; i++) {
			if (annotations[i] instanceof parameter)
				return ((parameter) annotations[i]).value();
		}
		return null;
	}
}
