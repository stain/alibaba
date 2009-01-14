package org.openrdf.elmo.base;

import java.net.URL;
import java.util.List;

import org.openrdf.elmo.RdfTypeFactory;
import org.openrdf.elmo.RoleMapper;
import org.openrdf.elmo.RoleMapperFactory;
import org.openrdf.elmo.rolemapper.ComplexMapper;
import org.openrdf.elmo.rolemapper.DirectMapper;
import org.openrdf.elmo.rolemapper.HierarchicalRoleMapper;
import org.openrdf.elmo.rolemapper.RoleClassLoader;
import org.openrdf.elmo.rolemapper.RoleMapperImpl;
import org.openrdf.elmo.rolemapper.SimpleRoleMapper;
import org.openrdf.elmo.rolemapper.TypeMapper;

public abstract class RoleMapperFactoryBase<URI> implements
		RoleMapperFactory<URI> {
	private static final String CONCEPTS = "META-INF/org.openrdf.elmo.concepts";
	private static String[] ROLES = { CONCEPTS,
			"META-INF/org.openrdf.elmo.behaviours",
			"META-INF/org.openrdf.elmo.roles",
			"META-INF/org.openrdf.elmo.factories" };
	private ClassLoader cl;
	private List<URL> jarFileUrls;

	public void setClassLoader(ClassLoader cl) {
		this.cl = cl;
	}

	public void setJarFileUrls(List<URL> jarFileUrls) {
		this.jarFileUrls = jarFileUrls;
	}

	public RoleMapper<URI> createRoleMapper() {
		DirectMapper d = new DirectMapper();
		TypeMapper t = new TypeMapper();
		SimpleRoleMapper r = new SimpleRoleMapper();
		RoleMapperImpl mapper = new RoleMapperImpl();
		mapper.setComplexMapper(new ComplexMapper());
		mapper.setHierarchicalRoleMapper(new HierarchicalRoleMapper(d, t, r));
		mapper.setRdfTypeFactory(getRdfTypeFactory());
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

	protected abstract RdfTypeFactory<URI> getRdfTypeFactory();
}
