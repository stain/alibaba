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
package org.openrdf.repository.sparql.config;

import java.util.HashSet;
import java.util.Set;

import org.openrdf.model.Graph;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.util.GraphUtil;
import org.openrdf.model.util.GraphUtilException;
import org.openrdf.repository.config.RepositoryConfigException;
import org.openrdf.repository.config.RepositoryImplConfigBase;

/**
 * Configuration for a SPARQL endpoint.
 * 
 * @author James Leigh
 */
public class SPARQLRepositoryConfig extends RepositoryImplConfigBase {

	public static final URI ENDPOINT = new URIImpl(
			"http://www.openrdf.org/config/repository/sparql#endpoint");

	public static final URI SUBJECT_SPACE = new URIImpl(
			"http://www.openrdf.org/config/repository/sparql#subjectSpace");

	private String url;

	private Set<String> subjects = new HashSet<String>();

	public SPARQLRepositoryConfig() {
		super(SPARQLRepositoryFactory.REPOSITORY_TYPE);
	}

	public SPARQLRepositoryConfig(String url) {
		setURL(url);
	}

	public String getURL() {
		return url;
	}

	public void setURL(String url) {
		this.url = url;
	}

	public Set<String> getSubjectSpaces() {
		return subjects;
	}

	public void setSubjectSpaces(Set<String> subjects) {
		this.subjects = subjects;
	}

	@Override
	public void validate() throws RepositoryConfigException {
		super.validate();
		if (url == null) {
			throw new RepositoryConfigException(
					"No URL specified for SPARQL repository");
		}
	}

	@Override
	public Resource export(Graph graph) {
		Resource implNode = super.export(graph);

		ValueFactory vf = graph.getValueFactory();
		if (url != null) {
			graph.add(implNode, ENDPOINT, vf.createURI(url));
		}
		for (String space : subjects) {
			graph.add(implNode, SUBJECT_SPACE, vf.createURI(space));
		}

		return implNode;
	}

	@Override
	public void parse(Graph graph, Resource implNode)
			throws RepositoryConfigException {
		super.parse(graph, implNode);

		try {
			URI uri = GraphUtil.getOptionalObjectURI(graph, implNode, ENDPOINT);
			if (uri != null) {
				setURL(uri.stringValue());
			}
			Set<Value> set = GraphUtil.getObjects(graph, implNode,
					SUBJECT_SPACE);
			subjects.clear();
			for (Value space : set) {
				subjects.add(space.stringValue());
			}
		} catch (GraphUtilException e) {
			throw new RepositoryConfigException(e.getMessage(), e);
		}
	}
}
