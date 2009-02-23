package org.openrdf.repository.object.config;

import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.openrdf.model.URIFactory;
import org.openrdf.model.impl.LiteralFactoryImpl;
import org.openrdf.model.impl.URIFactoryImpl;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.repository.object.ObjectFactory;
import org.openrdf.repository.object.behaviours.RDFObjectImpl;
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

public class ObjectFactoryManager {

	private static Map<ClassLoader, WeakReference<ClassFactory>> definers = new WeakHashMap<ClassLoader, WeakReference<ClassFactory>>();

	private static final String CONCEPTS = "META-INF/org.openrdf.concepts";
	private static String[] ROLES = { CONCEPTS,
			"META-INF/org.openrdf.behaviours" };

	private ObjectRepositoryConfig module;

	private ClassLoader cl;

	private ClassResolver resolver;

	private LiteralManager literalManager;

	private RoleMapper mapper;

	private PropertyMapper pm;

	public ObjectFactoryManager(ObjectRepositoryConfig module)
			throws ObjectStoreConfigException {
		this.module = module;
	}

	public synchronized void load(ClassLoader cl)
			throws ObjectStoreConfigException {
		if (!cl.equals(this.cl)) {
			this.cl = cl;
			URIFactory uf = new URIFactoryImpl();
			ClassFactory definer = createClassFactory(cl);

			pm = createPropertyMapper(definer);
			mapper = getRoleMapper(cl, uf);
			resolver = getClassResolver(mapper, definer, pm);
			literalManager = getLiteralManager(cl, uf);
		}
	}

	public synchronized ObjectFactory createObjectFactory() {
		return createObjectFactory(mapper, pm, literalManager, resolver);
	}

	protected ObjectFactory createObjectFactory(RoleMapper mapper,
			PropertyMapper pm, LiteralManager literalManager,
			ClassResolver resolver) {
		return new ObjectFactory(mapper, pm, literalManager, resolver);
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

	protected LiteralManager createLiteralManager(ClassLoader cl, URIFactory uf) {
		return new LiteralManager(cl, uf, new LiteralFactoryImpl());
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

	private ClassResolver getClassResolver(RoleMapper mapper, ClassFactory definer, PropertyMapper pm)
			throws ObjectStoreConfigException {
		ClassResolver resolver = createClassResolver(definer, mapper, pm);
		resolver.init();
		return resolver;
	}

	private RoleMapper getRoleMapper(ClassLoader cl, URIFactory uf) throws ObjectStoreConfigException {
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
		LiteralManager literalManager = createLiteralManager(cl, uf);
		for (ObjectRepositoryConfig.Association e : module.getDatatypes()) {
			literalManager.addDatatype(e.getJavaClass(), e.getRdfType());
		}
		return literalManager;
	}

}
