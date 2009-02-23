/*
 * Copyright (c) 2008, James Leigh All rights reserved.
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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
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
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.openrdf.model.Model;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.repository.object.codegen.model.RDFProperty;
import org.openrdf.repository.object.codegen.source.JavaCompiler;
import org.openrdf.repository.object.managers.LiteralManager;
import org.openrdf.repository.object.managers.RoleMapper;
import org.openrdf.repository.object.vocabulary.ELMO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Facade to CodeGenerator and OwlGenerator classes. This class provides a
 * simpler interface to create concept packages and build ontologies. Unlike the
 * composed classes, this class reads and creates jar packages.
 * 
 * @author James Leigh
 * 
 */
public class OntologyConverter {

	private static final String META_INF_ELMO_CONCEPTS = "META-INF/org.openrdf.concepts";

	private static final String META_INF_ELMO_BEHAVIOURS = "META-INF/org.openrdf.behaviours";

	private static final String META_INF_ELMO_DATATYPES = "META-INF/org.openrdf.datatypes";

	final Logger logger = LoggerFactory.getLogger(OntologyConverter.class);

	private boolean importJarOntologies = true;

	/** namespace -&gt; package */
	private Map<String, String> packages = new HashMap<String, String>();

	private Model model;

	private URLClassLoader cl;

	private String propertyNamesPrefix;

	private String[] baseClasses;

	private RoleMapper mapper;

	private LiteralManager literals;

	public OntologyConverter(Model model, URLClassLoader cl) {
		this.model = model;
		this.cl = cl;
	}

	public void setRoleMapper(RoleMapper mapper) {
		this.mapper = mapper;
	}

	public void setLiteralManager(LiteralManager literals) {
		this.literals = literals;
	}

	/**
	 * If the ontologies bundled with the included jars should be imported.
	 * 
	 * @return <code>true</code> if the ontology will be imported.
	 */
	public boolean isImportJarOntologies() {
		return importJarOntologies;
	}

	/**
	 * If the ontologies bundled with the included jars should be imported.
	 * 
	 * @param importJarOntologies
	 *            <code>true</code> if the ontology will be imported.
	 */
	public void setImportJarOntologies(boolean importJarOntologies) {
		this.importJarOntologies = importJarOntologies;
	}

	/**
	 * The property names prefix or null for default prefix.
	 */
	public String getPropertyNamesPrefix() {
		return propertyNamesPrefix;
	}

	/**
	 * The property names prefix or null for default prefix.
	 */
	public void setPropertyNamesPrefix(String propertyNamesPrefix) {
		this.propertyNamesPrefix = propertyNamesPrefix;
	}

	/**
	 * Array of Java Class names that all concepts will extend.
	 * 
	 * @return Array of Java Class names that all concepts will extend.
	 */
	public String[] getBaseClasses() {
		return baseClasses;
	}

	/**
	 * Array of Java Class names that all concepts will extend.
	 * 
	 * @param strings
	 */
	public void setBaseClasses(String[] strings) {
		this.baseClasses = strings;
	}

	/**
	 * Binds this namespace with the package name.
	 * @param pkgName
	 * @param namespace
	 */
	public void bindPackageToNamespace(String pkgName, String namespace) {
		packages.put(namespace, pkgName);
	}

	/**
	 * Generate Elmo concept Java classes from the ontology in the local
	 * repository.
	 * 
	 * @param jarOutputFile
	 * @throws Exception
	 * @see {@link #addOntology(URI, String)}
	 * @see {@link #addRdfSource(URL)}
	 */
	public void createClasses(File output) throws Exception {
		JavaNameResolver resolver = createJavaNameResolver(cl);
		resolver.setRoleMapper(mapper);
		resolver.setLiteralManager(literals);
		FileSourceCodeHandler handler = new FileSourceCodeHandler();
		generateSourceCode(cl, handler, resolver);
		if (handler.getClasses().isEmpty())
			throw new IllegalArgumentException("No classes found - Try a different namespace.");
		JavaCompiler javac = new JavaCompiler();
		List<File> classpath = getClassPath(cl);
		File dir = handler.getTarget();
		javac.compile(handler.getClasses(), dir, classpath);
		List<File> cp = new ArrayList<File>(classpath.size() + 1);
		cp.add(dir);
		cp.addAll(classpath);
		Set<String> concepts = new TreeSet<String>();
		Set<String> behaviours = new TreeSet<String>();
		behaviours.addAll(compileMethods(dir, cp, resolver));
		concepts.addAll(handler.getAnnotatedClasses());
		behaviours.addAll(handler.getAbstractClasses());
		List<String> literals = handler.getConcreteClasses();
		concepts.removeAll(literals);
		packageJar(output, dir, concepts, behaviours, literals);
	}

	private List<File> getClassPath(URLClassLoader cl)
			throws UnsupportedEncodingException {
		List<File> classpath = new ArrayList<File>();
		for (URL jar : cl.getURLs()) {
			classpath.add(asLocalFile(jar));
		}
		String classPath = System.getProperty("java.class.path");
		for (String path : classPath.split(File.pathSeparator)) {
			classpath.add(new File(path));
		}
		return classpath;
	}

	private void generateSourceCode(ClassLoader cl,
			FileSourceCodeHandler handler, JavaNameResolver resolver) throws Exception {
		CodeGenerator gen = new CodeGenerator(model);
		gen.setPropertyNamesPrefix(propertyNamesPrefix);
		if (baseClasses != null) {
			List<Class<?>> base = new ArrayList<Class<?>>();
			for (String bc : baseClasses) {
				base.add(Class.forName(bc, true, cl));
			}
			gen.setBaseClasses(base.toArray(new Class<?>[base.size()]));
		}
		for (Map.Entry<String, String> e : packages.entrySet()) {
			gen.bindPackageToNamespace(e.getValue(), e.getKey());
		}
		gen.setJavaNameResolver(resolver);
		gen.init();
		gen.exportSourceCode(handler);
	}

	private List<String> compileMethods(File target,
			List<File> cp, JavaNameResolver resolver) throws Exception {
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
					for (Resource subj : model.filter(null, RDFS.SUBPROPERTYOF, m).subjects()) {
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

	private JavaNameResolver createJavaNameResolver(ClassLoader cl) {
		JavaNameResolver resolver = new JavaNameResolver(cl);
		resolver.setModel(model);
		for (Map.Entry<String, String> e : model.getNamespaces().entrySet()) {
			resolver.bindPrefixToNamespace(e.getKey(), e.getValue());
		}
		if (propertyNamesPrefix != null) {
			for (Map.Entry<String, String> e : packages.entrySet()) {
				resolver.bindPrefixToNamespace(propertyNamesPrefix, e.getKey());
			}
		}
		for (Map.Entry<String, String> e : packages.entrySet()) {
			resolver.bindPackageToNamespace(e.getValue(), e.getKey());
		}
		return resolver;
	}

	private void packageJar(File output, File dir, Collection<String> concepts, Collection<String> behaviours, List<String> literals)
			throws Exception {
		FileOutputStream stream = new FileOutputStream(output);
		JarOutputStream jar = new JarOutputStream(stream);
		try {
			packaFiles(dir, dir, jar);
			printClasses(concepts, jar, META_INF_ELMO_CONCEPTS);
			printClasses(behaviours, jar, META_INF_ELMO_BEHAVIOURS);
			printClasses(literals, jar, META_INF_ELMO_DATATYPES);
		} finally {
			jar.close();
			stream.close();
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

	private void printClasses(Collection<String> roles, JarOutputStream jar, String entry)
			throws IOException {
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

	private File asLocalFile(URL rdf) throws UnsupportedEncodingException {
		return new File(URLDecoder.decode(rdf.getFile(), "UTF-8"));
	}

}
