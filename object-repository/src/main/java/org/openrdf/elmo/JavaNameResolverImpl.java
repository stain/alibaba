/*
 * Copyright (c) 2008, Zepheira All rights reserved.
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
package org.openrdf.elmo.codegen;

import static org.openrdf.query.QueryLanguage.SPARQL;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;

import org.openrdf.elmo.LiteralManager;
import org.openrdf.elmo.RoleMapper;
import org.openrdf.elmo.annotations.rdf;
import org.openrdf.model.Literal;
import org.openrdf.model.Namespace;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;

public class JavaNameResolverImpl implements JavaNameResolver {
	private static final String FILTER_REGEX_PATTERN = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
			+ "SELECT ?thing\n"
			+ "WHERE { ?thing rdf:type ?type .\n"
			+ "	FILTER regex(str(?thing), ?pattern, \"i\")}";

	/** namespace -&gt; package */
	private Map<String, String> packages = new HashMap<String, String>();

	/** namespace -&gt; prefix */
	private Map<String, String> prefixes = new HashMap<String, String>();

	private Map<QName, QName> aliases = new HashMap<QName, QName>();

	private RoleMapper<URI> roles;

	private LiteralManager<URI, Literal> literals;

	private ClassLoaderPackages cl;

	private Repository repository;

	private Set<String> nouns = new HashSet<String>();

	private static class ClassLoaderPackages extends ClassLoader {
		public ClassLoaderPackages(ClassLoader parent) {
			super(parent);
		}

		@Override
		public Package[] getPackages() {
			return super.getPackages();
		}
	}

	public JavaNameResolverImpl() {
		this(Thread.currentThread().getContextClassLoader());
	}

	public JavaNameResolverImpl(ClassLoader cl) {
		this.cl = new ClassLoaderPackages(cl);
	}

	public void setLiteralManager(LiteralManager<URI, Literal> literals) {
		this.literals = literals;
	}

	public void setRoleMapper(RoleMapper<URI> roles) {
		this.roles = roles;
	}

	public void setRepository(Repository repository) throws RepositoryException {
		this.repository = repository;
		Set<String> localNames = new HashSet<String>();
		RepositoryConnection con = repository.getConnection();
		try {
			RepositoryResult<Namespace> result = con.getNamespaces();
			try {
				while (result.hasNext()) {
					Namespace ns = result.next();
					bindPrefixToNamespace(ns.getPrefix(), ns.getName());
				}
			} finally {
				result.close();
			}
			RepositoryResult<Statement> stmts;
			stmts = con.getStatements(null, null, null, false);
			try {
				while (stmts.hasNext()) {
					Resource subj = stmts.next().getSubject();
					if (subj instanceof URI) {
						localNames.add(((URI) subj).getLocalName());
					}
				}
			} finally {
				stmts.close();
			}
		} finally {
			con.close();
		}
		for (String name : localNames) {
			if (!name.matches(".[A-Z_-]")) {
				nouns.add(name.toLowerCase());
			}
		}
	}

	public void assignAlias(QName name, QName alias) {
		aliases.put(name, alias);
	}

	public void assignAnonymous(QName name) {
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

	public QName getType(QName name) {
		if (aliases.containsKey(name))
			return aliases.get(name);
		return name;
	}

	public String getClassName(QName name) {
		if (name == null)
			return Object.class.getName();
		if (!packages.containsKey(name.getNamespaceURI())) {
			Class javaClass = findJavaClass(name);
			if (javaClass != null) {
				// TODO support n-dimension arrays
				if (javaClass.isArray())
					return javaClass.getComponentType().getName() + "[]";
				if (javaClass.getPackage() != null)
					return javaClass.getName();
			}
		}
		String pkg = getPackageName(name);
		String simple = initcap(name.getLocalPart());
		if (pkg == null)
			return simple;
		return pkg + '.' + simple;
	}

	public String getMethodName(QName name) {
		String ns = name.getNamespaceURI();
		String localPart = name.getLocalPart();
		if (prefixes.containsKey(ns))
			return prefixes.get(ns) + initcap(localPart);
		return enc(localPart);
	}

	public String getPackageName(QName qname) {
		if (packages.containsKey(qname.getNamespaceURI()))
			return packages.get(qname.getNamespaceURI());
		Class javaClass = findJavaClass(qname);
		if (javaClass == null || javaClass.getPackage() == null)
			return null;
		return javaClass.getPackage().getName();
	}

	public String getPropertyName(QName name) {
		String ns = name.getNamespaceURI();
		String localPart = name.getLocalPart();
		if (prefixes.containsKey(ns))
			return prefixes.get(ns) + initcap(localPart);
		return enc(localPart);
	}

	public String getPluralPropertyName(QName name) {
		String ns = name.getNamespaceURI();
		String localPart = name.getLocalPart();
		if (prefixes.containsKey(ns))
			return prefixes.get(ns) + plural(initcap(localPart));
		return plural(enc(localPart));
	}

	public String getSimpleName(QName name) {
		return initcap(name.getLocalPart());
	}

	private URIImpl asURI(QName qname) {
		return new URIImpl(qname.getNamespaceURI() + qname.getLocalPart());
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

	private Class findJavaClass(QName qname) {
		if (qname.equals(new QName(RDF.NAMESPACE, "XMLLiteral")))
			return literals.getClass(asURI(qname));
		Class klass = findBeanClassName(qname);
		if (klass != null)
			return klass;
		klass = findLoadedMethod(qname);
		if (klass != null)
			return klass;
		return literals.getClass(asURI(qname));
	}

	private Class findBeanClassName(QName qname) {
		URIImpl uri = asURI(qname);
		boolean recorded = roles.isTypeRecorded(uri);
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

	private Class findLoadedMethod(QName qname) {
		if (cl == null)
			return null;
		String sn = getSimpleName(qname);
		for (Package pkg : cl.getPackages()) {
			if (pkg.isAnnotationPresent(rdf.class)) {
				String namespace = pkg.getAnnotation(rdf.class).value()[0];
				if (qname.getNamespaceURI().equals(namespace)) {
					try {
						return Class.forName(pkg.getName() + '.' + sn);
					} catch (ClassNotFoundException e) {
						continue;
					}
				}
			}
		}
		return null;
	}

	private String plural(String singular) {
		if (singular.matches(".*[A-Z_-].*")
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
		if (nouns != null)
		return nouns.contains(word);
		try {
			if (repository == null)
				return false;
			RepositoryConnection con = repository.getConnection();
			try {
				TupleQuery query = con.prepareTupleQuery(SPARQL,
						FILTER_REGEX_PATTERN);
				ValueFactory vf = repository.getValueFactory();
				query.setBinding("pattern", vf.createLiteral("[#/:]$word\\$"));
				TupleQueryResult result = query.evaluate();
				try {
					return result.hasNext();
				} finally {
					result.close();
				}
			} finally {
				con.close();
			}
		} catch (QueryEvaluationException e) {
			return false;
		} catch (RepositoryException exc) {
			return false;
		} catch (MalformedQueryException exc) {
			throw new AssertionError(exc);
		}
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
