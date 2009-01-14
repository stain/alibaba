/*
 * Copyright (c) 2007, James Leigh All rights reserved.
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
package org.openrdf.elmo.sesame;

import info.aduna.platform.Platform;
import info.aduna.platform.PlatformFactory;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

import javax.persistence.EntityManagerFactory;
import javax.xml.namespace.QName;

import org.openrdf.elmo.ElmoEntityResolver;
import org.openrdf.elmo.ElmoManagerFactory;
import org.openrdf.elmo.ElmoModule;
import org.openrdf.elmo.LiteralManager;
import org.openrdf.elmo.RoleMapper;
import org.openrdf.elmo.dynacode.ClassFactory;
import org.openrdf.elmo.exceptions.ElmoIOException;
import org.openrdf.elmo.exceptions.ElmoInitializationException;
import org.openrdf.elmo.impl.AbstractBehaviourClassFactory;
import org.openrdf.elmo.impl.ElmoEntityCompositor;
import org.openrdf.elmo.impl.ElmoEntityResolverImpl;
import org.openrdf.elmo.impl.ElmoMapperClassFactory;
import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.query.QueryLanguage;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.config.RepositoryConfigException;
import org.openrdf.repository.contextaware.ContextAwareConnection;
import org.openrdf.repository.flushable.FlushableRepository;
import org.openrdf.repository.loader.LoaderRepository;
import org.openrdf.repository.loader.LoaderRepositoryFactory;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFParseException;
import org.openrdf.sail.memory.MemoryStore;

/**
 * Creates SesameBeanManagers.
 * 
 * @author James Leigh
 * 
 */
public class SesameManagerFactory implements ElmoManagerFactory, EntityManagerFactory {
	private static Map<ClassLoader, WeakReference<ClassFactory>> definers = new WeakHashMap<ClassLoader, WeakReference<ClassFactory>>();

	private static Map<ClassLoader, WeakReference<ClassFactory>> inferencingDefiners = new WeakHashMap<ClassLoader, WeakReference<ClassFactory>>();

	private ClassFactory definer;

	private ElmoEntityCompositor compositor;

	private ElmoEntityResolverImpl<URI> resolver;

	private LoaderRepository repository;

	private QName[] context;

	private LiteralManager<URI, Literal> literalManager;

	private QueryLanguage ql = QueryLanguage.SPARQL;

	private boolean openned = true;

	private boolean inferencing = false;

	private ElmoMapperClassFactory propertyMapper;

	private RoleMapper<URI> mapper;

	private boolean shutDownRepositoryOnClose = false;

	private AbstractBehaviourClassFactory abc;

	public SesameManagerFactory(ElmoModule module) {
		try {
			Repository repository = new SailRepository(new MemoryStore());
			repository.initialize();
			shutDownRepositoryOnClose = true;
			init(module, new LoaderRepository(repository));
		} catch (RepositoryException e) {
			throw new ElmoInitializationException(e);
		}
	}

	public SesameManagerFactory(ElmoModule module, Repository repository) {
		init(module, new LoaderRepository(repository));
	}

	public SesameManagerFactory(ElmoModule module, URL server, String repositoryId) {
		try {
			LoaderRepositoryFactory loader = new LoaderRepositoryFactory(server);
			LoaderRepository repository = loader.createRepository(repositoryId);
			shutDownRepositoryOnClose = true;
			init(module, repository);
		} catch (RepositoryConfigException e) {
			throw new ElmoInitializationException(e);
		} catch (RepositoryException e) {
			throw new ElmoInitializationException(e);
		}
	}

	public SesameManagerFactory(ElmoModule module, File dataDir, String repositoryId) {
		try {
			LoaderRepositoryFactory loader = new LoaderRepositoryFactory(dataDir);
			LoaderRepository repository = loader.createRepository(repositoryId);
			shutDownRepositoryOnClose = true;
			init(module, repository);
		} catch (RepositoryConfigException e) {
			throw new ElmoInitializationException(e);
		} catch (RepositoryException e) {
			throw new ElmoInitializationException(e);
		}
	}

	public SesameManagerFactory(ElmoModule module, String appId,
			String repositoryId) {
		this(module, getDataDir(appId), repositoryId);
	}

	public Repository getRepository() {
		return repository;
	}

	public QueryLanguage getQueryLanguage() {
		return ql;
	}

	public void setQueryLanguage(QueryLanguage ql) {
		this.ql = ql;
	}

	public void setInferencingEnabled(boolean enabled) {
		inferencing = enabled;
		if (enabled) {
			propertyMapper
					.setElmoPropertyFactoryClass(InferencingPropertyFactory.class);
		} else {
			propertyMapper
					.setElmoPropertyFactoryClass(SesamePropertyFactory.class);
		}
		setClassDefiner(getSharedDefiner(definer.getParent()));
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
		try {
			if (shutDownRepositoryOnClose) {
				repository.shutDown();
			} else {
				repository.clearLoadedContexts();
			}
		} catch (RepositoryException e) {
			throw new ElmoIOException(e);
		}
		openned = false;
	}

	public SesameManager createEntityManager() {
		return createElmoManager();
	}

	public SesameManager createEntityManager(Map properties) {
		return createElmoManager();
	}

	public SesameManager createElmoManager() {
		return createElmoManager(null);
	}

	public SesameManager createElmoManager(Locale locale) {
		if (!openned)
			throw new IllegalStateException("SesameManagerFactory is closed");
		SesameResourceManager rolesManager;
		ContextAwareConnection conn;
		try {
			// FIXME HttpRepo does not support reading its own uncommitted transactions
			String simpleName = repository.getDelegate().getClass().getSimpleName();
			boolean autoFlush = !simpleName.equals("HTTPRepository") && !simpleName.equals("SPARQLRepository");
			FlushableRepository repo = new FlushableRepository(repository, autoFlush);
			conn = new ContextAwareConnection(new FlushableRepository(repo));
			conn.setQueryLanguage(ql);
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
		} catch (RepositoryException e) {
			throw new ElmoInitializationException(e);
		}
		SesameManager manager = new SesameManager();
		manager.setConnection(conn);
		manager.setLocale(locale);
		manager.setLiteralManager(literalManager);
		manager.setRoleMapper(mapper);
		manager.setResourceManager(rolesManager);
		return manager;
	}

	private static File getDataDir(String appId) {
		Platform platform = PlatformFactory.getPlatform();
		return platform.getApplicationDataDir(appId);
	}

	private void init(ElmoModule module, LoaderRepository repository) {
		ClassLoader cl = module.getClassLoader();
		this.repository = repository;
		ValueFactory vf = repository.getValueFactory();
		literalManager = new SesameLiteralManager(vf);
		propertyMapper = new ElmoMapperClassFactory();
		propertyMapper.setElmoPropertyFactoryClass(SesamePropertyFactory.class);
		resolver = new ElmoEntityResolverImpl<URI>();
		compositor = new ElmoEntityCompositor();
		compositor.setInterfaceBehaviourResolver(propertyMapper);
		abc = new AbstractBehaviourClassFactory();
		compositor.setAbstractBehaviourResolver(abc);
		resolver.setElmoEntityCompositor(compositor);
		repository.setClassLoader(cl);
		literalManager.setClassLoader(cl);
		SesameRoleMapperFactory factory = new SesameRoleMapperFactory(vf);
		factory.setClassLoader(cl);
		factory.setJarFileUrls(module.getJarFileUrls());
		mapper = factory.createRoleMapper();
		resolver.setRoleMapper(mapper);
		setClassDefiner(getSharedDefiner(cl));
		setElmoModule(module);
		compositor.setBaseClassRoles(mapper.getConceptClasses());
		compositor.setBlackListedBehaviours(mapper.getConceptOnlyClasses());
		mapper.addBehaviour(SesameEntitySupport.class, RDFS.RESOURCE.stringValue());
	}

	private void setElmoModule(ElmoModule module) {
		for (ElmoModule.Association e : module.getDatatypes()) {
			literalManager.recordType(e.getJavaClass(), e.getRdfType());
		}
		for (ElmoModule.Association e : module.getConcepts()) {
			if (e.getRdfType() == null) {
				mapper.addConcept(e.getJavaClass());
			} else {
				mapper.addConcept(e.getJavaClass(), e.getRdfType());
			}
		}
		for (ElmoModule.Association e : module.getBehaviours()) {
			if (e.getRdfType() == null) {
				mapper.addBehaviour(e.getJavaClass());
			} else {
				mapper.addBehaviour(e.getJavaClass(), e.getRdfType());
			}
		}
		for (ElmoModule.Association e : module.getFactories()) {
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
			for (int i=1; i<context.length; i++) {
				context[i] = iter.next();
			}
		}
		for (Map.Entry<URL, String> e : module.getDatasets().entrySet()) {
			loadContext(e.getKey(), e.getValue());
		}
		for (String path : module.getResources()) {
			loadResources(path);
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

	private void loadResources(String path) throws ElmoInitializationException {
		try {
			repository.loadResources(path);
		} catch (RepositoryException e) {
			throw new ElmoInitializationException(e);
		} catch (RDFParseException e) {
			throw new ElmoInitializationException(e);
		} catch (IOException e) {
			throw new ElmoInitializationException(e);
		}
	}

	private void loadContext(URL dataset, String context)
			throws ElmoInitializationException {
		try {
			ValueFactory vf = repository.getValueFactory();
			repository.loadContext(dataset, vf.createURI(context));
		} catch (RepositoryException e) {
			throw new ElmoInitializationException(e);
		} catch (RDFParseException e) {
			throw new ElmoInitializationException(e);
		} catch (IOException e) {
			throw new ElmoInitializationException(e);
		}
	}

	private ClassFactory getSharedDefiner(ClassLoader cl) {
		ClassFactory definer = null;
		if (inferencing) {
			synchronized (inferencingDefiners) {
				WeakReference<ClassFactory> ref = inferencingDefiners.get(cl);
				if (ref != null) {
					definer = ref.get();
				}
				if (definer == null) {
					definer = new ClassFactory(cl);
					inferencingDefiners.put(cl,
							new WeakReference<ClassFactory>(definer));
				}
			}
		} else {
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
		}
		return definer;
	}

	private void setClassDefiner(ClassFactory definer) {
		this.definer = definer;
		propertyMapper.setClassDefiner(definer);
		abc.setClassDefiner(definer);
		compositor.setClassDefiner(definer);
	}
}
