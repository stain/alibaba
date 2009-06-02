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

import info.aduna.io.FileUtil;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.openrdf.model.Model;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.contextaware.ContextAwareRepository;
import org.openrdf.repository.object.annotations.triggeredBy;
import org.openrdf.repository.object.compiler.OWLCompiler;
import org.openrdf.repository.object.composition.AbstractClassFactory;
import org.openrdf.repository.object.composition.ClassCompositor;
import org.openrdf.repository.object.composition.ClassFactory;
import org.openrdf.repository.object.composition.ClassResolver;
import org.openrdf.repository.object.composition.PropertyMapperFactory;
import org.openrdf.repository.object.composition.SparqlBehaviourFactory;
import org.openrdf.repository.object.composition.helpers.PropertySetFactory;
import org.openrdf.repository.object.exceptions.ObjectStoreConfigException;
import org.openrdf.repository.object.managers.LiteralManager;
import org.openrdf.repository.object.managers.PropertyMapper;
import org.openrdf.repository.object.managers.RoleMapper;
import org.openrdf.repository.object.managers.TypeManager;
import org.openrdf.repository.object.managers.helpers.RoleClassLoader;
import org.openrdf.repository.object.trigger.Trigger;
import org.openrdf.repository.object.trigger.TriggerConnection;

/**
 * @author James Leigh
 * 
 */
public class ObjectRepository extends ContextAwareRepository {
	private ClassLoader cl;
	private Model schema;
	private RoleMapper mapper;
	private LiteralManager literals;
	private String pkgPrefix = "";
	private String propertyPrefix;
	private Map<URI, Map<String, String>> namespaces;
	private PropertyMapper pm;
	private ClassResolver resolver;
	private File dataDir;
	private File concepts;
	private File behaviours;
	private URL[] cp;
	private File composed;
	private Map<URI, Set<Trigger>> triggers;

	public ObjectRepository(RoleMapper mapper, LiteralManager literals,
			ClassLoader cl) {
		this.cl = cl;
		this.mapper = mapper;
		this.literals = literals;
	}

	public void setPackagePrefix(String prefix) {
		this.pkgPrefix = prefix;
	}

	public void setMemberPrefix(String prefix) {
		this.propertyPrefix = prefix;
	}

	/** graph -&gt; prefix -&gt; namespace */
	public void setSchemaNamespaces(Map<URI, Map<String, String>> map) {
		this.namespaces = map;
	}

	public void setSchema(Model schema) {
		this.schema = schema;
	}

	public void setBehaviourClassPath(URL[] cp) {
		this.cp = cp;
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

	public File getConceptJar() {
		return concepts;
	}

	public File getBehaviourJar() {
		return behaviours;
	}

	public ValueFactory getURIFactory() {
		return super.getValueFactory();
	}

	public ValueFactory getLiteralFactory() {
		return super.getValueFactory();
	}

	public void init(File dataDir) throws RepositoryException,
			ObjectStoreConfigException {
		assert dataDir != null;
		this.dataDir = dataDir;
		concepts = new File(dataDir, "concepts.jar");
		behaviours = new File(dataDir, "behaviours.jar");
		composed = new File(dataDir, "composed");
		if (schema != null && !schema.isEmpty()) {
			OWLCompiler compiler = new OWLCompiler(mapper, literals);
			compiler.setPackagePrefix(pkgPrefix);
			compiler.setMemberPrefix(propertyPrefix);
			compiler.setConceptJar(concepts);
			compiler.setBehaviourJar(behaviours);
			cl = compiler.compile(namespaces, schema, cl);
			schema = null;
			RoleClassLoader loader = new RoleClassLoader(mapper);
			loader.loadRoles(cl);
			literals.setClassLoader(cl);
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
		ClassFactory definer = createClassFactory(cl);
		pm = createPropertyMapper(definer);
		resolver = createClassResolver(definer, mapper, pm);
		Collection<Method> methods = mapper.getTriggerMethods();
		if (!methods.isEmpty()) {
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

	@Override
	public ObjectConnection getConnection() throws RepositoryException {
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
		return new TypeManager();
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
		ClassCompositor compositor = new ClassCompositor();
		compositor.setInterfaceBehaviourResolver(pmf);
		compositor.setAbstractBehaviourResolver(abc);
		compositor.setSparqlBehaviourFactory(sbf);
		resolver.setClassCompositor(compositor);
		resolver.setRoleMapper(mapper);
		pmf.setClassDefiner(definer);
		pmf.setPropertyMapper(pm);
		sbf.setClassDefiner(definer);
		sbf.setPropertyMapper(pm);
		abc.setClassDefiner(definer);
		compositor.setClassDefiner(definer);
		compositor.setBaseClassRoles(mapper.getConceptClasses());
		resolver.init();
		return resolver;
	}

	protected PropertyMapper createPropertyMapper(ClassLoader cl) {
		return new PropertyMapper(cl);
	}

	protected ClassFactory createClassFactory(ClassLoader cl) {
		return new ClassFactory(composed, cl);
	}

}
