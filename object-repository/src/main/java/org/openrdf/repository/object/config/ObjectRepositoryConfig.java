/*
 * Copyright (c) 2007-2009, James Leigh All rights reserved.
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
import static java.util.Collections.unmodifiableMap;
import static org.openrdf.repository.object.config.ObjectRepositorySchema.BASE_CLASS;
import static org.openrdf.repository.object.config.ObjectRepositorySchema.BEHAVIOUR;
import static org.openrdf.repository.object.config.ObjectRepositorySchema.CONCEPT;
import static org.openrdf.repository.object.config.ObjectRepositorySchema.DATATYPE;
import static org.openrdf.repository.object.config.ObjectRepositorySchema.FOLLOW_IMPORTS;
import static org.openrdf.repository.object.config.ObjectRepositorySchema.IMPORTS;
import static org.openrdf.repository.object.config.ObjectRepositorySchema.IMPORT_JARS;
import static org.openrdf.repository.object.config.ObjectRepositorySchema.JAR;
import static org.openrdf.repository.object.config.ObjectRepositorySchema.KNOWN_AS;
import static org.openrdf.repository.object.config.ObjectRepositorySchema.MEMBER_PREFIX;
import static org.openrdf.repository.object.config.ObjectRepositorySchema.PACKAGE_PREFIX;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openrdf.model.Literal;
import org.openrdf.model.Model;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.util.ModelException;
import org.openrdf.repository.contextaware.config.ContextAwareConfig;
import org.openrdf.repository.object.ObjectRepository;
import org.openrdf.repository.object.exceptions.ObjectStoreConfigException;
import org.openrdf.store.StoreConfigException;

/**
 * Defines the Scope of an {@link ObjectRepository} and its factory. This
 * includes roles, literals, factories, datasets, and contexts.
 * 
 * @author James Leigh
 * 
 */
public class ObjectRepositoryConfig extends ContextAwareConfig implements
		Cloneable {

	private static final String JAVA_NS = "java:";

	private ClassLoader cl;

	private Map<Class<?>, URI> datatypes = new HashMap<Class<?>, URI>();

	private Map<Class<?>, URI> concepts = new HashMap<Class<?>, URI>();

	private Map<Class<?>, URI> behaviours = new HashMap<Class<?>, URI>();

	private List<URL> jars = new ArrayList<URL>();

	private boolean importJarOntologies = true;

	private List<URL> ontologies = new ArrayList<URL>();

	private List<Class<?>> baseClasses = new ArrayList<Class<?>>();

	private String pkgPrefix;

	private String memberPrefix;

	private boolean followImports = true;

	public ObjectRepositoryConfig() {
		super();
		cl = Thread.currentThread().getContextClassLoader();
		if (cl == null) {
			cl = getClass().getClassLoader();
		}
	}

	public ObjectRepositoryConfig(ClassLoader cl) {
		this.cl = cl;
	}

	public void setClassLoader(ClassLoader cl) {
		this.cl = cl;
	}

	public ClassLoader getClassLoader() {
		return cl;
	}

	public List<Class<?>> getBaseClasses() {
		return baseClasses;
	}

	public void addBaseClass(Class<?> base) {
		baseClasses.add(base);
	}

	public String getPackagePrefix() {
		return pkgPrefix;
	}

	public void setPackagePrefix(String pkgPrefix) {
		this.pkgPrefix = pkgPrefix;
	}

	public String getMemberPrefix() {
		return memberPrefix;
	}

	public void setMemberPrefix(String prefix) {
		this.memberPrefix = prefix;
	}

	public Map<Class<?>, URI> getDatatypes() {
		return unmodifiableMap(datatypes);
	}

	/**
	 * Associates this class with the given datatype.
	 * 
	 * @param type
	 *            serializable class
	 * @param datatype
	 *            URI
	 * @throws ObjectStoreConfigException
	 */
	public void addDatatype(Class<?> type, URI datatype)
			throws ObjectStoreConfigException {
		if (datatypes.containsKey(type))
			throw new ObjectStoreConfigException(type.getSimpleName()
					+ " can only be added once");
		datatypes.put(type, datatype);
	}

	public Map<Class<?>, URI> getConcepts() {
		return unmodifiableMap(concepts);
	}

	/**
	 * Associates this concept with its annotated type.
	 * 
	 * @param concept
	 *            interface or class
	 * @throws ObjectStoreConfigException
	 */
	public void addConcept(Class<?> concept) throws ObjectStoreConfigException {
		if (concepts.containsKey(concept))
			throw new ObjectStoreConfigException(concept.getSimpleName()
					+ " can only be added once");
		concepts.put(concept, null);
	}

	/**
	 * Associates this concept with the given type.
	 * 
	 * @param concept
	 *            interface or class
	 * @param type
	 *            URI
	 * @throws ObjectStoreConfigException
	 */
	public void addConcept(Class<?> concept, URI type)
			throws ObjectStoreConfigException {
		if (concepts.containsKey(concept))
			throw new ObjectStoreConfigException(concept.getSimpleName()
					+ " can only be added once");
		concepts.put(concept, type);
	}

	public Map<Class<?>, URI> getBehaviours() {
		return unmodifiableMap(behaviours);
	}

	/**
	 * Associates this behaviour with its implemented type.
	 * 
	 * @param behaviour
	 *            class
	 * @throws ObjectStoreConfigException
	 */
	public void addBehaviour(Class<?> behaviour)
			throws ObjectStoreConfigException {
		if (behaviours.containsKey(behaviour))
			throw new ObjectStoreConfigException(behaviour.getSimpleName()
					+ " can only be added once");
		behaviours.put(behaviour, null);
	}

	/**
	 * Associates this behaviour with the given type.
	 * 
	 * @param behaviour
	 *            class
	 * @param type
	 *            URI
	 * @throws ObjectStoreConfigException
	 */
	public void addBehaviour(Class<?> behaviour, URI type)
			throws ObjectStoreConfigException {
		if (behaviours.containsKey(behaviour))
			throw new ObjectStoreConfigException(behaviour.getSimpleName()
					+ " can only be added once");
		behaviours.put(behaviour, type);
	}

	public List<URL> getJars() {
		return unmodifiableList(jars);
	}

	public void addJar(URL jarFile) {
		jars.add(jarFile);
	}

	public boolean isImportJarOntologies() {
		return importJarOntologies;
	}

	public void setImportJarOntologies(boolean importJarOntologies) {
		this.importJarOntologies = importJarOntologies;
	}

	public List<URL> getImports() {
		return unmodifiableList(ontologies);
	}

	public void addImports(URL ontology) {
		ontologies.add(ontology);
	}

	public boolean isFollowImports() {
		return followImports;
	}

	public void setFollowImports(boolean followImports) {
		this.followImports = followImports;
	}

	/**
	 * Include all the information from the given module in this module.
	 * 
	 * @param module
	 *            to be included
	 * @return this
	 */
	public ObjectRepositoryConfig clone() {
		try {
			Object o = super.clone();
			ObjectRepositoryConfig clone = (ObjectRepositoryConfig) o;
			clone.setReadContexts(copy(clone.getReadContexts()));
			clone.setAddContexts(copy(clone.getAddContexts()));
			clone.setRemoveContexts(copy(clone.getRemoveContexts()));
			clone.setArchiveContexts(copy(clone.getArchiveContexts()));
			clone.datatypes = new HashMap<Class<?>, URI>(datatypes);
			clone.concepts = new HashMap<Class<?>, URI>(concepts);
			clone.behaviours = new HashMap<Class<?>, URI>(behaviours);
			clone.jars = new ArrayList<URL>(jars);
			clone.ontologies = new ArrayList<URL>(ontologies);
			clone.baseClasses = new ArrayList<Class<?>>(baseClasses);
			Model model = new LinkedHashModel();
			Resource subj = clone.export(model);
			clone.parse(model, subj);
			return clone;
		} catch (StoreConfigException e) {
			throw new AssertionError(e);
		} catch (CloneNotSupportedException e) {
			throw new AssertionError(e);
		}
	}

	@Override
	public Resource export(Model model) {
		ValueFactory vf = ValueFactoryImpl.getInstance();
		Resource subj = super.export(model);
		for (Class<?> base : baseClasses) {
			model.add(subj, BASE_CLASS, vf.createURI(JAVA_NS, base.getName()));
		}
		if (pkgPrefix != null) {
			model.add(subj, PACKAGE_PREFIX, vf.createLiteral(pkgPrefix));
		}
		if (memberPrefix != null) {
			model.add(subj, MEMBER_PREFIX, vf.createLiteral(memberPrefix));
		}
		exportAssocation(subj, datatypes, DATATYPE, model);
		exportAssocation(subj, concepts, CONCEPT, model);
		exportAssocation(subj, behaviours, BEHAVIOUR, model);
		Literal bool = vf.createLiteral(importJarOntologies);
		model.add(subj, IMPORT_JARS, bool);
		bool = vf.createLiteral(followImports);
		model.add(subj, FOLLOW_IMPORTS, bool);
		for (URL jar : jars) {
			model.add(subj, JAR, vf.createURI(jar.toExternalForm()));
		}
		for (URL url : ontologies) {
			model.add(subj, IMPORTS, vf.createURI(url.toExternalForm()));
		}
		return subj;
	}

	@Override
	public void parse(Model model, Resource subj) throws StoreConfigException {
		super.parse(model, subj);
		try {
			baseClasses.clear();
			for (Value base : model.filter(subj, BASE_CLASS, null).objects()) {
				baseClasses.add(loadClass(base));
			}
			pkgPrefix = model.filter(subj, PACKAGE_PREFIX, null).objectString();
			memberPrefix = model.filter(subj, MEMBER_PREFIX, null)
					.objectString();
			parseAssocation(subj, datatypes, DATATYPE, model);
			parseAssocation(subj, concepts, CONCEPT, model);
			parseAssocation(subj, behaviours, BEHAVIOUR, model);
			Literal bool = model.filter(subj, IMPORT_JARS, null)
					.objectLiteral();
			if (bool != null) {
				importJarOntologies = bool.booleanValue();
			}
			jars.clear();
			for (Value obj : model.filter(subj, JAR, null).objects()) {
				jars.add(new URL(obj.stringValue()));
			}
			ontologies.clear();
			for (Value obj : model.filter(subj, IMPORTS, null).objects()) {
				ontologies.add(new URL(obj.stringValue()));
			}
		} catch (MalformedURLException e) {
			throw new ObjectStoreConfigException(e);
		} catch (ModelException e) {
			throw new ObjectStoreConfigException(e);
		}
	}

	private URI[] copy(URI[] ar) {
		URI[] result = new URI[ar.length];
		System.arraycopy(ar, 0, result, 0, ar.length);
		return result;
	}

	private void exportAssocation(Resource subj, Map<Class<?>, URI> assocation,
			URI relation, Model model) {
		ValueFactory vf = ValueFactoryImpl.getInstance();
		for (Map.Entry<Class<?>, URI> e : assocation.entrySet()) {
			URI name = vf.createURI(JAVA_NS, e.getKey().getName());
			model.add(subj, relation, name);
			if (e.getValue() != null) {
				model.add(name, KNOWN_AS, e.getValue());
			}
		}
	}

	private void parseAssocation(Resource subj, Map<Class<?>, URI> assocation,
			URI relation, Model model) throws ObjectStoreConfigException {
		assocation.clear();
		for (Value obj : model.filter(subj, relation, null).objects()) {
			Class<?> role = loadClass(obj);
			URI uri = model.filter((Resource) obj, KNOWN_AS, null).objectURI();
			assocation.put(role, uri);
		}
	}

	private Class<?> loadClass(Value base) throws ObjectStoreConfigException {
		if (base instanceof URI) {
			URI uri = (URI) base;
			if (JAVA_NS.equals(uri.getNamespace())) {
				String name = uri.getLocalName();
				try {
					return Class.forName(name, true, cl);
				} catch (ClassNotFoundException e) {
					throw new ObjectStoreConfigException(e);
				}
			}
		}
		throw new ObjectStoreConfigException("Invalid java URI: " + base);
	}
}
