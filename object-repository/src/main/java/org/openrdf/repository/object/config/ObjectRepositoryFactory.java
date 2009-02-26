package org.openrdf.repository.object.config;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.openrdf.model.LiteralFactory;
import org.openrdf.model.Model;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.URIFactory;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.model.impl.LiteralFactoryImpl;
import org.openrdf.model.impl.StatementImpl;
import org.openrdf.model.impl.URIFactoryImpl;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.repository.Repository;
import org.openrdf.repository.config.RepositoryFactory;
import org.openrdf.repository.config.RepositoryImplConfig;
import org.openrdf.repository.contextaware.config.ContextAwareFactory;
import org.openrdf.repository.object.ObjectRepository;
import org.openrdf.repository.object.behaviours.RDFObjectImpl;
import org.openrdf.repository.object.exceptions.ObjectStoreConfigException;
import org.openrdf.repository.object.managers.LiteralManager;
import org.openrdf.repository.object.managers.RoleMapper;
import org.openrdf.repository.object.managers.helpers.ComplexMapper;
import org.openrdf.repository.object.managers.helpers.DirectMapper;
import org.openrdf.repository.object.managers.helpers.HierarchicalRoleMapper;
import org.openrdf.repository.object.managers.helpers.RoleClassLoader;
import org.openrdf.repository.object.managers.helpers.SimpleRoleMapper;
import org.openrdf.repository.object.managers.helpers.TypeMapper;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.RDFParserRegistry;
import org.openrdf.rio.helpers.StatementCollector;
import org.openrdf.store.StoreConfigException;

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

	public ObjectRepository createRepository(ObjectRepositoryConfig config,
			Repository delegate) throws StoreConfigException {
		ObjectRepository repo = getRepository(config);
		repo.setDelegate(delegate);
		return repo;
	}

	public ObjectRepository createRepository(Repository delegate)
			throws StoreConfigException {
		return createRepository(getConfig(), delegate);
	}

	@Override
	public ObjectRepository getRepository(RepositoryImplConfig configuration)
			throws StoreConfigException {
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
			repo.setQueryResultLimit(config.getQueryResultLimit());

			return repo;
		}

		throw new StoreConfigException("Invalid configuration class: "
				+ configuration.getClass());
	}

	protected LiteralManager createLiteralManager(URIFactory uf,
			LiteralFactory lf) {
		return new LiteralManager(uf, lf);
	}

	protected RoleMapper createRoleMapper(ClassLoader cl, URIFactory vf,
			List<URL> jarFileUrls) throws ObjectStoreConfigException {
		DirectMapper d = new DirectMapper();
		TypeMapper t = new TypeMapper();
		SimpleRoleMapper r = new SimpleRoleMapper();
		RoleMapper mapper = new RoleMapper();
		mapper.setComplexMapper(new ComplexMapper());
		mapper.setHierarchicalRoleMapper(new HierarchicalRoleMapper(d, t, r));
		mapper.setURIFactory(vf);
		RoleClassLoader loader = new RoleClassLoader();
		loader.setClassLoader(cl);
		loader.setRoleMapper(mapper);
		loader.loadRoles();
		if (jarFileUrls != null) {
			for (URL url : jarFileUrls) {
				loader.scan(url);
			}
		}
		return mapper;
	}

	protected ObjectRepository createObjectRepository(RoleMapper mapper,
			LiteralManager literals, ClassLoader cl) {
		return new ObjectRepository(mapper, literals, cl);
	}

	private ObjectRepository getObjectRepository(ObjectRepositoryConfig module)
			throws ObjectStoreConfigException {
		ClassLoader cl = getClassLoader(module);
		URIFactory uf = new URIFactoryImpl();
		RoleMapper mapper = getRoleMapper(cl, uf, module);
		LiteralManager literals = getLiteralManager(cl, uf, module);
		ObjectRepository repo = createObjectRepository(mapper, literals, cl);
		List<URL> list = new ArrayList<URL>(module.getImports());
		if (!list.isEmpty()) {
			if (module.isImportJarOntologies()) {
				try {
					list.addAll(loadOntologyList(cl));
				} catch (IOException e) {
					throw new ObjectStoreConfigException(e);
				}
			}
			repo.setPackagePrefix(module.getPackagePrefix());
			repo.setPropertyPrefix(module.getMemberPrefix());
			repo.setSchema(read(list, module.isFollowImports()));
		}
		return repo;
	}

	private ClassLoader getClassLoader(ObjectRepositoryConfig module) {
		ClassLoader cl = module.getClassLoader();
		List<URL> jars = module.getJars();
		if (jars.isEmpty())
			return cl;
		URL[] array = jars.toArray(new URL[jars.size()]);
		return new URLClassLoader(array, cl);
	}

	private RoleMapper getRoleMapper(ClassLoader cl, URIFactory uf,
			ObjectRepositoryConfig module) throws ObjectStoreConfigException {
		RoleMapper mapper = createRoleMapper(cl, uf, module.getJars());
		mapper.addBehaviour(RDFObjectImpl.class, RDFS.RESOURCE);
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

	private LiteralManager getLiteralManager(ClassLoader cl, URIFactory uf,
			ObjectRepositoryConfig module) {
		LiteralManager literalManager = createLiteralManager(uf,
				new LiteralFactoryImpl());
		literalManager.setClassLoader(cl);
		for (Map.Entry<Class<?>, URI> e : module.getDatatypes().entrySet()) {
			literalManager.addDatatype(e.getKey(), e.getValue());
		}
		return literalManager;
	}

	private Model read(List<URL> ontologyUrls, boolean followImports)
			throws ObjectStoreConfigException {
		try {
			Model model = new LinkedHashModel();
			loadOntologyList(ontologyUrls, model, followImports);
			return model;
		} catch (IOException e) {
			throw new ObjectStoreConfigException(e);
		} catch (RDFParseException e) {
			throw new ObjectStoreConfigException(e);
		}
	}

	private void loadOntologyList(List<URL> ontologyUrls, Model model, boolean followImports)
			throws IOException, RDFParseException, ObjectStoreConfigException {
		for (URL url : ontologyUrls) {
			loadOntology(model, url, null);
		}
		if (followImports) {
			List<URL> urls = new ArrayList<URL>();
			for (Value obj : model.filter(null, OWL.IMPORTS, null).objects()) {
				if (obj instanceof URI) {
					URI uri = (URI) obj;
					if (!model.contains(null, null, null, uri)) {
						urls.add(new URL(uri.stringValue()));
					}
				}
			}
			if (!urls.isEmpty()) {
				loadOntologyList(urls, model, followImports);
			}
		}
	}

	private List<URL> loadOntologyList(ClassLoader cl) throws IOException {
		Properties ontologies = new Properties();
		String name = "META-INF/org.openrdf.ontologies";
		Enumeration<URL> resources = cl.getResources(name);
		while (resources.hasMoreElements()) {
			URL url = resources.nextElement();
			InputStream in = url.openStream();
			try {
				ontologies.load(in);
			} finally {
				in.close();
			}
		}
		Collection<?> list = ontologies.keySet();
		List<URL> urls = new ArrayList<URL>();
		for (Object key : list) {
			urls.add(cl.getResource((String) key));
		}
		return urls;
	}

	private void loadOntology(Model model, URL url, RDFFormat override)
			throws IOException, RDFParseException, ObjectStoreConfigException {
		URLConnection conn = url.openConnection();
		if (override == null) {
			conn.setRequestProperty("Accept", getAcceptHeader());
		} else {
			conn.setRequestProperty("Accept", override.getDefaultMIMEType());
		}
		ValueFactory vf = ValueFactoryImpl.getInstance();
		RDFFormat format = override;
		if (override == null) {
			format = RDFFormat.RDFXML;
			format = RDFFormat.forFileName(url.toString(), format);
			format = RDFFormat.forMIMEType(conn.getContentType(), format);
		}
		RDFParserRegistry registry = RDFParserRegistry.getInstance();
		RDFParser parser = registry.get(format).getParser();
		final URI uri = vf.createURI(url.toExternalForm());
		parser.setRDFHandler(new StatementCollector(model) {
			@Override
			public void handleStatement(Statement st) {
				Resource s = st.getSubject();
				URI p = st.getPredicate();
				Value o = st.getObject();
				super.handleStatement(new StatementImpl(s, p, o, uri));
			}
		});
		InputStream in = conn.getInputStream();
		try {
			parser.parse(in, url.toExternalForm());
		} catch (RDFHandlerException e) {
			throw new ObjectStoreConfigException(e);
		} catch (RDFParseException e) {
			if (override == null && format.equals(RDFFormat.NTRIPLES)) {
				// sometimes text/plain is used for rdf+xml
				loadOntology(model, url, RDFFormat.RDFXML);
			} else {
				throw e;
			}
		} finally {
			in.close();
		}
	}

	private String getAcceptHeader() {
		StringBuilder sb = new StringBuilder();
		String preferred = RDFFormat.RDFXML.getDefaultMIMEType();
		sb.append(preferred).append(";q=0.2");
		Set<RDFFormat> rdfFormats = RDFParserRegistry.getInstance().getKeys();
		for (RDFFormat format : rdfFormats) {
			for (String type : format.getMIMETypes()) {
				if (!preferred.equals(type)) {
					sb.append(", ").append(type);
				}
			}
		}
		return sb.toString();
	}

}
