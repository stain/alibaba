package org.openrdf.server.metadata;

import info.aduna.io.MavenUtil;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.openrdf.repository.Repository;
import org.openrdf.repository.manager.RepositoryProvider;
import org.openrdf.repository.object.ObjectRepository;
import org.openrdf.repository.object.config.ObjectRepositoryConfig;
import org.openrdf.repository.object.config.ObjectRepositoryFactory;

public class Server {
	private static final String VERSION = MavenUtil.loadVersion(
			"org.openrdf.alibaba", "alibaba-server-metadata", "devel");

	private static final String APP_NAME = "OpenRDF Alibaba metadata-server";

	private static final int DEFAULT_PORT = 8080;

	private static final Options options = new Options();
	static {
		options.addOption("p", "port", true, "Port the server should listen on");
		options.addOption("r", "repository", true, "Assigns the existing repository url (file: or http:)");
		options.addOption("d", "data", true, "Directory used for data storage and retrieval");
		options.addOption("h", "help", false, "Print Help (this message) and exit");
		options.addOption("v", "version", false, "Print version information and exit");
	}

	public static void main(String[] args) {
		try {
			CommandLine line = new GnuParser().parse(options, args);
			if (line.hasOption('h')) {
				HelpFormatter formatter = new HelpFormatter();
				String cmdLineSyntax = " -r repository [-d webapp] [options] ontology...";
				String header = "ontology... a list of RDF urls that should be compiled and loaded into the server.";
				formatter.printHelp(cmdLineSyntax, header, options, "");
				return;
			}
			if (line.hasOption('v')) {
				System.out.println(APP_NAME + " " + VERSION);
				return;
			}
			int port = DEFAULT_PORT;
			File dataDir = null;
			Repository repository = null;
			List<URL> imports = new ArrayList<URL>();
			if (line.hasOption('p')) {
				port = Integer.parseInt(line.getOptionValue('p'));
			}
			if (line.hasOption('r')) {
				repository = RepositoryProvider.getRepository(line.getOptionValue('r'));
			}
			if (line.hasOption('d')) {
				dataDir = new File(line.getOptionValue('d'));
			}
			for (String owl : line.getArgs()) {
				imports.add(getURL(owl));
			}
			if (repository == null) {
				throw new ParseException("Missig -r option");
			} else {
				ObjectRepository or;
				if (imports.isEmpty() && repository instanceof ObjectRepository) {
					or = (ObjectRepository) repository;
				} else {
					ObjectRepositoryFactory factory = new ObjectRepositoryFactory();
					ObjectRepositoryConfig config = factory.getConfig();
					for (URL url : imports) {
						config.addImports(url);
					}
					or = factory.createRepository(config, repository);
				}
				if (dataDir == null && repository.getDataDir() != null) {
					dataDir = new File(repository.getDataDir(), "www");
				} else if (dataDir == null) {
					dataDir = new File(".");
				}
				MetadataServer server = new MetadataServer(or, dataDir);
				server.setPort(port);
				server.start();
				System.out.println(server.getClass().getSimpleName()
						+ " listening on port " + port);
				System.out.println("repository: " + server.getRepository());
				System.out.println("data dir: " + server.getDataDir());
				server.join();
				server.stop();
				System.exit(0);
			}
		} catch (Exception e) {
			System.err.println(e.getMessage());
			System.exit(1);
		}
	}

	private static URL getURL(String path) throws MalformedURLException {
		if (path.startsWith("http:") || path.startsWith("https:"))
			return new URL(path);
		return new File(".").toURI().resolve(path).toURL();
	}

}
