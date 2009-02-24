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
package org.openrdf.repository.object.config;

import static java.util.Collections.unmodifiableList;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

import org.openrdf.repository.contextaware.config.ContextAwareConfig;
import org.openrdf.repository.object.ObjectRepository;

/**
 * Defines the Scope of an {@link ObjectRepository} and its factory. This
 * includes roles, literals, factories, datasets, and contexts.
 * 
 * @author James Leigh
 * 
 */
public class ObjectRepositoryConfig extends ContextAwareConfig {
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

	private List<URL> jars = new ArrayList<URL>();

	private boolean importJarOntologies = true;

	private List<URL> ontologies = new ArrayList<URL>();

	private String pkgPrefix = "";

	public ObjectRepositoryConfig() {
		super();
		cl = Thread.currentThread().getContextClassLoader();
		if (cl == null) {
			cl = getClass().getClassLoader();
		}
	}

	public ObjectRepositoryConfig(ClassLoader cl) {
		super();
		this.cl = cl;
	}

	public synchronized void setClassLoader(ClassLoader cl) {
		this.cl = cl;
	}

	public synchronized ClassLoader getClassLoader() {
		return cl;
	}

	/**
	 * Include all the information from the given module in this module.
	 * 
	 * @param module
	 *            to be included
	 * @return this
	 */
	public ObjectRepositoryConfig includeModule(ObjectRepositoryConfig module) {
		datatypes.addAll(module.datatypes);
		concepts.addAll(module.concepts);
		behaviours.addAll(module.behaviours);
		jars.addAll(module.jars);
		cl = new CombinedClassLoader(cl, module.cl);
		return this;
	}

	public List<Association> getDatatypes() {
		return unmodifiableList(datatypes);
	}

	public String getPkgPrefix() {
		return pkgPrefix;
	}

	public void setPkgPrefix(String pkgPrefix) {
		if (pkgPrefix == null) {
			this.pkgPrefix = "";
		} else {
			this.pkgPrefix = pkgPrefix;
		}
	}

	/**
	 * Associates this datatype with the given uri within this factory.
	 * 
	 * @param type
	 *            serializable class
	 * @param datatype
	 *            URI
	 */
	public ObjectRepositoryConfig addDatatype(Class<?> type, String uri) {
		datatypes.add(new Association(type, uri));
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
	public ObjectRepositoryConfig addConcept(Class<?> concept) {
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
	public ObjectRepositoryConfig addConcept(Class<?> concept, String type) {
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
	public ObjectRepositoryConfig addBehaviour(Class<?> behaviour) {
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
	public ObjectRepositoryConfig addBehaviour(Class<?> behaviour, String type) {
		behaviours.add(new Association(behaviour, type));
		return this;
	}

	public List<URL> getJarFileUrls() {
		return unmodifiableList(jars);
	}

	public ObjectRepositoryConfig addJar(URL jarFile) {
		jars.add(jarFile);
		return this;
	}

	public boolean isImportJarOntologies() {
		return importJarOntologies;
	}

	public void setImportJarOntologies(boolean importJarOntologies) {
		this.importJarOntologies = importJarOntologies;
	}

	public List<URL> getOntologyUrls() {
		return unmodifiableList(ontologies);
	}

	public ObjectRepositoryConfig addRdfSource(URL ontology) {
		ontologies.add(ontology);
		return this;
	}
}
