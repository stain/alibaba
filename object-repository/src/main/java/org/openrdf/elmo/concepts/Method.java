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
package org.openrdf.elmo.codegen.concepts;

import java.util.Set;

import org.openrdf.concepts.owl.ObjectProperty;
import org.openrdf.concepts.rdfs.Class;
import org.openrdf.elmo.annotations.rdf;

/** The base property for methods of code that handle message. */
public interface Method extends ObjectProperty {
	/** The subject is a submethod of a method. */
	@rdf("http://www.w3.org/2000/01/rdf-schema#subPropertyOf")
	Method getElmoSubMethodOf();
	/** The subject is a submethod of a method. */
	void setElmoSubMethodOf(Method subMethodOf);

	/** A domain of this method. */
	@rdf("http://www.w3.org/2000/01/rdf-schema#domain")
	public abstract Class getElmoDomain();
	/** A domain of this method. */
	public abstract void setElmoDomain(Class domain);

	/** A message type of this method. */
	@rdf("http://www.w3.org/2000/01/rdf-schema#range")
	public abstract Class getElmoRange();
	/** A message type of this method. */
	public abstract void setElmoRange(Class range);

	/** A Class that maybe referenced locally in this method. */
	@rdf("http://www.openrdf.org/rdf/2008/08/elmo#imports")
	Set<Class> getElmoImports();
	/** A Class that maybe referenced locally in this method. */
	void setElmoImports(Set<? extends Class> imports);

	/** The block of Java code for this method. */
	@rdf("http://www.openrdf.org/rdf/2008/08/elmo#java")
	String getElmoJava();
	/** The block of Java code for this method. */
	void setElmoJava(String java);

	/** The block of Groovy code for this method. */
	@rdf("http://www.openrdf.org/rdf/2008/08/elmo#groovy")
	String getElmoGroovy();
	/** The block of Groovy code for this method. */
	void setElmoGroovy(String groovy);

}
