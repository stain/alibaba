/*
 * Copyright (c) 2008-2009, Zepheira All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution. 
 * - Neither the name of the openrdf.org nor the names of its contributors may
 *   be used to endorse or promote products derived from this software without
 *   specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 */
package org.openrdf.repository.object.compiler;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.openrdf.model.Model;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.object.annotations.prefix;
import org.openrdf.repository.object.annotations.rdf;
import org.openrdf.repository.object.exceptions.ObjectStoreConfigException;
import org.openrdf.repository.object.managers.LiteralManager;
import org.openrdf.repository.object.managers.RoleMapper;
import org.openrdf.repository.object.vocabulary.OBJ;

/**
 * Resolves appropriate Java names from URIs.
 * 
 * @author James Leigh
 *
 */
public class JavaNameResolver {

	/** namespace -&gt; package */
	private Map<String, String> packages = new HashMap<String, String>();

	/** namespace -&gt; prefix */
	private Map<String, String> prefixes = new HashMap<String, String>();

	private Map<URI, URI> aliases = new HashMap<URI, URI>();

	private Model model;

	private RoleMapper roles;

	private LiteralManager literals;

	private ClassLoaderPackages cl;

	private Set<String> nouns = new HashSet<String>();

	private static class ClassLoaderPackages extends ClassLoader {
		private Set<Package> namespacePackages;

		public ClassLoaderPackages(ClassLoader parent) {
			super(parent);
			namespacePackages = new HashSet<Package>();
			for (Package pkg : getPackages()) {
				if (pkg.isAnnotationPresent(rdf.class)) {
					namespacePackages.add(pkg);
				}
			}
		}

		public Set<Package> getNamespacePackages() {
			return namespacePackages;
		}
	}

	public JavaNameResolver() {
		this(Thread.currentThread().getContextClassLoader());
	}

	public JavaNameResolver(ClassLoader cl) {
		this.cl = new ClassLoaderPackages(cl);
		for (Package pkg : this.cl.getNamespacePackages()) {
			if (pkg.isAnnotationPresent(prefix.class)) {
				String prefix = pkg.getAnnotation(prefix.class).value();
				String ns = pkg.getAnnotation(rdf.class).value();
				prefixes.put(ns, prefix);
			}
		}
	}

	public void setLiteralManager(LiteralManager literals) {
		this.literals = literals;
	}

	public void setRoleMapper(RoleMapper roles) {
		this.roles = roles;
	}

	public void setModel(Model model) {
		this.model = model;
		Set<String> localNames = new HashSet<String>();
		for (Resource subj : model.filter(null, RDF.TYPE, null).subjects()) {
			if (subj instanceof URI) {
				localNames.add(((URI) subj).getLocalName());
			}
		}
		for (String name : localNames) {
			if (name.matches("^[a-zA-Z][a-z]+$")) {
				nouns.add(name.toLowerCase());
			}
		}
	}

	public void assignAlias(URI name, URI alias) {
		aliases.put(name, alias);
	}

	public void assignAnonymous(URI name) {
		aliases.put(name, null);
	}

	public void bindPackageToNamespace(String packageName, String namespace) {
		packages.put(namespace, packageName);
	}

	public void bindPrefixToNamespace(String prefix, String namespace) {
		if (prefix == null || prefix.length() == 0) {
			prefixes.remove(namespace);
		} else {
			prefixes.put(namespace, prefix);
		}
	}

	public URI getType(URI name) {
		if (aliases.containsKey(name))
			return aliases.get(name);
		return name;
	}

	public boolean isAnonymous(URI name) {
		return getType(name) == null;
	}

	public String getClassName(URI name) throws ObjectStoreConfigException {
		if (name == null)
			return Object.class.getName();
		if (model.contains(name, OBJ.CLASS_NAME, null))
			return model.filter(name, OBJ.CLASS_NAME, null).objectString();
		Class javaClass = findJavaClass(name);
		if (javaClass != null) {
			// TODO support n-dimension arrays
			if (javaClass.isArray())
				return javaClass.getComponentType().getName() + "[]";
			if (javaClass.getPackage() != null)
				return javaClass.getName();
		}
		if (!packages.containsKey(name.getNamespace()))
			throw new ObjectStoreConfigException("Unknown type: " + name);
		String pkg = getPackageName(name);
		String simple = enc(name.getLocalName());
		if (pkg == null)
			return simple;
		return pkg + '.' + simple;
	}

	public boolean isCompiledAnnotation(URI name) {
		Class ann = findJavaClass(name);
		if (ann == null)
			return false;
		return ann.isAnnotation();
	}

	public boolean isAnnotationOfClasses(URI name) {
		Class javaClass = findJavaClass(name);
		if (javaClass == null)
			return false;
		try {
			Class<?> type = javaClass.getMethod("value").getReturnType();
			return type.equals(Class.class)
					|| type.getComponentType().equals(Class.class);
		} catch (NoSuchMethodException e) {
			return false;
		}
	}

	public String getMethodName(URI name) {
		if (model.contains(name, OBJ.NAME, null))
			return model.filter(name, OBJ.NAME, null).objectString();
		String ns = name.getNamespace();
		String localPart = enc(name.getLocalName());
		if (prefixes.containsKey(ns))
			return prefixes.get(ns) + initcap(localPart);
		return localPart;
	}

	public String getPackageName(URI uri) {
		if (model.contains(uri, OBJ.CLASS_NAME, null)) {
			String className = model.filter(uri, OBJ.CLASS_NAME, null)
					.objectString();
			int idx = className.lastIndexOf('.');
			if (idx > 0)
				return className.substring(0, idx);
			return null;
		}
		if (packages.containsKey(uri.getNamespace()))
			return packages.get(uri.getNamespace());
		Class javaClass = findJavaClass(uri);
		if (javaClass == null || javaClass.getPackage() == null)
			return null;
		return javaClass.getPackage().getName();
	}

	public String getMemberName(URI name) {
		if (model.contains(name, OBJ.NAME, null))
			return model.filter(name, OBJ.NAME, null).objectString();
		String ns = name.getNamespace();
		String localPart = name.getLocalName();
		if (prefixes.containsKey(ns))
			return prefixes.get(ns) + initcap(localPart);
		return enc(localPart);
	}

	public String getMemberPrefix(String ns) {
		if (prefixes.containsKey(ns))
			return prefixes.get(ns);
		return "";
	}

	public String getPluralPropertyName(URI name) {
		if (model.contains(name, OBJ.NAME, null))
			return model.filter(name, OBJ.NAME, null).objectString();
		String ns = name.getNamespace();
		String localPart = name.getLocalName();
		if (prefixes.containsKey(ns))
			return prefixes.get(ns) + plural(initcap(localPart));
		return plural(enc(localPart));
	}

	public String getSimpleName(URI name) {
		if (model.contains(name, OBJ.CLASS_NAME, null)) {
			String className = model.filter(name, OBJ.CLASS_NAME, null)
					.objectString();
			int idx = className.lastIndexOf('.');
			if (idx > 0)
				return className.substring(idx + 1);
			return className;
		}
		return enc(name.getLocalName());
	}

	private String enc(String str) {
		if (str.length() == 0)
			return "";
		char[] name = str.toCharArray();
		StringBuffer sb = new StringBuffer(name.length);
		for (int i = 0; i < name.length; i++) {
			if (name[i] == '-' || name[i] == '.') {
				name[i + 1] = Character.toUpperCase(name[i + 1]);
			} else {
				sb.append(name[i]);
			}
		}
		return sb.toString();
	}

	private Class findJavaClass(URI uri) {
		if (uri.equals(RDF.XMLLITERAL))
			return literals.findClass(uri);
		Class klass = findBeanClassName(uri);
		if (klass != null)
			return klass;
		klass = findLoadedMethod(uri);
		if (klass != null)
			return klass;
		klass = roles.findAnnotationType(uri);
		if (klass != null)
			return klass;
		return literals.findClass(uri);
	}

	private Class findBeanClassName(URI uri) {
		boolean recorded = roles.isRecordedConcept(uri);
		if (recorded) {
			Collection<Class<?>> rs = roles.findRoles(uri);
			for (Class r : rs) {
				if (r.isInterface() && uri.equals(roles.findType(r))
						&& r.getSimpleName().equals(uri.getLocalName())) {
					return r;
				}
			}
			for (Class r : rs) {
				if (r.isInterface() && uri.equals(roles.findType(r))) {
					return r;
				}
			}
		}
		return null;
	}

	private Class findLoadedMethod(URI URI) {
		if (cl == null)
			return null;
		String sn = getSimpleName(URI);
		for (Package pkg : cl.getNamespacePackages()) {
			String namespace = pkg.getAnnotation(rdf.class).value();
			if (URI.getNamespace().equals(namespace)) {
				try {
					return Class.forName(pkg.getName() + '.' + sn);
				} catch (ClassNotFoundException e) {
					continue;
				}
			}
		}
		return null;
	}

	private String plural(String singular) {
		if (singular.matches(".+[A-Z_-].*")
				&& !isNoun(singular.replaceAll(".*(?=[A-Z])|.*[_-]", ""))) {
			return singular;
		} else if (singular.endsWith("s") && !singular.endsWith("ss")) {
			return singular;
		} else if (singular.endsWith("ed")) {
			return singular;
		} else if (singular.endsWith("y") && (singular.length() > 1)) {
			char c = singular.charAt(singular.length() - 2);
			if (c == 'a' || c == 'o' || c == 'e' || c == 'u' || c == 'i') {
				return singular + "s";
			} else {
				return singular.substring(0, singular.length() - 1) + "ies";
			}
		} else if (singular.endsWith("s") || singular.endsWith("x")) {
			return singular + "es";
		} else {
			return singular + "s";
		}
	}

	/**
	 * If this is word is a thing in our repository it is a noun. An alternative
	 * is to use a wordnet database.
	 */
	private boolean isNoun(String word) {
		return nouns.contains(word.toLowerCase());
	}

	private String initcap(String str) {
		if (str.length() == 0)
			return "";
		char[] name = str.toCharArray();
		StringBuffer sb = new StringBuffer(name.length);
		for (int i = 0; i < name.length; i++) {
			if (i == 0) {
				sb.append(Character.toUpperCase(name[i]));
			} else if (name[i] == '-' || name[i] == '.') {
				name[i + 1] = Character.toUpperCase(name[i + 1]);
			} else {
				sb.append(name[i]);
			}
		}
		String string = sb.toString();
		if (!Character.isLetter(string.charAt(0))) {
			string = "_" + string;
		}
		return string;
	}
}
