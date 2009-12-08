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

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.openrdf.model.Model;
import org.openrdf.model.Resource;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.repository.object.compiler.JavaNameResolver;
import org.openrdf.repository.object.compiler.source.JavaBuilder;
import org.openrdf.repository.object.compiler.source.JavaClassBuilder;
import org.openrdf.repository.object.vocabulary.OBJ;

/**
 * Utility class for working with an rdf:Property in a {@link Model}.
 * 
 * @author James Leigh
 *
 */
public class RDFProperty extends RDFEntity {

	public RDFProperty(Model model, Resource self) {
		super(model, self);
	}

	public boolean isLocalized() {
		if (model.contains(self, OBJ.LOCALIZED, null))
			return true;
		return false;
	}

	public boolean isReadOnly() {
		ValueFactory vf = ValueFactoryImpl.getInstance();
		return model.contains(self, OBJ.READ_ONLY, vf.createLiteral(true));
	}

	public boolean isClassDomain() {
		return getValues(RDFS.DOMAIN).contains(OWL.CLASS);
	}

	public boolean isClassRange() {
		Set<Value> set = new HashSet<Value>();
		set.addAll(getValues(RDFS.RANGE));
		set.addAll(getValues(OBJ.COMPONENT_TYPE));
		return set.contains(OWL.CLASS);
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

}
