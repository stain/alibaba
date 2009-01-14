package org.openrdf.elmo;

public interface RoleMapperFactory<URI> {
	public abstract RoleMapper<URI> createRoleMapper();
}
