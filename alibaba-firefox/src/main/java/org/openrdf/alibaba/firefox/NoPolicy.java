package org.openrdf.alibaba.firefox;

import java.security.CodeSource;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Policy;

/**
 * Lets us grant all permissions to all codesources.
 * 
 * @author James Leigh
 */
public class NoPolicy extends Policy {

	@Override
	public PermissionCollection getPermissions(CodeSource codesource) {
		PermissionCollection pc = new Permissions();
		pc.add(new java.security.AllPermission());
		return pc;
	}
}
