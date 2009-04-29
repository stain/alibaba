package org.openrdf.server.metadata.resources;

import info.aduna.net.ParsedURI;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.traits.RDFObjectBehaviour;
import org.openrdf.server.metadata.annotations.operation;
import org.openrdf.server.metadata.annotations.parameter;
import org.openrdf.server.metadata.annotations.rel;
import org.openrdf.server.metadata.annotations.title;
import org.openrdf.server.metadata.annotations.type;
import org.openrdf.server.metadata.concepts.WebResource;
import org.openrdf.server.metadata.http.Request;
import org.openrdf.server.metadata.http.Response;

import eu.medsea.util.MimeUtil;

public class MetadataResource {
	private File file;
	private WebResource target;

	public MetadataResource(File file, WebResource target) {
		this.file = file;
		this.target = target;
	}

	public File getFile() {
		return file;
	}

	public WebResource getWebResource() {
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

	protected String getOperationName(String rel) throws RepositoryException {
		Map<String, Method> map = getOperationMethods(false, true);
		for (Map.Entry<String, Method> e : map.entrySet()) {
			if (e.getValue().isAnnotationPresent(rel.class)) {
				for (String value : e.getValue().getAnnotation(rel.class)
						.value()) {
					if (rel.equals(value)) {
						return e.getKey();
					}
				}
			}
		}
		return null;
	}

	public List<String> getLinks() throws RepositoryException {
		Map<String, Method> map = getOperationMethods(false, true);
		List<String> result = new ArrayList<String>(map.size());
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String, Method> e : map.entrySet()) {
			sb.delete(0, sb.length());
			sb.append("<").append(target.getResource().stringValue());
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

	protected Response methodNotAllowed(Request req)
			throws RepositoryException {
		StringBuilder sb = new StringBuilder();
		sb.append("OPTIONS, TRACE");
		for (String method : getAllowedMethods(req)) {
			sb.append(", ").append(method);
		}
		return new Response().status(405).header("Allow", sb.toString());
	}

	private Set<String> getAllowedMethods(Request req)
			throws RepositoryException {
		Set<String> set = new LinkedHashSet<String>();
		String name = req.getOperation();
		if (findGetterMethod(name) != null) {
			set.add("GET");
			set.add("HEAD");
		}
		if (findOperationMethod(name) != null) {
			set.add("POST");
		}
		if (findSetterMethod(name) != null) {
			set.add("PUT");
			set.add("DELETE");
		}
		return set;
	}

	protected String getContentType() throws RepositoryException {
		if (target.getMediaType() != null)
			return target.getMediaType();
		String mimeType = MimeUtil.getMagicMimeType(file);
		if (mimeType == null)
			return "application/octet-stream";
		target.setMediaType(mimeType);
		target.getObjectConnection().commit();
		return mimeType;
	}

	protected Method findGetterMethod(String name)
			throws RepositoryException {
		return getOperationMethods(false, true).get(name);
	}

	protected Method findSetterMethod(String name)
			throws RepositoryException {
		return getOperationMethods(true, false).get(name);
	}

	protected Method findOperationMethod(String name)
			throws RepositoryException {
		return getOperationMethods(true, true).get(name);
	}

	protected Map<String, Method> getOperationMethods(boolean isReqBody, boolean isRespBody) throws RepositoryException {
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
