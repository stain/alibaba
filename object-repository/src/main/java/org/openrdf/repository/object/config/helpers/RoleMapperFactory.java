package org.openrdf.repository.object.config.helpers;

import java.net.URL;
import java.util.List;

import org.openrdf.model.URIFactory;
import org.openrdf.repository.object.managers.RoleMapper;
import org.openrdf.repository.object.managers.helpers.ComplexMapper;
import org.openrdf.repository.object.managers.helpers.DirectMapper;
import org.openrdf.repository.object.managers.helpers.HierarchicalRoleMapper;
import org.openrdf.repository.object.managers.helpers.RoleClassLoader;
import org.openrdf.repository.object.managers.helpers.SimpleRoleMapper;
import org.openrdf.repository.object.managers.helpers.TypeMapper;

public class RoleMapperFactory {
	private static final String CONCEPTS = "META-INF/org.openrdf.elmo.concepts";
	private static String[] ROLES = { CONCEPTS,
			"META-INF/org.openrdf.elmo.behaviours",
			"META-INF/org.openrdf.elmo.roles",
			"META-INF/org.openrdf.elmo.factories" };
	private ClassLoader cl;
	private List<URL> jarFileUrls;
	private URIFactory vf;

	public RoleMapperFactory(URIFactory vf) {
		this.vf = vf;
	}

	public void setClassLoader(ClassLoader cl) {
		this.cl = cl;
	}

	public void setJarFileUrls(List<URL> jarFileUrls) {
		this.jarFileUrls = jarFileUrls;
	}

	public RoleMapper createRoleMapper() {
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
}
