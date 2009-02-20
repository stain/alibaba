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
package org.openrdf.elmo.codegen;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.openrdf.elmo.ElmoModule;
import org.openrdf.elmo.LiteralManager;
import org.openrdf.elmo.codegen.concepts.CodeMethod;
import org.openrdf.elmo.codegen.vocabulary.ELMO;
import org.openrdf.elmo.sesame.SesameLiteralManager;
import org.openrdf.elmo.sesame.SesameManager;
import org.openrdf.elmo.sesame.SesameManagerFactory;
import org.openrdf.model.Literal;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.contextaware.ContextAwareConnection;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.repository.util.RDFInserter;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.owl.OntologyWriter;
import org.openrdf.sail.memory.MemoryStore;
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

	private static final String META_INF_ELMO_CONCEPTS = "META-INF/org.openrdf.elmo.concepts";

	private static final String META_INF_ELMO_BEHAVIOURS = "META-INF/org.openrdf.elmo.behaviours";

	private static final String META_INF_ELMO_DATATYPES = "META-INF/org.openrdf.elmo.datatypes";

	private static final String META_INF_ONTOLOGIES = "META-INF/org.openrdf.elmo.ontologies";

	private static final Options options = new Options();
	static {
		Option pkg = new Option("b", "bind", true,
				"Binds the package name and namespace together");
		pkg.setArgName("package=uri");
		Option jar = new Option("j", "jar", true,
				"filename where the jar will be saved");
		jar.setArgName("jar file");
		Option file = new Option("r", "rdf", true,
				"filename where the rdf ontology will be saved");
		file.setArgName("RDF file");
		Option prefix = new Option("p", "prefix", true,
				"prefix the property names with namespace prefix");
		prefix.setArgName("prefix");
		prefix.setOptionalArg(true);
		Option baseClass = new Option("e", "extends", true,
				"super class that all concepts should extend");
		baseClass.setArgName("full class name");
		options.addOption(baseClass);
		options.addOption(prefix);
		options.addOption("h", "help", false, "print this message");
		options.addOption(pkg);
		options.addOption(jar);
		options.addOption(file);
	}

	public static void main(String[] args) throws Exception {
		try {
			CommandLine line = new GnuParser().parse(options, args);
			if (line.hasOption('h')) {
				HelpFormatter formatter = new HelpFormatter();
				String cmdLineSyntax = "codegen [options] [ontology | jar]...";
				String header = "[ontology | jar]... are a list of RDF and jar files that should be imported before converting.";
				formatter.printHelp(cmdLineSyntax, header, options, "");
				return;
			}
			if (!line.hasOption('b'))
				throw new ParseException("Required bind option missing");
			if (!line.hasOption('j') && !line.hasOption('r'))
				throw new ParseException("Required jar or rdf option missing");
			if (line.hasOption('j') && line.hasOption('r'))
				throw new ParseException(
						"Only one jar or rdf option can be present");
			OntologyConverter converter = new OntologyConverter();
			if (line.hasOption('p')) {
				String prefix = line.getOptionValue('p');
				if (prefix == null) {
					prefix = "";
				}
				converter.setPropertyNamesPrefix(prefix);
			}
			if (line.hasOption('e')) {
				converter.setBaseClasses(line.getOptionValues('e'));
			}
			findJars(line.getArgs(), 0, converter);
			findRdfSources(line.getArgs(), 0, converter);
			for (String value : line.getOptionValues('b')) {
				String[] split = value.split("=", 2);
				if (split.length != 2) {
					throw new ParseException("Invalid bind option: " + value);
				}
				converter.bindPackageToNamespace(split[0], split[1]);
			}
			converter.init();
			if (line.hasOption('j')) {
				converter.createClasses(new File(line.getOptionValue('j')));
			} else {
				converter.createOntology(new File(line.getOptionValue('r')));
			}
			return;
		} catch (ParseException exp) {
			System.err.println(exp.getMessage());
			System.exit(1);
		}
	}

	private static void findRdfSources(String[] args, int offset,
			OntologyConverter converter) throws MalformedURLException {
		for (int i = offset; i < args.length; i++) {
			URL url;
			File file = new File(args[i]);
			if (file.isDirectory() || args[i].endsWith(".jar"))
				continue;
			if (file.exists()) {
				url = file.toURI().toURL();
			} else {
				url = new URL(args[i]);
			}
			converter.addRdfSource(url);
		}
	}

	private static void findJars(String[] args, int offset,
			OntologyConverter converter) throws MalformedURLException {
		for (int i = offset; i < args.length; i++) {
			URL url;
			File file = new File(args[i]);
			if (file.exists()) {
				url = file.toURI().toURL();
			} else {
				url = new URL(args[i]);
			}
			if (file.isDirectory() || args[i].endsWith(".jar")) {
				converter.addJar(url);
			}
		}
	}

	final Logger logger = LoggerFactory.getLogger(OntologyConverter.class);

	private boolean importJarOntologies = true;

	private List<URL> jars = new ArrayList<URL>();

	private List<URL> rdfSources = new ArrayList<URL>();

	private Map<String, String> namespaces = new HashMap<String, String>();

	/** namespace -&gt; package */
	private Map<String, String> packages = new HashMap<String, String>();

	private Repository repository;

	private URLClassLoader cl;

	private String propertyNamesPrefix;

	private String[] baseClasses;

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
	 * Add a jar of classes to include in the class-path.
	 * 
	 * @param url
	 */
	public void addJar(URL url) {
		jars.add(url);
	}

	/**
	 * Adds an RDF file to the local repository.
	 * 
	 * @param url
	 */
	public void addRdfSource(URL url) {
		rdfSources.add(url);
	}

	/**
	 * Set the prefix that should be used for this ontology namespace.
	 * 
	 * @param prefix
	 * @param namespace
	 */
	public void setNamespace(String prefix, String namespace) {
		namespaces.put(prefix, namespace);
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
	 * Create the local repository and load the RDF files.
	 * 
	 * @throws Exception
	 */
	public void init() throws Exception {
		cl = createClassLoader(jars);
		Thread.currentThread().setContextClassLoader(cl);
		repository = createRepository(cl);
		for (URL url : rdfSources) {
			loadOntology(repository, url);
		}
	}

	/**
	 * Generate an OWL ontology from the JavaBeans in the included jars.
	 * 
	 * @param rdfOutputFile
	 * @throws Exception
	 * @see {@link #addOntology(URI, String)}
	 * @see {@link #addJar(URL)}
	 */
	public void createOntology(File rdfOutputFile) throws Exception {
		List<Class<?>> beans = new ArrayList<Class<?>>();
		if (packages.isEmpty()) {
			beans.addAll(findBeans(null, jars, cl));
		} else {
			for (String packageName : packages.values()) {
				beans.addAll(findBeans(packageName, jars, cl));
			}
		}
		ValueFactory vf = repository.getValueFactory();
		SesameLiteralManager manager = new SesameLiteralManager(vf);
		manager.setClassLoader(cl);
		createOntology(beans, manager, rdfOutputFile);
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
	public void createClasses(File jarOutputFile) throws Exception {
		createJar(repository, cl, jarOutputFile);
	}

	protected Repository createRepository() throws RepositoryException {
		Repository repository = new SailRepository(new MemoryStore());
		repository.initialize();
		return repository;
	}

	protected SesameManagerFactory createSesameManager(Repository repository,
			URLClassLoader cl) {
		return new SesameManagerFactory(new ElmoModule(cl), repository);
	}

	private URLClassLoader createClassLoader(List<URL> importJars)
			throws MalformedURLException {
		Thread thread = Thread.currentThread();
		ClassLoader cl = thread.getContextClassLoader();
		String name = OntologyConverter.class.getName().replace('.', '/');
		if (cl == null || cl.getResource(name + ".class") == null) {
			cl = OntologyConverter.class.getClassLoader();
		}
		URL[] classpath = importJars.toArray(new URL[0]);
		if (cl instanceof URLClassLoader) {
			URL[] urls = ((URLClassLoader) cl).getURLs();
			URL[] jars = classpath;
			classpath = new URL[jars.length + urls.length];
			System.arraycopy(jars, 0, classpath, 0, jars.length);
			System.arraycopy(urls, 0, classpath, jars.length, urls.length);
		}
		return URLClassLoader.newInstance(classpath, cl);
	}

	private Repository createRepository(ClassLoader cl)
			throws RepositoryException, IOException, RDFParseException {
		Repository repository = createRepository();
		RepositoryConnection conn = repository.getConnection();
		try {
			for (Map.Entry<String, String> e : namespaces.entrySet()) {
				conn.setNamespace(e.getKey(), e.getValue());
			}
		} finally {
			conn.close();
		}
		if (importJarOntologies) {
			for (String owl : loadOntologyList(cl)) {
				URL url = cl.getResource(owl);
				loadOntology(repository, url);
			}
		}
		return repository;
	}

	@SuppressWarnings("unchecked")
	private Collection<String> loadOntologyList(ClassLoader cl)
			throws IOException {
		Properties ontologies = new Properties();
		String name = "META-INF/org.openrdf.elmo.ontologies";
		Enumeration<URL> resources = cl.getResources(name);
		while (resources.hasMoreElements()) {
			URL url = resources.nextElement();
			ontologies.load(url.openStream());
		}
		Collection<?> list = ontologies.keySet();
		return (Collection<String>) list;
	}

	private void loadOntology(Repository repository, URL url)
			throws RepositoryException, IOException, RDFParseException {
		String filename = url.toString();
		RDFFormat format = formatForFileName(filename);
		RepositoryConnection conn = repository.getConnection();
		ValueFactory vf = repository.getValueFactory();
		try {
			String uri = url.toExternalForm();
			conn.add(url, uri, format, vf.createURI(uri));
		} finally {
			conn.close();
		}
	}

	private RDFFormat formatForFileName(String filename) {
		RDFFormat format = RDFFormat.forFileName(filename);
		if (format != null)
			return format;
		if (filename.endsWith(".owl"))
			return RDFFormat.RDFXML;
		throw new IllegalArgumentException("Unknow RDF format for " + filename);
	}

	private List<Class<?>> findBeans(String pkgName, List<URL> urls,
			URLClassLoader cl) throws Exception {
		List<Class<?>> beans = new ArrayList<Class<?>>();
		for (URL jar : urls) {
			JarFile file = new JarFile(asLocalFile(jar));
			Enumeration<JarEntry> entries = file.entries();
			while (entries.hasMoreElements()) {
				JarEntry entry = entries.nextElement();
				String name = entry.getName();
				if (name.contains("-") || !name.endsWith(".class"))
					continue;
				name = name.replace('/', '.').replace('\\', '.');
				if (pkgName == null || name.startsWith(pkgName)
						&& name.substring(pkgName.length() + 1).contains(".")) {
					name = name.replaceAll(".class$", "");
					beans.add(Class.forName(name, true, cl));
				}
			}
		}
		return beans;
	}

	private void createOntology(List<Class<?>> beans,
			LiteralManager<URI, Literal> manager, File output) throws Exception {
		RDFFormat format = formatForFileName(output.getName());
		Writer out = new FileWriter(output);
		try {
			RepositoryConnection conn = repository.getConnection();
			OwlGenerator gen = new OwlGenerator();
			gen.setLiteralManager(manager);
			for (Map.Entry<String, String> e : packages.entrySet()) {
				String namespace = e.getKey();
				String pkgName = e.getValue();
				String prefix = pkgName.substring(pkgName.lastIndexOf('.') + 1);
				conn.setNamespace(prefix, namespace);
				gen.setNamespace(pkgName, prefix, namespace);
			}
			RDFHandler inserter = new RDFInserter(conn);
			Set<URI> ontologies = gen.exportOntology(beans, inserter);
			OntologyWriter writer = new OntologyWriter(format, out);
			writer.setConnection(conn);
			writer.startRDF();
			for (URI ontology : ontologies) {
				writer.printOntology(ontology);
			}
			writer.endRDF();
			conn.close();
		} finally {
			out.close();
		}
	}

	private void createJar(Repository repository, URLClassLoader cl, File output)
			throws Exception {
		JavaNameResolverImpl resolver = createJavaNameResolver(repository, cl);
		SesameManagerFactory factory = createSesameManager(repository, cl);
		resolver.setRoleMapper(factory.getRoleMapper());
		resolver.setLiteralManager(factory.getLiteralManager());
		FileSourceCodeHandler handler = new FileSourceCodeHandler();
		generateSourceCode(factory, cl, handler, resolver);
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
		behaviours.addAll(compileMethods(factory, dir, cp, resolver));
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

	private void generateSourceCode(SesameManagerFactory factory, ClassLoader cl,
			FileSourceCodeHandler handler, JavaNameResolverImpl resolver) throws Exception {
		CodeGenerator gen = new CodeGenerator();
		gen.setPropertyNamesPrefix(propertyNamesPrefix);
		if (baseClasses != null) {
			List<Class<?>> base = new ArrayList<Class<?>>();
			for (String bc : baseClasses) {
				base.add(Class.forName(bc, true, cl));
			}
			gen.setBaseClasses(base.toArray(new Class<?>[base.size()]));
		}
		gen.setSesameManagerFactory(factory);
		for (Map.Entry<String, String> e : packages.entrySet()) {
			gen.bindPackageToNamespace(e.getValue(), e.getKey());
		}
		gen.setJavaNameResolver(resolver);
		gen.init();
		gen.exportSourceCode(handler);
	}

	private List<String> compileMethods(SesameManagerFactory factory, File target,
			List<File> cp, JavaNameResolverImpl resolver) throws Exception {
		SesameManager manager = factory.createElmoManager();
		Set<URI> methods = new LinkedHashSet<URI>();
		List<String> roles = new ArrayList<String>();
		ContextAwareConnection con = manager.getConnection();
		ValueFactory vf = con.getRepository().getValueFactory();
		methods.add(vf.createURI(ELMO.METHOD_URI));
		while (!methods.isEmpty()) {
			for (URI m : methods) {
				if (packages.containsKey(m.getNamespace())) {
					CodeMethod method = (CodeMethod) manager.find(m);
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
				RepositoryResult<Statement> stmts;
				stmts = con.getStatements(null, RDFS.SUBPROPERTYOF, m);
				try {
					while (stmts.hasNext()) {
						Statement st = stmts.next();
						if (st.getSubject() instanceof URI) {
							methods.add((URI) st.getSubject());
						} else {
							logger.warn("BNode Methods not supported");
						}
					}
				} finally {
					stmts.close();
				}
			}
		}
		return roles;
	}

	private JavaNameResolverImpl createJavaNameResolver(Repository repository, ClassLoader cl)
			throws RepositoryException {
		JavaNameResolverImpl resolver = new JavaNameResolverImpl(cl);
		resolver.setRepository(repository);
		for (Map.Entry<String, String> e : namespaces.entrySet()) {
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
			packOntologies(rdfSources, jar);
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

	private void packOntologies(List<URL> rdfSources, JarOutputStream jar)
			throws RepositoryException, RDFParseException, IOException {
		Map<String, URI> ontologies = new HashMap<String, URI>();
		for (URL rdf : rdfSources) {
			String path = "META-INF/ontologies/";
			path += asLocalFile(rdf).getName();
			URI ontology = findOntology(rdf);
			if (ontology != null) {
				ontologies.put(path, ontology);
				jar.putNextEntry(new JarEntry(path));
				copyInto(rdf, jar);
			}
		}
		if (ontologies.isEmpty())
			return;
		jar.putNextEntry(new JarEntry(META_INF_ONTOLOGIES));
		PrintStream out = new PrintStream(jar);
		for (Map.Entry<String, URI> e : ontologies.entrySet()) {
			out.print(e.getKey());
			out.print("\t=\t");
			out.println(e.getValue().toString());
		}
		out.flush();
	}

	private File asLocalFile(URL rdf) throws UnsupportedEncodingException {
		return new File(URLDecoder.decode(rdf.getFile(), "UTF-8"));
	}

	private URI findOntology(URL rdf) throws RepositoryException,
			RDFParseException, IOException {
		Repository repository = createRepository();
		ValueFactory vf = repository.getValueFactory();
		loadOntology(repository, rdf);
		RepositoryConnection conn = repository.getConnection();
		try {
			Statement st = first(conn, RDF.TYPE, OWL.ONTOLOGY);
			if (st != null)
				return (URI) st.getSubject();
			st = first(conn, RDFS.ISDEFINEDBY, null);
			if (st != null)
				return (URI) st.getObject();
			st = first(conn, RDF.TYPE, OWL.CLASS);
			if (st != null)
				return vf.createURI(((URI) st.getSubject()).getNamespace());
			st = first(conn, RDF.TYPE, RDFS.CLASS);
			if (st != null)
				return vf.createURI(((URI) st.getSubject()).getNamespace());
			return null;
		} finally {
			conn.clear();
			conn.close();
		}
	}

	private Statement first(RepositoryConnection conn, URI pred, Value obj)
			throws RepositoryException {
		RepositoryResult<Statement> stmts;
		stmts = conn.getStatements(null, pred, obj, true);
		try {
			if (stmts.hasNext())
				return stmts.next();
			return null;
		} finally {
			stmts.close();
		}
	}

}
