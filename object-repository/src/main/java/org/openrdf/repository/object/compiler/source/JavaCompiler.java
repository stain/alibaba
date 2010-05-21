/*
 * Copyright (c) 2007-2008, James Leigh All rights reserved.
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
package org.openrdf.repository.object.compiler.source;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.openrdf.repository.object.exceptions.ObjectCompileException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Java Compiler that can detects the present of a JDK.
 * 
 * @author James Leigh
 * 
 */
public class JavaCompiler {

	final Logger logger = LoggerFactory.getLogger(JavaCompiler.class);

	private String version = "5";

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public void compile(Iterable<String> content, File dir, List<File> classpath)
			throws ObjectCompileException, IOException, InterruptedException {
		List<File> source = new ArrayList<File>();
		for (String name : content) {
			String filename = name.replace('.', File.separatorChar);
			source.add(new File(dir.getAbsoluteFile(), filename + ".java"));
		}
		if (javac(buildJavacArgs(source, classpath)) != 0)
			throw new ObjectCompileException("Could not compile");
	}

	/**
	 * Try and run any available compiler. Try embedded compilers before
	 * external commands. Only fail if all compilers have be attempted.
	 */
	private int javac(String[] args) throws IOException, InterruptedException {
		int result = javaCompilerTool(args);
		if (result < 0) {
			result = javaSunTools(args);
		}
		if (result == 0)
			return result;
		result = javacCommand(args);
		if (result >= 0)
			return result;
		throw new AssertionError("No Compiler Found");
	}

	/**
	 * Requires JDK6. This will sometimes try and write to user.dir and may
	 * throw an {@link java.security.AccessControlException}.
	 */
	private int javaCompilerTool(String[] args) {
		try {
			Class<?> provider = forName("javax.tools.ToolProvider");
			Method getJavaCompiler = provider
					.getMethod("getSystemJavaCompiler");
			Class<?> tool = forName("javax.tools.Tool");
			Method run = tool.getMethod("run", InputStream.class,
					OutputStream.class, OutputStream.class, args.getClass());
			Object compiler = getJavaCompiler.invoke(null);
			if (compiler == null) {
				return -1;
			}
			logger.debug("invoke javax.tools.JavaCompiler#run");
			Object[] param = new Object[] { null, null, null, args };
			Object result = run.invoke(compiler, param);
			return ((Number) result).intValue();
		} catch (InvocationTargetException e) {
			logger.warn(e.toString());
			return -1;
		} catch (ClassNotFoundException e) {
			return -1;
		} catch (IllegalArgumentException e) {
			return -1;
		} catch (IllegalAccessException e) {
			return -1;
		} catch (SecurityException e) {
			return -1;
		} catch (NoSuchMethodException e) {
			return -1;
		}
	}

	private Class<?> forName(String className) throws ClassNotFoundException {
		ClassLoader cl = getClass().getClassLoader();
		if (cl == null)
			return Class.forName(className);
		synchronized (cl) {
			return Class.forName(className, true, cl);
		}
	}

	/**
	 * Requires Sun tools.jar in class-path.
	 */
	private int javaSunTools(String[] args) {
		try {
			Class<?> sun;
			try {
				sun = forName("com.sun.tools.javac.Main");
			} catch (ClassNotFoundException e) {
				return -1;
			}
			Method method = sun.getMethod("compile", args.getClass());
			logger.debug("invoke com.sun.tools.javac.Main#compile");
			Object result = method.invoke(null, new Object[] { args });
			return ((Number) result).intValue();
		} catch (InvocationTargetException e) {
			logger.warn(e.toString());
			return -1;
		} catch (IllegalArgumentException e) {
			return -1;
		} catch (IllegalAccessException e) {
			return -1;
		} catch (SecurityException e) {
			return -1;
		} catch (NoSuchMethodException e) {
			return -1;
		}
	}

	/**
	 * Requires JDK installation.
	 * 
	 * @throws InterruptedException
	 * @throws IOException
	 */
	private int javacCommand(String[] args) throws IOException,
			InterruptedException {
		String javac = findJavac();
		if (javac == null)
			return -1;
		return exec(javac, args);
	}

	private String findJavac() {
		String javac = findJavac(System.getProperty("jdk.home"));
		if (javac == null)
			javac = findJavac(System.getProperty("java.home"));
		if (javac == null)
			javac = findJavac(System.getenv("JAVA_HOME"));
		if (javac == null) {
			String systemPath = System.getenv("PATH");
			for (String path : systemPath.split(File.pathSeparator)) {
				File file = new File(path, "javac");
				if (exists(file))
					return file.getPath();
			}
		}
		return javac;
	}

	private String findJavac(String home) {
		if (home == null)
			return null;
		File javac = new File(new File(home, "bin"), "javac");
		if (exists(javac))
			return javac.getPath();
		javac = new File(new File(home, "bin"), "javac.exe");
		if (exists(javac))
			return javac.getPath();
		File parent = new File(home).getParentFile();
		javac = new File(new File(parent, "bin"), "javac");
		if (exists(javac))
			return javac.getPath();
		javac = new File(new File(parent, "bin"), "javac.exe");
		if (exists(javac))
			return javac.getPath();
		for (File dir : parent.listFiles()) {
			javac = new File(new File(dir, "bin"), "javac");
			if (exists(javac))
				return javac.getPath();
			javac = new File(new File(dir, "bin"), "javac.exe");
			if (exists(javac))
				return javac.getPath();
		}
		return null;
	}

	private boolean exists(File javac) {
		try {
			return javac.exists();
		} catch (SecurityException e) {
			logger.warn(e.getMessage());
			return false;
		}
	}

	private int exec(String cmd, String[] args) throws IOException,
			InterruptedException {
		logger.debug("exec {}", cmd);
		String[] cmdArray = new String[1 + args.length];
		cmdArray[0] = cmd;
		System.arraycopy(args, 0, cmdArray, 1, args.length);
		File wd = File.createTempFile("javac", "dir");
		wd.delete();
		wd = wd.getParentFile();
		final Process exec = Runtime.getRuntime().exec(cmdArray, null, wd);
		Thread gobbler = new Thread() {
			@Override
			public void run() {
				try {
					InputStream in = exec.getInputStream();
					InputStreamReader isr = new InputStreamReader(in);
					BufferedReader br = new BufferedReader(isr);
					String line = null;
					while ((line = br.readLine()) != null)
						System.out.println(line);
				} catch (IOException ioe) {
					logger.error(ioe.getMessage(), ioe);
				}
			}
		};
		gobbler.start();
		InputStream stderr = exec.getErrorStream();
		InputStreamReader isr = new InputStreamReader(stderr);
		BufferedReader br = new BufferedReader(isr);
		String line = null;
		while ((line = br.readLine()) != null)
			System.err.println(line);
		return exec.waitFor();
	}

	private String[] buildJavacArgs(List<File> sources, List<File> classpath) {
		String[] args = new String[6 + sources.size()];
		args[0] = "-source";
		args[1] = version;
		args[2] = "-target";
		args[3] = version;
		args[4] = "-classpath";
		StringBuilder sb = new StringBuilder();
		for (File jar : classpath) {
			sb.append(jar.getAbsolutePath());
			sb.append(File.pathSeparatorChar);
		}
		args[5] = sb.toString();
		for (int i = 0, n = sources.size(); i < n; i++) {
			args[6 + i] = sources.get(i).getAbsolutePath();
		}
		return args;
	}
}
