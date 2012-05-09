package org.openrdf.repository.object.compiler.source;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClassPathBuilder {
	private final Logger logger = LoggerFactory
			.getLogger(ClassPathBuilder.class);
	private final Set<URI> classpath = new LinkedHashSet<URI>();

	public ClassPathBuilder() {
		String classPath = System.getProperty("java.class.path");
		for (String path : classPath.split(File.pathSeparator)) {
			classpath.add(new File(path).toURI());
		}
	}

	public ClassPathBuilder append(ClassLoader cl) {
		appendManifest(cl);
		appendURLClassLoader(cl);
		return this;
	}

	public List<File> toFileList() {
		List<File> list = new ArrayList<File>(classpath.size());
		for (URI uri : classpath) {
			try {
				list.add(new File(uri));
			} catch (IllegalArgumentException e) {
				logger.warn("Not a local class path entry", e);
			}
		}
		return list;
	}

	private void appendManifest(ClassLoader cl) {
		try {
			Enumeration<URL> resources = cl
					.getResources("META-INF/MANIFEST.MF");
			while (resources.hasMoreElements()) {
				URL url = resources.nextElement();
				try {
					if ("jar".equalsIgnoreCase(url.getProtocol())) {
						appendManifest(url, cl);
					}
				} catch (IOException e) {
					logger.warn(e.toString(), e);
				} catch (URISyntaxException e) {
					logger.warn(e.toString(), e);
				}
			}
		} catch (IOException e) {
			logger.error(e.toString(), e);
		}
	}

	private void appendManifest(URL url, ClassLoader cl)
			throws URISyntaxException, IOException {
		String jar = url.getPath();
		if (jar.lastIndexOf('!') > 0) {
			jar = jar.substring(0, jar.lastIndexOf('!'));
		}
		java.net.URI uri = new java.net.URI(jar);
		Manifest manifest = new Manifest();
		InputStream in = url.openStream();
		try {
			manifest.read(in);
		} finally {
			in.close();
		}
		Attributes attributes = manifest.getMainAttributes();
		String dependencies = attributes.getValue("Class-Path");
		if (dependencies == null) {
			dependencies = attributes.getValue("Class-path");
		}
		if (dependencies != null) {
			for (String entry : dependencies.split("\\s+")) {
				if (entry.length() > 0) {
					classpath.add(uri.resolve(entry));
				}
			}
		}
	}

	private void appendURLClassLoader(ClassLoader cl) {
		if (cl instanceof URLClassLoader) {
			for (URL jar : ((URLClassLoader) cl).getURLs()) {
				try {
					classpath.add(jar.toURI());
				} catch (URISyntaxException e) {
					logger.error(e.toString(), e);
				}
			}
		}
		if (cl.getParent() != null) {
			appendURLClassLoader(cl.getParent());
		}
	}

}
