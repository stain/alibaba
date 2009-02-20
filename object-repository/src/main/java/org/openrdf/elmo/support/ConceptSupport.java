/*
 * Copyright (c) 2008, James Leigh All rights reserved.
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

import org.openrdf.concepts.rdf.Property;
import org.openrdf.concepts.rdfs.Datatype;
import org.openrdf.elmo.codegen.JavaNameResolver;
import org.openrdf.elmo.codegen.builder.JavaCodeBuilder;
import org.openrdf.elmo.codegen.concepts.CodeClass;
import org.openrdf.elmo.codegen.concepts.CodeMessageClass;
import org.openrdf.elmo.codegen.concepts.CodeMethod;
import org.openrdf.elmo.codegen.source.JavaClassBuilder;

public abstract class ConceptSupport implements CodeClass {
	private CodeClass self;

	public ConceptSupport(CodeClass self) {
		this.self = self;
	}

	public File generateSourceCode(File dir, JavaNameResolver resolver)
			throws Exception {
		File source = createSourceFile(dir, resolver);
		JavaClassBuilder jcb = new JavaClassBuilder(source);
		JavaCodeBuilder builder = new JavaCodeBuilder(jcb, resolver);
		if (self instanceof Datatype) {
			builder.classHeader(self);
			builder.stringConstructor(self);
		} else {
			builder.interfaceHeader(self);
			builder.constants(self);
			for (Property prop : getDeclaredProperties()) {
				if (prop instanceof CodeMethod
						&& ((CodeMethod) prop).isMethod())
					continue;
				builder.property(self, prop);
			}
			for (CodeMessageClass type : getMessageTypes()) {
				builder.message(type);
			}
		}
		builder.close();
		return source;
	}

	private File createSourceFile(File dir, JavaNameResolver resolver) {
		String pkg = resolver.getPackageName(getQName());
		String simple = resolver.getSimpleName(getQName());
		File folder = dir;
		if (pkg != null) {
			folder = new File(dir, pkg.replace('.', '/'));
		}
		folder.mkdirs();
		File source = new File(folder, simple + ".java");
		return source;
	}

}
