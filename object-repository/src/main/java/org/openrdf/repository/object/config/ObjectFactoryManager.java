package org.openrdf.repository.object.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.WeakHashMap;

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
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.repository.object.ObjectFactory;
import org.openrdf.repository.object.behaviours.RDFObjectImpl;
import org.openrdf.repository.object.codegen.OntologyConverter;
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

public class ObjectFactoryManager {

	private static Map<ClassLoader, WeakReference<ClassFactory>> definers = new WeakHashMap<ClassLoader, WeakReference<ClassFactory>>();

	private static final String CONCEPTS = "META-INF/org.openrdf.concepts";

	private static final Set<URI> BUILD_IN = new HashSet(Arrays.asList(
			RDFS.RESOURCE, RDFS.CONTAINER, RDF.ALT, RDF.BAG, RDF.SEQ, RDF.LIST));

	private static String[] ROLES = { CONCEPTS,
			"META-INF/org.openrdf.behaviours" };

	private ObjectRepositoryConfig module;

	private File jar;

	private URIFactory uf = new URIFactoryImpl();

	private LiteralManager baseLiterals;

	private RoleMapper baseMapper;

	private ClassLoader cl;

	private ClassResolver resolver;

	private LiteralManager literals;

	private RoleMapper mapper;

	private PropertyMapper pm;

	public ObjectFactoryManager(ObjectRepositoryConfig module) {
		this.module = module;
	}

	public void setJarFile(File jar) {
		this.jar = jar;
	}

	public void init() throws ObjectStoreConfigException {
		cl = getClassLoader(module);
		baseMapper = getRoleMapper(cl, uf);
		baseLiterals = getLiteralManager(cl, uf);
		List<URL> list = module.getOntologyUrls();
		if (list.isEmpty()) {
			ClassFactory definer = createClassFactory(cl);
			pm = createPropertyMapper(definer);
			resolver = getClassResolver(baseMapper, definer, pm);
		} else {
			list = new ArrayList<URL>(list);
			if (module.isImportJarOntologies()) {
				try {
					list.addAll(loadOntologyList(cl));
				} catch (IOException e) {
					throw new ObjectStoreConfigException(e);
				}
			}
			cl = compile(read(list), jar, cl);
			baseMapper = getRoleMapper(cl, uf);
			baseLiterals = getLiteralManager(cl, uf);
			ClassFactory definer = createClassFactory(cl);
			pm = createPropertyMapper(definer);
			resolver = getClassResolver(baseMapper, definer, pm);
		}
		mapper = baseMapper;
		literals = baseLiterals;
	}

	public synchronized void setOntologyModel(Model model)
			throws ObjectStoreConfigException {
		ClassLoader cl = compile(model, jar, this.cl);
		mapper = getRoleMapper(cl, uf);
		literals = getLiteralManager(cl, uf);
		ClassFactory definer = createClassFactory(cl);
		pm = createPropertyMapper(definer);
		resolver = getClassResolver(mapper, definer, pm);
	}

	public synchronized ObjectFactory createObjectFactory() {
		return createObjectFactory(mapper, pm, literals, resolver, cl);
	}

	protected ObjectFactory createObjectFactory(RoleMapper mapper,
			PropertyMapper pm, LiteralManager literalManager,
			ClassResolver resolver, ClassLoader cl) {
		return new ObjectFactory(mapper, pm, literalManager, resolver, cl);
	}

	protected ClassResolver createClassResolver(ClassFactory definer,
			RoleMapper mapper, PropertyMapper pm)
			throws ObjectStoreConfigException {
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
		return resolver;
	}

	protected PropertyMapper createPropertyMapper(ClassLoader cl) {
		return new PropertyMapper(cl);
	}

	protected LiteralManager createLiteralManager(ClassLoader cl,
			URIFactory uf, LiteralFactory lf) {
		return new LiteralManager(cl, uf, lf);
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
		for (String roles : ROLES) {
			loader.loadClasses(roles, roles == CONCEPTS);
		}
		if (jarFileUrls != null) {
			for (URL url : jarFileUrls) {
				loader.scan(url, ROLES);
			}
		}
		return mapper;
	}

	protected ClassFactory createClassFactory(ClassLoader cl) {
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

	private ClassResolver getClassResolver(RoleMapper mapper,
			ClassFactory definer, PropertyMapper pm)
			throws ObjectStoreConfigException {
		ClassResolver resolver = createClassResolver(definer, mapper, pm);
		resolver.init();
		return resolver;
	}

	private RoleMapper getRoleMapper(ClassLoader cl, URIFactory uf)
			throws ObjectStoreConfigException {
		RoleMapper mapper = createRoleMapper(cl, uf, module.getJarFileUrls());
		mapper.addBehaviour(RDFObjectImpl.class, RDFS.RESOURCE.stringValue());
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
		return mapper;
	}

	private LiteralManager getLiteralManager(ClassLoader cl, URIFactory uf) {
		LiteralManager literalManager = createLiteralManager(cl, uf,
				new LiteralFactoryImpl());
		for (ObjectRepositoryConfig.Association e : module.getDatatypes()) {
			literalManager.addDatatype(e.getJavaClass(), e.getRdfType());
		}
		return literalManager;
	}

	private ClassLoader getClassLoader(ObjectRepositoryConfig module) {
		ClassLoader cl = module.getClassLoader();
		List<URL> jars = module.getJarFileUrls();
		if (jars.isEmpty())
			return cl;
		URL[] array = jars.toArray(new URL[jars.size()]);
		return new URLClassLoader(array, cl);
	}

	private Model read(List<URL> ontologyUrls)
			throws ObjectStoreConfigException {
		try {
			Model model = new LinkedHashModel();
			loadOntologyList(ontologyUrls, model);
			return model;
		} catch (IOException e) {
			throw new ObjectStoreConfigException(e);
		} catch (RDFParseException e) {
			throw new ObjectStoreConfigException(e);
		}
	}

	private void loadOntologyList(List<URL> ontologyUrls, Model model)
			throws IOException, RDFParseException, ObjectStoreConfigException {
		for (URL url : ontologyUrls) {
			loadOntology(model, url, null);
		}
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
			loadOntologyList(urls, model);
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

	private ClassLoader compile(Model model, File jar, ClassLoader cl)
			throws ObjectStoreConfigException {
		Set<String> existing = new HashSet<String>();
		Set<String> unknown = new HashSet<String>();
		for (Resource subj : model.filter(null, RDF.TYPE, null).subjects()) {
			if (subj instanceof URI) {
				URI uri = (URI) subj;
				String ns = uri.getNamespace();
				if (BUILD_IN.contains(uri))
					continue;
				if (baseMapper.isTypeRecorded(uri)
						|| baseLiterals.findClass(uri) != null) {
					existing.add(ns);
				} else {
					unknown.add(ns);
				}
			}
		}
		unknown.removeAll(existing);
		if (unknown.isEmpty())
			return cl;
		OntologyConverter compiler = new OntologyConverter(model, cl);
		compiler.setLiteralManager(baseLiterals);
		compiler.setRoleMapper(baseMapper);
		for (String ns : unknown) {
			String prefix = findPrefix(ns, model);
			String pkgName = module.getPkgPrefix() + prefix;
			compiler.bindPackageToNamespace(pkgName, ns);
		}
		try {
			compiler.createJar(jar);
			return new URLClassLoader(new URL[] { jar.toURI().toURL() }, cl);
		} catch (Exception e) {
			throw new ObjectStoreConfigException(e);
		}
	}

	private String findPrefix(String ns, Model model) {
		String prefix = null;
		for (Map.Entry<String, String> e : model.getNamespaces().entrySet()) {
			if (ns.equals(e.getValue()) && e.getKey().length() > 0) {
				prefix = e.getKey();
				break;
			}
		}
		if (prefix == null) {
			prefix = "ns" + Integer.toHexString(ns.hashCode());
		}
		return prefix;
	}

}
