package org.openrdf.elmo.rolemapper;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.openrdf.elmo.RoleMapper;
import org.openrdf.elmo.annotations.factory;
import org.openrdf.elmo.exceptions.ElmoInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RoleClassLoader<URI> {
	private final Logger logger = LoggerFactory.getLogger(DirectMapper.class);

	private RoleMapper<URI> roleMapper;

	private ClassLoader cl;

	public void setRoleMapper(RoleMapper<URI> roleMapper) {
		this.roleMapper = roleMapper;
	}

	public void setClassLoader(ClassLoader cl) {
		this.cl = cl;
	}

	/**
	 * Loads and registers roles listed in resource.
	 */
	public void loadClasses(String roles, boolean concept) {
		try {
			ClassLoader elmo = RoleClassLoader.class.getClassLoader();
			load(cl, roles, concept, load(elmo, roles, concept, new HashSet<URL>()));
		} catch (Exception e) {
			throw new ElmoInitializationException(e);
		}
	}

	public void scan(URL url, String... roles) {
		try {
			Scanner scanner = new Scanner(cl);
			load(scanner.scan(url, roles), cl, false);
		} catch (Exception e) {
			throw new ElmoInitializationException(e);
		}
	}

	private Set<URL> load(ClassLoader cl, String roles, boolean concept, Set<URL> exclude)
			throws IOException, ClassNotFoundException {
		if (cl == null)
			return exclude;
		Scanner scanner = new Scanner(cl, roles);
		Enumeration<URL> resources = cl.getResources(roles);
		while (resources.hasMoreElements()) {
			URL url = resources.nextElement();
			if (!exclude.contains(url)) {
				exclude.add(url);
				logger.debug("Reading roles from {}", url);
				try {
					Properties p = new Properties();
					p.load(url.openStream());
					if (p.isEmpty()) {
						load(scanner.scan(url), cl, concept);
					} else {
						load(p, cl, concept);
					}
				} catch (IOException e) {
					String msg = e.getMessage() + " in: " + url;
					IOException ioe = new IOException(msg);
					ioe.initCause(e);
					throw ioe;
				}
			}
		}
		return exclude;
	}

	private void load(List<String> roles, ClassLoader cl, boolean concept)
			throws ClassNotFoundException, IOException {
		for (String role : roles) {
			try {
				Class<?> clazz = Class.forName(role, true, cl);
				recordRole(clazz, null, concept);
			} catch (Exception e) {
				logger.error(e.toString(), e);
			}
		}
	}

	private void load(Properties p, ClassLoader cl, boolean concept)
			throws ClassNotFoundException, IOException {
		for (Map.Entry<Object, Object> e : p.entrySet()) {
			String role = (String) e.getKey();
			String types = (String) e.getValue();
			try {
				Class<?> clazz = Class.forName(role, true, cl);
				for (String rdf : types.split("\\s+")) {
					recordRole(clazz, rdf, concept);
				}
			} catch (Exception exc) {
				logger.error(exc.toString(), exc);
			}
		}
	}

	private void recordRole(Class<?> clazz, String uri, boolean concept) {
		if (uri == null || uri.length() == 0) {
			if (clazz.isInterface() || concept) {
				roleMapper.addConcept(clazz);
			} else if (isFactory(clazz)) {
				roleMapper.addFactory(clazz);
			} else {
				roleMapper.addBehaviour(clazz);
			}
		} else {
			if (clazz.isInterface() || concept) {
				roleMapper.addConcept(clazz, uri);
			} else if (isFactory(clazz)) {
				roleMapper.addFactory(clazz);
			} else {
				roleMapper.addBehaviour(clazz, uri);
			}
		}
	}

	private boolean isFactory(Class<?> clazz) {
		if (Modifier.isAbstract(clazz.getModifiers()))
			return false;
		for (Method m : clazz.getMethods()) {
			if (m.isAnnotationPresent(factory.class))
				return true;
		}
		return false;
	}
}
