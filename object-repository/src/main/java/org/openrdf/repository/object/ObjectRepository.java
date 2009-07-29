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
import org.openrdf.query.GraphQuery;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.contextaware.ContextAwareConnection;
import org.openrdf.repository.contextaware.ContextAwareRepository;
import org.openrdf.repository.object.annotations.triggeredBy;
import org.openrdf.repository.object.behaviours.CompileTrigger;
import org.openrdf.repository.object.compiler.OWLCompiler;
import org.openrdf.repository.object.compiler.OntologyLoader;
import org.openrdf.repository.object.composition.AbstractClassFactory;
import org.openrdf.repository.object.composition.ClassFactory;
import org.openrdf.repository.object.composition.ClassResolver;
import org.openrdf.repository.object.composition.PropertyMapperFactory;
import org.openrdf.repository.object.composition.SparqlBehaviourFactory;
import org.openrdf.repository.object.composition.helpers.PropertySetFactory;
import org.openrdf.repository.object.config.ObjectRepositoryFactory;
import org.openrdf.repository.object.exceptions.ObjectStoreConfigException;
import org.openrdf.repository.object.managers.LiteralManager;
import org.openrdf.repository.object.managers.PropertyMapper;
import org.openrdf.repository.object.managers.RoleMapper;
import org.openrdf.repository.object.managers.TypeManager;
import org.openrdf.repository.object.managers.helpers.RoleClassLoader;
import org.openrdf.repository.object.trigger.Trigger;
import org.openrdf.repository.object.trigger.TriggerConnection;
import org.openrdf.repository.object.vocabulary.OBJ;
import org.openrdf.rio.RDFParseException;

/**
 * Creates the {@link ObjectConnection} used to interact with the repository.
 * Use {@link ObjectRepositoryFactory} to create this.
 * 
 * @author James Leigh
 * 
 */
public class ObjectRepository extends ContextAwareRepository {
	private static final String PREFIX = "PREFIX obj:<" + OBJ.NAMESPACE + ">\n"
			+ "PREFIX owl:<" + OWL.NAMESPACE + ">\n" + "PREFIX rdfs:<"
			+ RDFS.NAMESPACE + ">\n" + "PREFIX rdf:<" + RDF.NAMESPACE + ">\n";
	private static final String CONSTRUCT_SCHEMA = PREFIX
			+ "CONSTRUCT { ?s ?p ?o } WHERE { ?s ?p ?o { ?s a rdfs:Datatype } UNION "
			+ "{ ?s a owl:Class } UNION { ?s a rdfs:Class } UNION "
			+ "{ ?s a owl:DeprecatedClass } UNION { ?s a owl:DeprecatedProperty } UNION "
			+ "{ ?s a owl:DatatypeProperty } UNION { ?s a owl:ObjectProperty } UNION "
			+ "{ ?s a owl:Restriction } UNION { ?s a owl:Ontology } UNION "
			+ "{ ?s a rdf:Property } UNION { ?s a owl:FunctionalProperty } UNION "
			+ "{ ?s owl:complementOf [] } UNION { ?s owl:intersectionOf [] } UNION "
			+ "{ ?s owl:oneOf []} UNION { ?s owl:unionOf [] } UNION "
			+ "{ ?s rdfs:domain [] } UNION { ?s rdfs:range [] } UNION "
			+ "{ ?s rdfs:subClassOf [] } UNION { ?s rdfs:subPropertyOf [] } UNION "
			+ "{ ?s owl:onProperty [] } UNION { ?s obj:matches ?lit } }";
	private ClassLoader baseClassLoader;
	private RoleMapper baseRoleMapper;
	private LiteralManager baseLiteralManager;
	private String pkgPrefix = "";
	private String propertyPrefix;
	private boolean followImports;
	private List<URL> imports = Collections.emptyList();
	private URL[] cp;
	private File dataDir;
	private int revision;
	private ClassLoader cl;
	private RoleMapper mapper;
	private LiteralManager literals;
	private PropertyMapper pm;
	private ClassResolver resolver;
	private Map<URI, Set<Trigger>> triggers;
	private Set<ObjectConnection> compileAfter;

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

	public synchronized void setCompileRepository(boolean compileRepository) {
		if (compileRepository && compileAfter == null) {
			compileAfter = new HashSet<ObjectConnection>();
		} else if (!compileRepository && compileAfter != null) {
			compileAfter = null;
		}
	}

	public synchronized void compileAfter(ObjectConnection con) {
		if (isCompileRepository()) {
			compileAfter.add(con);
		}
	}

	public synchronized void closed(ObjectConnection con) throws RepositoryException {
		if (isCompileRepository() && compileAfter.remove(con)
				&& compileAfter.isEmpty()) {
			Model schema = new LinkedHashModel();
			loadSchema(schema);
			try {
				compile(schema);
			} catch (ObjectStoreConfigException e) {
				throw new RepositoryException(e);
			} catch (RDFParseException e) {
				throw new RepositoryException(e);
			} catch (IOException e) {
				throw new RepositoryException(e);
			}
		}
	}

	/**
	 * Called by {@link ObjectRepositoryFactory} when the delegate repository
	 * has already been initialized.
	 */
	public void init(File dataDir) throws RepositoryException,
			ObjectStoreConfigException {
		assert dataDir != null;
		this.dataDir = dataDir;
		Model schema = new LinkedHashModel();
		try {
			if (isCompileRepository()) {
				loadSchema(schema);
			}
			compile(schema);
		} catch (RDFParseException e) {
			throw new ObjectStoreConfigException(e);
		} catch (IOException e) {
			throw new ObjectStoreConfigException(e);
		}
	}

	@Override
	public void shutDown() throws RepositoryException {
		super.shutDown();
		if (dataDir != null && !dataDir.equals(getDataDir())) {
			try {
				FileUtil.deleteDir(dataDir);
			} catch (IOException e) {
				throw new RepositoryException(e);
			}
		}
	}

	/**
	 * Creates a new ObjectConnection that will need to be closed by the caller.
	 */
	@Override
	public synchronized ObjectConnection getConnection() throws RepositoryException {
		ObjectConnection con;
		TriggerConnection tc = null;
		RepositoryConnection conn = getDelegate().getConnection();
		ObjectFactory factory = createObjectFactory(mapper, pm, literals,
				resolver, cl);
		if (triggers != null) {
			conn = tc = new TriggerConnection(conn, triggers);
		}
		con = new ObjectConnection(this, conn, factory, createTypeManager());
		if (tc != null) {
			tc.setObjectConnection(con);
		}
		con.setIncludeInferred(isIncludeInferred());
		con.setMaxQueryTime(getMaxQueryTime());
		//con.setQueryResultLimit(getQueryResultLimit());
		con.setQueryLanguage(getQueryLanguage());
		con.setReadContexts(getReadContexts());
		con.setAddContexts(getAddContexts());
		con.setRemoveContexts(getRemoveContexts());
		con.setArchiveContexts(getArchiveContexts());
		return con;
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
		PropertyMapperFactory pmf = new PropertyMapperFactory();
		pmf.setPropertyMapperFactoryClass(PropertySetFactory.class);
		SparqlBehaviourFactory sbf = new SparqlBehaviourFactory();
		ClassResolver resolver = new ClassResolver();
		AbstractClassFactory abc = new AbstractClassFactory();
		resolver.setInterfaceBehaviourResolver(pmf);
		resolver.setAbstractBehaviourResolver(abc);
		resolver.setSparqlBehaviourFactory(sbf);
		resolver.setRoleMapper(mapper);
		pmf.setClassDefiner(definer);
		pmf.setPropertyMapper(pm);
		sbf.setClassDefiner(definer);
		sbf.setPropertyMapper(pm);
		abc.setClassDefiner(definer);
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

	private void compile(Model schema) throws RepositoryException,
			ObjectStoreConfigException, RDFParseException, IOException {
		revision++;
		cl = baseClassLoader;
		mapper = baseRoleMapper.clone();
		literals = baseLiteralManager.clone();
		File concepts = new File(dataDir, "concepts" + revision + ".jar");
		File behaviours = new File(dataDir, "behaviours" + revision + ".jar");
		File composed = new File(dataDir, "composed" + revision);
		OntologyLoader ontologies = new OntologyLoader(schema);
		ontologies.loadOntologies(imports);
		if (followImports) {
			ontologies.followImports();
		}
		Map<URI, Map<String, String>> namespaces;
		namespaces = getNamespaces(ontologies.getNamespaces());
		if (schema != null && !schema.isEmpty()) {
			OWLCompiler compiler = new OWLCompiler(mapper, literals);
			compiler.setPackagePrefix(pkgPrefix);
			compiler.setMemberPrefix(propertyPrefix);
			compiler.setConceptJar(concepts);
			compiler.setBehaviourJar(behaviours);
			cl = compiler.compile(namespaces, schema, cl);
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
		cl = definer;
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

	private Map<URI, Map<String, String>> getNamespaces(Map<URI, Map<String, String>> namespaces)
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

	private void loadSchema(Model schema) throws RepositoryException, AssertionError {
		RepositoryConnection conn = getDelegate().getConnection();
		try {
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
			// TODO follow imports
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
		List<Value> lists = new ArrayList<Value>(schema.filter(null, RDF.REST,
				null).objects());
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

}
