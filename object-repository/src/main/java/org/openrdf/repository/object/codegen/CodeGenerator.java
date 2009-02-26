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

import info.aduna.io.file.FileUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import org.openrdf.repository.object.codegen.model.RDFProperty;
import org.openrdf.repository.object.codegen.source.JavaCompiler;
import org.openrdf.repository.object.managers.LiteralManager;
import org.openrdf.repository.object.managers.RoleMapper;
import org.openrdf.repository.object.vocabulary.ELMO;
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

	private static final Pattern PACKAGE = Pattern.compile("package ([^;]*);");

	private static final Pattern CLASS_NAME = Pattern
			.compile("public (interface|class|abstract class) (\\S*) ");

	private static final Pattern ANNOTATED = Pattern.compile(".*"
			+ PACKAGE.pattern() + ".*@.*" + CLASS_NAME.pattern() + ".*",
			Pattern.DOTALL);

	private static final Pattern CONCRETE = Pattern.compile(
			".*public class .*", Pattern.DOTALL);

	private static final String JAVA_NS = "java:";

	private static final String META_INF_ELMO_BEHAVIOURS = "META-INF/org.openrdf.behaviours";

	private static final String META_INF_ELMO_CONCEPTS = "META-INF/org.openrdf.concepts";

	private static final String META_INF_ELMO_DATATYPES = "META-INF/org.openrdf.datatypes";

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

	final Logger logger = LoggerFactory.getLogger(CodeGenerator.class);

	BlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>();

	private Set<String> concepts = new TreeSet<String>();

	private String[] baseClasses = new String[0];

	private Set<String> datatypes = new TreeSet<String>();

	private Exception exception;

	private Model model;

	/** namespace -&gt; package */
	private Map<String, String> packages = new HashMap<String, String>();

	private String memberPrefix;

	private JavaNameResolver resolver;

	private List<Thread> threads = new ArrayList<Thread>();

	public CodeGenerator(Model model, ClassLoader cl, RoleMapper mapper, LiteralManager literals) {
		this.model = model;
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
	}

	public void setBaseClasses(String[] baseClasses) {
		this.baseClasses = baseClasses;
	}

	public void setMemberPrefix(String propertyNamesPrefix) {
		this.memberPrefix = propertyNamesPrefix;
	}

	public void bindPackageToNamespace(String pkgName, String namespace) {
		packages.put(namespace, pkgName);
		resolver.bindPackageToNamespace(pkgName, namespace);
	}

	/**
	 * Generate Elmo concept Java classes from the ontology in the local
	 * repository.
	 * 
	 * @param jar
	 * @throws Exception
	 * @see {@link #addOntology(URI, String)}
	 * @see {@link #addImports(URL)}
	 */
	public ClassLoader compileConcepts(File jar, ClassLoader cl)
			throws Exception {
		File target = FileUtil.createTempDir(getClass().getSimpleName());
		List<File> classpath = getClassPath(cl);
		List<String> classes = buildConcepts(target);
		new JavaCompiler().compile(classes, target, classpath);
		packageJar(jar, target, concepts, datatypes);
		FileUtil.deleteDir(target);
		return new URLClassLoader(new URL[] { jar.toURI().toURL() }, cl);
	}

	public ClassLoader compileBehaviours(File jar, ClassLoader cl) throws Exception,
			IOException {
		File target = FileUtil.createTempDir(getClass().getSimpleName());
		List<File> classpath = getClassPath(cl);
		classpath.add(target);
		List<String> classes = compileMethods(target, classpath, resolver);
		if (classes.isEmpty()) {
			FileUtil.deleteDir(target);
			return cl;
		}
		packageJar(jar, target, classes);
		FileUtil.deleteDir(target);
		return new URLClassLoader(new URL[] { jar.toURI().toURL() }, cl);
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
		for (int i = 0; i < 3; i++) {
			threads.add(new Thread(helper));
		}
		for (Thread thread : threads) {
			thread.start();
		}
		final List<String> content = new ArrayList<String>();
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
						buildClass(bean, target, pkg, content);
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
						buildDatatype(bean, target, pkg, content);
					}
				});
			}
		}
		for (int i = 0, n = threads.size(); i < n; i++) {
			queue.add(helper);
		}
		for (String namespace : packages.keySet()) {
			buildPackage(target, namespace, content);
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

	private void buildClass(RDFClass bean, File target, String packageName,
			List<String> content) {
		try {
			File file = bean.generateSourceCode(target, resolver);
			handleSource(file, content);
		} catch (Exception exc) {
			logger.error("Error processing {}", bean);
			if (exception == null) {
				exception = exc;
			}
		}
	}

	private void buildDatatype(RDFClass bean, File target, String packageName,
			List<String> content) {
		try {
			File file = bean.generateSourceCode(target, resolver);
			handleSource(file, content);
		} catch (Exception exc) {
			logger.error("Error processing {}", bean);
			if (exception == null) {
				exception = exc;
			}
		}
	}

	private void buildPackage(File target, String namespace,
			List<String> content) throws Exception {
		RDFOntology ont = findOntology(namespace);
		File file = ont.generatePackageInfo(target, namespace, resolver);
		handleSource(file, content);
	}

	private List<String> compileMethods(File target, List<File> cp,
			JavaNameResolver resolver) throws Exception {
		Set<URI> methods = new LinkedHashSet<URI>();
		List<String> roles = new ArrayList<String>();
		methods.add(ELMO.METHOD);
		while (!methods.isEmpty()) {
			for (URI m : methods) {
				if (packages.containsKey(m.getNamespace())) {
					RDFProperty method = new RDFProperty(model, m);
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

	private void copyInto(URL source, OutputStream out)
			throws FileNotFoundException, IOException {
		logger.debug("Packaging {}", source);
		InputStream in = source.openStream();
		try {
			int read;
			byte[] buf = new byte[512];
			while ((read = in.read(buf)) > 0) {
				out.write(buf, 0, read);
			}
		} finally {
			in.close();
		}
	}

	private JavaNameResolver createJavaNameResolver(ClassLoader cl,
			RoleMapper mapper, LiteralManager literals) {
		JavaNameResolver resolver = new JavaNameResolver(cl);
		resolver.setModel(model);
		for (Map.Entry<String, String> e : model.getNamespaces().entrySet()) {
			resolver.bindPrefixToNamespace(e.getKey(), e.getValue());
		}
		if (memberPrefix != null) {
			for (Map.Entry<String, String> e : packages.entrySet()) {
				resolver.bindPrefixToNamespace(memberPrefix, e.getKey());
			}
		}
		for (Map.Entry<String, String> e : packages.entrySet()) {
			resolver.bindPackageToNamespace(e.getValue(), e.getKey());
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

	private List<File> getClassPath(ClassLoader cl)
			throws UnsupportedEncodingException {
		List<File> classpath = new ArrayList<File>();
		String classPath = System.getProperty("java.class.path");
		for (String path : classPath.split(File.pathSeparator)) {
			classpath.add(new File(path));
		}
		return getClassPath(classpath, cl);
	}

	private List<File> getClassPath(List<File> classpath, ClassLoader cl)
			throws UnsupportedEncodingException {
		if (cl == null) {
			return classpath;
		} else if (cl instanceof URLClassLoader) {
			for (URL jar : ((URLClassLoader) cl).getURLs()) {
				classpath.add(asLocalFile(jar));
			}
			return classpath;
		} else {
			return getClassPath(classpath, cl.getParent());
		}
	}

	private String getPackageName(String code) {
		Matcher m = PACKAGE.matcher(code);
		m.find();
		String pkg = m.group(1);
		return pkg;
	}

	private String getSimpleClassName(String code) {
		Matcher m;
		m = CLASS_NAME.matcher(code);
		if (m.find())
			return m.group(2);
		return null;
	}

	private synchronized void handleSource(File file, List<String> content)
			throws IOException {
		String code = read(file);
		String pkg = getPackageName(code);
		String name = getSimpleClassName(code);
		if (name == null)
			name = "package-info";
		String className = pkg + '.' + name;
		logger.debug("Saving {}", className);
		content.add(className);
		if (CONCRETE.matcher(code).matches()) {
			datatypes.add(className);
		} else if (ANNOTATED.matcher(code).matches()) {
			concepts.add(className);
		}
	}

	private void packaFiles(File base, File dir, JarOutputStream jar)
			throws IOException, FileNotFoundException {
		for (File file : dir.listFiles()) {
			if (file.isDirectory()) {
				packaFiles(base, file, jar);
			} else if (file.exists()) {
				String path = file.getAbsolutePath();
				path = path.substring(base.getAbsolutePath().length() + 1);
				// replace separatorChar by '/' on all platforms
				if (File.separatorChar != '/') {
					path = path.replace(File.separatorChar, '/');
				}
				jar.putNextEntry(new JarEntry(path));
				copyInto(file.toURI().toURL(), jar);
				file.delete();
			}
		}
	}

	private void packageJar(File output, File dir, Collection<String> behaviours)
			throws Exception {
		FileOutputStream stream = new FileOutputStream(output);
		JarOutputStream jar = new JarOutputStream(stream);
		try {
			packaFiles(dir, dir, jar);
			printClasses(behaviours, jar, META_INF_ELMO_BEHAVIOURS);
		} finally {
			jar.close();
			stream.close();
		}
	}

	private void packageJar(File output, File dir, Collection<String> concepts,
			Collection<String> literals) throws Exception {
		FileOutputStream stream = new FileOutputStream(output);
		JarOutputStream jar = new JarOutputStream(stream);
		try {
			packaFiles(dir, dir, jar);
			printClasses(concepts, jar, META_INF_ELMO_CONCEPTS);
			printClasses(literals, jar, META_INF_ELMO_DATATYPES);
		} finally {
			jar.close();
			stream.close();
		}
	}

	private void printClasses(Collection<String> roles, JarOutputStream jar,
			String entry) throws IOException {
		PrintStream out = null;
		for (String name : roles) {
			if (out == null) {
				jar.putNextEntry(new JarEntry(entry));
				out = new PrintStream(jar);
			}
			out.println(name);
		}
		if (out != null) {
			out.flush();
		}
	}

	private String read(File file) throws IOException {
		StringBuilder sb = new StringBuilder();
		Reader reader = new FileReader(file);
		try {
			int size;
			char[] cbuf = new char[512];
			while ((size = reader.read(cbuf)) >= 0) {
				sb.append(cbuf, 0, size);
			}
		} finally {
			reader.close();
		}
		return sb.toString();
	}

}