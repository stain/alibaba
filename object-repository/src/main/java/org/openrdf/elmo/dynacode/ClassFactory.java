package org.openrdf.elmo.dynacode;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.LoaderClassPath;
import javassist.NotFoundException;
import javassist.bytecode.Descriptor;

import org.openrdf.repository.object.exceptions.ElmoCompositionException;

public class ClassFactory extends ClassLoader {
	private static final URL exists;

	static {
		try {
			exists = new URL("http://java/"
					+ ClassFactory.class.getName().replace('.', '/')
					+ "#exists");
		} catch (MalformedURLException e) {
			throw new AssertionError(e);
		}
	}

	private Reference<ClassPool> cp;
	private File target;
	private ConcurrentMap<String, byte[]> bytecodes;
	private List<ClassLoader> alternatives = new ArrayList<ClassLoader>();

	/**
	 * Creates a new Class Factory using the current context class loader.
	 */
	public ClassFactory() {
		this(Thread.currentThread().getContextClassLoader());
	}

	/**
	 * Create a given Class Factory with the given class loader.
	 * 
	 * @param parent
	 */
	public ClassFactory(ClassLoader parent) {
		super(parent);
		bytecodes = new ConcurrentHashMap<String, byte[]>();
		String property = System.getProperty("elmobeans.target");
		if (property != null) {
			target = new File(property);
		}
	}

	/**
	 * Create the new Java Class from this template.
	 * 
	 * @param template
	 * @return new Java Class Object
	 * @throws ElmoCompositionException
	 */
	public Class<?> createClass(ClassTemplate template)
			throws ElmoCompositionException {
		CtClass cc = template.getCtClass();
		String name = cc.getName();
		try {
			byte[] bytecode = cc.toBytecode();
			cc.detach();
			return defineClass(name, bytecode);
		} catch (IOException e) {
			throw new ElmoCompositionException(e);
		} catch (CannotCompileException e) {
			throw new ElmoCompositionException(e);
		}
	}

	/**
	 * Create a new Class template, which can later be used to create a Java
	 * class.
	 * 
	 * @param className
	 * @return temporary Class template
	 */
	public ClassTemplate createClassTemplate(String className) {
		ClassPool cp = getClassPool();
		return new ClassTemplate(cp.makeClass(className), this);
	}

	/**
	 * Create a new Class template, which can later be used to create a Java
	 * class.
	 * 
	 * @param name
	 * @param class1 super class
	 * @return temporary Class template
	 */
	public ClassTemplate createClassTemplate(String name, Class<?> class1) {
		try {
			ClassPool cp = getClassPool();
			CtClass cc = cp.makeClass(name, cp.get(class1.getName()));
			return new ClassTemplate(cc, this);
		} catch (NotFoundException e) {
			throw new ElmoCompositionException(e);
		}
	}

	@Override
	public URL getResource(String name) {
		if (bytecodes.containsKey(name))
			return exists;
		URL url = super.getResource(name);
		if (url != null)
			return url;
		synchronized (alternatives) {
			for (ClassLoader cl : alternatives) {
				url = cl.getResource(name);
				if (url != null)
					return url;
			}
		}
		return null;
	}

	@Override
	public InputStream getResourceAsStream(String name) {
		if (bytecodes.containsKey(name)) {
			byte[] b = bytecodes.get(name);
			return new ByteArrayInputStream(b);
		}
		InputStream stream = getParent().getResourceAsStream(name);
		if (stream != null)
			return stream;
		synchronized (alternatives) {
			for (ClassLoader cl : alternatives) {
				stream = cl.getResourceAsStream(name);
				if (stream != null)
					return stream;
			}
		}
		return null;
	}

	@Override
	public Enumeration<URL> getResources(String name) throws IOException {
		Enumeration<URL> resources = super.getResources(name);
		if (resources.hasMoreElements())
			return resources;
		synchronized (alternatives) {
			for (ClassLoader cl : alternatives) {
				resources = cl.getResources(name);
				if (resources.hasMoreElements())
					return resources;
			}
		}
		return resources;
	}

	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		try {
			return super.findClass(name);
		} catch (ClassNotFoundException e) {
			synchronized (alternatives) {
				for (ClassLoader cl : alternatives) {
					try {
						return cl.loadClass(name);
					} catch (ClassNotFoundException e1) {
						continue;
					}
				}
			}
			throw e;
		}
	}

	/**
	 * Causes all created Java Classes to be saved into this directory.
	 * @param folder
	 */
	public void setTraget(File folder) {
		target = folder;
	}

	void appendClassLoader(ClassLoader cl) {
		synchronized (alternatives) {
			alternatives.add(cl);
		}
	}

	private synchronized ClassPool getClassPool() {
		ClassPool pool = cp == null ? null : cp.get();
		if (pool == null) {
			pool = new ClassPool();
			pool.appendClassPath(new LoaderClassPath(this));
			cp = new SoftReference<ClassPool>(pool);
		}
		return pool;
	}

	private Class defineClass(String name, byte[] bytecode) {
		String resource = name.replace('.', '/') + ".class";
		if (target != null) {
			saveResource(resource, bytecode);
		}
		bytecodes.putIfAbsent(resource, bytecode);
		return defineClass(name, bytecode, 0, bytecode.length);
	}

	private void saveResource(String fileName, byte[] bytecode) {
		try {
			File file = new File(target, fileName);
			file.getParentFile().mkdirs();
			FileOutputStream out = new FileOutputStream(file);
			try {
				out.write(bytecode);
			} finally {
				out.close();
			}
		} catch (Exception e) {
		}
	}

	Class<?> getJavaClass(CtClass cc) throws ClassNotFoundException {
		if (cc.isPrimitive()) {
			if (cc.equals(CtClass.booleanType))
				return Boolean.TYPE;
			if (cc.equals(CtClass.byteType))
				return Byte.TYPE;
			if (cc.equals(CtClass.charType))
				return Character.TYPE;
			if (cc.equals(CtClass.doubleType))
				return Double.TYPE;
			if (cc.equals(CtClass.floatType))
				return Float.TYPE;
			if (cc.equals(CtClass.intType))
				return Integer.TYPE;
			if (cc.equals(CtClass.longType))
				return Long.TYPE;
			if (cc.equals(CtClass.shortType))
				return Short.TYPE;
			throw new AssertionError();
		}
		String name = Descriptor.toJavaName(Descriptor.toJvmName(cc));
		return Class.forName(name, true, this);
	}
}
