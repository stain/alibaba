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
package org.openrdf.http.object;

import info.aduna.io.MavenUtil;

import java.io.File;
import java.net.BindException;

import javax.servlet.Filter;
import javax.xml.transform.TransformerConfigurationException;

import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.servlet.FilterHolder;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.jetty.servlet.ServletHolder;
import org.openrdf.http.object.cache.CachingFilter;
import org.openrdf.http.object.filters.ContentMD5Filter;
import org.openrdf.http.object.filters.GUnzipFilter;
import org.openrdf.http.object.filters.GZipFilter;
import org.openrdf.http.object.filters.MD5ValidationFilter;
import org.openrdf.http.object.filters.ProxyPathFilter;
import org.openrdf.http.object.filters.ServerNameFilter;
import org.openrdf.http.object.filters.TraceFilter;
import org.openrdf.repository.Repository;
import org.openrdf.repository.object.ObjectRepository;

/**
 * Manages the start and stop stages of the server.
 * 
 * @author James Leigh
 * 
 */
public class HTTPObjectServer {
	private static final String VERSION = MavenUtil.loadVersion(
			"org.openrdf.alibaba", "alibaba-server-object", "devel");
	private static final String APP_NAME = "OpenRDF AliBaba object-server";
	protected static final String DEFAULT_NAME = APP_NAME + "/" + VERSION;

	private Server server;
	private ObjectRepository repository;
	private int port;
	private HTTPObjectServlet servlet;
	private ServerNameFilter name;
	private ProxyPathFilter abs;

	public HTTPObjectServer(ObjectRepository repository, File www, File cache, String passwd)
			throws TransformerConfigurationException {
		this.repository = repository;
		servlet = new HTTPObjectServlet(repository, www, passwd);
		ServletHandler handler = new ServletHandler();
		handler.addServletWithMapping(new ServletHolder(servlet), "/*");
		add(handler, name = new ServerNameFilter(DEFAULT_NAME));
		add(handler, abs = new ProxyPathFilter());
		add(handler, new TraceFilter());
		add(handler, new MD5ValidationFilter());
		add(handler, new ContentMD5Filter());
		add(handler, new GUnzipFilter());
		add(handler, new CachingFilter(cache, 1024));
		add(handler, new GZipFilter());
		server = new Server();
		server.addHandler(handler);
	}

	private void add(ServletHandler h, Filter f) {
		h.addFilterWithMapping(new FilterHolder(f), "/*", Handler.ALL);
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public Repository getRepository() {
		return repository;
	}

	public String getServerName() {
		return name.getServerName();
	}

	public void setServerName(String serverName) {
		this.name.setServerName(serverName);
	}

	public String getAbsolutePrefix() {
		return abs.getAbsolutePrefix();
	}

	public void setAbsolutePrefix(String prefix) {
		abs.setAbsolutePrefix(prefix);
	}

	public void start() throws BindException, Exception {
		Connector connector = new SocketConnector();
		connector.setPort(port);
		server.setConnectors(new Connector[] { connector });
		server.start();
	}

	public boolean isRunning() {
		return server.isRunning();
	}

	/**
	 * Method may return before socket is released.
	 */
	public void stop() throws Exception {
		server.stop();
	}

}
