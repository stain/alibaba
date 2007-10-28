package org.openrdf.alibaba.firefox;

import java.net.URL;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Policy;

/**
 * Lets us grant all permissions to local codesources.
 * 
 * @author James Leigh
 */
public class LocalTrustPolicy extends Policy {
	private Policy outerPolicy;

	/**
	 * Sets the outer policy so that we can defer to it for code sources that we
	 * are not told about.
	 * 
	 * @param policy
	 */
	public void setOuterPolicy(Policy policy) {
		this.outerPolicy = policy;
	}

	@Override
	public void refresh() {
		if (outerPolicy != null) {
			outerPolicy.refresh();
		}
	}

	@Override
	public PermissionCollection getPermissions(CodeSource codesource) {
		PermissionCollection pc = getPermissionCollection(codesource);
		URL url = codesource.getLocation();
		if (url == null || url.toExternalForm().startsWith("file:")) {
			pc.add(new java.security.AllPermission());
		}
		return pc;
	}

	private PermissionCollection getPermissionCollection(CodeSource codesource) {
		if (outerPolicy == null)
			return new Permissions();
		return outerPolicy.getPermissions(codesource);
	}
}
