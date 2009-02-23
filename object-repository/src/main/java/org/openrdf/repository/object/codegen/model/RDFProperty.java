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
package org.openrdf.repository.object.codegen.model;

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
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.repository.object.codegen.JavaNameResolver;
import org.openrdf.repository.object.codegen.source.JavaClassBuilder;
import org.openrdf.repository.object.codegen.source.JavaCodeBuilder;
import org.openrdf.repository.object.codegen.source.JavaCompiler;
import org.openrdf.repository.object.vocabulary.ELMO;

public class RDFProperty extends RDFEntity {
	public RDFProperty(Model model, Resource self) {
		super(model, self);
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

	public RDFProperty getRDFMethod(URI pred) {
		Resource subj = model.filter(self, pred, null).objectResource();
		if (subj == null)
			return null;
		return new RDFProperty(model, subj);
	}

	public boolean isMethod() {
		return isMethod(this, new HashSet<RDFProperty>());
	}

	private boolean isMethod(RDFProperty method,
			Set<RDFProperty> set) {
		if (ELMO.METHOD.equals(method.getURI()))
			return true;
		set.add(method);
		for (RDFProperty prop : method.getRDFProperties(RDFS.SUBPROPERTYOF)) {
			if (!set.contains(prop) && isMethod(prop, set))
				return true;
		}
		return false;
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
		String java = getString(ELMO.JAVA);
		if (java != null)
			return msgCompileJ(resolver, dir, classpath);
		return msgCompileG(resolver, dir, classpath);
	}
	


	private String msgCompileJ(JavaNameResolver resolver, File dir,
			List<File> classpath) throws Exception {
		if (getString(ELMO.JAVA) == null)
			return null;
		String pkg = resolver.getPackageName(this.getURI());
		String simple = resolver.getSimpleName(this.getURI());
		File pkgDir = new File(dir, pkg.replace('.', '/'));
		File source = new File(pkgDir, simple + ".java");
		printJavaFileJ(source, resolver, pkg, simple);
		String name = simple;
		if (pkg != null) {
			name = pkg + '.' + simple;
		}
		compileJ(name, dir, classpath);
		return name;
	}

	private void printJavaFileJ(File source, JavaNameResolver resolver,
			String pkg, String simple) throws FileNotFoundException {
		JavaClassBuilder out = new JavaClassBuilder(source);
		JavaCodeBuilder builder = new JavaCodeBuilder(out, resolver);
		builder.classHeader(this);
		builder.constructor(this);
		printMethodJ(builder, resolver);
		builder.close();
	}

	private void printMethodJ(JavaCodeBuilder builder, JavaNameResolver resolver) {
		RDFClass code = (RDFClass) getRDFClass(RDFS.RANGE);
		builder.method(getURI(), code, getString(ELMO.JAVA));
		List<RDFProperty> properties = code.getParameters();
		if (properties.size() > 1) {
			builder.methodAliasMap(code);
		}
	}

	private void compileJ(String name, File dir, List<File> classpath) throws Exception {
		JavaCompiler javac = new JavaCompiler();
		javac.compile(singleton(name), dir, classpath);
	}

	private static final String UNIT_CLASS = "org.codehaus.groovy.control.CompilationUnit";
	private static final String GROOVY_CLASS = "groovy.lang.GroovyClassLoader";
	private static final String CONFIG_CLASS = "org.codehaus.groovy.control.CompilerConfiguration";

	private String msgCompileG(JavaNameResolver resolver, File dir,
			List<File> classpath) throws Exception {
		if (getString(ELMO.GROOVY) == null)
			return null;
		String pkg = resolver.getPackageName(this.getURI());
		String simple = resolver.getSimpleName(this.getURI());
		File pkgDir = new File(dir, pkg.replace('.', '/'));
		File source = new File(pkgDir, simple + ".groovy");
		printJavaFileG(source, resolver, pkg, simple);
		compileG(source, dir, classpath);
		if (pkg == null)
			return simple;
		return pkg + '.' + simple;
	}

	private void printJavaFileG(File source, JavaNameResolver resolver,
			String pkg, String simple) throws FileNotFoundException {
		JavaClassBuilder out = new JavaClassBuilder(source);
		JavaCodeBuilder builder = new JavaCodeBuilder(out, resolver);
		builder.setGroovy(true);
		builder.classHeader(this);
		builder.constructor(this);
		printMethodG(builder, resolver);
		builder.close();
	}

	private void printMethodG(JavaCodeBuilder builder, JavaNameResolver resolver) {
		RDFClass code = (RDFClass) getRDFClass(RDFS.RANGE);
		builder.method(getURI(), code, getString(ELMO.GROOVY));
		List<RDFProperty> properties = code.getParameters();
		if (properties.size() > 1) {
			builder.methodAliasMap(code);
		}
	}
	
	private void compileG(File source, File dir, List<File> classpath) throws Exception {
		try {
			// vocabulary
			Class<?> CompilerConfiguration = Class.forName(CONFIG_CLASS);
			Class<?> GroovyClassLoader = Class.forName(GROOVY_CLASS);
			Class<?> CompilationUnit = Class.forName(UNIT_CLASS);
			Constructor<?> newGroovyClassLoader = GroovyClassLoader.getConstructor(ClassLoader.class, CompilerConfiguration, Boolean.TYPE);
			Constructor<?> newCompilationUnit = CompilationUnit.getConstructor(CompilerConfiguration, CodeSource.class, GroovyClassLoader);
			Method setTargetDirectory = CompilerConfiguration.getMethod("setTargetDirectory", File.class);
			Method setClasspathList = CompilerConfiguration.getMethod("setClasspathList", List.class);
			Method addSource = CompilationUnit.getMethod("addSource", File.class);
			Method compile = CompilationUnit.getMethod("compile");
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
		} catch (Exception e) {
			throw e;
		}
	}

}
