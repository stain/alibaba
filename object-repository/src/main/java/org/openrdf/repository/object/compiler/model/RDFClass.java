/*
 * Copyright (c) 2008-2009, Zepheira All rights reserved.
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
package org.openrdf.repository.object.compiler.model;

import static java.util.Collections.singleton;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.openrdf.model.Model;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.datatypes.XMLDatatypeUtil;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.model.vocabulary.XMLSchema;
import org.openrdf.repository.object.compiler.JavaNameResolver;
import org.openrdf.repository.object.compiler.RDFList;
import org.openrdf.repository.object.compiler.source.JavaBuilder;
import org.openrdf.repository.object.compiler.source.JavaClassBuilder;
import org.openrdf.repository.object.compiler.source.JavaCompiler;
import org.openrdf.repository.object.exceptions.ObjectStoreConfigException;
import org.openrdf.repository.object.vocabulary.OBJ;

/**
 * Helper object for traversing the OWL model.
 * 
 * @author James Leigh
 *
 */
public class RDFClass extends RDFEntity {
	private static final URI NOTHING = new URIImpl(OWL.NAMESPACE + "Nothing");
	private static final String CONFIG_CLASS = "org.codehaus.groovy.control.CompilerConfiguration";
	private static final String GROOVY_CLASS = "groovy.lang.GroovyClassLoader";
	private static final String UNIT_CLASS = "org.codehaus.groovy.control.CompilationUnit";

	public RDFClass(Model model, Resource self) {
		super(model, self);
	}

	public boolean isDatatype() {
		if (self instanceof URI
				&& XMLDatatypeUtil.isBuiltInDatatype((URI) self))
			return true;
		if (self.equals(RDFS.LITERAL))
			return true;
		return isA(RDFS.DATATYPE);
	}

	public RDFClass getRange(URI pred) {
		return getRange(new RDFProperty(model, pred));
	}

	public RDFClass getRange(RDFProperty property) {
		if (property.isLocalized()) {
			return new RDFClass(property.getModel(), XMLSchema.STRING);
		}
		for (RDFClass c : getRDFClasses(RDFS.SUBCLASSOF)) {
			if (c.isA(OWL.RESTRICTION)) {
				if (property.equals(c.getRDFProperty(OWL.ONPROPERTY))) {
					RDFClass type = c.getRDFClass(OWL.ALLVALUESFROM);
					if (type != null) {
						return type;
					}
				}
			}
		}
		for (RDFClass c : getRDFClasses(RDFS.SUBCLASSOF)) {
			if (c.isA(OWL.RESTRICTION) || c.equals(this))
				continue;
			RDFClass type = ((RDFClass) c).getRange(property);
			if (type != null) {
				return type;
			}
		}
		for (RDFClass r : property.getRDFClasses(RDFS.RANGE)) {
			return r;
		}
		for (RDFProperty p : property.getRDFProperties(RDFS.SUBPROPERTYOF)) {
			RDFClass superRange = getRange(p);
			if (superRange != null) {
				return superRange;
			}
		}
		return new RDFClass(getModel(), RDFS.RESOURCE);
	}

	public boolean isFunctional(RDFProperty property) {
		if (isFunctionalProperty(property))
			return true;
		BigInteger one = BigInteger.valueOf(1);
		for (RDFClass c : getRDFClasses(RDFS.SUBCLASSOF)) {
			if (c.isA(OWL.RESTRICTION)) {
				if (property.equals(c.getRDFProperty(OWL.ONPROPERTY))) {
					if (one.equals(c.getBigInteger(OWL.MAXCARDINALITY))
							|| one.equals(c.getBigInteger(OWL.CARDINALITY))) {
						return true;
					}
				}
			}
		}
		RDFClass range = getRange(property);
		if (range == null)
			return false;
		if (NOTHING.equals(range.getURI()))
			return true;
		for (RDFClass c : getRDFClasses(RDFS.SUBCLASSOF)) {
			if (c.isA(OWL.RESTRICTION) || c.equals(this))
				continue;
			if (c.isFunctional(property)) {
				return true;
			}
		}
		return false;
	}

	public List<? extends Value> getList(URI pred) {
		List<? extends Value> list = null;
		for (Value obj : model.filter(self, pred, null).objects()) {
			if (list == null && obj instanceof Resource) {
				list = new RDFList(model, (Resource) obj).asList();
			} else {
				List<? extends Value> other = new RDFList(model, (Resource) obj)
						.asList();
				if (!list.equals(other)) {
					other.removeAll(list);
					((List) list).addAll(other);
				}
			}
		}
		return list;
	}

	public List<RDFProperty> getParameters() {
		TreeSet<String> set = new TreeSet<String>();
		for (Resource prop : model.filter(null, RDFS.DOMAIN, self).subjects()) {
			if (!model.contains(prop, RDF.TYPE, OWL.ANNOTATIONPROPERTY)) {
				if (prop instanceof URI && !prop.stringValue().startsWith(OBJ.NAMESPACE)) {
					set.add(prop.stringValue());
				}
			}
		}
		URI ont = model.filter(self, RDFS.ISDEFINEDBY, null).objectURI();
		for (Value sup : model.filter(self, RDFS.SUBCLASSOF, null).objects()) {
			if (model.contains((Resource) sup, RDFS.ISDEFINEDBY, ont)) {
				for (Resource prop : model.filter(null, RDFS.DOMAIN, sup)
						.subjects()) {
					if (!model.contains(prop, RDF.TYPE, OWL.ANNOTATIONPROPERTY)) {
						if (prop instanceof URI && !prop.stringValue().startsWith(OBJ.NAMESPACE)) {
							set.add(prop.stringValue());
						}
					}
				}
			}
		}
		List<RDFProperty> list = new ArrayList<RDFProperty>();
		for (String uri : set) {
			list.add(new RDFProperty(model, new URIImpl(uri)));
		}
		return list;
	}

	public RDFProperty getResponseProperty() {
		RDFProperty obj = new RDFProperty(model, OBJ.OBJECT_RESPONSE);
		RDFProperty lit = new RDFProperty(model, OBJ.LITERAL_RESPONSE);
		RDFProperty fobj = new RDFProperty(model, OBJ.FUNCTIONAL_OBJECT_RESPONSE);
		RDFProperty flit = new RDFProperty(model, OBJ.FUNCITONAL_LITERAL_RESPONSE);
		boolean objUsed = false;
		boolean litUsed = false;
		boolean fobjUsed = false;
		boolean flitUsed = false;
		boolean obj0 = false;
		boolean lit0 = false;
		boolean fobj0 = false;
		boolean flit0 = false;
		int usedCount = 0;
		for (RDFClass c : getRestrictions()) {
			RDFProperty property = c.getRDFProperty(OWL.ONPROPERTY);
			boolean nothing = NOTHING.stringValue().equals(c.getString(OWL.ALLVALUESFROM));
			BigInteger card = c.getBigInteger(OWL.CARDINALITY);
			BigInteger max = c.getBigInteger(OWL.MAXCARDINALITY);
			nothing |= card != null && 0 == card.intValue();
			nothing |= max != null && 0 == max.intValue();
			if (!nothing) {
				usedCount++;
			}
			if (obj.equals(property)) {
				objUsed = true;
				obj0 |= nothing;
			} else if (lit.equals(property)) {
				litUsed = true;
				lit0 |= nothing;
			} else if (fobj.equals(property)) {
				fobjUsed = true;
				fobj0 |= nothing;
			} else if (flit.equals(property)) {
				flitUsed = true;
				flit0 |= nothing;
			}
			if (objUsed && !obj0)
				return obj;
			if (litUsed && !lit0)
				return lit;
			if (fobjUsed && !fobj0)
				return fobj;
			if (flitUsed && !flit0)
				return flit;
		}
		if (objUsed)
			return obj;
		if (litUsed)
			return lit;
		if (fobjUsed)
			return fobj;
		if (flitUsed)
			return flit;
		return obj;
	}

	public boolean isMinCardinality(RDFProperty property) {
		BigInteger one = BigInteger.valueOf(1);
		for (RDFClass c : getRDFClasses(RDFS.SUBCLASSOF)) {
			if (c.isA(OWL.RESTRICTION)) {
				if (property.equals(c.getRDFProperty(OWL.ONPROPERTY))) {
					if (one.equals(c.getBigInteger(OWL.MAXCARDINALITY))
							&& one.equals(c.getBigInteger(OWL.MINCARDINALITY))
							|| one.equals(c.getBigInteger(OWL.CARDINALITY))) {
						return true;
					}
				}
			} else if (equals(c)) {
				continue;
			} else if (c.isMinCardinality(property)) {
				return true;
			}
		}
		return false;
	}

	public boolean isEmpty(JavaNameResolver resolver) {
		Collection<RDFProperty> properties = getDeclaredProperties();
		if (properties.size() > 1)
			return false;
		if (!properties.isEmpty() && !OBJ.TARGET.equals(properties.iterator().next().getURI()))
			return false;
		if (!getDeclaredMessages(resolver).isEmpty())
			return false;
		// TODO check annotations
		return false;
	}

	public File generateSourceCode(File dir, JavaNameResolver resolver)
			throws Exception {
		File source = createSourceFile(dir, resolver);
		JavaClassBuilder jcb = new JavaClassBuilder(source);
		JavaBuilder builder = new JavaBuilder(jcb, resolver);
		if (isDatatype()) {
			builder.classHeader(this, resolver.getSimpleName(getURI()));
			builder.stringConstructor(this);
		} else {
			builder.interfaceHeader(this);
			builder.constants(this);
			for (RDFProperty prop : getDeclaredProperties()) {
				builder.property(this, prop);
			}
			for (RDFClass type : getDeclaredMessages(resolver)) {
				builder.message(type, true).end();
			}
		}
		builder.close();
		return source;
	}

	public List<RDFProperty> getFunctionalDatatypeProperties() {
		List<RDFProperty> list = new ArrayList<RDFProperty>();
		for (RDFProperty property : getProperties()) {
			if (isFunctional(property) && getRange(property).isDatatype()) {
				list.add(property);
			}
		}
		return list;
	}

	public boolean precedes(RDFClass p) {
		return model.contains(self, OBJ.PRECEDES, p.self)
				|| model.contains(self, RDFS.SUBCLASSOF, p.self);
	}

	/**
	 * Compiles the method into a collection of classes and resource stored in
	 * the given directory.
	 * 
	 * @param resolver
	 *            utility class to look up corresponding Java names
	 * @param namespaces prefix -&gt; namespace
	 * @param dir
	 *            target directory of byte-code
	 * @param classpath
	 *            available class-path to compile with
	 * @return the full class name of the created role
	 * @throws Exception
	 */
	public Set<String> msgCompile(JavaNameResolver resolver,
			Map<String, String> namespaces, File dir, List<File> classpath)
			throws Exception {
		Set<String> result = new HashSet<String>();
		String pkg = resolver.getPackageName(this.getURI());
		for (Statement st : getStatements(OBJ.MESSAGE_IMPLS)) {
			URI lang = st.getPredicate();
			String code = st.getObject().stringValue();
			String simple = resolver.getSimpleImplName(this.getURI(), code);
			File pkgDir = new File(dir, pkg.replace('.', '/'));
			pkgDir.mkdirs();
			if (OBJ.JAVA.equals(lang)) {
				File source = new File(pkgDir, simple + ".java");
				printJavaFile(source, resolver, pkg, simple, code, false);
				String name = simple;
				if (pkg != null) {
					name = pkg + '.' + simple;
				}
				compileJ(name, dir, classpath);
				result.add(name);
			} else if (OBJ.GROOVY.equals(lang)) {
				File source = new File(pkgDir, simple + ".groovy");
				printJavaFile(source, resolver, pkg, simple, code, true);
				compileG(source, dir, classpath);
				String name = simple;
				if (pkg != null) {
					name = pkg + '.' + simple;
				}
				result.add(name);
			} else if (OBJ.SPARQL.equals(lang)) {
				File source = new File(pkgDir, simple + ".java");
				JavaClassBuilder out = new JavaClassBuilder(source);
				JavaBuilder builder = new JavaBuilder(out, resolver);
				builder.classHeader(this, simple);
				for (RDFClass msg : getMessages(resolver)) {
					builder.sparql(msg, this, code, namespaces);
					if (msg.getParameters().size() > 1) {
						builder.methodAliasMap(msg);
					}
				}
				builder.close();
				String name = simple;
				if (pkg != null) {
					name = pkg + '.' + simple;
				}
				compileJ(name, dir, classpath);
				result.add(name);
			} else if (OBJ.XSLT.equals(lang)) {
				File source = new File(pkgDir, simple + ".java");
				JavaClassBuilder out = new JavaClassBuilder(source);
				JavaBuilder builder = new JavaBuilder(out, resolver);
				builder.classHeader(this, simple);
				for (RDFClass msg : getMessages(resolver)) {
					builder.xslt(msg, this, code, namespaces);
					if (msg.getParameters().size() > 1) {
						builder.methodAliasMap(msg);
					}
				}
				builder.close();
				String name = simple;
				if (pkg != null) {
					name = pkg + '.' + simple;
				}
				compileJ(name, dir, classpath);
				result.add(name);
			}
		}
		return result;
	}

	protected Collection<RDFClass> getRestrictions() {
		Collection<RDFClass> restrictions = new LinkedHashSet<RDFClass>();
		for (RDFClass c : getRDFClasses(RDFS.SUBCLASSOF)) {
			if (c.isA(OWL.RESTRICTION)) {
				restrictions.add(c);
			} else if (equals(c)) {
				continue;
			} else {
				restrictions.addAll(c.getRestrictions());
			}
		}
		return restrictions;
	}

	protected boolean isFunctionalProperty(RDFProperty property) {
		if (property.isA(OWL.FUNCTIONALPROPERTY))
			return true;
		if (property.getStrings(OBJ.LOCALIZED).contains("functional"))
			return true;
		if (property.getURI().equals(OBJ.TARGET))
			return true;
		if (property.getURI().equals(OBJ.FUNCITONAL_LITERAL_RESPONSE))
			return true;
		if (property.getURI().equals(OBJ.FUNCTIONAL_OBJECT_RESPONSE))
			return true;
		return false;
	}

	private List<? extends RDFClass> getClassList(URI pred) {
		List<? extends Value> list = getList(pred);
		if (list == null)
			return Collections.EMPTY_LIST;
		List<RDFClass> result = new ArrayList<RDFClass>();
		for (Value value : list) {
			if (value instanceof Resource) {
				Resource subj = (Resource) value;
				result.add(new RDFClass(model, subj));
			}
		}
		return result;
	}

	private boolean isMessageClass(JavaNameResolver resolver) {
		if (resolver.isAnonymous(getURI()))
			return false;
		return isMessage(this, new HashSet<RDFEntity>());
	}

	private boolean isMessage(RDFEntity message, Set<RDFEntity> set) {
		if (OBJ.MESSAGE.equals(message.getURI()))
			return true;
		set.add(message);
		for (RDFClass sup : message.getRDFClasses(RDFS.SUBCLASSOF)) {
			if (!set.contains(sup) && isMessage(sup, set))
				return true;
		}
		return false;
	}

	private BigInteger getBigInteger(URI pred) {
		Value value = model.filter(self, pred, null).objectValue();
		if (value == null)
			return null;
		return new BigInteger(value.stringValue());
	}

	private RDFProperty getRDFProperty(URI pred) {
		Resource subj = model.filter(self, pred, null).objectResource();
		if (subj == null)
			return null;
		return new RDFProperty(model, subj);
	}

	private Collection<RDFClass> getDeclaredMessages(JavaNameResolver resolver) {
		List<RDFClass> list = new ArrayList<RDFClass>();
		for (Resource res : model.filter(null, OWL.ALLVALUESFROM, self)
				.subjects()) {
			if (model.contains(res, OWL.ONPROPERTY, OBJ.TARGET)) {
				for (Resource msg : model.filter(null, RDFS.SUBCLASSOF, res)
						.subjects()) {
					RDFClass rc = new RDFClass(model, msg);
					if (rc.isMessageClass(resolver)) {
						list.add(rc);
					}
				}
			}
		}
		return list;
	}

	private Collection<RDFProperty> getProperties() {
		return getProperties(new HashSet<Resource>(), new ArrayList<RDFProperty>());
	}

	private Collection<RDFProperty> getProperties(Set<Resource> exclude, Collection<RDFProperty> list) {
		if (exclude.add(getResource())) {
			list.addAll(getDeclaredProperties());
			for (RDFClass sup : getRDFClasses(RDFS.SUBCLASSOF)) {
				list = sup.getProperties(exclude, list);
			}
		}
		return list;
	}

	private Collection<RDFProperty> getDeclaredProperties() {
		TreeSet<String> set = new TreeSet<String>();
		for (Resource prop : model.filter(null, RDFS.DOMAIN, self).subjects()) {
			if (prop instanceof URI) {
				set.add(prop.stringValue());
			}
		}
		for (RDFClass res : getRDFClasses(RDFS.SUBCLASSOF)) {
			if (res.isA(OWL.RESTRICTION)) {
				RDFProperty prop = res.getRDFProperty(OWL.ONPROPERTY);
				if (isFunctional(prop) == isFunctionalProperty(prop)) {
					set.add(prop.getURI().stringValue());
				}
			}
		}
		List<RDFProperty> list = new ArrayList<RDFProperty>(set.size());
		for (String uri : set) {
			list.add(new RDFProperty(model, new URIImpl(uri)));
		}
		return list;
	}

	private void printJavaFile(File source, JavaNameResolver resolver,
			String pkg, String simple, String code, boolean groovy)
			throws ObjectStoreConfigException, FileNotFoundException {
		JavaClassBuilder out = new JavaClassBuilder(source);
		JavaBuilder builder = new JavaBuilder(out, resolver);
		builder.setGroovy(groovy);
		builder.classHeader(this, simple);
		for (RDFClass msg : getMessages(resolver)) {
			builder.message(msg, this, code);
			if (msg.getParameters().size() > 1) {
				builder.methodAliasMap(msg);
			}
		}
		builder.close();
	}

	private List<RDFClass> getMessages(JavaNameResolver resolver) {
		List<RDFClass> list = new ArrayList<RDFClass>();
		if (isMessageClass(resolver)) {
			list.add((RDFClass) this);
		}
		for (RDFClass msg : getClassList(OWL.INTERSECTIONOF)) {
			if (msg.isMessageClass(resolver)) {
				list.add(msg);
			}
		}
		for (RDFClass msg : getRDFClasses(OWL.EQUIVALENTCLASS)) {
			if (msg.isMessageClass(resolver)) {
				list.add(msg);
			}
		}
		return list;
	}

	private void compileJ(String name, File dir, List<File> classpath)
			throws Exception {
		JavaCompiler javac = new JavaCompiler();
		javac.compile(singleton(name), dir, classpath);
	}

	private void compileG(File source, File dir, List<File> classpath)
			throws Exception {
		// vocabulary
		Class<?> CompilerConfiguration = Class.forName(CONFIG_CLASS);
		Class<?> GroovyClassLoader = Class.forName(GROOVY_CLASS);
		Class<?> CompilationUnit = Class.forName(UNIT_CLASS);
		Constructor<?> newGroovyClassLoader = GroovyClassLoader.getConstructor(
				ClassLoader.class, CompilerConfiguration, Boolean.TYPE);
		Constructor<?> newCompilationUnit = CompilationUnit.getConstructor(
				CompilerConfiguration, CodeSource.class, GroovyClassLoader);
		Method setTargetDirectory = CompilerConfiguration.getMethod(
				"setTargetDirectory", File.class);
		Method setClasspathList = CompilerConfiguration.getMethod(
				"setClasspathList", List.class);
		Method addSource = CompilationUnit.getMethod("addSource", File.class);
		Method compile = CompilationUnit.getMethod("compile");
		try {
			// logic
			Object config = CompilerConfiguration.newInstance();
			setTargetDirectory.invoke(config, dir);
			List<String> list = new ArrayList<String>(classpath.size());
			for (File cp : classpath) {
				list.add(cp.getAbsolutePath());
			}
			setClasspathList.invoke(config, list);
			ClassLoader cl = Thread.currentThread().getContextClassLoader();
			Object gcl = newGroovyClassLoader.newInstance(cl, config, true);
			Object unit = newCompilationUnit.newInstance(config, null, gcl);
			addSource.invoke(unit, source);
			compile.invoke(unit);
		} catch (InvocationTargetException e) {
			if (e.getCause() instanceof Error)
				throw (Error) e.getCause();
			if (e.getCause() instanceof Exception)
				throw (Exception) e.getCause();
			throw e;
		}
	}
}
