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
package org.openrdf.repository.object.codegen;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.openrdf.model.Model;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.repository.object.codegen.model.RDFClass;
import org.openrdf.repository.object.codegen.model.RDFEntity;
import org.openrdf.repository.object.codegen.model.RDFOntology;
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

	final Logger logger = LoggerFactory.getLogger(CodeGenerator.class);

	private Model model;

	private OwlNormalizer normalizer;

	/** namespace -&gt; package */
	private Map<String, String> packages = new HashMap<String, String>();

	private String propertyNamesPrefix;

	private JavaNameResolver resolver = new JavaNameResolver();

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

	public CodeGenerator(Model model) {
		this.model = model;
	}

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

	public void setJavaNameResolver(JavaNameResolver resolver) {
		this.resolver = resolver;
	}

	public void init() throws Exception {
		normalizer = new OwlNormalizer(new RDFDataSource(model));
		normalizer.normalize();
		for (URI uri : normalizer.getAnonymousClasses()) {
			String ns = uri.getNamespace();
			URI name = new URIImpl(ns + uri.getLocalName());
			resolver.assignAnonymous(name);
		}
		for (Map.Entry<URI, URI> e : normalizer.getAliases().entrySet()) {
			String ns1 = e.getKey().getNamespace();
			URI name = new URIImpl(ns1 + e.getKey().getLocalName());
			String ns2 = e.getValue().getNamespace();
			URI alias = new URIImpl(ns2 + e.getValue().getLocalName());
			resolver.assignAlias(name, alias);
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
		Set<Resource> classes = model.filter(null, RDF.TYPE, OWL.CLASS)
				.subjects();
		for (Resource o : new ArrayList<Resource>(classes)) {
			final RDFClass bean = new RDFClass(model, o);
			if (bean.getURI() == null)
				continue;
			String namespace = bean.getURI().getNamespace();
			if (packages.containsKey(namespace)) {
				addBaseClass(bean);
				final String pkg = packages.get(namespace);
				queue.add(new Runnable() {
					public void run() {
						buildClass(bean, pkg, handler);
					}
				});
			}
		}
		for (Resource o : model.filter(null, RDF.TYPE, RDFS.DATATYPE)
				.subjects()) {
			final RDFClass bean = new RDFClass(model, o);
			if (bean.getURI() == null)
				continue;
			String namespace = bean.getURI().getNamespace();
			if (packages.containsKey(namespace)) {
				final String pkg = packages.get(namespace);
				queue.add(new Runnable() {
					public void run() {
						buildDatatype(bean, pkg, handler);
					}
				});
			}
		}
		for (int i = 0, n = threads.size(); i < n; i++) {
			queue.add(helper);
		}
		for (String namespace : packages.keySet()) {
			buildPackage(namespace, handler);
		}
		for (Thread thread : threads) {
			thread.join();
		}
		if (exception != null)
			throw exception;
	}

	private void addBaseClass(RDFClass klass) {
		if (!containKnownNamespace(klass.getRDFClasses(RDFS.SUBCLASSOF))) {
			for (Class b : baseClasses) {
				URI name = new URIImpl(JAVA_NS + b.getName());
				model.add(klass.getURI(), RDFS.SUBCLASSOF, name);
			}
		}
	}

	private boolean containKnownNamespace(Set<? extends RDFEntity> set) {
		boolean contain = false;
		for (RDFEntity e : set) {
			URI name = e.getURI();
			if (name == null)
				continue;
			if (packages.containsKey(name.getNamespace())) {
				contain = true;
			}
		}
		return contain;
	}

	private void buildClass(RDFClass bean, String packageName,
			FileSourceCodeHandler handler) {
		try {
			File dir = handler.getTarget();
			File file = ((RDFClass) bean).generateSourceCode(dir, resolver);
			handler.handleSource(file);
			if (bean.isMessageClass()) {
				RDFClass msg = (RDFClass) bean;
				if (msg.isMessageClass()
						&& resolver.getType(msg.getURI()) != null) {
					file = msg.generateInvokeSourceCode(dir, resolver);
					handler.handleSource(file);
				}
			}
		} catch (Exception exc) {
			logger.error("Error processing {}", bean);
			if (exception == null) {
				exception = exc;
			}
		}
	}

	private void buildDatatype(RDFClass bean, String packageName,
			FileSourceCodeHandler handler) {
		try {
			File dir = handler.getTarget();
			File file = ((RDFClass) bean).generateSourceCode(dir, resolver);
			handler.handleSource(file);
		} catch (Exception exc) {
			logger.error("Error processing {}", bean);
			if (exception == null) {
				exception = exc;
			}
		}
	}

	private void buildPackage(String namespace, FileSourceCodeHandler handler)
			throws Exception {
		RDFOntology ont = findOntology(namespace);
		File dir = handler.getTarget();
		RDFOntology code = (RDFOntology) ont;
		File file = code.generatePackageInfo(dir, namespace, resolver);
		handler.handleSource(file);
	}

	private RDFOntology findOntology(String namespace) {
		if (namespace.endsWith("#"))
			return new RDFOntology(model, new URIImpl(namespace.substring(0,
					namespace.length() - 1)));
		return new RDFOntology(model, new URIImpl(namespace));
	}
}