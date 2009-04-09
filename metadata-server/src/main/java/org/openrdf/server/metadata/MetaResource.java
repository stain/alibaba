package org.openrdf.server.metadata;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Providers;

import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectFactory;
import org.openrdf.server.metadata.annotations.operation;
import org.openrdf.server.metadata.annotations.parameter;
import org.openrdf.server.metadata.annotations.purpose;
import org.openrdf.store.StoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.NotFoundException;

@Path("{path:.*}")
public class MetaResource {
	private Logger logger = LoggerFactory.getLogger(MetaResource.class);
	private ObjectConnection con;
	private URI uri;
	private ValueFactory vf;
	private ObjectFactory of;

	public MetaResource(ObjectConnection con, URI uri) {
		this.con = con;
		this.uri = uri;
		vf = con.getValueFactory();
		of = con.getObjectFactory();
	}

	@GET
	public Response get(@Context UriInfo info, @Context Request request)
			throws Throwable {
		ResponseBuilder rb;
		MultivaluedMap<String, String> params = info.getQueryParameters();
		String name = getPurpose(params);
		// get RDFObject
		Object target = con.getObject(uri);
		// lookup method
		Method method = getPurposeMethod(name, false, target);
		if (method == null)
			throw new NotFoundException("Not Found <" + uri.stringValue() + "?"
					+ name + ">");
		try {
			// invoke method
			Object[] args = getParameters(method, params);
			Object entity = method.invoke(target, args);
			// return result
			rb = Response.ok().entity(entity);
		} catch (InvocationTargetException e) {
			throw e.getCause();
		}
		return rb.build();
	}

	@PUT
	public void put(@Context UriInfo info, @Context Request request,
			@Context HttpHeaders headers, @Context Providers providers,
			InputStream in) throws Throwable {
		MultivaluedMap<String, String> params = info.getQueryParameters();
		String name = getPurpose(params);
		// get RDFObject
		Object target = con.getObject(uri);
		// lookup method
		Method method = getPurposeMethod(name, true, target);
		if (method == null) {
			if (getPurposeMethod(name, false, target) == null) {
				throw new NotFoundException("Not Found <" + uri.stringValue()
						+ "?" + name + ">");
			} else {
				StringBuilder sb = new StringBuilder();
				sb.append("GET, DELETE");
				for (Method m : con.getObject(uri).getClass().getMethods()) {
					if (m.isAnnotationPresent(operation.class)) {
						sb.append(", POST");
						break;
					}
				}
				ResponseBuilder rb = Response.status(405);
				rb = rb.header("Allow", sb.toString());
				throw new WebApplicationException(rb.build());
			}
		}
		try {
			// invoke method
			Object[] args = getParameters(method, params, headers, providers,
					in);
			method.invoke(target, args);
			for (Object arg : args) {
				if (arg instanceof Closeable) {
					try {
						((Closeable) arg).close();
					} catch (IOException e) {
						logger.warn(e.getMessage(), e);
					}
				}
			}
		} catch (InvocationTargetException e) {
			throw e.getCause();
		}

	}

	public void delete() throws StoreException {
		con.begin();
		con.clear(uri);
		con.removeMatch(uri, null, null);
		con.removeMatch(null, null, uri);
		con.commit();
	}

	private String getPurpose(MultivaluedMap<String, String> params) {
		for (String key : params.keySet()) {
			List<String> values = params.get(key);
			if (values == null || values.size() == 0 || values.size() == 1
					&& values.get(0).length() == 0) {
				return key;
			}
		}
		return null;
	}

	private Method getPurposeMethod(String name, boolean isVoid, Object target)
			throws StoreException {
		for (Method m : target.getClass().getMethods()) {
			if (isVoid != m.getReturnType().equals(Void.TYPE))
				continue;
			purpose ann = m.getAnnotation(purpose.class);
			if (ann == null)
				continue;
			for (String value : ann.value()) {
				if (name.equals(value))
					return m;
			}
		}
		return null;
	}

	private Object[] getParameters(Method method,
			MultivaluedMap<String, String> params) throws StoreException,
			IOException {
		Class<?>[] ptypes = method.getParameterTypes();
		Annotation[][] anns = method.getParameterAnnotations();
		Type[] gtypes = method.getGenericParameterTypes();
		Object[] args = new Object[ptypes.length];
		for (int i = 0; i < args.length; i++) {
			String[] names = getParameterNames(anns[i]);
			if (names != null) {
				args[i] = getParameter(names, gtypes[i], ptypes[i], params);
			}
		}
		return args;
	}

	private Object[] getParameters(Method method,
			MultivaluedMap<String, String> params, HttpHeaders headers,
			Providers providers, InputStream in) throws StoreException,
			IOException {
		Class<?>[] ptypes = method.getParameterTypes();
		Annotation[][] anns = method.getParameterAnnotations();
		Type[] gtypes = method.getGenericParameterTypes();
		Object[] args = new Object[ptypes.length];
		for (int i = 0; i < args.length; i++) {
			String[] names = getParameterNames(anns[i]);
			if (names == null) {
				MultivaluedMap<String, String> map = headers
						.getRequestHeaders();
				MediaType media = headers.getMediaType();
				MessageBodyReader reader = providers.getMessageBodyReader(
						ptypes[i], gtypes[i], anns[i], media);
				args[i] = reader.readFrom(ptypes[i], gtypes[i], anns[i], media,
						map, in);
			} else {
				args[i] = getParameter(names, gtypes[i], ptypes[i], params);
			}
		}
		return args;
	}

	private Object getParameter(String[] names, Type type, Class<?> klass,
			MultivaluedMap<String, String> params) throws StoreException {
		Set<Object> result = new HashSet<Object>();
		for (String name : names) {
			if (params.containsKey(name)) {
				if (klass.equals(Set.class)) {
					// TODO
					throw new UnsupportedOperationException();
				}
				List<String> values = params.get(name);
				if (klass.equals(Set.class)) {
					result.addAll(values);
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

	private <T> T toObject(String value, Class<T> klass) throws StoreException {
		if (String.class.equals(klass)) {
			return klass.cast(value);
		} else if (of.isNamedConcept(klass)) {
			try {
				return klass.cast(con.getObject(value));
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
