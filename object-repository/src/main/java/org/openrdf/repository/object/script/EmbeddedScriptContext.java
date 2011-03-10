package org.openrdf.repository.object.script;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class EmbeddedScriptContext {
	private final Set<String> classes = new HashSet<String>();
	private final Set<String> packages = new HashSet<String>();
	private final Map<String, String> assignments = new HashMap<String, String>();
	private Class<?> returnType = Object.class;
	private boolean withThis;

	public void importClass(String className) {
		classes.add(className);
	}

	public void importPackage(String pkgName) {
		packages.add(pkgName);
	}

	public void assignRDFObject(String name, String uri) {
		assignments.put(name, uri);
	}

	public boolean isWithThis() {
		return withThis;
	}

	public void setWithThis(boolean withThis) {
		this.withThis = withThis;
	}

	public Set<String> getClasses() {
		return classes;
	}

	public Set<String> getPackages() {
		return packages;
	}

	public Map<String, String> getAssignments() {
		return assignments;
	}

	public Class<?> getReturnType() {
		return returnType;
	}

	public void setReturnType(Class<?> returnType) {
		this.returnType = returnType;
	}

}
