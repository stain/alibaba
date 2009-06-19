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
package org.openrdf.server.metadata.behaviours;

import static org.openrdf.query.QueryLanguage.SPARQL;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.openrdf.model.Namespace;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.query.GraphQuery;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.impl.DatasetImpl;
import org.openrdf.query.impl.GraphQueryResultImpl;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.server.metadata.annotations.operation;
import org.openrdf.server.metadata.annotations.rel;
import org.openrdf.server.metadata.annotations.title;
import org.openrdf.server.metadata.annotations.type;
import org.openrdf.server.metadata.concepts.WebResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parses RDF from a file.
 * 
 * @author James Leigh
 *
 */
public abstract class NamedGraphSupport implements WebResource {
	private Logger logger = LoggerFactory.getLogger(NamedGraphSupport.class);

	private static final String CONSTRUCT_ALL = "CONSTRUCT {?subj ?pred ?obj}\n"
			+ "WHERE {?subj ?pred ?obj}";

	@rel("alternate")
	@title("RDF Graph")
	@operation("graph")
	@type( { "application/rdf+xml", "application/x-turtle", "text/rdf+n3",
			"application/trix", "application/x-trig" })
	public GraphQueryResult metadata() throws RepositoryException,
			RDFHandlerException, QueryEvaluationException,
			MalformedQueryException {
		Resource self = getResource();
		if (self instanceof URI) {
			DatasetImpl dataset = new DatasetImpl();
			dataset.addDefaultGraph((URI) self);

			RepositoryConnection con = getObjectConnection();
			GraphQuery query = con.prepareGraphQuery(SPARQL, CONSTRUCT_ALL);
			query.setDataset(dataset);

			// Use the namespaces of the repository (not the query)
			RepositoryResult<Namespace> namespaces = con.getNamespaces();
			Map<String, String> map = new HashMap<String, String>();
			while (namespaces.hasNext()) {
				Namespace ns = namespaces.next();
				map.put(ns.getPrefix(), ns.getName());
			}
			return new GraphQueryResultImpl(map, query.evaluate());
		} else {
			return null;
		}
	}

	public void extractMetadata(File file) throws RepositoryException, IOException {
		ObjectConnection con = getObjectConnection();
		String media = getMediaType();
		MediaType m = MediaType.valueOf(media);
		String mime = m.getType() + "/" + m.getSubtype();
		RDFFormat format = RDFFormat.forMIMEType(mime);
		try {
			con.add(file, getResource().stringValue(), format);
		} catch (RDFParseException e) {
			logger.warn(e.getMessage(), e);
		}
	}
}
