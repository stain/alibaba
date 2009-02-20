/*
 * Copyright (c) 2007-2008, James Leigh All rights reserved.
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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.xml.namespace.QName;

import org.openrdf.concepts.owl.Ontology;
import org.openrdf.concepts.rdfs.Datatype;
import org.openrdf.elmo.ElmoQuery;
import org.openrdf.elmo.Entity;
import org.openrdf.elmo.codegen.concepts.CodeClass;
import org.openrdf.elmo.codegen.concepts.CodeMessageClass;
import org.openrdf.elmo.codegen.concepts.CodeOntology;
import org.openrdf.elmo.sesame.SesameManager;
import org.openrdf.elmo.sesame.SesameManagerFactory;
import org.openrdf.elmo.sesame.roles.SesameEntity;
import org.openrdf.model.URI;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converts OWL ontologies into JavaBeans. This class can be used to create Elmo
 * concepts or other JavaBean interfaces or classes.
 * 
 * @author James Leigh
 * 
 */
public class CodeGenerator {

	private static final String JAVA_NS = "java:";

	private static final String SELECT_CLASSES = "PREFIX rdfs: <"
			+ RDFS.NAMESPACE
			+ "> PREFIX owl: <"
			+ OWL.NAMESPACE
			+ "> SELECT DISTINCT ?bean WHERE { { ?bean a owl:Class } UNION {?bean a rdfs:Datatype } }";

	final Logger logger = LoggerFactory.getLogger(CodeGenerator.class);

	private SesameManagerFactory factory;

	private OwlNormalizer normalizer;

	/** namespace -&gt; package */
	private Map<String, String> packages = new HashMap<String, String>();

	private String propertyNamesPrefix;

	private JavaNameResolverImpl resolver;

	private Class<?>[] baseClasses = new Class<?>[0];

	private List<Thread> threads = new ArrayList<Thread>();

	private Exception exception;

	BlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>();

	Runnable helper = new Runnable() {
		public void run() {
			try {
				for (Runnable r = queue.take(); r != helper; r = queue.take()) {
					r.run();
				}
			} catch (InterruptedException e) {
				logger.error(e.toString(), e);
			}
		}
	};

	public Class<?>[] getBaseClasses() {
		return baseClasses;
	}

	public void setBaseClasses(Class<?>[] baseClasses) {
		this.baseClasses = baseClasses;
	}

	public String getPropertyNamesPrefix() {
		return propertyNamesPrefix;
	}

	public void setPropertyNamesPrefix(String prefixPropertyNames) {
		this.propertyNamesPrefix = prefixPropertyNames;
	}

	public void bindPackageToNamespace(String pkgName, String namespace) {
		packages.put(namespace, pkgName);
	}

	public void setSesameManagerFactory(SesameManagerFactory factory) {
		this.factory = factory;
	}

	public void setJavaNameResolver(JavaNameResolverImpl resolver) {
		this.resolver = resolver;
	}

	public void init() throws Exception {
		normalizer = new OwlNormalizer();
		SesameManager manager = factory.createElmoManager();
		try {
			normalizer.setSesameManager(manager);
			normalizer.normalize();
			for (URI uri : normalizer.getAnonymousClasses()) {
				String ns = uri.getNamespace();
				QName name = new QName(ns, uri.getLocalName());
				resolver.assignAnonymous(name);
			}
			for (Map.Entry<URI, URI> e : normalizer.getAliases().entrySet()) {
				String ns1 = e.getKey().getNamespace();
				QName name = new QName(ns1, e.getKey().getLocalName());
				String ns2 = e.getValue().getNamespace();
				QName alias = new QName(ns2, e.getValue().getLocalName());
				resolver.assignAlias(name, alias);
			}
		} finally {
			manager.close();
		}
		for (int i = 0; i < 3; i++) {
			threads.add(new Thread(helper));
		}
		for (Thread thread : threads) {
			thread.start();
		}
	}

	public void exportSourceCode(final FileSourceCodeHandler handler)
			throws Exception {
		final SesameManager manager = factory.createElmoManager();
		try {
			ElmoQuery query;
			query = manager.createQuery(SELECT_CLASSES);
			for (Object o : query.getResultList()) {
				final SesameEntity bean = (SesameEntity) o;
				if (bean.getQName() == null)
					continue;
				String namespace = bean.getQName().getNamespaceURI();
				if (packages.containsKey(namespace)) {
					addBaseClass(manager, bean);
					final String pkg = packages.get(namespace);
					queue.add(new Runnable() {
						public void run() {
							buildClassOrDatatype(bean, pkg, manager, handler);
						}
					});
				}
			}
			for (int i = 0, n = threads.size(); i < n; i++) {
				queue.add(helper);
			}
			for (String namespace : packages.keySet()) {
				buildPackage(namespace, manager, handler);
			}
			for (Thread thread : threads) {
				thread.join();
			}
			if (exception != null)
				throw exception;
		} finally {
			manager.close();
		}
	}

	private void addBaseClass(SesameManager manager, SesameEntity bean) {
		org.openrdf.concepts.rdfs.Class klass;
		klass = (org.openrdf.concepts.rdfs.Class) bean;
		Class<org.openrdf.concepts.rdfs.Class> rdfsClass = org.openrdf.concepts.rdfs.Class.class;
		if (!containKnownNamespace(klass.getRdfsSubClassOf())) {
			for (Class b : baseClasses) {
				QName name = new QName(JAVA_NS, b.getName());
				klass.getRdfsSubClassOf().add(
						manager.designate(name, rdfsClass));
			}
		}
	}

	private boolean containKnownNamespace(Set<? extends Entity> set) {
		boolean contain = false;
		for (Entity e : set) {
			QName name = e.getQName();
			if (name == null)
				continue;
			if (packages.containsKey(name.getNamespaceURI())) {
				contain = true;
			}
		}
		return contain;
	}

	private void buildClassOrDatatype(SesameEntity bean, String packageName,
			SesameManager manager, FileSourceCodeHandler handler) {
		try {
			if (bean instanceof Datatype) {
				buildDatatype(bean, packageName, manager, handler);
			} else {
				buildClass(bean, packageName, manager, handler);
			}
		} catch (Exception exc) {
			logger.error("Error processing {}", bean);
			if (exception == null) {
				exception = exc;
			}
		}
	}

	private void buildClass(SesameEntity bean, String packageName,
			SesameManager manager, FileSourceCodeHandler handler)
			throws Exception {
		File dir = handler.getTarget();
		File file = ((CodeClass) bean).generateSourceCode(dir, resolver);
		handler.handleSource(file);
		if (bean instanceof CodeMessageClass) {
			CodeMessageClass msg = (CodeMessageClass) bean;
			if (msg.isMessageClass() && resolver.getType(msg.getQName()) != null) {
				file = msg.generateInvokeSourceCode(dir, resolver);
				handler.handleSource(file);
			}
		}
	}

	private void buildDatatype(SesameEntity bean, String packageName,
			SesameManager manager, FileSourceCodeHandler handler)
			throws Exception {
		File dir = handler.getTarget();
		File file = ((CodeClass) bean).generateSourceCode(dir, resolver);
		handler.handleSource(file);
	}

	private void buildPackage(String namespace, SesameManager manager,
			FileSourceCodeHandler handler) throws Exception {
		Ontology ont = findOntology(namespace, manager);
		File dir = handler.getTarget();
		CodeOntology code = (CodeOntology) ont;
		File file = code.generatePackageInfo(dir, namespace, resolver);
		handler.handleSource(file);
	}

	private Ontology findOntology(String namespace, SesameManager manager) {
		if (namespace.endsWith("#"))
			return manager.designate(new QName(namespace
					.substring(0, namespace.length() - 1)), Ontology.class);
		return manager.designate(new QName(namespace), Ontology.class);
	}
}