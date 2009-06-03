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
package org.openrdf.server.metadata;

import java.io.File;
import java.io.IOException;

import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.config.RepositoryConfigException;
import org.openrdf.repository.object.ObjectRepository;

import com.sun.grizzly.http.SelectorThread;
import com.sun.grizzly.http.algorithms.NoParsingAlgorithm;
import com.sun.grizzly.http.servlet.ServletAdapter;

/**
 * Manages the start and stop stages of the server.
 * 
 * @author James Leigh
 *
 */
public class MetadataServer {
	private SelectorThread server;
	private ObjectRepository repository;
	private File dataDir;
	private MetadataServlet servlet;

	public MetadataServer(ObjectRepository repository, File dataDir) {
		this.repository = repository;
		this.dataDir = dataDir;
		server = new SelectorThread();
		server.setAlgorithmClassName(NoParsingAlgorithm.class.getName());
		servlet = new MetadataServlet(repository, dataDir);
		ServletAdapter adapter = new ServletAdapter();
		adapter.setServletInstance(servlet);
		server.setAdapter(adapter);
	}

	public int getPort() {
		return server.getPort();
	}

	public void setPort(int port) {
		server.setPort(port);
	}

	public File getDataDir() {
		return dataDir;
	}

	public Repository getRepository() {
		return repository;
	}

	public String getServerName() {
		return servlet.getServerName();
	}

	public void setServerName(String serverName) {
		servlet.setServerName(serverName);
	}

	public void start() throws IOException, RepositoryConfigException,
			RepositoryException {
		try {
			server.listen();
		} catch (InstantiationException e) {
		    IOException _e = new IOException();
		    _e.initCause(e);
		    throw _e;
		}
	}

	public void stop() throws RepositoryException {
		server.stopEndpoint();
	}

}
