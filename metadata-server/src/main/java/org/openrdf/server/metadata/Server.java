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
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.openrdf.model.Graph;
import org.openrdf.model.Resource;
import org.openrdf.model.impl.GraphImpl;
import org.openrdf.model.util.GraphUtil;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.Repository;
import org.openrdf.repository.config.RepositoryConfig;
import org.openrdf.repository.config.RepositoryConfigSchema;
import org.openrdf.repository.manager.RepositoryManager;
import org.openrdf.repository.manager.RepositoryProvider;
import org.openrdf.repository.object.ObjectRepository;
import org.openrdf.repository.object.config.ObjectRepositoryConfig;
import org.openrdf.repository.object.config.ObjectRepositoryFactory;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.Rio;
import org.openrdf.rio.helpers.StatementCollector;

public class Server {
	private static final String METADATA_TEMPLATE = "META-INF/templates/metadata.ttl";

	private static final int DEFAULT_PORT = 8080;

	private static final Options options = new Options();
	static {
		options.addOption("n", "name", true, "Server name");
		options
				.addOption("p", "port", true,
						"Port the server should listen on");
		options.addOption("m", "manager", true,
				"The repository manager or server url");
		options.addOption("i", "id", true,
				"The existing repository id in the local manager");
		options.addOption("r", "repository", true,
				"The existing repository url (file: or http:)");
		options.addOption("w", "www", true,
				"Directory used for data storage and retrieval");
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
				String cmdLineSyntax = " [-r repository | -m manager [-i id]] [-w webdir] [options] ontology...";
				String header = "ontology... a list of RDF or JAR urls that should be compiled and loaded into the server.";
				formatter.printHelp(cmdLineSyntax, header, options, "");
				return;
			}
			if (line.hasOption('v')) {
				System.out.println(MetadataServlet.DEFAULT_NAME);
				return;
			}
			int port = DEFAULT_PORT;
			File dataDir = null;
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
				if (line.hasOption('m') && line.hasOption('i')) {
					String id = line.getOptionValue('i');
					repository = manager.getRepository(id);
				} else if (manager.hasRepositoryConfig("metadata")) {
					repository = manager.getRepository("metadata");
				} else {
					manager.addRepositoryConfig(createRepositoryConfig());
					repository = manager.getRepository("metadata");
				}
			}
			if (line.hasOption('w')) {
				dataDir = new File(line.getOptionValue('w'));
			} else if (line.hasOption('r') && repository.getDataDir() != null) {
				dataDir = new File(repository.getDataDir(), "www");
			} else if (line.hasOption('m')
					&& isDirectory(manager.getLocation())) {
				File base = new File(manager.getLocation().toURI());
				dataDir = new File(base, "www");
			} else {
				dataDir = new File("www").getAbsoluteFile();
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
				for (URL url : imports) {
					if (url.toExternalForm().toLowerCase().endsWith(".jar")
							|| isDirectory(url)) {
						config.addConceptJar(url);
					} else {
						config.addImports(url);
					}
				}
				or = factory.createRepository(config, repository);
			}
			MetadataServer server = new MetadataServer(or, dataDir);
			if (line.hasOption('n')) {
				server.setServerName(line.getOptionValue('n'));
			}
			server.setPort(port);
			server.start();
			System.out.println(server.getClass().getSimpleName()
					+ " listening on port " + port);
			System.out.println("repository: " + server.getRepository());
			System.out.println("data dir: " + server.getDataDir());
			server.join();
			server.stop();
			System.exit(0);
		} catch (Exception e) {
			System.err.println(e.getMessage());
			System.exit(1);
		}
	}

	private static boolean isDirectory(URL url) throws URISyntaxException {
		return url.getProtocol().equalsIgnoreCase("file")
				&& new File(url.toURI()).isDirectory();
	}

	private static RepositoryConfig createRepositoryConfig() throws Exception {
		Graph graph = new GraphImpl();
		RDFParser rdfParser = Rio.createParser(RDFFormat.TURTLE);
		rdfParser.setRDFHandler(new StatementCollector(graph));

		ClassLoader cl = Server.class.getClassLoader();
		String base = new File(".").getAbsoluteFile().toURI().toASCIIString();
		InputStream in = cl.getResourceAsStream(METADATA_TEMPLATE);
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
