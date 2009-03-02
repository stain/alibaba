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

import info.aduna.io.file.FileUtil;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.openrdf.model.Model;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.contextaware.ContextAwareRepository;
import org.openrdf.repository.object.annotations.triggeredBy;
import org.openrdf.repository.object.codegen.CodeGenerator;
import org.openrdf.repository.object.composition.AbstractClassFactory;
import org.openrdf.repository.object.composition.ClassCompositor;
import org.openrdf.repository.object.composition.ClassFactory;
import org.openrdf.repository.object.composition.ClassResolver;
import org.openrdf.repository.object.composition.PropertyMapperFactory;
import org.openrdf.repository.object.composition.helpers.PropertySetFactory;
import org.openrdf.repository.object.exceptions.ObjectStoreConfigException;
import org.openrdf.repository.object.managers.LiteralManager;
import org.openrdf.repository.object.managers.PropertyMapper;
import org.openrdf.repository.object.managers.RoleMapper;
import org.openrdf.repository.object.managers.TypeManager;
import org.openrdf.repository.object.managers.helpers.RoleClassLoader;
import org.openrdf.repository.object.trigger.Trigger;
import org.openrdf.repository.object.trigger.TriggerConnection;
import org.openrdf.store.StoreException;

/**
 * @author James Leigh
 * 
 */
public class ObjectRepository extends ContextAwareRepository {
	private static final Set<URI> BUILD_IN = new HashSet(Arrays.asList(
			RDFS.RESOURCE, RDFS.CONTAINER, RDF.ALT, RDF.BAG, RDF.SEQ, RDF.LIST));

	private ClassLoader cl;
	private Model schema;
	private RoleMapper mapper;
	private LiteralManager literals;
	private String pkgPrefix = "";
	private String propertyPrefix;
	private PropertyMapper pm;
	private ClassResolver resolver;
	private File dataDir;
	private File concepts;
	private File behaviours;
	private File composed;
	private Map<URI, Set<Trigger>> triggers;

	public ObjectRepository(RoleMapper mapper, LiteralManager literals,
			ClassLoader cl) {
		this.cl = cl;
		this.mapper = mapper;
		this.literals = literals;
	}

	public void setPackagePrefix(String prefix) {
		if (prefix == null) {
			this.pkgPrefix = "";
		} else {
			this.pkgPrefix = prefix;
		}
	}

	public void setPropertyPrefix(String prefix) {
		this.propertyPrefix = prefix;
	}

	public void setSchema(Model schema) {
		this.schema = schema;
	}

	@Override
	public void setDataDir(File dataDir) {
		super.setDataDir(dataDir);
		setInternalDataDir(dataDir);
	}

	private void setInternalDataDir(File dataDir) {
		this.dataDir = dataDir;
		if (concepts == null) {
			concepts = new File(dataDir, "concepts.jar");
		}
		if (behaviours == null) {
			behaviours = new File(dataDir, "behaviours.jar");
		}
		if (composed == null) {
			composed = new File(dataDir, "composed");
		}
	}

	public void setConceptJar(File jar) {
		this.concepts = jar;
	}

	public void setBehaviourJar(File jar) {
		this.behaviours = jar;
	}

	public void setComposedDir(File dir) {
		this.composed = dir;
	}

	@Override
	public void initialize() throws StoreException {
		super.initialize();
		init();
	}

	void init() throws StoreException {
		if (getDataDir() == null) {
			try {
				setInternalDataDir(FileUtil.createTempDir(getClass()
						.getSimpleName()));
			} catch (IOException e) {
				throw new StoreException(e);
			}
		}
		if (schema != null && !schema.isEmpty()) {
			cl = compile(schema, cl);
			schema = null;
			RoleClassLoader loader = new RoleClassLoader();
			loader.setClassLoader(cl);
			loader.setRoleMapper(mapper);
			try {
				loader.loadRoles();
			} catch (ObjectStoreConfigException e) {
				// something wrong with the compiler
				throw new StoreException(e);
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
					set.add(new Trigger(method, pm));
				}
			}
		}
	}

	@Override
	public void shutDown() throws StoreException {
		super.shutDown();
		if (getDataDir() == null) {
			try {
				FileUtil.deleteDir(dataDir);
			} catch (IOException e) {
				throw new StoreException(e);
			}
		}
	}

	@Override
	public ObjectConnection getConnection() throws StoreException {
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
		con.setQueryResultLimit(getQueryResultLimit());
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
		ClassResolver resolver = new ClassResolver();
		ClassCompositor compositor = new ClassCompositor();
		compositor.setInterfaceBehaviourResolver(pmf);
		AbstractClassFactory abc = new AbstractClassFactory();
		compositor.setAbstractBehaviourResolver(abc);
		resolver.setClassCompositor(compositor);
		resolver.setRoleMapper(mapper);
		pmf.setClassDefiner(definer);
		pmf.setPropertyMapper(pm);
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

	private ClassLoader compile(Model model, ClassLoader cl)
			throws StoreException {
		Set<String> unknown = findUndefinedNamespaces(model);
		if (unknown.isEmpty())
			return cl;
		CodeGenerator compiler = new CodeGenerator(model, cl, mapper, literals);
		for (String ns : unknown) {
			String prefix = findPrefix(ns, model);
			String pkgName = pkgPrefix + prefix;
			if (!Character.isLetter(pkgName.charAt(0))) {
				pkgName = "_" + pkgName;
			}
			compiler.bindPackageToNamespace(pkgName, ns);
		}
		if (propertyPrefix != null) {
			compiler.setMemberPrefix(propertyPrefix);
		}
		try {
			cl = compiler.compileConcepts(concepts, cl);
			return compiler.compileBehaviours(behaviours, cl);
		} catch (Exception e) {
			throw new StoreException(e);
		}
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

	private String findPrefix(String ns, Model model) {
		for (Map.Entry<String, String> e : model.getNamespaces().entrySet()) {
			if (ns.equals(e.getValue()) && e.getKey().length() > 0) {
				return e.getKey();
			}
		}
		return "ns" + Integer.toHexString(ns.hashCode());
	}

}
