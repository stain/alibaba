package org.openrdf.repository.object.config;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

import org.openrdf.elmo.RoleMapper;
import org.openrdf.elmo.dynacode.ClassFactory;
import org.openrdf.elmo.impl.AbstractBehaviourClassFactory;
import org.openrdf.elmo.impl.ElmoEntityCompositor;
import org.openrdf.elmo.impl.ElmoEntityResolverImpl;
import org.openrdf.elmo.impl.ElmoMapperClassFactory;
import org.openrdf.elmo.sesame.SesameEntitySupport;
import org.openrdf.elmo.sesame.SesameLiteralManager;
import org.openrdf.elmo.sesame.SesamePropertyFactory;
import org.openrdf.elmo.sesame.SesameRoleMapperFactory;
import org.openrdf.model.LiteralFactory;
import org.openrdf.model.URI;
import org.openrdf.model.URIFactory;
import org.openrdf.model.impl.LiteralFactoryImpl;
import org.openrdf.model.impl.URIFactoryImpl;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.repository.Repository;
import org.openrdf.repository.config.RepositoryFactory;
import org.openrdf.repository.config.RepositoryImplConfig;
import org.openrdf.repository.contextaware.config.ContextAwareFactory;
import org.openrdf.repository.object.ObjectRepository;
import org.openrdf.store.StoreConfigException;

public class ObjectRepositoryFactory extends ContextAwareFactory {

	/**
	 * The type of repositories that are created by this factory.
	 * 
	 * @see RepositoryFactory#getRepositoryType()
	 */
	public static final String REPOSITORY_TYPE = "openrdf:ObjectRepository";

	private static Map<ClassLoader, WeakReference<ClassFactory>> definers = new WeakHashMap<ClassLoader, WeakReference<ClassFactory>>();

	@Override
	public String getRepositoryType() {
		return REPOSITORY_TYPE;
	}

	@Override
	public ObjectRepositoryConfig getConfig() {
		return new ObjectRepositoryConfig();
	}

	public ObjectRepository createRepository(ObjectRepositoryConfig config,
			Repository delegate) throws StoreConfigException {
		ObjectRepository repo = getRepository(config);
		repo.setDelegate(delegate);
		return repo;
	}

	public ObjectRepository createRepository(Repository delegate)
			throws StoreConfigException {
		ObjectRepository repo = getRepository(getConfig());
		repo.setDelegate(delegate);
		return repo;
	}

	@Override
	public ObjectRepository getRepository(RepositoryImplConfig configuration)
			throws StoreConfigException {
		if (configuration instanceof ObjectRepositoryConfig) {
			ObjectRepositoryConfig config = (ObjectRepositoryConfig) configuration;

			ObjectRepository repo = new ObjectRepository();

			repo.setIncludeInferred(config.isIncludeInferred());
			repo.setMaxQueryTime(config.getMaxQueryTime());
			repo.setQueryLanguage(config.getQueryLanguage());
			repo.setReadContexts(config.getReadContexts());
			repo.setAddContexts(config.getAddContexts());
			repo.setRemoveContexts(config.getRemoveContexts());
			repo.setArchiveContexts(config.getArchiveContexts());

			initialize(config, repo);

			return repo;
		}

		throw new StoreConfigException("Invalid configuration class: "
				+ configuration.getClass());
	}

	private void initialize(ObjectRepositoryConfig module,
			ObjectRepository repository) {
		ClassLoader cl = module.getClassLoader();
		URIFactory uf = new URIFactoryImpl();
		LiteralFactory lf = new LiteralFactoryImpl();
		SesameLiteralManager literalManager = new SesameLiteralManager(uf, lf);
		ElmoMapperClassFactory propertyMapper = new ElmoMapperClassFactory();
		propertyMapper.setElmoPropertyFactoryClass(SesamePropertyFactory.class);
		ElmoEntityResolverImpl resolver = new ElmoEntityResolverImpl<URI>();
		ElmoEntityCompositor compositor = new ElmoEntityCompositor();
		compositor.setInterfaceBehaviourResolver(propertyMapper);
		AbstractBehaviourClassFactory abc = new AbstractBehaviourClassFactory();
		compositor.setAbstractBehaviourResolver(abc);
		resolver.setElmoEntityCompositor(compositor);
		literalManager.setClassLoader(cl);
		SesameRoleMapperFactory factory = new SesameRoleMapperFactory(uf);
		factory.setClassLoader(cl);
		factory.setJarFileUrls(module.getJarFileUrls());
		RoleMapper<URI> mapper = factory.createRoleMapper();
		resolver.setRoleMapper(mapper);
		ClassFactory definer = getSharedDefiner(cl);
		propertyMapper.setClassDefiner(definer);
		abc.setClassDefiner(definer);
		compositor.setClassDefiner(definer);
		for (ObjectRepositoryConfig.Association e : module.getDatatypes()) {
			literalManager.recordType(e.getJavaClass(), e.getRdfType());
		}
		for (ObjectRepositoryConfig.Association e : module.getConcepts()) {
			if (e.getRdfType() == null) {
				mapper.addConcept(e.getJavaClass());
			} else {
				mapper.addConcept(e.getJavaClass(), e.getRdfType());
			}
		}
		for (ObjectRepositoryConfig.Association e : module.getBehaviours()) {
			if (e.getRdfType() == null) {
				mapper.addBehaviour(e.getJavaClass());
			} else {
				mapper.addBehaviour(e.getJavaClass(), e.getRdfType());
			}
		}
		for (ObjectRepositoryConfig.Association e : module.getFactories()) {
			if (e.getRdfType() == null) {
				mapper.addFactory(e.getJavaClass());
			} else {
				mapper.addFactory(e.getJavaClass(), e.getRdfType());
			}
		}
		compositor.setBaseClassRoles(mapper.getConceptClasses());
		compositor.setBlackListedBehaviours(mapper.getConceptOnlyClasses());
		mapper.addBehaviour(SesameEntitySupport.class, RDFS.RESOURCE
				.stringValue());
		repository.setLiteralManager(literalManager);
		repository.setElmoEntityResolver(resolver);
		repository.setRoleMapper(mapper);
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
