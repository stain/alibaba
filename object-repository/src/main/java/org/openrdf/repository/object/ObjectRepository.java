/*
 * Copyright (c) 2007-2009, James Leigh All rights reserved.
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
package org.openrdf.repository.object;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;

import org.openrdf.model.ValueFactory;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.contextaware.ContextAwareRepository;
import org.openrdf.repository.object.config.ObjectFactoryManager;
import org.openrdf.repository.object.managers.TypeManager;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;
import org.openrdf.store.StoreException;

/**
 * @author James Leigh
 * 
 */
public class ObjectRepository extends ContextAwareRepository {
	private ObjectFactoryManager ofm;

	public ObjectRepository(ObjectFactoryManager ofm) {
		this.ofm = ofm;
	}

	@Override
	public ObjectConnection getConnection() throws StoreException {
		RepositoryConnection conn = getDelegate().getConnection();
		ObjectFactory factory = ofm.createObjectFactory();
		ObjectConnection con = new ObjectConnection(this, conn, factory,
				createTypeManager());
		con.setIncludeInferred(isIncludeInferred());
		con.setMaxQueryTime(getMaxQueryTime());
		con.setQueryResultLimit(getQueryResultLimit());
		con.setQueryLanguage(getQueryLanguage());
		con.setReadContexts(getReadContexts());
		con.setAddContexts(getAddContexts());
		con.setRemoveContexts(getRemoveContexts());
		con.setArchiveContexts(getArchiveContexts());
		return con;
	}

	protected TypeManager createTypeManager() {
		return new TypeManager();
	}

	private void importJarOntologies(ClassLoader cl)
			throws RepositoryException, IOException, RDFParseException {
			for (String owl : loadOntologyList(cl)) {
				URL url = cl.getResource(owl);
				loadOntology(this, url);
			}
	}

	@SuppressWarnings("unchecked")
	private Collection<String> loadOntologyList(ClassLoader cl)
			throws IOException {
		Properties ontologies = new Properties();
		String name = "META-INF/org.openrdf.elmo.ontologies";
		Enumeration<URL> resources = cl.getResources(name);
		while (resources.hasMoreElements()) {
			URL url = resources.nextElement();
			ontologies.load(url.openStream());
		}
		Collection<?> list = ontologies.keySet();
		return (Collection<String>) list;
	}

	private void loadOntology(Repository repository, URL url)
			throws RepositoryException, IOException, RDFParseException {
		String filename = url.toString();
		RDFFormat format = formatForFileName(filename);
		RepositoryConnection conn = repository.getConnection();
		ValueFactory vf = repository.getValueFactory();
		try {
			String uri = url.toExternalForm();
			conn.add(url, uri, format, vf.createURI(uri));
		} finally {
			conn.close();
		}
	}

	private RDFFormat formatForFileName(String filename) {
		RDFFormat format = RDFFormat.forFileName(filename);
		if (format != null)
			return format;
		if (filename.endsWith(".owl"))
			return RDFFormat.RDFXML;
		throw new IllegalArgumentException("Unknow RDF format for " + filename);
	}

}
