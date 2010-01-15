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

import info.aduna.io.FileUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.openrdf.model.Graph;
import org.openrdf.model.Resource;
import org.openrdf.model.impl.GraphImpl;
import org.openrdf.model.util.GraphUtil;
import org.openrdf.model.util.GraphUtilException;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.Repository;
import org.openrdf.repository.config.RepositoryConfig;
import org.openrdf.repository.config.RepositoryConfigException;
import org.openrdf.repository.config.RepositoryConfigSchema;
import org.openrdf.repository.manager.RepositoryManager;
import org.openrdf.repository.manager.RepositoryProvider;
import org.openrdf.repository.object.ObjectRepository;
import org.openrdf.repository.object.config.ObjectRepositoryConfig;
import org.openrdf.repository.object.config.ObjectRepositoryFactory;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.Rio;
import org.openrdf.rio.helpers.StatementCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Command line tool for launching the server.
 * 
 * @author James Leigh
 * 
 */
public class Server {
	private static final String REPOSITORY_TEMPLATE = "META-INF/templates/object.ttl";
	private static final int DEFAULT_PORT = 8080;
	private static final Logger logger = LoggerFactory.getLogger(Server.class);
	private static final Collection<File> temporary = new ArrayList<File>();

	private static final Options options = new Options();
	static {
		options.addOption("n", "name", true, "Server name");
		options.addOption("identitypath", true,
				"Path prefix used when absolute-URI request-target is percent encoded in path");
		options
				.addOption("p", "port", true,
						"Port the server should listen on");
		options.addOption("m", "manager", true,
				"The repository manager or server url");
		options.addOption("i", "id", true,
				"The existing repository id in the local manager");
		options.addOption("r", "repository", true,
				"The existing repository url (relative file: or http:)");
		options.addOption("t", "template", true,
				"A repository configuration template url "
						+ "(relative file: or http:)");
		options.addOption("password", true,
				"The secret root password file used to bootstrap the system");
		options.addOption("trust", false,
				"Allow all server code to read, write, and execute all files and directories "
						+ "according to the file system's ACL");
		options.addOption("s", "static", false,
				"Only read behaviour operations from the command line");
		options.addOption("w", "www", true,
				"Directory used for data storage and retrieval");
		options.addOption("c", "cache", true,
				"Directory used for transient storage");
		options.addOption("h", "help", false,
				"Print help (this message) and exit");
		options.addOption("v", "version", false,
				"Print version information and exit");
	}

	public static void main(String[] args) {
		try {
			CommandLine line = new GnuParser().parse(options, args);
			if (line.hasOption('h')) {
				HelpFormatter formatter = new HelpFormatter();
				String cmdLineSyntax = " [-r repository | -m manager [-i id [-t config]]] [-w webdir] [options] ontology...";
				String header = "ontology... a list of RDF or JAR urls that should be compiled and loaded into the server.";
				formatter.printHelp(cmdLineSyntax, header, options, "");
				return;
			}
			if (line.hasOption('v')) {
				System.out.println(HTTPObjectServer.DEFAULT_NAME);
				return;
			}
			int port = DEFAULT_PORT;
			File wwwDir = null;
			File cacheDir;
			RepositoryManager manager = null;
			Repository repository = null;
			List<URL> imports = new ArrayList<URL>();
			if (line.hasOption('p')) {
				port = Integer.parseInt(line.getOptionValue('p'));
			}
			if (line.hasOption('r')) {
				String url = line.getOptionValue('r');
				repository = RepositoryProvider.getRepository(url);
			} else {
				if (line.hasOption('m')) {
					String dir = line.getOptionValue('m');
					manager = RepositoryProvider.getRepositoryManager(dir);
				} else {
					manager = RepositoryProvider.getRepositoryManager(".");
				}
				if (line.hasOption('i')) {
					String id = line.getOptionValue('i');
					if (manager.hasRepositoryConfig(id)) {
						repository = manager.getRepository(id);
					} else {
						URL url = getRepositoryConfigURL(line);
						manager.addRepositoryConfig(createConfig(url));
						repository = manager.getRepository(id);
						if (repository == null)
							throw new RepositoryConfigException(
									"Repository id and config id don't match: "
											+ id);
					}
				} else if (manager.hasRepositoryConfig("object")) {
					repository = manager.getRepository("object");
				} else {
					URL url = getRepositoryConfigURL(line);
					manager.addRepositoryConfig(createConfig(url));
					repository = manager.getRepository("object");
				}
			}
			if (line.hasOption('w')) {
				wwwDir = new File(line.getOptionValue('w'));
			} else if (line.hasOption('r') && repository.getDataDir() != null) {
				wwwDir = new File(repository.getDataDir(), "www");
			} else if (line.hasOption('m')
					&& isDirectory(manager.getLocation())) {
				File base = new File(manager.getLocation().toURI());
				wwwDir = new File(base, "www");
			} else {
				wwwDir = new File("www").getAbsoluteFile();
			}
			cacheDir = getCacheDir(line, manager, repository);
			String passwd = null;
			if (line.hasOption("password")) {
				String file = line.getOptionValue("password");
				BufferedReader reader = new BufferedReader(new FileReader(file));
				try {
					passwd = reader.readLine();
					if (passwd.length() == 0) {
						passwd = null;
					}
				} finally {
					reader.close();
				}
			}
			if (!line.hasOption("trust")) {
				if (repository.getDataDir() == null) {
					HTTPObjectPolicy.apply(line.getArgs(), wwwDir, cacheDir);
				} else {
					File repositoriesDir = repository.getDataDir()
							.getParentFile();
					HTTPObjectPolicy.apply(line.getArgs(), repositoriesDir,
							wwwDir, cacheDir);
				}
			}
			for (String owl : line.getArgs()) {
				imports.add(getURL(owl));
			}
			ObjectRepository or;
			if (imports.isEmpty() && repository instanceof ObjectRepository) {
				or = (ObjectRepository) repository;
			} else {
				ObjectRepositoryFactory factory = new ObjectRepositoryFactory();
				ObjectRepositoryConfig config = factory.getConfig();
				if (!line.hasOption('s')) {
					config.setCompileRepository(true);
				}
				if (!imports.isEmpty()) {
					for (URL url : imports) {
						if (url.toExternalForm().toLowerCase().endsWith(".jar")
								|| isDirectory(url)) {
							config.addConceptJar(url);
						} else {
							config.addImports(url);
						}
					}
				}
				or = factory.createRepository(config, repository);
			}
			HTTPObjectServer server = new HTTPObjectServer(or, wwwDir, cacheDir, passwd);
			server.setPort(port);
			if (line.hasOption('n')) {
				server.setServerName(line.getOptionValue('n'));
			}
			if (line.hasOption("identitypath")) {
				String identitypath = line.getOptionValue("identitypath");
				if (!identitypath.startsWith("/"))
					throw new RepositoryConfigException("identitypath must start with '/'");
				server.setIdentityPathPrefix(identitypath);
			}
			server.start();
			Thread.sleep(1000);
			if (server.isRunning()) {
				System.out.println(server.getClass().getSimpleName()
						+ " listening on port " + server.getPort());
				System.out.println("repository: " + server.getRepository());
				System.out.println("www dir: " + wwwDir);
				System.out.println("cache dir: " + cacheDir);
			}
		} catch (Exception e) {
			if (e.getMessage() != null) {
				System.err.println(e.getMessage());
			} else {
				e.printStackTrace(System.err);
			}
			System.exit(1);
		}
	}

	private static File getCacheDir(CommandLine line,
			RepositoryManager manager, Repository repository)
			throws URISyntaxException, MalformedURLException {
		final File cacheDir;
		if (line.hasOption('c')) {
			cacheDir = new File(line.getOptionValue('c'));
		} else if (line.hasOption('r') && repository.getDataDir() != null) {
			cacheDir = new File(repository.getDataDir(), "cache");
		} else if (line.hasOption('m')
				&& isDirectory(manager.getLocation())) {
			File base = new File(manager.getLocation().toURI());
			cacheDir = new File(base, "cache");
		} else {
			cacheDir = new File("cache").getAbsoluteFile();
		}
		deleteOnExit(cacheDir);
		return cacheDir;
	}

	private static void deleteOnExit(File dir) {
		synchronized (temporary) {
			if (temporary.isEmpty()) {
				Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
					public void run() {
						synchronized (temporary) {
							for (File dir : temporary) {
								try {
									if (dir.isDirectory()) {
										for (File f : dir.listFiles()) {
											if (f.isDirectory()) {
												FileUtil.deleteDir(f);
											}
										}
									} else {
										dir.delete();
									}
								} catch (IOException e) {
									// ignore
								}
							}
						}
					}
				}, "Server.cleanup"));
			}
			temporary.add(dir);
		}
	}

	private static boolean isDirectory(URL url) throws URISyntaxException {
		return url.getProtocol().equalsIgnoreCase("file")
				&& new File(url.toURI()).isDirectory();
	}

	private static URL getRepositoryConfigURL(CommandLine line)
			throws MalformedURLException {
		if (line.hasOption('t')) {
			String relative = line.getOptionValue('t');
			URL url = new File(".").toURI().resolve(relative).toURL();
			logger.info("Using repository configuration template: {}", url);
			return url;
		} else {
			ClassLoader cl = Server.class.getClassLoader();
			return cl.getResource(REPOSITORY_TEMPLATE);
		}
	}

	private static RepositoryConfig createConfig(URL url) throws IOException,
			RDFParseException, RDFHandlerException, GraphUtilException,
			RepositoryConfigException {
		Graph graph = new GraphImpl();
		RDFParser rdfParser = Rio.createParser(RDFFormat.TURTLE);
		rdfParser.setRDFHandler(new StatementCollector(graph));

		String base = new File(".").getAbsoluteFile().toURI().toASCIIString();
		URLConnection con = url.openConnection();
		StringBuilder sb = new StringBuilder();
		for (String mimeType : RDFFormat.TURTLE.getMIMETypes()) {
			if (sb.length() < 1) {
				sb.append(", ");
			}
			sb.append(mimeType);
		}
		con.setRequestProperty("Accept", sb.toString());
		InputStream in = con.getInputStream();
		try {
			rdfParser.parse(in, base);
		} finally {
			in.close();
		}

		Resource node = GraphUtil.getUniqueSubject(graph, RDF.TYPE,
				RepositoryConfigSchema.REPOSITORY);
		RepositoryConfig config = RepositoryConfig.create(graph, node);
		config.validate();
		return config;
	}

	private static URL getURL(String path) throws MalformedURLException {
		return new File(".").toURI().resolve(path).toURL();
	}

}
