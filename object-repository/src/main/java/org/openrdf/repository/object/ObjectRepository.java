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

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

import javax.persistence.EntityManagerFactory;
import javax.xml.namespace.QName;

import org.openrdf.elmo.ElmoEntityResolver;
import org.openrdf.elmo.LiteralManager;
import org.openrdf.elmo.RoleMapper;
import org.openrdf.elmo.dynacode.ClassFactory;
import org.openrdf.elmo.impl.AbstractBehaviourClassFactory;
import org.openrdf.elmo.impl.ElmoEntityCompositor;
import org.openrdf.elmo.impl.ElmoEntityResolverImpl;
import org.openrdf.elmo.impl.ElmoMapperClassFactory;
import org.openrdf.elmo.sesame.SesameEntitySupport;
import org.openrdf.elmo.sesame.SesameLiteralManager;
import org.openrdf.elmo.sesame.SesamePropertyFactory;
import org.openrdf.elmo.sesame.SesameResourceManager;
import org.openrdf.elmo.sesame.SesameRoleMapperFactory;
import org.openrdf.elmo.sesame.SesameTypeManager;
import org.openrdf.model.Literal;
import org.openrdf.model.LiteralFactory;
import org.openrdf.model.URI;
import org.openrdf.model.URIFactory;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.repository.Repository;
import org.openrdf.repository.contextaware.ContextAwareConnection;
import org.openrdf.repository.contextaware.ContextAwareRepository;
import org.openrdf.repository.object.config.ObjectConfig;
import org.openrdf.repository.object.exceptions.ElmoInitializationException;
import org.openrdf.store.StoreException;

/**
 * Creates SesameBeanManagers.
 * 
 * @author James Leigh
 * 
 */
public class ObjectRepository implements EntityManagerFactory {
	private static Map<ClassLoader, WeakReference<ClassFactory>> definers = new WeakHashMap<ClassLoader, WeakReference<ClassFactory>>();

	private ElmoEntityCompositor compositor;

	private ElmoEntityResolverImpl<URI> resolver;

	private ContextAwareRepository repository;

	private QName[] context;

	private LiteralManager<URI, Literal> literalManager;

	private boolean openned = true;

	private ElmoMapperClassFactory propertyMapper;

	private RoleMapper<URI> mapper;

	private AbstractBehaviourClassFactory abc;

	public ObjectRepository(ObjectConfig module, Repository repository) {
		ClassLoader cl = module.getClassLoader();
		if (repository instanceof ContextAwareRepository) {
			this.repository = (ContextAwareRepository) repository;
		} else {
			this.repository = new ContextAwareRepository(repository);
		}
		URIFactory uf = repository.getURIFactory();
		LiteralFactory lf = repository.getLiteralFactory();
		literalManager = new SesameLiteralManager(uf, lf);
		propertyMapper = new ElmoMapperClassFactory();
		propertyMapper.setElmoPropertyFactoryClass(SesamePropertyFactory.class);
		resolver = new ElmoEntityResolverImpl<URI>();
		compositor = new ElmoEntityCompositor();
		compositor.setInterfaceBehaviourResolver(propertyMapper);
		abc = new AbstractBehaviourClassFactory();
		compositor.setAbstractBehaviourResolver(abc);
		resolver.setElmoEntityCompositor(compositor);
		literalManager.setClassLoader(cl);
		SesameRoleMapperFactory factory = new SesameRoleMapperFactory(uf);
		factory.setClassLoader(cl);
		factory.setJarFileUrls(module.getJarFileUrls());
		mapper = factory.createRoleMapper();
		resolver.setRoleMapper(mapper);
		ClassFactory definer = getSharedDefiner(cl);
		propertyMapper.setClassDefiner(definer);
		abc.setClassDefiner(definer);
		compositor.setClassDefiner(definer);
		setElmoModule(module);
		compositor.setBaseClassRoles(mapper.getConceptClasses());
		compositor.setBlackListedBehaviours(mapper.getConceptOnlyClasses());
		mapper.addBehaviour(SesameEntitySupport.class, RDFS.RESOURCE
				.stringValue());
	}

	public ContextAwareRepository getRepository() {
		return repository;
	}

	public RoleMapper<URI> getRoleMapper() {
		return mapper;
	}

	public LiteralManager<URI, Literal> getLiteralManager() {
		return literalManager;
	}

	public ElmoEntityResolver<URI> getElmoEntityResolver() {
		return resolver;
	}

	public boolean isOpen() {
		return openned;
	}

	public void close() {
		openned = false;
	}

	public ObjectConnection createEntityManager() {
		return createElmoManager();
	}

	public ObjectConnection createEntityManager(Map properties) {
		return createElmoManager();
	}

	public ObjectConnection createElmoManager() {
		return createElmoManager(null);
	}

	public ObjectConnection createElmoManager(Locale locale) {
		if (!openned)
			throw new IllegalStateException("SesameManagerFactory is closed");
		SesameResourceManager rolesManager;
		ContextAwareConnection conn;
		try {
			conn = repository.getConnection();
			rolesManager = new SesameResourceManager();
			rolesManager.setConnection(conn);
			rolesManager.setSesameTypeRepository(new SesameTypeManager(conn));
			rolesManager.setRoleMapper(mapper);
			rolesManager.setElmoEntityResolver(resolver);
			if (context != null && context.length > 0) {
				URI[] resources = createURI(rolesManager, context);
				conn.setAddContexts(resources[0]);
				conn.setRemoveContexts(resources[0]);
				if (Arrays.asList(resources).contains(null)) {
					// if read from default context -> read from everything
					conn.setReadContexts();
				} else {
					conn.setReadContexts(resources);
				}
			}
		} catch (StoreException e) {
			throw new ElmoInitializationException(e);
		}
		ObjectConnection manager = new ObjectConnection();
		manager.setConnection(conn);
		manager.setLocale(locale);
		manager.setLiteralManager(literalManager);
		manager.setRoleMapper(mapper);
		manager.setResourceManager(rolesManager);
		return manager;
	}

	private void setElmoModule(ObjectConfig module) {
		for (ObjectConfig.Association e : module.getDatatypes()) {
			literalManager.recordType(e.getJavaClass(), e.getRdfType());
		}
		for (ObjectConfig.Association e : module.getConcepts()) {
			if (e.getRdfType() == null) {
				mapper.addConcept(e.getJavaClass());
			} else {
				mapper.addConcept(e.getJavaClass(), e.getRdfType());
			}
		}
		for (ObjectConfig.Association e : module.getBehaviours()) {
			if (e.getRdfType() == null) {
				mapper.addBehaviour(e.getJavaClass());
			} else {
				mapper.addBehaviour(e.getJavaClass(), e.getRdfType());
			}
		}
		for (ObjectConfig.Association e : module.getFactories()) {
			if (e.getRdfType() == null) {
				mapper.addFactory(e.getJavaClass());
			} else {
				mapper.addFactory(e.getJavaClass(), e.getRdfType());
			}
		}
		if (module.getGraph() != null) {
			context = new QName[1 + module.getIncludedGraphs().size()];
			context[0] = module.getGraph();
			Iterator<QName> iter = module.getIncludedGraphs().iterator();
			for (int i = 1; i < context.length; i++) {
				context[i] = iter.next();
			}
		}
	}

	private URI[] createURI(SesameResourceManager rolesManager,
			QName... context) {
		URI[] result = new URI[context.length];
		for (int i = 0; i < result.length; i++) {
			if (context[i] == null) {
				result[i] = null;
			} else {
				result[i] = (URI) rolesManager.createResource(context[i]);
			}
		}
		return result;
	}

	private ClassFactory getSharedDefiner(ClassLoader cl) {
		ClassFactory definer = null;
		synchronized (definers) {
			WeakReference<ClassFactory> ref = definers.get(cl);
			if (ref != null) {
				definer = ref.get();
			}
			if (definer == null) {
				definer = new ClassFactory(cl);
				definers.put(cl, new WeakReference<ClassFactory>(definer));
			}
		}
		return definer;
	}
}
