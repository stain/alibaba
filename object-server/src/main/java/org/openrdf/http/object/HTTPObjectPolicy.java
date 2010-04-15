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

import java.io.File;
import java.io.FilePermission;
import java.io.IOException;
import java.lang.reflect.ReflectPermission;
import java.net.MalformedURLException;
import java.net.SocketPermission;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Policy;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.PropertyPermission;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HTTPObjectPolicy extends Policy {
	private static Logger logger = LoggerFactory.getLogger(HTTPObjectPolicy.class);
	private static CodeSource source = HTTPObjectPolicy.class
			.getProtectionDomain().getCodeSource();
	/** loaded from a writable location */
	private final PermissionCollection plugins;
	private final PermissionCollection jars;
	private final List<String> writableLocations;

	public static boolean apply(String[] readable, File... writable) {
		try {
			Policy.setPolicy(new HTTPObjectPolicy(readable, writable));
			System.setSecurityManager(new SecurityManager());
			logger.info("Restricted file system access in effect");
			return true;
		} catch (SecurityException e) {
			// a policy must already be applied
			logger.debug(e.toString(), e);
			return false;
		}
	}

	@Override
	public PermissionCollection getPermissions(CodeSource codesource) {
		if (source == codesource || source != null && source.equals(codesource))
			return jars;
		if (codesource == null || codesource.getLocation() == null)
			return plugins;
		String location = codesource.getLocation().toExternalForm();
		if (!location.startsWith("file:"))
			return plugins;
		for (String url : writableLocations) {
			if (location.startsWith(url))
				return plugins;
		}
		return jars;
	}

	private HTTPObjectPolicy(String[] readable, File... directories) {
		plugins = new Permissions();
		plugins.add(new PropertyPermission("*", "read"));
		plugins.add(new RuntimePermission("getenv.*"));
		plugins.add(new SocketPermission("*", "connect,resolve"));
		plugins.add(new SocketPermission("*", "accept,listen"));
		plugins.add(new ReflectPermission("suppressAccessChecks"));
		plugins.add(new RuntimePermission("accessDeclaredMembers"));
		plugins.add(new RuntimePermission("getClassLoader"));
		plugins.add(new RuntimePermission("createClassLoader"));
		plugins.add(new RuntimePermission("accessDeclaredMembers"));
		plugins.add(new RuntimePermission("getProtectionDomain"));
		plugins.add(new RuntimePermission("setContextClassLoader"));
		plugins.add(new RuntimePermission("accessClassInPackage.sun.*"));
		File home = new File(System.getProperty("user.home"));
		addReadableDirectory(new File(home, ".mime-types.properties"));
		addReadableDirectory(new File(home, ".mime.types"));
		addReadableDirectory(new File(home, ".magic.mime"));
		plugins.add(new FilePermission("/usr/share/mime/-", "read"));
		plugins.add(new FilePermission("/usr/local/share/mime/-", "read"));
		plugins.add(new FilePermission(home.getAbsolutePath() + "/.local/share/mime/-", "read"));
		plugins.add(new FilePermission("/usr/share/mimelnk/-", "read"));
		plugins.add(new FilePermission("/usr/share/file/-", "read"));
		plugins.add(new FilePermission("/etc/magic.mime", "read"));
		// sub directories must come before parent directories (so relative
		// links can be followed)
		addClassPath(System.getProperty("java.ext.dirs"));
		addClassPath(System.getProperty("java.endorsed.dirs"));
		addClassPath(System.getProperty("java.class.path"));
		addClassPath(System.getProperty("sun.boot.class.path"));
		addClassPath(System.getProperty("jdk.home"));
		addClassPath(System.getProperty("java.home"));
		addClassPath(new File("").getAbsolutePath());
		addClassPath(readable);
		writableLocations = new ArrayList<String>(directories.length + 1);
		addWritableDirectories(directories);
		jars = new Permissions();
		Enumeration<Permission> elements = plugins.elements();
		while (elements.hasMoreElements()) {
			jars.add(elements.nextElement());
		}
		jars.add(new RuntimePermission("shutdownHooks"));
		jars.add(new RuntimePermission("accessClassInPackage.sun.misc"));
		jars.add(new RuntimePermission("createSecurityManager"));
		addJavaPath(System.getProperty("jdk.home"));
		addJavaPath(System.getProperty("java.home"));
		addJavaPath(System.getenv("JAVA_HOME"));
		addPath(System.getProperty("java.library.path"));
		addPath(System.getenv("PATH"));
	}

	private void addClassPath(String... paths) {
		for (String path : paths) {
			if (path == null)
				continue;
			for (String dir : path.split(File.pathSeparator)) {
				addReadableDirectory(new File(dir));
			}
		}
	}

	private void addReadableDirectory(File file) {
		addReadableLinks(file);
		String abs = file.getAbsolutePath();
		plugins.add(new FilePermission(abs, "read"));
		logger.debug("FilePermission {} read", abs);
		if (file.isDirectory()) {
			abs = abs + File.separatorChar + "-";
			plugins.add(new FilePermission(abs, "read"));
			logger.debug("FilePermission {} read", abs);
		}
	}

	private void addReadableLinks(File file) {
		try {
			File[] listFiles = file.listFiles();
			if (listFiles != null) {
				for (File f : listFiles) {
					addReadableLinks(f);
				}
			}
			String sourcePath = file.getAbsolutePath();
			String targetPath = file.getCanonicalPath();
			if (!sourcePath.equals(targetPath)) {
				plugins.add(new FilePermission(targetPath, "read"));
				logger.debug("FilePermission {} read", targetPath);
				if (file.isDirectory()) {
					for (File f : file.getCanonicalFile().listFiles()) {
						addReadableLinks(f.getAbsoluteFile());
					}
				}
			}
		} catch (IOException e) {
			logger.debug(e.toString(), e);
		}
	}

	private void addWritableDirectories(File... directories) {
		for (File dir : directories) {
			addWriteableDirectory(dir);
			try {
				writableLocations.add(url(dir));
			} catch (MalformedURLException e) {
				// skip directory
			}
		}
		try {
			File tmp = File.createTempFile("server", "tmp");
			tmp.delete();
			addWriteableDirectory(tmp.getParentFile());
			writableLocations.add(url(tmp.getParentFile()));
		} catch (IOException e) {
			// can't write to tmp
		}
	}

	private String url(File dir) throws MalformedURLException {
		return dir.toURI().toURL().toExternalForm();
	}

	private void addWriteableDirectory(File dir) {
		String path = dir.getAbsolutePath();
		plugins.add(new FilePermission(path, "read"));
		plugins.add(new FilePermission(path, "write"));
		logger.debug("FilePermission {} read write", path);
		path = path + File.separatorChar + "-";
		plugins.add(new FilePermission(path, "read"));
		plugins.add(new FilePermission(path, "write"));
		plugins.add(new FilePermission(path, "delete"));
		logger.debug("FilePermission {} read write delete", path);
	}

	private void addJavaPath(String path) {
		if (path != null) {
			File parent = new File(path).getParentFile();
			addPath(parent.getAbsolutePath());
		}
	}

	private void addPath(String... paths) {
		for (String path : paths) {
			if (path == null)
				continue;
			for (String dir : path.split(File.pathSeparator)) {
				String file = new File(dir).getAbsolutePath();
				jars.add(new FilePermission(file, "read"));
				logger.debug("FilePermission {} read from jars", file);
				file = file + File.separatorChar + "-";
				jars.add(new FilePermission(file, "read"));
				jars.add(new FilePermission(file, "execute"));
				logger.debug("FilePermission {} read execute from jars", file);
			}
		}
	}

}
