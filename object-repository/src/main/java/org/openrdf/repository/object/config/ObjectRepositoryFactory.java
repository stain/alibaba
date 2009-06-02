package org.openrdf.repository.object.config;

import info.aduna.io.FileUtil;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.openrdf.model.Model;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.config.RepositoryConfigException;
import org.openrdf.repository.config.RepositoryFactory;
import org.openrdf.repository.config.RepositoryImplConfig;
import org.openrdf.repository.contextaware.config.ContextAwareFactory;
import org.openrdf.repository.object.ObjectRepository;
import org.openrdf.repository.object.behaviours.RDFObjectImpl;
import org.openrdf.repository.object.compiler.OntologyLoader;
import org.openrdf.repository.object.exceptions.ObjectStoreConfigException;
import org.openrdf.repository.object.managers.LiteralManager;
import org.openrdf.repository.object.managers.RoleMapper;
import org.openrdf.repository.object.managers.helpers.RoleClassLoader;
import org.openrdf.rio.RDFParseException;

public class ObjectRepositoryFactory extends ContextAwareFactory {

	/**
	 * The type of repositories that are created by this factory.
	 * 
	 * @see RepositoryFactory#getRepositoryType()
	 */
	public static final String REPOSITORY_TYPE = "openrdf:ObjectRepository";

	@Override
	public String getRepositoryType() {
		return REPOSITORY_TYPE;
	}

	@Override
	public ObjectRepositoryConfig getConfig() {
		return new ObjectRepositoryConfig();
	}

	/**
	 * Create an ObjectRepository from a previously initialised delegate.
	 */
	public ObjectRepository createRepository(ObjectRepositoryConfig config,
			Repository delegate) throws RepositoryConfigException,
			RepositoryException {
		ObjectRepository repo = getRepository(config);
		repo.setDelegate(delegate);
		try {
			File dataDir = repo.getDataDir();
			if (dataDir == null) {
				dataDir = FileUtil.createTempDir(getClass().getSimpleName());
			}
			repo.init(dataDir);
		} catch (IOException e) {
			throw new RepositoryException(e);
		}
		return repo;
	}

	/**
	 * Create an ObjectRepository from a previously initialised delegate.
	 */
	public ObjectRepository createRepository(Repository delegate)
			throws RepositoryConfigException, RepositoryException {
		return createRepository(getConfig(), delegate);
	}

	/**
	 * Create an uninitialised ObjectRepository without a delegate.
	 */
	@Override
	public ObjectRepository getRepository(RepositoryImplConfig configuration)
			throws RepositoryConfigException {
		if (configuration instanceof ObjectRepositoryConfig) {
			ObjectRepositoryConfig config = (ObjectRepositoryConfig) configuration;

			ObjectRepository repo = getObjectRepository(config);

			repo.setIncludeInferred(config.isIncludeInferred());
			repo.setMaxQueryTime(config.getMaxQueryTime());
			repo.setQueryLanguage(config.getQueryLanguage());
			repo.setReadContexts(config.getReadContexts());
			repo.setAddContexts(config.getAddContexts());
			repo.setRemoveContexts(config.getRemoveContexts());
			repo.setArchiveContexts(config.getArchiveContexts());
			// repo.setQueryResultLimit(config.getQueryResultLimit());

			return repo;
		}

		throw new RepositoryConfigException("Invalid configuration class: "
				+ configuration.getClass());
	}

	protected LiteralManager createLiteralManager(ValueFactory uf,
			ValueFactory lf) {
		return new LiteralManager(uf, lf);
	}

	protected RoleMapper createRoleMapper(ValueFactory vf)
			throws ObjectStoreConfigException {
		return new RoleMapper(vf);
	}

	protected ObjectRepository createObjectRepository(RoleMapper mapper,
			LiteralManager literals, ClassLoader cl) {
		return new ObjectRepository(mapper, literals, cl);
	}

	private ObjectRepository getObjectRepository(ObjectRepositoryConfig module)
			throws ObjectStoreConfigException {
		try {
			ClassLoader cl = getClassLoader(module);
			ValueFactory uf = new ValueFactoryImpl();
			RoleMapper mapper = getRoleMapper(cl, uf, module);
			LiteralManager literals = getLiteralManager(cl, uf, module);
			ObjectRepository repo = createObjectRepository(mapper, literals, cl);
			List<URL> list = new ArrayList<URL>(module.getImports());
			if (!list.isEmpty()) {
				OntologyLoader loader = new OntologyLoader();
				loader.loadOntologies(list);
				if (module.isFollowImports()) {
					loader.followImports();
				}
				Model model = loader.getModel();
				repo.setSchema(model);
				repo.setSchemaNamespaces(loader.getNamespaces());
				repo.setPackagePrefix(module.getPackagePrefix());
				repo.setMemberPrefix(module.getMemberPrefix());
			}
			List<URL> jars = module.getBehaviourJars();
			repo.setBehaviourClassPath(jars.toArray(new URL[jars.size()]));
			return repo;
		} catch (IOException e) {
			throw new ObjectStoreConfigException(e);
		} catch (RDFParseException e) {
			throw new ObjectStoreConfigException(e);
		}
	}

	private ClassLoader getClassLoader(ObjectRepositoryConfig module) {
		ClassLoader cl = module.getClassLoader();
		List<URL> jars = module.getConceptJars();
		if (jars.isEmpty())
			return cl;
		URL[] array = jars.toArray(new URL[jars.size()]);
		return new URLClassLoader(array, cl);
	}

	private RoleMapper getRoleMapper(ClassLoader cl, ValueFactory uf,
			ObjectRepositoryConfig module) throws ObjectStoreConfigException {
		RoleMapper mapper = createRoleMapper(uf);
		mapper.addBehaviour(RDFObjectImpl.class, RDFS.RESOURCE);
		RoleClassLoader loader = new RoleClassLoader(mapper);
		loader.loadRoles(cl);
		if (module.getConceptJars() != null) {
			for (URL url : module.getConceptJars()) {
				loader.scan(url, cl);
			}
		}
		for (Map.Entry<Class<?>, URI> e : module.getAnnotations().entrySet()) {
			if (e.getValue() == null) {
				mapper.addAnnotation(e.getKey());
			} else {
				mapper.addAnnotation(e.getKey(), e.getValue());
			}
		}
		for (Map.Entry<Class<?>, URI> e : module.getConcepts().entrySet()) {
			if (e.getValue() == null) {
				mapper.addConcept(e.getKey());
			} else {
				mapper.addConcept(e.getKey(), e.getValue());
			}
		}
		for (Map.Entry<Class<?>, URI> e : module.getBehaviours().entrySet()) {
			if (e.getValue() == null) {
				mapper.addBehaviour(e.getKey());
			} else {
				mapper.addBehaviour(e.getKey(), e.getValue());
			}
		}
		return mapper;
	}

	private LiteralManager getLiteralManager(ClassLoader cl, ValueFactory uf,
			ObjectRepositoryConfig module) {
		LiteralManager literalManager = createLiteralManager(uf,
				new ValueFactoryImpl());
		literalManager.setClassLoader(cl);
		for (Map.Entry<Class<?>, URI> e : module.getDatatypes().entrySet()) {
			literalManager.addDatatype(e.getKey(), e.getValue());
		}
		return literalManager;
	}

}
