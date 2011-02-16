/*
 * Copyright (c) 2009, James Leigh All rights reserved.
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
package org.openrdf.repository.object.vocabulary;

import java.util.Arrays;
import java.util.Collection;

import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;

/**
 * Static vocabulary for the object ontology.
 * 
 * @author James Leigh
 * 
 */
public class OBJ {
	public static final String NAMESPACE = "http://www.openrdf.org/rdf/2009/object#";
	public static final URI COMPONENT_TYPE = new URIImpl(NAMESPACE
			+ "componentType");
	public static final URI FUNCITONAL_LITERAL_RESPONSE = new URIImpl(NAMESPACE
			+ "functionalLiteralResponse");
	public static final URI FUNCTIONAL_OBJECT_RESPONSE = new URIImpl(NAMESPACE
			+ "functionalObjectResponse");
	public static final URI SCRIPT = new URIImpl(NAMESPACE + "script");
	public static final URI GROOVY = new URIImpl(NAMESPACE + "groovy");
	public static final URI IMPORTS = new URIImpl(NAMESPACE + "imports");
	public static final URI JAVA = new URIImpl(NAMESPACE + "java");
	public static final URI LITERAL_RESPONSE = new URIImpl(NAMESPACE
			+ "literalResponse");
	public static final URI LOCALIZED = new URIImpl(NAMESPACE + "localized");
	public static final URI MESSAGE = new URIImpl(NAMESPACE + "Message");
	public static final URI CLASS_NAME = new URIImpl(NAMESPACE + "className");
	public static final URI MATCHES = new URIImpl(NAMESPACE + "matches");
	public static final URI NAME = new URIImpl(NAMESPACE + "name");
	public static final URI OBJECT_RESPONSE = new URIImpl(NAMESPACE
			+ "objectResponse");
	public static final URI PRECEDES = new URIImpl(NAMESPACE + "precedes");
	public static final URI PROCEED = new URIImpl(NAMESPACE + "proceed");
	public static final URI READ_ONLY = new URIImpl(NAMESPACE + "readOnly");
	public static final URI SPARQL = new URIImpl(NAMESPACE + "sparql");
	public static final URI TARGET = new URIImpl(NAMESPACE + "target");
	public static final URI XSLT = new URIImpl(NAMESPACE + "xslt");
	public static final Collection<URI> MESSAGE_IMPLS = Arrays
			.asList(new URI[] { JAVA, GROOVY, SPARQL, XSLT, SCRIPT });

	private OBJ() {
		// prevent instantiation
	}

}
