package org.openrdf.server.metadata;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.openrdf.model.Literal;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectFactory;
import org.openrdf.result.GraphResult;
import org.openrdf.server.metadata.annotations.parameter;
import org.openrdf.server.metadata.annotations.purpose;
import org.openrdf.store.StoreException;

import com.sun.jersey.api.NotFoundException;

@Path("{path:.*}")
public class MetaResource {
	private ObjectConnection con;
	private URI uri;

	public MetaResource(ObjectConnection con, URI uri) {
		this.con = con;
		this.uri = uri;
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
		Method method = getView(name, target);
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
	public void put(@Context UriInfo info, GraphResult graph)
			throws StoreException {
		con.begin();
		con.clear(uri);
		Statement st;
		while ((st = graph.next()) != null) {
			con.add(st, uri);
		}
		con.commit();
	}

	@DELETE
	public void delete() throws StoreException {
		con.clear(uri);
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

	private Method getView(String name, Object target) throws StoreException {
		for (Method m : target.getClass().getMethods()) {
			purpose ann = m.getAnnotation(purpose.class);
			if (ann != null) {
				for (String value : ann.value()) {
					if (name.equals(value))
						return m;
				}
			}
		}
		return null;
	}

	private Object[] getParameters(Method method,
			MultivaluedMap<String, String> params) throws StoreException {
		Class<?>[] parameterTypes = method.getParameterTypes();
		Annotation[][] annotations = method.getParameterAnnotations();
		Type[] types = method.getGenericParameterTypes();
		Object[] args = new Object[parameterTypes.length];
		for (int i = 0; i < args.length; i++) {
			String[] names = getParameterNames(annotations[i]);
			if (names != null) {
				args[i] = getParameter(names, types[i], parameterTypes[i],
						params);
			}
		}
		return args;
	}

	private Object getParameter(String[] names, Type type, Class<?> klass,
			MultivaluedMap<String, String> params) throws StoreException {
		ValueFactory vf = con.getValueFactory();
		ObjectFactory of = con.getObjectFactory();
		Set<Object> result = new HashSet<Object>();
		for (String name : names) {
			if (params.containsKey(name)) {
				if (klass.equals(Set.class)) {
					// TODO
					throw new UnsupportedOperationException();
				}
				if (klass.equals(Set.class)) {
					result.addAll(params.get(name));
				} else if (String.class.equals(klass)) {
					return params.getFirst(name);
				} else {
					URI datatype = vf.createURI("java:", klass.getName());
					Literal lit = vf.createLiteral(params.getFirst(name),
							datatype);
					return of.createObject(lit);
				}
			}
		}
		if (klass.equals(Set.class))
			return result;
		return null;
	}

	private String[] getParameterNames(Annotation[] annotations) {
		for (int i = 0; i < annotations.length; i++) {
			if (annotations[i] instanceof parameter)
				return ((parameter) annotations[i]).value();
		}
		return null;
	}
}
