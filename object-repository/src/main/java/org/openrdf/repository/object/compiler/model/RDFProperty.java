/*
 * Copyright (c) 2008-2009, Zepheira All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution. 
 * - Neither the name of the openrdf.org nor the names of its contributors may
 *   be used to endorse or promote products derived from this software without
 *   specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 */
package org.openrdf.repository.object.compiler.model;

import static java.util.Collections.singleton;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openrdf.model.Model;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.repository.object.compiler.JavaNameResolver;
import org.openrdf.repository.object.compiler.source.JavaBuilder;
import org.openrdf.repository.object.compiler.source.JavaClassBuilder;
import org.openrdf.repository.object.compiler.source.JavaCompiler;
import org.openrdf.repository.object.exceptions.ObjectStoreConfigException;
import org.openrdf.repository.object.vocabulary.OBJ;

public class RDFProperty extends RDFEntity {
	private static final String CONFIG_CLASS = "org.codehaus.groovy.control.CompilerConfiguration";
	private static final String GROOVY_CLASS = "groovy.lang.GroovyClassLoader";
	private static final String UNIT_CLASS = "org.codehaus.groovy.control.CompilationUnit";

	public RDFProperty(Model model, Resource self) {
		super(model, self);
	}

	public RDFProperty getRDFMethod(URI pred) {
		Resource subj = model.filter(self, pred, null).objectResource();
		if (subj == null)
			return null;
		return new RDFProperty(model, subj);
	}

	public Set<RDFProperty> getRDFProperties(URI pred) {
		Set<RDFProperty> set = new HashSet<RDFProperty>();
		for (Value value : model.filter(self, pred, null).objects()) {
			if (value instanceof Resource) {
				Resource subj = (Resource) value;
				set.add(new RDFProperty(model, subj));
			}
		}
		return set;
	}

	public boolean isLocalized() {
		if (model.contains(self, OBJ.LOCALIZED, null))
			return true;
		return false;
	}

	public boolean isMethodOrTrigger() {
		HashSet<RDFProperty> set = new HashSet<RDFProperty>();
		for (RDFProperty prop : getRDFProperties(RDFS.SUBPROPERTYOF)) {
			if (!set.contains(prop) && isMethod(prop, set))
				return true;
		}
		return false;
	}

	public boolean isTrigger() {
		HashSet<RDFProperty> set = new HashSet<RDFProperty>();
		for (RDFProperty prop : getRDFProperties(RDFS.SUBPROPERTYOF)) {
			if (!set.contains(prop) && isTrigger(prop, set))
				return true;
		}
		return false;
	}

	public boolean isClassDomain() {
		return getValues(RDFS.DOMAIN).contains(OWL.CLASS)
				|| getValues(OBJ.COMPONENT_TYPE).contains(OWL.CLASS);
	}

	/**
	 * Compiles the method into a collection of classes and resource stored in
	 * the given directory.
	 * 
	 * @param resolver
	 *            utility class to look up coresponding Java names
	 * @param dir
	 *            target directory of byte-code
	 * @param classpath
	 *            available class-path to compile with
	 * @return the full class name of the created role
	 * @throws Exception
	 */
	public String msgCompile(JavaNameResolver resolver, File dir,
			List<File> classpath) throws Exception {
		String java = getString(OBJ.JAVA);
		if (java != null)
			return msgCompileJ(resolver, dir, classpath);
		return msgCompileG(resolver, dir, classpath);
	}

	public File generateAnnotationCode(File dir, JavaNameResolver resolver)
			throws Exception {
		File source = createSourceFile(dir, resolver);
		JavaClassBuilder jcb = new JavaClassBuilder(source);
		JavaBuilder builder = new JavaBuilder(jcb, resolver);
		builder.annotationHeader(this);
		builder.close();
		return source;
	}

	private File createSourceFile(File dir, JavaNameResolver resolver) {
		String pkg = resolver.getPackageName(getURI());
		String simple = resolver.getSimpleName(getURI());
		File folder = dir;
		if (pkg != null) {
			folder = new File(dir, pkg.replace('.', '/'));
		}
		folder.mkdirs();
		return new File(folder, simple + ".java");
	}

	private void compileG(File source, File dir, List<File> classpath)
			throws Exception {
		// vocabulary
		Class<?> CompilerConfiguration = Class.forName(CONFIG_CLASS);
		Class<?> GroovyClassLoader = Class.forName(GROOVY_CLASS);
		Class<?> CompilationUnit = Class.forName(UNIT_CLASS);
		Constructor<?> newGroovyClassLoader = GroovyClassLoader.getConstructor(
				ClassLoader.class, CompilerConfiguration, Boolean.TYPE);
		Constructor<?> newCompilationUnit = CompilationUnit.getConstructor(
				CompilerConfiguration, CodeSource.class, GroovyClassLoader);
		Method setTargetDirectory = CompilerConfiguration.getMethod(
				"setTargetDirectory", File.class);
		Method setClasspathList = CompilerConfiguration.getMethod(
				"setClasspathList", List.class);
		Method addSource = CompilationUnit.getMethod("addSource", File.class);
		Method compile = CompilationUnit.getMethod("compile");
		try {
			// logic
			Object config = CompilerConfiguration.newInstance();
			setTargetDirectory.invoke(config, dir);
			List<String> list = new ArrayList<String>(classpath.size());
			for (File cp : classpath) {
				list.add(cp.getAbsolutePath());
			}
			setClasspathList.invoke(config, list);
			ClassLoader cl = Thread.currentThread().getContextClassLoader();
			Object gcl = newGroovyClassLoader.newInstance(cl, config, true);
			Object unit = newCompilationUnit.newInstance(config, null, gcl);
			addSource.invoke(unit, source);
			compile.invoke(unit);
		} catch (InvocationTargetException e) {
			if (e.getCause() instanceof Exception)
				throw (Exception) e.getCause();
			throw e;
		}
	}

	private void compileJ(String name, File dir, List<File> classpath)
			throws Exception {
		JavaCompiler javac = new JavaCompiler();
		javac.compile(singleton(name), dir, classpath);
	}

	private boolean isMethod(RDFProperty method, Set<RDFProperty> set) {
		if (OBJ.METHOD.equals(method.getURI()))
			return true;
		if (OBJ.OBJECT_TRIGGER.equals(method.getURI()))
			return true;
		if (OBJ.DATATYPE_TRIGGER.equals(method.getURI()))
			return true;
		set.add(method);
		for (RDFProperty prop : method.getRDFProperties(RDFS.SUBPROPERTYOF)) {
			if (!set.contains(prop) && isMethod(prop, set))
				return true;
		}
		return false;
	}

	private boolean isTrigger(RDFProperty method, Set<RDFProperty> set) {
		if (OBJ.OBJECT_TRIGGER.equals(method.getURI()))
			return true;
		if (OBJ.DATATYPE_TRIGGER.equals(method.getURI()))
			return true;
		set.add(method);
		for (RDFProperty prop : method.getRDFProperties(RDFS.SUBPROPERTYOF)) {
			if (!set.contains(prop) && isTrigger(prop, set))
				return true;
		}
		return false;
	}

	private String msgCompileG(JavaNameResolver resolver, File dir,
			List<File> classpath) throws Exception {
		if (getString(OBJ.GROOVY) == null)
			return null;
		String pkg = resolver.getPackageName(this.getURI());
		String simple = resolver.getSimpleName(this.getURI());
		File pkgDir = new File(dir, pkg.replace('.', '/'));
		pkgDir.mkdirs();
		File source = new File(pkgDir, simple + ".groovy");
		printJavaFile(source, resolver, pkg, simple, getString(OBJ.GROOVY),
				true);
		compileG(source, dir, classpath);
		if (pkg == null)
			return simple;
		return pkg + '.' + simple;
	}

	private String msgCompileJ(JavaNameResolver resolver, File dir,
			List<File> classpath) throws Exception {
		if (getString(OBJ.JAVA) == null)
			return null;
		String pkg = resolver.getPackageName(this.getURI());
		String simple = resolver.getSimpleName(this.getURI());
		File pkgDir = new File(dir, pkg.replace('.', '/'));
		pkgDir.mkdirs();
		File source = new File(pkgDir, simple + ".java");
		printJavaFile(source, resolver, pkg, simple, getString(OBJ.JAVA), false);
		String name = simple;
		if (pkg != null) {
			name = pkg + '.' + simple;
		}
		compileJ(name, dir, classpath);
		return name;
	}

	private void printJavaFile(File source, JavaNameResolver resolver,
			String pkg, String simple, String code, boolean groovy)
			throws ObjectStoreConfigException, FileNotFoundException {
		JavaClassBuilder out = new JavaClassBuilder(source);
		JavaBuilder builder = new JavaBuilder(out, resolver);
		builder.setGroovy(groovy);
		builder.classHeader(this);
		if (isTrigger()) {
			builder.trigger(this, code);
		} else {
			RDFClass range = getRDFClass(RDFS.RANGE);
			builder.message(range, this, code);
			List<RDFProperty> properties = range.getParameters();
			if (properties.size() > 1) {
				builder.methodAliasMap(range);
			}
		}
		builder.close();
	}

}
