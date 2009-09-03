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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;

import org.openrdf.model.Namespace;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
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
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.RDFParserFactory;
import org.openrdf.rio.RDFParserRegistry;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.RDFWriterFactory;
import org.openrdf.rio.RDFWriterRegistry;
import org.openrdf.rio.helpers.RDFHandlerWrapper;
import org.openrdf.server.metadata.annotations.method;
import org.openrdf.server.metadata.annotations.operation;
import org.openrdf.server.metadata.annotations.rel;
import org.openrdf.server.metadata.annotations.title;
import org.openrdf.server.metadata.annotations.type;
import org.openrdf.server.metadata.concepts.WebResource;
import org.openrdf.server.metadata.exceptions.MethodNotAllowedException;
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
	public GraphQueryResult exportNamedGraph() throws RepositoryException,
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

	@method("PATCH")
	public void patchNamedGraph(GraphQueryResult patch, File file)
			throws RepositoryException, IOException, RDFParseException,
			RDFHandlerException, QueryEvaluationException,
			MimeTypeParseException {
		File parent = file.getParentFile();
		if (!parent.canWrite())
			throw new MethodNotAllowedException();
		File tmp = new File(parent, "$patching" + file.getName());
		Charset charset = getCharset(getMediaType());
		RDFFormat format = RDFFormat.forMIMEType(mimeType(getMediaType()));
		RDFParserFactory pfactory = RDFParserRegistry.getInstance().get(format);
		RDFWriterFactory wfactory = RDFWriterRegistry.getInstance().get(format);
		assert pfactory != null && wfactory != null;
		ObjectConnection con = getObjectConnection();
		String base = getResource().stringValue();
		Writer writer = null;
		FileOutputStream out = new FileOutputStream(tmp);
		try {
			RDFWriter rdf;
			if (charset == null) {
				rdf = wfactory.getWriter(out);
			} else {
				writer = new OutputStreamWriter(out, charset);
				rdf = wfactory.getWriter(writer);
			}
			// TODO writer.setBaseURI(base);
			RDFParser parser = pfactory.getParser();
			parser.setRDFHandler(new RDFHandlerWrapper(rdf) {
				@Override
				public void endRDF() throws RDFHandlerException {
					// don't close the file yet
				}
			});
			FileInputStream in = new FileInputStream(file);
			try {
				if (charset == null) {
					parser.parse(in, base);
				} else {
					Reader reader = new InputStreamReader(in, charset);
					parser.parse(reader, base);
				}
			} finally {
				in.close();
			}
			while (patch.hasNext()) {
				Statement st = patch.next();
				rdf.handleStatement(st);
				con.add(st, getResource());
			}
			rdf.endRDF();
		} finally {
			if (writer == null) {
				out.close();
			} else {
				writer.close();
			}
		}
		con.setAutoCommit(true); // prepare()
		file.delete();
		if (!tmp.renameTo(file)) {
			throw new MethodNotAllowedException();
		}
	}

	public void extractMetadata(File file) throws RepositoryException,
			IOException {
		ObjectConnection con = getObjectConnection();
		String mime = mimeType(getMediaType());
		RDFFormat format = RDFFormat.forMIMEType(mime);
		String iri = getResource().stringValue();
		try {
			con.add(file, iri, format);
		} catch (RDFParseException e) {
			logger.warn("Could not parse " + iri + ": " + e.getMessage(), e);
		}
	}

	private String mimeType(String media) {
		if (media == null)
			return null;
		int idx = media.indexOf(';');
		if (idx > 0)
			return media.substring(0, idx);
		return media;
	}

	private Charset getCharset(String mediaType) throws MimeTypeParseException {
		if (mediaType == null)
			return null;
		MimeType m = new MimeType(mediaType);
		String name = m.getParameters().get("charset");
		if (name == null)
			return null;
		return Charset.forName(name);
	}
}
