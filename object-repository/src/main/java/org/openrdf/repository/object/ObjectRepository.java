/*
 * Copyright (c) 2007-2009, James Leigh All rights reserved.
 * Copyright (c) 2011, Talis Inc. Some rights reserved.
 * Copyright (c) 2011, 3 Round Stones Inc. Some rights reserved.
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
package org.openrdf.repository.object;

import static org.openrdf.query.QueryLanguage.SPARQL;
import info.aduna.io.FileUtil;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openrdf.model.Literal;
import org.openrdf.model.Model;
import org.openrdf.model.Namespace;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.query.BindingSet;
import org.openrdf.query.GraphQuery;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.config.RepositoryConfigException;
import org.openrdf.repository.contextaware.ContextAwareConnection;
import org.openrdf.repository.contextaware.ContextAwareRepository;
import org.openrdf.repository.object.annotations.triggeredBy;
import org.openrdf.repository.object.compiler.OWLCompiler;
import org.openrdf.repository.object.compiler.OntologyLoader;
import org.openrdf.repository.object.composition.ClassFactory;
import org.openrdf.repository.object.composition.ClassResolver;
import org.openrdf.repository.object.config.ObjectRepositoryFactory;
import org.openrdf.repository.object.exceptions.ObjectStoreConfigException;
import org.openrdf.repository.object.managers.LiteralManager;
import org.openrdf.repository.object.managers.PropertyMapper;
import org.openrdf.repository.object.managers.RoleMapper;
import org.openrdf.repository.object.managers.TypeManager;
import org.openrdf.repository.object.managers.helpers.RoleClassLoader;
import org.openrdf.repository.object.trigger.Trigger;
import org.openrdf.repository.object.trigger.TriggerConnection;
import org.openrdf.repository.object.vocabulary.MSG;
import org.openrdf.repository.object.vocabulary.OBJ;
import org.openrdf.repository.query.NamedQuery;
import org.openrdf.repository.query.NamedQueryRepository;
import org.openrdf.repository.query.config.NamedQueryRepositoryFactory;
import org.openrdf.rio.RDFParseException;
import org.openrdf.store.blob.BlobStore;
import org.openrdf.store.blob.BlobStoreFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates the {@link ObjectConnection} used to interact with the repository.
 * Use {@link ObjectRepositoryFactory} to create this.
 * 
 * @author James Leigh
 * @author Steve Battle
 * 
 */
public class ObjectRepository extends ContextAwareRepository implements NamedQueryRepository {
	private static final String SELECT_GRAPH_BY_TYPE = "SELECT ?graph ?subject ?predicate ?object\n"
			+ "WHERE { ?graph a $type GRAPH ?graph { ?subject ?predicate ?object } }";
	private static final URI[] LIST_PROPERTIES = new URI[] { RDF.REST,
			OWL.DISTINCTMEMBERS, OWL.UNIONOF, OWL.INTERSECTIONOF, OWL.ONEOF };
	private static final String PREFIX = "PREFIX msg:<" + MSG.NAMESPACE + ">\n"
			+ "PREFIX obj:<" + OBJ.NAMESPACE + ">\n" + "PREFIX owl:<"
			+ OWL.NAMESPACE + ">\n" + "PREFIX rdfs:<" + RDFS.NAMESPACE + ">\n"
			+ "PREFIX rdf:<" + RDF.NAMESPACE + ">\n";
	private static final String WHERE_SCHEMA = "{ ?s a rdfs:Datatype } UNION "
			+ "{ ?s a owl:Class } UNION { ?s a rdfs:Class } UNION { ?s a owl:DeprecatedClass } UNION "
			+ "{ ?s a owl:AnnotationProperty } UNION { ?s a owl:DeprecatedProperty } UNION "
			+ "{ ?s a owl:DatatypeProperty } UNION { ?s a owl:ObjectProperty } UNION "
			+ "{ ?s a owl:Restriction } UNION { ?s a owl:Ontology } UNION "
			+ "{ ?s a rdf:Property } UNION { ?s a owl:FunctionalProperty } UNION "
			+ "{ ?s owl:complementOf [] } UNION { ?s owl:intersectionOf [] } UNION "
			+ "{ ?s owl:oneOf []} UNION { ?s owl:unionOf [] } UNION "
			+ "{ ?s owl:equivalentClass []} UNION { ?s owl:equivalentProperty []} UNION "
			+ "{ ?s rdfs:domain [] } UNION { ?s rdfs:range [] } UNION "
			+ "{ ?s rdfs:subClassOf [] } UNION { ?s rdfs:subPropertyOf [] } UNION "
			+ "{ ?s owl:onProperty [] } UNION { [owl:hasValue ?s] } UNION "
			+ "{ ?s msg:matching ?lit } UNION { ?s obj:matches ?lit }\n";
	private static final String CONSTRUCT_SCHEMA = PREFIX
			+ "CONSTRUCT { ?s ?p ?o }\n" + "WHERE { ?s ?p ?o .\n"
			+ WHERE_SCHEMA + "}";
	private static final Pattern PATTERN = Pattern.compile("composed(\\d+)",
			Pattern.CASE_INSENSITIVE);
	private static final Collection<File> temporary = new ArrayList<File>();

	private static void deleteOnExit(File dir) {
		synchronized (temporary) {
			if (temporary.isEmpty()) {
				Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
					public void run() {
						synchronized (temporary) {
							for (File dir : temporary) {
								try {
									if (dir.isDirectory()) {
										FileUtil.deleteDir(dir);
									} else {
										dir.delete();
									}
								} catch (IOException e) {
									// ignore
								}
							}
						}
					}
				}, "ObjectRepository.cleanup"));
			}
			temporary.add(dir);
		}
	}

	private Logger logger = LoggerFactory.getLogger(ObjectRepository.class);
	private boolean initialized;
	private final ClassLoader baseClassLoader;
	private final RoleMapper baseRoleMapper;
	private final LiteralManager baseLiteralManager;
	private String pkgPrefix = "";
	private String propertyPrefix;
	private boolean followImports;
	private List<URL> imports = Collections.emptyList();
	private URL[] cp;
	private File libDir;
	private volatile int revision;
	private ClassLoader cl;
	private RoleMapper mapper;
	private LiteralManager literals;
	private PropertyMapper pm;
	private ClassResolver resolver;
	private Map<URI, Set<Trigger>> triggers;
	private final Object compiling = new Object();
	private final Object compiled = new Object();
	private volatile Set<ObjectConnection> compileAfter;
	private List<Runnable> schemaListeners = new ArrayList<Runnable>();
	private URI schemaGraph;
	private URI schemaGraphType;
	private long schemaHash;
	/** Support for NamedQueryRepository */
	private NamedQueryRepository delegate ;
	private String blobStoreUrl;
	private Map<String, String> blobStoreParameters;
	private BlobStore blobs;

	public ObjectRepository(RoleMapper mapper, LiteralManager literals,
			ClassLoader cl) {
		this.baseClassLoader = cl;
		this.baseRoleMapper = mapper;
		this.baseLiteralManager = literals;
	}

	/**
	 * Assigns a name prefix prepended to the beginning of all compiled
	 * packages.
	 * 
	 * @param prefix
	 *            such as "compiled."
	 */
	public void setPackagePrefix(String prefix) {
		this.pkgPrefix = prefix;
	}

	/**
	 * Assigns the member prefix to use when compiling classes.
	 */
	public void setMemberPrefix(String prefix) {
		this.propertyPrefix = prefix;
	}

	/**
	 * Additional classpath that should be included after the model is compiled.
	 */
	public void setBehaviourClassPath(URL[] cp) {
		this.cp = cp;
	}

	public void setFollowImports(boolean followImports) {
		this.followImports = followImports;
	}

	public void setOWLImports(List<URL> imports) {
		this.imports = imports;
	}

	public void setSchemaGraph(URI graphURI) throws RepositoryException {
		boolean changed = false;
		synchronized (this) {
			if (schemaGraph == null || !schemaGraph.equals(graphURI)) {
				schemaGraph = graphURI;
				changed = true;
			}
		}
		if (changed) {
			recompileWithNotification();
		}
	}

	public void setSchemaGraphType(URI rdfType) throws RepositoryException {
		boolean changed = false;
		synchronized (this) {
			if (schemaGraphType == null || !schemaGraphType.equals(rdfType)) {
				schemaGraphType = rdfType;
				changed = true;
			}
		}
		if (changed) {
			recompileWithNotification();
		}
	}

	public boolean addSchemaListener(Runnable action) {
		return schemaListeners.add(action);
	}

	public boolean removeSchemaListener(Runnable action) {
		return schemaListeners.remove(action);
	}

	public String getBlobStoreUrl() {
		return blobStoreUrl;
	}

	public void setBlobStoreUrl(String blobStoreUrl) {
		this.blobStoreUrl = blobStoreUrl;
	}

	public Map<String, String> getBlobStoreParameters() {
		return blobStoreParameters;
	}

	public void setBlobStoreParameters(Map<String, String> blobStoreParameters) {
		this.blobStoreParameters = blobStoreParameters;
	}

	@Override
	public void initialize() throws RepositoryException {
		super.initialize();
		try {
			init(getDataDir());
		} catch (ObjectStoreConfigException e) {
			throw new RepositoryException(e);
		}
	}

	public ValueFactory getURIFactory() {
		return super.getValueFactory();
	}

	public ValueFactory getLiteralFactory() {
		return super.getValueFactory();
	}

	public boolean isCompileRepository() {
		return compileAfter != null;
	}

	public synchronized void setCompileRepository(boolean compileRepository)
			throws ObjectStoreConfigException, RepositoryException {
		if (compileRepository && compileAfter == null) {
			compileAfter = new HashSet<ObjectConnection>();
			if (initialized) {
				recompileWithNotification();
			}
		} else if (!compileRepository && compileAfter != null) {
			compileAfter = null;
		}
	}

	public BlobStore getBlobStore() {
		return blobs;
	}

	public void setBlobStore(BlobStore store) {
		this.blobs = store;
	}

	/**
	 * Called by {@link ObjectRepositoryFactory} when the delegate repository
	 * has already been initialized.
	 */
	public synchronized void init(File dataDir) throws RepositoryException,
			ObjectStoreConfigException {
		initialized = true;
		if (dataDir == null) {
			try {
				libDir = createTempDir("lib");
			} catch (IOException e) {
				throw new RepositoryException(e);
			}
		} else {
			libDir = new File(dataDir, "lib");
			libDir.mkdirs();
		}
		deleteOnExit(libDir);
		synchronized (compiling) {
			Model schema = new LinkedHashModel();
			try {
				if (isCompileRepository()) {
					loadSchema(schema);
				}
				compile(schema);
				schemaHash = hash(schema);
				System.gc();
			} catch (RDFParseException e) {
				throw new ObjectStoreConfigException(e);
			} catch (IOException e) {
				throw new ObjectStoreConfigException(e);
			}
		}
		if (blobStoreUrl != null && blobs == null) {
			try {
				java.net.URI base = new File(dataDir, ".").toURI();
				String url = base.resolve(blobStoreUrl).toString();
				BlobStoreFactory bsf = BlobStoreFactory.newInstance();
				blobs = bsf.openBlobStore(url, blobStoreParameters);
			} catch (IOException e) {
				throw new ObjectStoreConfigException(e);
			} catch (IllegalArgumentException e) {
				throw new ObjectStoreConfigException(e);
			}
		}
	}

	/**
	 * Creates a new ObjectConnection that will need to be closed by the caller.
	 */
	@Override
	public ObjectConnection getConnection() throws RepositoryException {
		ObjectConnection con;
		RepositoryConnection conn = getDelegate().getConnection();
		synchronized (compiled) {
			ObjectFactory factory = createObjectFactory(mapper, pm, literals,
					resolver, cl);
			TriggerConnection tc = null;
			if (triggers != null) {
				conn = tc = new TriggerConnection(conn, triggers);
			}
			con = new ObjectConnection(this, conn, factory, createTypeManager(), blobs);
			if (tc != null) {
				tc.setObjectConnection(con);
			}
		}
		con.setIncludeInferred(isIncludeInferred());
		con.setMaxQueryTime(getMaxQueryTime());
		// con.setQueryResultLimit(getQueryResultLimit());
		con.setQueryLanguage(getQueryLanguage());
		con.setReadContexts(getReadContexts());
		con.setAddContexts(getAddContexts());
		con.setRemoveContexts(getRemoveContexts());
		con.setArchiveContexts(getArchiveContexts());
		return con;
	}
	
	@Override
	public void setDelegate(Repository delegate) {
		try {
			// if the delegate is an extant NamedQueryRepository the factory returns it directly
			// otherwise it is wrapped
			this.delegate = new NamedQueryRepositoryFactory().createRepository(delegate) ;
			super.setDelegate(this.delegate);
		} catch (RepositoryConfigException e) {
			logger.error(e.toString(), e) ;
		} catch (RepositoryException e) {
			logger.error(e.toString(), e) ;
		}
	}

	public NamedQuery createNamedQuery(URI uri, String queryString)
			throws RepositoryException {
		return createNamedQuery(uri, getQueryLanguage(), queryString,
				uri.stringValue());
	}

	public NamedQuery createNamedQuery(URI uri, QueryLanguage ql,
			String queryString) throws RepositoryException {
		return createNamedQuery(uri, ql, queryString, uri.stringValue());
	}
	
	/* Delegate support for the NamedQueryRepository interface */

	public NamedQuery createNamedQuery(URI uri, QueryLanguage ql,
			String queryString, String baseURI) throws RepositoryException {
		return delegate.createNamedQuery(uri, ql, queryString, baseURI);
	}

	public void removeNamedQuery(URI uri) throws RepositoryException {
		delegate.removeNamedQuery(uri) ;
	}

	public URI[] getNamedQueryIDs() throws RepositoryException {
		return delegate.getNamedQueryIDs() ;
	}

	public NamedQuery getNamedQuery(URI uri) throws RepositoryException {
		return delegate.getNamedQuery(uri) ;
	}

	protected void compileAfter(ObjectConnection con) {
		if (isCompileRepository()) {
			synchronized (compileAfter) {
				compileAfter.add(con);
			}
		}
	}

	protected void closed(ObjectConnection con) throws RepositoryException {
		if (unscheduleRecompile(con)) {
			recompileWithNotification();
		}
	}

	protected TypeManager createTypeManager() {
		return new TypeManager(mapper.isNamedTypePresent());
	}

	protected ObjectFactory createObjectFactory(RoleMapper mapper,
			PropertyMapper pm, LiteralManager literalManager,
			ClassResolver resolver, ClassLoader cl) {
		return new ObjectFactory(mapper, pm, literalManager, resolver, cl);
	}

	protected ClassResolver createClassResolver(ClassFactory definer,
			RoleMapper mapper, PropertyMapper pm) {
		ClassResolver resolver = new ClassResolver();
		resolver.setPropertyMapper(pm);
		resolver.setRoleMapper(mapper);
		resolver.setClassDefiner(definer);
		resolver.setBaseClassRoles(mapper.getConceptClasses());
		resolver.init();
		return resolver;
	}

	protected PropertyMapper createPropertyMapper(ClassLoader cl) {
		return new PropertyMapper(cl, mapper.isNamedTypePresent());
	}

	protected ClassFactory createClassFactory(File composed, ClassLoader cl) {
		return new ClassFactory(composed, cl);
	}

	private boolean unscheduleRecompile(ObjectConnection con) {
		if (isCompileRepository()) {
			synchronized (compileAfter) {
				if (con == null || compileAfter.remove(con))
					return compileAfter.isEmpty();
			}
		}
		return false;
	}

	private void recompileWithNotification() throws RepositoryException {
		boolean changed = false;
		synchronized (compiling) {
			if (isCompileRepository() && !isRecompileScheduled()) {
				changed = recompile();
			}
		}
		if (changed) {
			for (Runnable action : schemaListeners) {
				action.run();
			}
		}
	}

	private boolean isRecompileScheduled() {
		synchronized (compileAfter) {
			return !compileAfter.isEmpty();
		}
	}

	private boolean recompile() throws RepositoryException {
		Model schema = new LinkedHashModel();
		loadSchema(schema);
		try {
			long hash = hash(schema);
			if (schemaHash != hash) {
				compileSchema(schema);
				schemaHash = hash;
				System.gc();
				return true;
			}
		} catch (ObjectStoreConfigException e) {
			throw new RepositoryException(e);
		} catch (RDFParseException e) {
			throw new RepositoryException(e);
		} catch (IOException e) {
			throw new RepositoryException(e);
		}
		return false;
	}

	private File createTempDir(String name) throws IOException {
		String tmpDirStr = System.getProperty("java.io.tmpdir");
		if (tmpDirStr != null) {
			File tmpDir = new File(tmpDirStr);
			if (!tmpDir.exists()) {
				tmpDir.mkdirs();
			}
		}
		File tmp = File.createTempFile(name, "");
		tmp.delete();
		tmp.mkdir();
		return tmp;
	}

	private long hash(Model schema) {
		long hash = 0;
		for (Statement st : schema) {
			Resource subj = st.getSubject();
			URI pred = st.getPredicate();
			Value obj = st.getObject();
			boolean relevant = OWL.NAMESPACE.equals(pred.getNamespace());
			relevant |= RDFS.NAMESPACE.equals(pred.getNamespace());
			relevant |= RDF.NAMESPACE.equals(pred.getNamespace());
			relevant |= MSG.NAMESPACE.equals(pred.getNamespace());
			relevant |= OBJ.NAMESPACE.equals(pred.getNamespace());
			final URI ANN = OWL.ANNOTATIONPROPERTY;
			if (relevant || mapper.isRecordedAnnotation(pred)
					|| schema.contains(pred, RDF.TYPE, ANN)) {
				hash += 31 * pred.hashCode();
				if (subj instanceof URI) {
					hash += 961 * subj.hashCode();
				}
				if (obj instanceof URI) {
					hash += obj.hashCode();
				}
				if (obj instanceof Literal) {
					hash += obj.hashCode();
				}
			}
		}
		return hash;
	}

	private void compile(Model schema) throws RepositoryException,
			ObjectStoreConfigException, RDFParseException, IOException {
		try {
			compileSchema(schema);
		} catch (ObjectStoreConfigException e) {
			try {
				logger.error(e.toString(), e);
				if (schema == null || schema.isEmpty())
					throw e;
				compileSchema(new LinkedHashModel());
			} catch (ObjectStoreConfigException e2) {
				throw e;
			} catch (RepositoryException e2) {
				throw e;
			} catch (RDFParseException e2) {
				throw e;
			} catch (IOException e2) {
				throw e;
			}
		} catch (RepositoryException e) {
			try {
				logger.error(e.toString(), e);
				if (schema == null || schema.isEmpty())
					throw e;
				compileSchema(new LinkedHashModel());
			} catch (ObjectStoreConfigException e2) {
				throw e;
			} catch (RepositoryException e2) {
				throw e;
			} catch (RDFParseException e2) {
				throw e;
			} catch (IOException e2) {
				throw e;
			}
		} catch (RDFParseException e) {
			try {
				logger.error(e.toString(), e);
				if (schema == null || schema.isEmpty())
					throw e;
				compileSchema(new LinkedHashModel());
			} catch (ObjectStoreConfigException e2) {
				throw e;
			} catch (RepositoryException e2) {
				throw e;
			} catch (RDFParseException e2) {
				throw e;
			} catch (IOException e2) {
				throw e;
			}
		} catch (IOException e) {
			try {
				logger.error(e.toString(), e);
				if (schema == null || schema.isEmpty())
					throw e;
				compileSchema(new LinkedHashModel());
			} catch (ObjectStoreConfigException e2) {
				throw e;
			} catch (RepositoryException e2) {
				throw e;
			} catch (RDFParseException e2) {
				throw e;
			} catch (IOException e2) {
				throw e;
			}
		}
	}

	private void compileSchema(Model schema) throws RepositoryException,
			ObjectStoreConfigException, RDFParseException, IOException {
		logger.info("Compiling schema");
		incrementRevision();
		ClassLoader cl = baseClassLoader;
		RoleMapper mapper = baseRoleMapper.clone();
		LiteralManager literals = baseLiteralManager.clone();
		File concepts = new File(libDir, "concepts" + revision + ".jar");
		File behaviours = new File(libDir, "behaviours" + revision + ".jar");
		final File composed = new File(libDir, "composed" + revision);
		OntologyLoader ontologies = new OntologyLoader(schema);
		ontologies.loadOntologies(imports);
		if (followImports) {
			ontologies.followImports();
		}
		Map<URI, Map<String, String>> namespaces;
		namespaces = getNamespaces(schema, ontologies.getNamespaces());
		if (schema != null && !schema.isEmpty()) {
			OWLCompiler compiler = new OWLCompiler(mapper, literals);
			compiler.setModel(schema);
			compiler.setPackagePrefix(pkgPrefix);
			compiler.setMemberPrefix(propertyPrefix);
			compiler.setPrefixNamespaces(namespaces);
			compiler.setClassLoader(cl);
			cl = compiler.createConceptJar(concepts);
			compiler.setClassLoader(cl);
			cl = compiler.createBehaviourJar(behaviours);
			RoleClassLoader loader = new RoleClassLoader(mapper);
			loader.loadRoles(cl);
			literals.setClassLoader(cl);
		}
		if (isCompileRepository()) {
			mapper.addBehaviour(CompileTrigger.class, RDFS.RESOURCE);
		}
		if (cp != null && cp.length > 0) {
			cl = new URLClassLoader(cp, cl);
			RoleClassLoader loader = new RoleClassLoader(mapper);
			loader.loadRoles(cl);
			for (URL url : cp) {
				loader.scan(url, cl);
			}
			literals.setClassLoader(cl);
		}
		ClassFactory definer = createClassFactory(composed, cl);
		if (composed.exists()) {
			deleteOnExit(composed);
		}
		if (behaviours.exists()) {
			behaviours.deleteOnExit();
		}
		if (concepts.exists()) {
			concepts.deleteOnExit();
		}
		cl = definer;
		synchronized (compiled) {
			this.cl = cl;
			this.mapper = mapper;
			this.literals = literals;
			pm = createPropertyMapper(definer);
			resolver = createClassResolver(definer, mapper, pm);
			Collection<Method> methods = mapper.getTriggerMethods();
			if (methods.isEmpty()) {
				triggers = null;
			} else {
				triggers = new HashMap<URI, Set<Trigger>>(methods.size());
				for (Method method : methods) {
					for (String uri : method.getAnnotation(triggeredBy.class)
							.value()) {
						URI key = getURIFactory().createURI(uri);
						Set<Trigger> set = triggers.get(key);
						if (set == null) {
							triggers.put(key, set = new HashSet<Trigger>());
						}
						set.add(new Trigger(method));
					}
				}
			}
		}
	}

	private void incrementRevision() {
		File[] listFiles = libDir.listFiles();
		if (listFiles != null) {
			for (File file : listFiles) {
				Matcher m = PATTERN.matcher(file.getName());
				if (m.matches()) {
					int version = Integer.parseInt(m.group(1));
					if (revision < version) {
						revision = version;
					}
				}
			}
		}
		revision++;
	}

	private Map<URI, Map<String, String>> getNamespaces(Model schema,
			Map<URI, Map<String, String>> namespaces)
			throws RepositoryException {
		if (!isCompileRepository())
			return namespaces;
		ContextAwareConnection con = super.getConnection();
		try {
			Map<String, String> map = new HashMap<String, String>();
			RepositoryResult<Namespace> ns = con.getNamespaces();
			try {
				while (ns.hasNext()) {
					Namespace n = ns.next();
					map.put(n.getPrefix(), n.getName());
					schema.setNamespace(n.getPrefix(), n.getName());
				}
			} finally {
				ns.close();
			}
			namespaces.put(null, map);
		} finally {
			con.close();
		}
		return namespaces;
	}

	private void loadSchema(Model schema) throws RepositoryException,
			AssertionError {
		RepositoryConnection conn = getDelegate().getConnection();
		try {
			if (schemaGraph == null && schemaGraphType == null) {
				GraphQuery query = conn.prepareGraphQuery(SPARQL, CONSTRUCT_SCHEMA);
				GraphQueryResult result = query.evaluate();
				try {
					while (result.hasNext()) {
						schema.add(result.next());
					}
				} finally {
					result.close();
				}
				addLists(conn, schema);
			} else if (schemaGraph == null) {
				TupleQuery qry = conn.prepareTupleQuery(SPARQL, SELECT_GRAPH_BY_TYPE);
				qry.setBinding("type", schemaGraphType);
				TupleQueryResult result = qry.evaluate();
				try {
					while (result.hasNext()) {
						BindingSet b = result.next();
						Resource subj = (Resource) b.getValue("subject");
						URI pred = (URI) b.getValue("predicate");
						Value obj = b.getValue("object");
						Resource grp = (Resource) b.getValue("graph");
						schema.add(subj, pred, obj, grp);
					}
				} finally {
					result.close();
				}
			} else {
				RepositoryResult<Statement> result;
				result = conn.getStatements(null, null, null, true, schemaGraph);
				try {
					while (result.hasNext()) {
						schema.add(result.next());
					}
				} finally {
					result.close();
				}
			}
		} catch (MalformedQueryException e) {
			throw new AssertionError(e);
		} catch (QueryEvaluationException e) {
			throw new RepositoryException(e);
		} finally {
			conn.close();
		}
	}

	private void addLists(RepositoryConnection conn, Model schema)
			throws RepositoryException {
		boolean modified = false;
		List<Value> lists = new ArrayList<Value>();
		for (URI pred : LIST_PROPERTIES) {
			lists.addAll(schema.filter(null, pred, null).objects());
		}
		for (Value list : lists) {
			if (list instanceof Resource
					&& !schema.contains((Resource) list, null, null)) {
				RepositoryResult<Statement> stmts = conn.getStatements(
						(Resource) list, null, null, true);
				try {
					while (stmts.hasNext()) {
						schema.add(stmts.next());
						modified = true;
					}
				} finally {
					stmts.close();
				}
			}
		}
		if (modified) {
			addLists(conn, schema);
		}
	}

	public abstract static class CompileTrigger implements RDFObject {
		private static final String MSGNS = MSG.NAMESPACE;
		private static final String OBJNS = OBJ.NAMESPACE;
		private static final String RDFSNS = RDFS.NAMESPACE;
		private static final String OWLNS = OWL.NAMESPACE;

		@triggeredBy( { OWLNS + "imports", OWLNS + "complementOf",
				OWLNS + "intersectionOf", OWLNS + "oneOf",
				OWLNS + "onProperty", OWLNS + "unionOf", RDFSNS + "domain",
				RDFSNS + "range", RDFSNS + "subClassOf",
				RDFSNS + "subPropertyOf", MSGNS + "matching",
				MSGNS + "precedes", MSGNS + "triggeredBy", MSGNS + "imports",
				MSGNS + "sparql", MSGNS + "xslt", MSGNS + "script",
				MSGNS + "query", MSGNS + "expect", MSGNS + "header",
				MSGNS + "realm", MSGNS + "rel", MSGNS + "transform",
				OBJNS + "matches" })
		public void schemaChanged() {
			ObjectConnection con = getObjectConnection();
			con.getRepository().compileAfter(con);
		}
	}

}
