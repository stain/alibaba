/*
 * Copyright (c) 2008, Zepheira All rights reserved.
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
package org.openrdf.elmo.codegen.support;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.List;

import org.openrdf.concepts.rdf.Property;
import org.openrdf.elmo.codegen.JavaNameResolver;
import org.openrdf.elmo.codegen.builder.JavaCodeBuilder;
import org.openrdf.elmo.codegen.concepts.CodeMessageClass;
import org.openrdf.elmo.codegen.source.JavaClassBuilder;

public abstract class GroovyMethodCompiler implements org.openrdf.elmo.codegen.concepts.Method {
	private static final String UNIT_CLASS = "org.codehaus.groovy.control.CompilationUnit";
	private static final String GROOVY_CLASS = "groovy.lang.GroovyClassLoader";
	private static final String CONFIG_CLASS = "org.codehaus.groovy.control.CompilerConfiguration";
	private org.openrdf.elmo.codegen.concepts.Method self;

	public GroovyMethodCompiler(org.openrdf.elmo.codegen.concepts.Method self) {
		this.self = self;
	}

	public String msgCompile(JavaNameResolver resolver, File dir,
			List<File> classpath) throws Exception {
		if (getElmoGroovy() == null)
			return null;
		String pkg = resolver.getPackageName(this.getQName());
		String simple = resolver.getSimpleName(this.getQName());
		File pkgDir = new File(dir, pkg.replace('.', '/'));
		File source = new File(pkgDir, simple + ".groovy");
		printJavaFile(source, resolver, pkg, simple);
		compile(source, dir, classpath);
		if (pkg == null)
			return simple;
		return pkg + '.' + simple;
	}

	private void printJavaFile(File source, JavaNameResolver resolver,
			String pkg, String simple) throws FileNotFoundException {
		JavaClassBuilder out = new JavaClassBuilder(source);
		JavaCodeBuilder builder = new JavaCodeBuilder(out, resolver);
		builder.setGroovy(true);
		builder.classHeader(self);
		builder.constructor(self);
		printMethod(builder, resolver);
		builder.close();
	}

	private void printMethod(JavaCodeBuilder builder, JavaNameResolver resolver) {
		CodeMessageClass code = (CodeMessageClass) getElmoRange();
		builder.method(getQName(), code, getElmoGroovy());
		List<Property> properties = code.getParameters();
		if (properties.size() > 1) {
			builder.methodAliasMap(code);
		}
	}
	
	private void compile(File source, File dir, List<File> classpath) throws Exception {
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
