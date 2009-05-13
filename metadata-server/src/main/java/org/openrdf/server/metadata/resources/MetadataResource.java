package org.openrdf.server.metadata.resources;

import info.aduna.net.ParsedURI;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.repository.object.traits.RDFObjectBehaviour;
import org.openrdf.server.metadata.annotations.operation;
import org.openrdf.server.metadata.annotations.parameter;
import org.openrdf.server.metadata.annotations.rel;
import org.openrdf.server.metadata.annotations.title;
import org.openrdf.server.metadata.annotations.type;
import org.openrdf.server.metadata.concepts.WebResource;
import org.openrdf.server.metadata.http.Request;
import org.openrdf.server.metadata.http.Response;

public class MetadataResource {
	private File file;
	private RDFObject target;

	public MetadataResource(File file, RDFObject target) {
		this.file = file;
		this.target = target;
	}

	public File getFile() {
		return file;
	}

	public WebResource getWebResource() {
		if (target instanceof WebResource)
			return (WebResource) target;
		return null;
	}

	protected WebResource addWebResourceDesignation() throws RepositoryException {
		ObjectConnection con = getObjectConnection();
		WebResource web = con.addDesignation(target, WebResource.class);
		target = web;
		return web;
	}

	public Object getTarget() {
		return target;
	}

	public URI getURI() {
		return (URI) target.getResource();
	}

	public ObjectConnection getObjectConnection() {
		return target.getObjectConnection();
	}

	public URI createURI(String uriSpec) {
		ParsedURI base = new ParsedURI(getURI().stringValue());
		base.normalize();
		ParsedURI uri = new ParsedURI(uriSpec);
		ValueFactory vf = target.getObjectConnection().getValueFactory();
		return vf.createURI(base.resolve(uri).toString());
	}

	public List<String> getLinks() throws RepositoryException {
		Map<String, List<Method>> map = getOperationMethods(false, true);
		List<String> result = new ArrayList<String>(map.size());
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String, List<Method>> e : map.entrySet()) {
			sb.delete(0, sb.length());
			sb.append("<").append(target.getResource().stringValue());
			sb.append("?").append(e.getKey()).append(">");
			for (Method m : e.getValue()) {
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
			}
			result.add(sb.toString());
		}
		return result;
	}

	protected String getOperationName(String rel, Request req) throws ParseException {
		Map<String, List<Method>> map = getOperationMethods(false, true);
		for (Map.Entry<String, List<Method>> e : map.entrySet()) {
			for (Method m : e.getValue()) {
				if (m.isAnnotationPresent(rel.class)) {
					for (String value : m.getAnnotation(rel.class).value()) {
						if (rel.equals(value)) {
							if (m.isAnnotationPresent(type.class)) {
								for (String media : m.getAnnotation(type.class).value()) {
									if (req.isAcceptable(m.getReturnType(), media))
										return e.getKey();
								}
							} else {
								return e.getKey();
							}
						}
					}
				}
			}
		}
		return null;
	}

	protected Response methodNotAllowed(Request req) throws RepositoryException {
		StringBuilder sb = new StringBuilder();
		sb.append("OPTIONS, TRACE");
		for (String method : getAllowedMethods(req)) {
			sb.append(", ").append(method);
		}
		return new Response().status(405).header("Allow", sb.toString());
	}

	protected Set<String> getAllowedMethods(Request req)
			throws RepositoryException {
		Set<String> set = new LinkedHashSet<String>();
		String name = req.getOperation();
		if (name == null && file.canRead()
				|| !findGetterMethods(name).isEmpty()) {
			set.add("GET");
			set.add("HEAD");
		}
		if (name == null) {
			if (!file.exists() || file.canWrite()) {
				set.add("PUT");
			}
			if (file.exists() && file.getParentFile().canWrite()) {
				set.add("DELETE");
			}
		} else if (!findSetterMethods(name).isEmpty()) {
			set.add("PUT");
			set.add("DELETE");
		}
		Map<String, List<Method>> map = getOperationMethods(true, true);
		for (String method : map.keySet()) {
			set.add(method);
		}
		return set;
	}

	protected List<Method> findGetterMethods(String name)
			throws RepositoryException {
		List<Method> list = getOperationMethods(false, true).get(name);
		if (list == null)
			return Collections.EMPTY_LIST;
		return list;
	}

	protected List<Method> findSetterMethods(String name)
			throws RepositoryException {
		List<Method> list = getOperationMethods(true, false).get(name);
		if (list == null)
			return Collections.EMPTY_LIST;
		return list;
	}

	protected List<Method> findOperationMethods(String name)
			throws RepositoryException {
		List<Method> list = getOperationMethods(true, true).get(name);
		if (list == null)
			return Collections.EMPTY_LIST;
		return list;
	}

	protected Map<String, List<Method>> getOperationMethods(boolean isReqBody,
			boolean isRespBody) {
		Map<String, List<Method>> map = new HashMap<String, List<Method>>();
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
				List<Method> list = map.get(value);
				if (list == null) {
					map.put(value, list = new ArrayList<Method>());
				}
				list.add(m);
			}
		}
		return map;
	}

	protected Method findBestMethod(Request req, List<Method> methods)
			throws ParseException {
		Method best = null;
		loop: for (Method method : methods) {
			Class<?>[] ptypes = method.getParameterTypes();
			Annotation[][] anns = method.getParameterAnnotations();
			Type[] gtypes = method.getGenericParameterTypes();
			Object[] args = new Object[ptypes.length];
			for (int i = 0; i < args.length; i++) {
				String[] names = getParameterNames(anns[i]);
				if (names == null) {
					if (!req.isReadable(ptypes[i], gtypes[i]))
						continue loop;
				}
			}
			best = method;
			if (!method.getReturnType().equals(Void.TYPE)) {
				if (method.isAnnotationPresent(type.class)) {
					for (String media : method.getAnnotation(type.class)
							.value()) {
						if (!req.isAcceptable(method.getReturnType(), media))
							continue;
						return best;
					}
					continue loop;
				} else {
					if (!req.isAcceptable(method.getReturnType()))
						continue loop;
				}
			}
			return best;
		}
		return best;
	}

	protected Object invoke(Method method, Request req)
			throws RepositoryException, IOException, IllegalAccessException,
			InvocationTargetException {
		Object[] args = getParameters(method, req);
		try {
			Object entity = method.invoke(target, args);
			if (entity instanceof RDFObjectBehaviour) {
				entity = ((RDFObjectBehaviour) entity).getBehaviourDelegate();
			}
			return entity;
		} finally {
			for (Object arg : args) {
				if (arg instanceof Closeable) {
					((Closeable) arg).close();
				}
			}
		}
	}

	private Object[] getParameters(Method method, Request req)
			throws RepositoryException, IOException {
		Class<?>[] ptypes = method.getParameterTypes();
		Annotation[][] anns = method.getParameterAnnotations();
		Type[] gtypes = method.getGenericParameterTypes();
		Object[] args = new Object[ptypes.length];
		for (int i = 0; i < args.length; i++) {
			String[] names = getParameterNames(anns[i]);
			if (names == null) {
				args[i] = req.getBody(ptypes[i], gtypes[i]);
			} else if (names.length == 0
					&& ptypes[i].isAssignableFrom(Map.class)) {
				args[i] = req.getParameterMap();
			} else {
				args[i] = req.getParameter(names, gtypes[i], ptypes[i]);
			}
		}
		return args;
	}

	private String[] getParameterNames(Annotation[] annotations) {
		for (int i = 0; i < annotations.length; i++) {
			if (annotations[i] instanceof parameter)
				return ((parameter) annotations[i]).value();
		}
		return null;
	}

}
