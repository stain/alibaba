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
package org.openrdf.repository.object.compiler;

import info.aduna.io.file.FileUtil;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.openrdf.model.Model;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.repository.object.compiler.model.RDFClass;
import org.openrdf.repository.object.compiler.model.RDFEntity;
import org.openrdf.repository.object.compiler.model.RDFOntology;
import org.openrdf.repository.object.compiler.model.RDFProperty;
import org.openrdf.repository.object.compiler.source.JavaCompiler;
import org.openrdf.repository.object.managers.LiteralManager;
import org.openrdf.repository.object.managers.RoleMapper;
import org.openrdf.repository.object.managers.helpers.RoleClassLoader;
import org.openrdf.repository.object.vocabulary.OBJ;
import org.openrdf.store.StoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converts OWL ontologies into Java source code.
 * 
 * @author James Leigh
 * 
 */
public class OWLCompiler {
	private static final Options options = new Options();
	static {
		Option jar = new Option("j", "jar", true,
				"filename where the jar will be saved");
		jar.setArgName("jar file");
		Option jarOntologies = new Option("o", null, true,
				"import jar ontologies");
		jarOntologies.setArgName("jar ontologies");
		Option imports = new Option("i", "import", true,
				"jar file that should be imported before compiling");
		imports.setArgName("included jar file");
		Option prefix = new Option("p", "prefix", true,
				"prefix the property names with namespace prefix");
		prefix.setArgName("prefix");
		prefix.setOptionalArg(true);
		Option follow = new Option("f", "follow", true, "follow imports");
		Option baseClass = new Option("e", "extends", true,
				"super class that all concepts should extend");
		baseClass.setArgName("full class name");
		options.addOption("h", "help", false, "print this message");
		options.addOption(baseClass);
		options.addOption(prefix);
		options.addOption(jar);
		options.addOption(jarOntologies);
		options.addOption(imports);
		options.addOption(follow);
	}

	public static void main(String[] args) throws Exception {
		try {
			CommandLine line = new GnuParser().parse(options, args);
			if (line.hasOption('h')) {
				HelpFormatter formatter = new HelpFormatter();
				String cmdLineSyntax = OWLCompiler.class.getSimpleName()
						+ " [options] ontology...";
				String header = "ontology... a list of RDF files that should be compiled together.";
				formatter.printHelp(cmdLineSyntax, header, options, "");
				return;
			}
			File jar;
			if (line.hasOption('j')) {
				jar = new File(line.getOptionValue('j'));
			} else if (line.getArgs().length > 0) {
				String filename = line.getArgs()[line.getArgs().length - 1];
				if (!new File(filename).exists()) {
					filename = new File(new URL(filename).getPath()).getName();
				}
				if (filename.contains(".")) {
					filename = filename.substring(0, filename.lastIndexOf('.'));
				}
				jar = new File(filename + ".jar");
			} else {
				throw new ParseException("Missig -j option");
			}
			List<URL> list = getURLs(line.getOptionValues('i'));
			ClassLoader cl = Thread.currentThread().getContextClassLoader();
			cl = new URLClassLoader(list.toArray(new URL[list.size()]), cl);
			RoleMapper mapper = new RoleMapper();
			new RoleClassLoader(mapper).loadRoles(cl);
			LiteralManager literals = new LiteralManager();
			literals.setClassLoader(cl);
			OWLCompiler converter = new OWLCompiler(mapper, literals);
			if (line.hasOption('p')) {
				String prefix = line.getOptionValue('p');
				if (prefix == null) {
					prefix = "";
				}
				converter.setMemberPrefix(prefix);
			}
			if (line.hasOption('e')) {
				converter.setBaseClasses(line.getOptionValues('e'));
			}
			boolean follow = true;
			if (line.hasOption('f')) {
				follow = Boolean.parseBoolean(line.getOptionValue('f'));
			}
			boolean jaro = true;
			if (line.hasOption('o')) {
				jaro = Boolean.parseBoolean(line.getOptionValue('o'));
			}
			List<URL> urls = getURLs(line.getArgs());
			OntologyLoader loader = new OntologyLoader();
			if (jaro) {
				loader.loadOntologies(cl);
			}
			loader.loadOntologies(urls);
			if (follow) {
				loader.followImports();
			}
			Model model = loader.getModel();
			urls.addAll(loader.getImported());
			converter.setOntologies(urls);
			converter.setConceptJar(jar);
			converter.compile(model, cl);
			return;
		} catch (ParseException exp) {
			System.err.println(exp.getMessage());
			System.exit(1);
		}
	}

	private static List<URL> getURLs(String[] args)
			throws MalformedURLException {
		List<URL> list = new ArrayList<URL>();
		if (args == null)
			return list;
		for (String arg : args) {
			File file = new File(arg);
			if (file.exists()) {
				list.add(file.toURI().toURL());
			} else {
				list.add(new URL(arg));
			}
		}
		return list;
	}

	private class ConceptBuilder implements Runnable {
		private final RDFClass bean;
		private final List<String> content;
		private final File target;

		private ConceptBuilder(File target, List<String> content, RDFClass bean) {
			this.target = target;
			this.content = content;
			this.bean = bean;
		}

		public void run() {
			try {
				bean.generateSourceCode(target, resolver);
				URI uri = bean.getURI();
				String pkg = resolver.getPackageName(uri);
				String simple = resolver.getSimpleName(uri);
				String className = pkg + '.' + simple;
				boolean anon = resolver.isAnonymous(uri) && bean.isEmpty();
				synchronized (content) {
					logger.debug("Saving {}", className);
					content.add(className);
					if (!anon) {
						concepts.add(className);
					}
				}
			} catch (Exception exc) {
				logger.error("Error processing {}", bean);
				if (exception == null) {
					exception = exc;
				}
			}
		}
	}

	private final class DatatypeBuilder implements Runnable {
		private final RDFClass bean;
		private final List<String> content;
		private final File target;

		private DatatypeBuilder(List<String> content, RDFClass bean, File target) {
			this.content = content;
			this.bean = bean;
			this.target = target;
		}

		public void run() {
			try {
				bean.generateSourceCode(target, resolver);
				String pkg = resolver.getPackageName(bean.getURI());
				String simple = resolver.getSimpleName(bean.getURI());
				String className = pkg + '.' + simple;
				synchronized (content) {
					logger.debug("Saving {}", className);
					content.add(className);
					datatypes.add(className);
				}
			} catch (Exception exc) {
				logger.error("Error processing {}", bean);
				if (exception == null) {
					exception = exc;
				}
			}
		}
	}

	private static final Set<URI> BUILD_IN = new HashSet(Arrays.asList(
			RDFS.RESOURCE, RDFS.CONTAINER, RDF.ALT, RDF.BAG, RDF.SEQ, RDF.LIST));

	private static final String JAVA_NS = "java:";

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
	final Logger logger = LoggerFactory.getLogger(OWLCompiler.class);
	BlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>();
	private String[] baseClasses = new String[0];
	private File behaviours;
	private Set<String> concepts = new TreeSet<String>();
	private File conceptsJar;
	private Set<String> datatypes = new TreeSet<String>();
	private Exception exception;
	private LiteralManager literals;
	private RoleMapper mapper;
	private String memberPrefix;
	private Model model;
	/** namespace -&gt; package */
	private Map<String, String> packages = new HashMap<String, String>();
	private String pkgPrefix = "";
	private JavaNameResolver resolver;
	private Collection<URL> ontologies;

	public OWLCompiler(RoleMapper mapper, LiteralManager literals) {
		this.mapper = mapper;
		this.literals = literals;
	}

	public void setBaseClasses(String[] baseClasses) {
		this.baseClasses = baseClasses;
	}

	public void setBehaviourJar(File jar) {
		this.behaviours = jar;
	}

	public void setConceptJar(File jar) {
		this.conceptsJar = jar;
	}

	public void setPackagePrefix(String prefix) {
		if (prefix == null) {
			this.pkgPrefix = "";
		} else {
			this.pkgPrefix = prefix;
		}
	}

	public void setMemberPrefix(String prefix) {
		this.memberPrefix = prefix;
	}

	public void setOntologies(Collection<URL> ontologies) {
		this.ontologies = ontologies;
	}

	public synchronized ClassLoader compile(Model model, ClassLoader cl)
			throws StoreException {
		this.concepts.clear();
		this.datatypes.clear();
		this.exception = null;
		this.packages.clear();
		this.model = model;
		Set<String> unknown = findUndefinedNamespaces(model);
		if (unknown.isEmpty())
			return cl;
		OwlNormalizer normalizer = new OwlNormalizer(new RDFDataSource(model));
		normalizer.normalize();
		resolver = createJavaNameResolver(cl, mapper, literals);
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
		for (String ns : unknown) {
			String prefix = findPrefix(ns, model);
			String pkgName = pkgPrefix + prefix;
			if (!Character.isLetter(pkgName.charAt(0))) {
				pkgName = "_" + pkgName;
			}
			packages.put(ns, pkgName);
			resolver.bindPackageToNamespace(pkgName, ns);
		}
		try {
			cl = compileConcepts(conceptsJar, cl);
			return compileBehaviours(behaviours, cl);
		} catch (Exception e) {
			throw new StoreException(e);
		}
	}

	private void addBaseClass(RDFClass klass) {
		if (!containKnownNamespace(klass.getRDFClasses(RDFS.SUBCLASSOF))) {
			for (String b : baseClasses) {
				URI name = new URIImpl(JAVA_NS + b);
				model.add(klass.getURI(), RDFS.SUBCLASSOF, name);
			}
		}
	}

	private File asLocalFile(URL rdf) throws UnsupportedEncodingException {
		return new File(URLDecoder.decode(rdf.getFile(), "UTF-8"));
	}

	private List<String> buildConcepts(final File target) throws Exception {
		List<Thread> threads = new ArrayList<Thread>();
		for (int i = 0; i < 3; i++) {
			threads.add(new Thread(helper));
		}
		for (Thread thread : threads) {
			thread.start();
		}
		Set<String> usedNamespaces = new HashSet<String>(packages.size());
		List<String> content = new ArrayList<String>();
		Set<Resource> classes = model.filter(null, RDF.TYPE, OWL.CLASS)
				.subjects();
		for (Resource o : new ArrayList<Resource>(classes)) {
			final RDFClass bean = new RDFClass(model, o);
			if (bean.getURI() == null)
				continue;
			String namespace = bean.getURI().getNamespace();
			if (packages.containsKey(namespace)) {
				usedNamespaces.add(namespace);
				addBaseClass(bean);
				queue.add(new ConceptBuilder(target, content, bean));
			}
		}
		for (Resource o : model.filter(null, RDF.TYPE, RDFS.DATATYPE)
				.subjects()) {
			final RDFClass bean = new RDFClass(model, o);
			if (bean.getURI() == null)
				continue;
			String namespace = bean.getURI().getNamespace();
			if (packages.containsKey(namespace)) {
				usedNamespaces.add(namespace);
				queue.add(new DatatypeBuilder(content, bean, target));
			}
		}
		for (int i = 0, n = threads.size(); i < n; i++) {
			queue.add(helper);
		}
		for (String namespace : usedNamespaces) {
			RDFOntology ont = findOntology(namespace);
			ont.generatePackageInfo(target, namespace, resolver);
			String pkg = packages.get(namespace);
			String name = "package-info";
			String className = pkg + '.' + name;
			synchronized (content) {
				logger.debug("Saving {}", className);
				content.add(className);
			}
		}
		for (Thread thread1 : threads) {
			thread1.join();
		}
		if (exception != null)
			throw exception;
		if (content.isEmpty())
			throw new IllegalArgumentException(
					"No classes found - Try a different namespace.");
		return content;
	}

	private ClassLoader compileBehaviours(File jar, ClassLoader cl)
			throws Exception, IOException {
		File target = FileUtil.createTempDir(getClass().getSimpleName());
		List<File> classpath = getClassPath(cl);
		classpath.add(target);
		List<String> classes = compileMethods(target, classpath, resolver);
		if (classes.isEmpty()) {
			FileUtil.deleteDir(target);
			return cl;
		}
		JarPacker packer = new JarPacker(target);
		packer.setBehaviours(classes);
		packer.packageJar(jar);
		FileUtil.deleteDir(target);
		return new URLClassLoader(new URL[] { jar.toURI().toURL() }, cl);
	}

	/**
	 * Generate concept Java classes from the ontology in the local repository.
	 * 
	 * @param jar
	 * @throws Exception
	 * @see {@link #addOntology(URI, String)}
	 * @see {@link #addImports(URL)}
	 */
	private ClassLoader compileConcepts(File jar, ClassLoader cl)
			throws Exception {
		File target = FileUtil.createTempDir(getClass().getSimpleName());
		List<File> classpath = getClassPath(cl);
		List<String> classes = buildConcepts(target);
		new JavaCompiler().compile(classes, target, classpath);
		JarPacker packer = new JarPacker(target);
		packer.setConcepts(concepts);
		packer.setDatatypes(datatypes);
		packer.setOntologies(ontologies);
		packer.packageJar(jar);
		FileUtil.deleteDir(target);
		return new URLClassLoader(new URL[] { jar.toURI().toURL() }, cl);
	}

	private List<String> compileMethods(File target, List<File> cp,
			JavaNameResolver resolver) throws Exception {
		Set<URI> methods = new LinkedHashSet<URI>();
		List<String> roles = new ArrayList<String>();
		methods.add(OBJ.METHOD);
		methods.add(OBJ.LITERAL_TRIGGER);
		methods.add(OBJ.OBJECT_TRIGGER);
		while (!methods.isEmpty()) {
			for (URI m : methods) {
				RDFProperty method = new RDFProperty(model, m);
				if (method.isMethodOrTrigger()
						&& packages.containsKey(m.getNamespace())) {
					String concept = method.msgCompile(resolver, target, cp);
					if (concept != null) {
						roles.add(concept);
					}
				}
				// TODO compile entire set at once
			}
			ArrayList<URI> copy = new ArrayList<URI>(methods);
			methods.clear();
			for (URI m : copy) {
				for (Resource subj : model.filter(null, RDFS.SUBPROPERTYOF, m)
						.subjects()) {
					if (subj instanceof URI) {
						methods.add((URI) subj);
					} else {
						logger.warn("BNode Methods not supported");
					}
				}
			}
		}
		return roles;
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

	private JavaNameResolver createJavaNameResolver(ClassLoader cl,
			RoleMapper mapper, LiteralManager literals) {
		JavaNameResolver resolver = new JavaNameResolver(cl);
		resolver.setModel(model);
		for (Map.Entry<String, String> e : packages.entrySet()) {
			resolver.bindPackageToNamespace(e.getValue(), e.getKey());
		}
		for (Map.Entry<String, String> e : model.getNamespaces().entrySet()) {
			resolver.bindPrefixToNamespace(e.getKey(), e.getValue());
		}
		if (memberPrefix != null) {
			for (Map.Entry<String, String> e : packages.entrySet()) {
				resolver.bindPrefixToNamespace(memberPrefix, e.getKey());
			}
		}
		resolver.setRoleMapper(mapper);
		resolver.setLiteralManager(literals);
		return resolver;
	}

	private RDFOntology findOntology(String namespace) {
		if (namespace.endsWith("#"))
			return new RDFOntology(model, new URIImpl(namespace.substring(0,
					namespace.length() - 1)));
		return new RDFOntology(model, new URIImpl(namespace));
	}

	private String findPrefix(String ns, Model model) {
		for (Map.Entry<String, String> e : model.getNamespaces().entrySet()) {
			if (ns.equals(e.getValue()) && e.getKey().length() > 0) {
				return e.getKey();
			}
		}
		return "ns" + Integer.toHexString(ns.hashCode());
	}

	private Set<String> findUndefinedNamespaces(Model model) {
		Set<String> existing = new HashSet<String>();
		Set<String> unknown = new HashSet<String>();
		for (Resource subj : model.filter(null, RDF.TYPE, null).subjects()) {
			if (subj instanceof URI) {
				URI uri = (URI) subj;
				String ns = uri.getNamespace();
				if (BUILD_IN.contains(uri))
					continue;
				if (mapper.isTypeRecorded(uri)
						|| literals.findClass(uri) != null) {
					existing.add(ns);
				} else {
					unknown.add(ns);
				}
			}
		}
		unknown.removeAll(existing);
		return unknown;
	}

	private List<File> getClassPath(ClassLoader cl)
			throws UnsupportedEncodingException {
		List<File> classpath = new ArrayList<File>();
		String classPath = System.getProperty("java.class.path");
		for (String path : classPath.split(File.pathSeparator)) {
			classpath.add(new File(path));
		}
		getClassPath(classpath, cl);
		return getClassPath(classpath, getClass().getClassLoader());
	}

	private List<File> getClassPath(List<File> classpath, ClassLoader cl)
			throws UnsupportedEncodingException {
		if (cl == null) {
			return classpath;
		} else if (cl instanceof URLClassLoader) {
			for (URL jar : ((URLClassLoader) cl).getURLs()) {
				File file = asLocalFile(jar);
				if (!classpath.contains(file)) {
					classpath.add(file);
				}
			}
			return classpath;
		} else {
			return getClassPath(classpath, cl.getParent());
		}
	}

}