/*
 * Copyright (c) 2007, James Leigh All rights reserved.
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
package org.openrdf.elmo;

import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;
import static java.util.Collections.unmodifiableSet;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.xml.namespace.QName;

/**
 * Defines the Scope of an ElmoManager and its factory. This includes roles,
 * literals, factories, datasets, and contexts.
 * 
 * @author James Leigh
 * 
 */
public class ElmoModule {
	public static class Association {
		private Class<?> javaClass;

		private String rdfType;

		private Association(Class<?> javaClass, String rdfType) {
			this.javaClass = javaClass;
			this.rdfType = rdfType;
		}

		public Class<?> getJavaClass() {
			return javaClass;
		}

		public String getRdfType() {
			return rdfType;
		}

		public String toString() {
			return javaClass.getName() + "=" + rdfType;
		}
	}

	private static class CombinedClassLoader extends ClassLoader {
		private ClassLoader alt;

		public CombinedClassLoader(ClassLoader parent, ClassLoader alt) {
			super(parent);
			this.alt = alt;
		}

		@Override
		public URL getResource(String name) {
			URL resource = super.getResource(name);
			if (resource == null)
				return alt.getResource(name);
			return resource;
		}

		@Override
		public InputStream getResourceAsStream(String name) {
			InputStream stream = super.getResourceAsStream(name);
			if (stream == null)
				return alt.getResourceAsStream(name);
			return stream;
		}

		@Override
		public Enumeration<URL> getResources(String name) throws IOException {
			Vector<URL> list = new Vector<URL>();
			Enumeration<URL> e = super.getResources(name);
			while (e.hasMoreElements()) {
				list.add(e.nextElement());
			}
			e = alt.getResources(name);
			while (e.hasMoreElements()) {
				list.add(e.nextElement());
			}
			return list.elements();
		}

		@Override
		public Class<?> loadClass(String name) throws ClassNotFoundException {
			try {
				return super.loadClass(name);
			} catch (ClassNotFoundException e) {
				try {
					return alt.loadClass(name);
				} catch (ClassNotFoundException e2) {
					throw e;
				}
			}
		}

	}

	private ClassLoader cl;

	private List<Association> datatypes = new ArrayList<Association>();

	private List<Association> concepts = new ArrayList<Association>();

	private List<Association> behaviours = new ArrayList<Association>();

	private List<Association> factories = new ArrayList<Association>();

	private QName graph;

	private Set<QName> includedGraphs = new LinkedHashSet<QName>();

	private Map<URL, String> datasets = new HashMap<URL, String>();

	private List<String> resources = new ArrayList<String>();

	private List<URL> jars = new ArrayList<URL>();

	private ClassLoader urlClassLoader;

	public ElmoModule() {
		super();
		cl = Thread.currentThread().getContextClassLoader();
		if (cl == null) {
			cl = getClass().getClassLoader();
		}
	}

	public ElmoModule(ClassLoader cl) {
		super();
		this.cl = cl;
	}

	public synchronized ClassLoader getClassLoader() {
		if (urlClassLoader == null) {
			if (jars.isEmpty()) {
				urlClassLoader = cl;
			} else {
				URL[] array = jars.toArray(new URL[jars.size()]);
				urlClassLoader = new URLClassLoader(array, cl);
			}
		}
		return urlClassLoader;
	}

	public QName getGraph() {
		return graph;
	}

	/**
	 * Sets the primary graph of this module. This limits the readable scope to
	 * this and other included graphs and causes any add operations to be added
	 * to this graph.
	 * 
	 * @param graph
	 * @return this
	 */
	public ElmoModule setGraph(QName graph) {
		this.graph = graph;
		return this;
	}

	@Deprecated
	public ElmoModule setContext(QName graph) {
		return setGraph(graph);
	}

	/**
	 * Include all the information from the given module in this module.
	 * 
	 * @param module
	 *            to be included
	 * @return this
	 */
	public ElmoModule includeModule(ElmoModule module) {
		datatypes.addAll(module.datatypes);
		concepts.addAll(module.concepts);
		behaviours.addAll(module.behaviours);
		factories.addAll(module.factories);
		includedGraphs.add(module.graph);
		includedGraphs.addAll(module.includedGraphs);
		datasets.putAll(module.datasets);
		resources.addAll(module.resources);
		if (!module.jars.isEmpty()) {
			cl = new CombinedClassLoader(cl, module.getClassLoader());
		} else if (!cl.equals(module.cl)) {
			cl = new CombinedClassLoader(cl, module.cl);
		}
		return this;
	}

	public Set<QName> getIncludedGraphs() {
		return unmodifiableSet(includedGraphs);
	}

	public List<Association> getDatatypes() {
		return unmodifiableList(datatypes);
	}

	/**
	 * Associates this datatype with the given uri within this factory.
	 * 
	 * @param type
	 *            serializable class
	 * @param datatype
	 *            URI
	 */
	public ElmoModule addDatatype(Class<?> type, String uri) {
		datatypes.add(new Association(type, uri));
		return this;
	}

	@Deprecated
	public ElmoModule addLiteral(Class<?> type, String uri) {
		return addDatatype(type, uri);
	}

	/**
	 * Associates this role with its default subject type.
	 * 
	 * @param role
	 *            concept or behaviour
	 */
	@Deprecated
	public ElmoModule addRole(Class<?> role) {
		if (role.isInterface()) {
			addConcept(role);
		} else {
			addBehaviour(role);
		}
		return this;
	}

	/**
	 * Associates this role with the given subject type.
	 * 
	 * @param role
	 *            concept or behaviour
	 * @param type
	 *            URI
	 */
	@Deprecated
	public ElmoModule addRole(Class<?> role, String type) {
		if (role.isInterface()) {
			addConcept(role, type);
		} else {
			addBehaviour(role, type);
		}
		return this;
	}

	public List<Association> getConcepts() {
		return unmodifiableList(concepts);
	}

	/**
	 * Associates this concept with its default subject type.
	 * 
	 * @param concept
	 *            interface or class
	 */
	public ElmoModule addConcept(Class<?> concept) {
		concepts.add(new Association(concept, null));
		return this;
	}

	/**
	 * Associates this concept with the given subject type.
	 * 
	 * @param concept
	 *            interface or class
	 * @param type
	 *            URI
	 */
	public ElmoModule addConcept(Class<?> concept, String type) {
		concepts.add(new Association(concept, type));
		return this;
	}

	public List<Association> getBehaviours() {
		return unmodifiableList(behaviours);
	}

	/**
	 * Associates this behaviour with its default subject type.
	 * 
	 * @param behaviour
	 *            class
	 */
	public ElmoModule addBehaviour(Class<?> behaviour) {
		behaviours.add(new Association(behaviour, null));
		return this;
	}

	/**
	 * Associates this behaviour with the given subject type.
	 * 
	 * @param behaviour
	 *            class
	 * @param type
	 *            URI
	 */
	public ElmoModule addBehaviour(Class<?> behaviour, String type) {
		behaviours.add(new Association(behaviour, type));
		return this;
	}

	public List<Association> getFactories() {
		return unmodifiableList(factories);
	}

	/**
	 * Associates this factory with its default subject type.
	 * 
	 * @param factory
	 *            class
	 */
	public ElmoModule addFactory(Class<?> factory) {
		factories.add(new Association(factory, null));
		return this;
	}

	/**
	 * Associates this factory with the given subject type.
	 * 
	 * @param factory
	 *            class
	 * @param type
	 *            URI
	 */
	public ElmoModule addFactory(Class<?> factory, String type) {
		factories.add(new Association(factory, type));
		return this;
	}

	public Map<URL, String> getDatasets() {
		return unmodifiableMap(datasets);
	}

	/**
	 * Marks this dataset to be loaded into the repository under a context of
	 * the same URL.
	 * 
	 * @param dataset
	 * @return this
	 */
	public ElmoModule addDataset(URL dataset) {
		return addDataset(dataset, dataset.toExternalForm());
	}

	/**
	 * Marks this dataset to be loaded and replace any data in the given
	 * context.
	 * 
	 * @param dataset
	 * @param context
	 * @return this
	 */
	public ElmoModule addDataset(URL dataset, String context) {
		datasets.put(dataset, context);
		return this;
	}

	public List<String> getResources() {
		return unmodifiableList(resources);
	}

	/**
	 * Load a resource listing datasets - optionally assigned to a
	 * context
	 * 
	 * @param path
	 * @return
	 */
	public ElmoModule addResources(String path) {
		resources.add(path);
		return this;
	}

	public List<URL> getJarFileUrls() {
		return unmodifiableList(jars);
	}

	public ElmoModule addJarFileUrl(URL jarFile) {
		jars.add(jarFile);
		urlClassLoader = null;
		return this;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((cl == null) ? 0 : cl.hashCode());
		result = prime * result + ((graph == null) ? 0 : graph.hashCode());
		result = prime * result
				+ ((datasets == null) ? 0 : datasets.hashCode());
		result = prime * result
				+ ((factories == null) ? 0 : factories.hashCode());
		result = prime * result
				+ ((includedGraphs == null) ? 0 : includedGraphs.hashCode());
		result = prime * result + ((jars == null) ? 0 : jars.hashCode());
		result = prime * result
				+ ((datatypes == null) ? 0 : datatypes.hashCode());
		result = prime * result
				+ ((resources == null) ? 0 : resources.hashCode());
		result = prime * result
				+ ((concepts == null) ? 0 : concepts.hashCode());
		result = prime * result
				+ ((behaviours == null) ? 0 : behaviours.hashCode());
		result = prime * result
				+ ((urlClassLoader == null) ? 0 : urlClassLoader.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final ElmoModule other = (ElmoModule) obj;
		if (cl == null) {
			if (other.cl != null)
				return false;
		} else if (!cl.equals(other.cl))
			return false;
		if (graph == null) {
			if (other.graph != null)
				return false;
		} else if (!graph.equals(other.graph))
			return false;
		if (datasets == null) {
			if (other.datasets != null)
				return false;
		} else if (!datasets.equals(other.datasets))
			return false;
		if (factories == null) {
			if (other.factories != null)
				return false;
		} else if (!factories.equals(other.factories))
			return false;
		if (includedGraphs == null) {
			if (other.includedGraphs != null)
				return false;
		} else if (!includedGraphs.equals(other.includedGraphs))
			return false;
		if (jars == null) {
			if (other.jars != null)
				return false;
		} else if (!jars.equals(other.jars))
			return false;
		if (datatypes == null) {
			if (other.datatypes != null)
				return false;
		} else if (!datatypes.equals(other.datatypes))
			return false;
		if (resources == null) {
			if (other.resources != null)
				return false;
		} else if (!resources.equals(other.resources))
			return false;
		if (concepts == null) {
			if (other.concepts != null)
				return false;
		} else if (!concepts.equals(other.concepts))
			return false;
		if (behaviours == null) {
			if (other.behaviours != null)
				return false;
		} else if (!behaviours.equals(other.behaviours))
			return false;
		if (urlClassLoader == null) {
			if (other.urlClassLoader != null)
				return false;
		} else if (!urlClassLoader.equals(other.urlClassLoader))
			return false;
		return true;
	}

	@Override
	public String toString() {
		if (graph != null)
			return graph.toString();
		Set<Package> pkg = new LinkedHashSet<Package>();
		for (Association concept : concepts) {
			pkg.add(concept.getJavaClass().getPackage());
		}
		return pkg.toString();
	}
}
