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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
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
import org.openrdf.server.metadata.annotations.operation;
import org.openrdf.server.metadata.annotations.parameter;
import org.openrdf.server.metadata.annotations.rel;
import org.openrdf.server.metadata.annotations.title;
import org.openrdf.server.metadata.annotations.type;
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

	public ResponseBuilder get() throws Throwable {
		String name = getOperation();
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
					return Response.status(307).location(net);
				}
			}
			if (entity == null) {
				throw new NotFoundException("Not Found <" + uri.stringValue()
						+ "?" + name + ">");
			}
			return Response.ok().entity(entity);
		} catch (InvocationTargetException e) {
			throw e.getCause();
		}
	}

	public ResponseBuilder put(HttpHeaders headers, InputStream in) throws Throwable {
		String name = getOperation();
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
			return Response.noContent();
		} catch (InvocationTargetException e) {
			throw e.getCause();
		}

	}

	public ResponseBuilder post(HttpHeaders headers, InputStream in) throws Throwable {
		String name = getOperation();
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
					return Response.status(307).location(net);
				}
			}
			if (entity == null) {
				return Response.noContent();
			}
			return Response.ok().entity(entity);
		} catch (InvocationTargetException e) {
			throw e.getCause();
		}

	}

	public ResponseBuilder delete() throws Throwable {
		String name = getOperation();
		// get RDFObject
		Object target = con.getObject(uri);
		// lookup method
		Method method = findSetterMethod(name, target);
		if (method == null)
			return methodNotAllowed();
		try {
			// invoke method
			invoke(method, target);
			// save any changes made
			con.setAutoCommit(true);
			return Response.noContent();
		} catch (InvocationTargetException e) {
			throw e.getCause();
		}

	}

	public Set<String> getAllowedMethods() throws RepositoryException {
		Set<String> set = new LinkedHashSet<String>();
		String name = getOperation();
		Object target = con.getObject(uri);
		if (findGetterMethod(name, target) != null) {
			set.add("GET");
			set.add("HEAD");
		}
		if (findOperationMethod(name, target) != null) {
			set.add("POST");
		}
		if (findSetterMethod(name, target) != null) {
			set.add("PUT");
			set.add("DELETE");
		}
		return set;
	}

	public Iterable<String> getLinks() throws RepositoryException {
		Object target = con.getObject(uri);
		Map<String, Method> map = getOperationMethods(target, false, true);
		List<String> result = new ArrayList<String>(map.size());
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String, Method> e : map.entrySet()) {
			sb.delete(0, sb.length());
			sb.append("<").append(uri.stringValue());
			sb.append("?").append(e.getKey()).append(">");
			Method m = e.getValue();
			if (m.isAnnotationPresent(rel.class)) {
				sb.append("; rel=\"");
				for (String value : m.getAnnotation(rel.class).value()) {
					sb.append(value).append(" ");
				}
				sb.setCharAt(sb.length() - 1, '"');
			}
			if (m.isAnnotationPresent(type.class)) {
				sb.append("; type=\"");
				for (String value : m.getAnnotation(type.class).value()) {
					sb.append(value).append(" ");
				}
				sb.setCharAt(sb.length() - 1, '"');
			}
			if (m.isAnnotationPresent(title.class)) {
				for (String value : m.getAnnotation(title.class).value()) {
					sb.append("; title=\"").append(value).append("\"");
				}
			}
			result.add(sb.toString());
		}
		return result;
	}

	private String getOperation() {
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
		return getOperationMethods(target, false, true).get(name);
	}

	private Method findSetterMethod(String name, Object target)
			throws RepositoryException {
		return getOperationMethods(target, true, false).get(name);
	}

	private Method findOperationMethod(String name, Object target)
			throws RepositoryException {
		return getOperationMethods(target, true, true).get(name);
	}

	private Map<String,Method> getOperationMethods(Object target,
			boolean isReqBody, boolean isRespBody) throws RepositoryException {
		Map<String, Method> map = new HashMap<String, Method>();
		for (Method m : target.getClass().getMethods()) {
			if (isRespBody != !m.getReturnType().equals(Void.TYPE))
				continue;
			operation ann = m.getAnnotation(operation.class);
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
				map.put(value, m);
			}
		}
		return map;
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
		try {
			Object entity = method.invoke(target, args);
			if (entity instanceof RDFObjectBehaviour) {
				entity = ((RDFObjectBehaviour) entity).getBehaviourDelegate();
			}
			return entity;
		} finally {
			for (Object arg : args) {
				if (arg instanceof Closeable) {
					try {
						((Closeable) arg).close();
					} catch (Exception e) {
						logger.warn(e.getMessage(), e);
					}
				}
			}
		}
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
			} else if (names == null && headers == null) {
				args[i] = null;
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
