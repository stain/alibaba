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

import static java.util.Collections.singleton;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

import org.openrdf.concepts.rdf.Property;
import org.openrdf.elmo.codegen.JavaCompiler;
import org.openrdf.elmo.codegen.JavaNameResolver;
import org.openrdf.elmo.codegen.builder.JavaCodeBuilder;
import org.openrdf.elmo.codegen.concepts.CodeMessageClass;
import org.openrdf.elmo.codegen.concepts.Method;
import org.openrdf.elmo.codegen.source.JavaClassBuilder;

public abstract class JavaMethodCompiler implements Method {
	private Method self;

	public JavaMethodCompiler(Method self) {
		this.self = self;
	}

	public String msgCompile(JavaNameResolver resolver, File dir,
			List<File> classpath) throws Exception {
		if (getElmoJava() == null)
			return null;
		String pkg = resolver.getPackageName(this.getQName());
		String simple = resolver.getSimpleName(this.getQName());
		File pkgDir = new File(dir, pkg.replace('.', '/'));
		File source = new File(pkgDir, simple + ".java");
		printJavaFile(source, resolver, pkg, simple);
		String name = simple;
		if (pkg != null) {
			name = pkg + '.' + simple;
		}
		compile(name, dir, classpath);
		return name;
	}

	private void printJavaFile(File source, JavaNameResolver resolver,
			String pkg, String simple) throws FileNotFoundException {
		JavaClassBuilder out = new JavaClassBuilder(source);
		JavaCodeBuilder builder = new JavaCodeBuilder(out, resolver);
		builder.classHeader(self);
		builder.constructor(self);
		printMethod(builder, resolver);
		builder.close();
	}

	private void printMethod(JavaCodeBuilder builder, JavaNameResolver resolver) {
		CodeMessageClass code = (CodeMessageClass) getElmoRange();
		builder.method(getQName(), code, getElmoJava());
		List<Property> properties = code.getParameters();
		if (properties.size() > 1) {
			builder.methodAliasMap(code);
		}
	}

	private void compile(String name, File dir, List<File> classpath) throws Exception {
		JavaCompiler javac = new JavaCompiler();
		javac.compile(singleton(name), dir, classpath);
	}
}
