/*
 * Copyright (c) 2007-2009, James Leigh All rights reserved.
 * Copyright (c) 2011 Talis Inc., Some rights reserved.
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
package org.openrdf.repository.object.compiler;

import info.aduna.net.ParsedURI;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.Model;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.model.vocabulary.XMLSchema;
import org.openrdf.repository.object.vocabulary.MSG;
import org.openrdf.repository.object.vocabulary.OBJ;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Applies a series of rules against the ontology, making it easier to convert
 * into Java classes. This includes applying some OWL reasoning on properties,
 * renaming anonymous and foreign classes.
 * 
 * @author James Leigh
 * 
 */
public class OwlNormalizer {
	private final Logger logger = LoggerFactory.getLogger(OwlNormalizer.class);
	private RDFDataSource manager;
	private Set<URI> anonymousClasses = new HashSet<URI>();
	private Map<URI, URI> aliases = new HashMap<URI, URI>();
	private Map<String, URI> ontologies;
	private Map<String, String> implNames = new HashMap<String, String>();
	private Set<String> commonNS = new HashSet<String>(Arrays.asList(
			RDF.NAMESPACE, RDFS.NAMESPACE, OWL.NAMESPACE));

	public OwlNormalizer(RDFDataSource manager) {
		this.manager = manager;
	}

	public URI getOriginal(URI alias) {
		if (anonymousClasses.contains(alias))
			return null;
		if (aliases.containsKey(alias))
			return aliases.get(alias);
		return alias;
	}

	public Map<URI, URI> getAliases() {
		return aliases;
	}

	public Set<URI> getAnonymousClasses() {
		return anonymousClasses;
	}

	public Map<String, String> getImplNames() {
		return implNames;
	}

	public void normalize() {
		renameDeprecatedNamespaces();
		infer();
		createJavaAnnotations();
		checkPropertyDomains();
		checkPropertyRanges();
		ontologies = findOntologies();
		hasValueFromList();
		subClassOneOf();
		subClassIntersectionOf();
		mergeDuplicateRestrictions();
		distributeEquivalentClasses();
		renameAnonymousClasses();
		mergeUnionClasses();
		distributeSubMessage();
		checkMessageTargets();
		addPrecedesToSubClasses();
	}

	/**
	 * Treat owl:complementOf, owl:intersectionOf, owl:oneOf, and owl:unionOf as
	 * annotations so they will be saved in the concept header.
	 */
	private void createJavaAnnotations() {
		if (manager.contains(null, RDFS.SUBPROPERTYOF, null)) {
			manager.add(RDFS.SUBPROPERTYOF, RDF.TYPE, OWL.ANNOTATIONPROPERTY);
		}
		if (manager.contains(null, OWL.EQUIVALENTCLASS, null)) {
			manager.add(OWL.EQUIVALENTCLASS, RDF.TYPE, OWL.ANNOTATIONPROPERTY);
			manager.add(OWL.EQUIVALENTCLASS, RDFS.RANGE, OWL.CLASS);
		}
		if (manager.contains(null, OWL.COMPLEMENTOF, null)) {
			manager.add(OWL.COMPLEMENTOF, RDF.TYPE, OWL.ANNOTATIONPROPERTY);
			manager.add(OWL.COMPLEMENTOF, RDF.TYPE, OWL.FUNCTIONALPROPERTY);
			manager.add(OWL.COMPLEMENTOF, RDFS.RANGE, OWL.CLASS);
		}
		if (manager.contains(null, OWL.INTERSECTIONOF, null)) {
			manager.add(OWL.INTERSECTIONOF, RDF.TYPE, OWL.ANNOTATIONPROPERTY);
			manager.add(OWL.INTERSECTIONOF, OBJ.COMPONENT_TYPE, OWL.CLASS);
		}
		if (manager.contains(null, OWL.ONEOF, null) || manager.contains(null, OWL.HASVALUE, null)) {
			manager.add(OWL.ONEOF, RDF.TYPE, OWL.ANNOTATIONPROPERTY);
		}
		if (manager.contains(null, OWL.UNIONOF, null)) {
			manager.add(OWL.UNIONOF, RDF.TYPE, OWL.ANNOTATIONPROPERTY);
			manager.add(OWL.UNIONOF, OBJ.COMPONENT_TYPE, OWL.CLASS);
		}
		if (manager.contains(RDFS.LITERAL, null, null)) {
			manager.add(RDFS.LITERAL, RDF.TYPE, RDFS.DATATYPE);
		}
	}

	private Model match(Value subj, URI pred, Value obj) {
		if (subj instanceof Resource)
			return manager.match((Resource) subj, pred, obj);
		return new LinkedHashModel();
	}

	private boolean contains(Value subj, URI pred, Value obj) {
		if (subj instanceof Resource)
			return manager.contains((Resource) subj, pred, obj);
		return false;
	}

	private void infer() {
		logger.debug("inferring");
		ValueFactory vf = getValueFactory();
		propagateSubClassType(RDFS.CLASS);
		symmetric(OWL.INVERSEOF);
		symmetric(OWL.EQUIVALENTCLASS);
		symmetric(OWL.EQUIVALENTPROPERTY);
		symmetric(OWL.DISJOINTWITH);
		setSubjectType(RDF.FIRST, null, RDF.LIST);
		setSubjectType(RDF.REST, null, RDF.LIST);
		setSubjectType(RDFS.SUBCLASSOF, null, OWL.CLASS);
		setSubjectType(OWL.ONEOF, null, OWL.CLASS);
		setSubjectType(OWL.UNIONOF, null, OWL.CLASS);
		setSubjectType(OWL.DISJOINTWITH, null, OWL.CLASS);
		setSubjectType(OWL.COMPLEMENTOF, null, OWL.CLASS);
		setSubjectType(OWL.EQUIVALENTCLASS, null, OWL.CLASS);
		setSubjectType(OWL.INTERSECTIONOF, null, OWL.CLASS);
		setSubjectType(OWL.ONPROPERTY, null, OWL.RESTRICTION);
		setSubjectType(RDF.TYPE, RDFS.CLASS, OWL.CLASS);
		setSubjectType(RDF.TYPE, OWL.DEPRECATEDCLASS, OWL.CLASS);
		setSubjectType(RDF.TYPE, OWL.RESTRICTION, OWL.CLASS);
		setSubjectType(RDF.TYPE, OWL.ANNOTATIONPROPERTY, RDF.PROPERTY);
		setSubjectType(RDF.TYPE, OWL.DEPRECATEDPROPERTY, RDF.PROPERTY);
		setSubjectType(RDF.TYPE, OWL.OBJECTPROPERTY, RDF.PROPERTY);
		setSubjectType(RDF.TYPE, OWL.DATATYPEPROPERTY, RDF.PROPERTY);
		setSubjectType(RDF.TYPE, OWL.FUNCTIONALPROPERTY, RDF.PROPERTY);
		setObjectType(RDFS.SUBCLASSOF, OWL.CLASS);
		setObjectType(OWL.ALLVALUESFROM, OWL.CLASS);
		setObjectType(OWL.ONEOF, RDF.LIST);
		setObjectType(OWL.UNIONOF, RDF.LIST);
		setObjectType(OWL.INTERSECTIONOF, RDF.LIST);
		setObjectType(RDFS.ISDEFINEDBY, OWL.ONTOLOGY);
		setSubjectType(OWL.INVERSEOF, null, OWL.OBJECTPROPERTY);
		setObjectType(OWL.INVERSEOF, OWL.OBJECTPROPERTY);
		setObjectType(RDFS.RANGE, OWL.CLASS);
		setObjectType(RDFS.DOMAIN, OWL.CLASS);
		setSubjectType(RDFS.RANGE, null, RDF.PROPERTY);
		setSubjectType(RDFS.DOMAIN, null, RDF.PROPERTY);
		setSubjectType(RDFS.SUBPROPERTYOF, null, RDF.PROPERTY);
		setObjectType(RDFS.SUBPROPERTYOF, RDF.PROPERTY);
		setDatatype(vf, OWL.CARDINALITY, XMLSchema.NON_NEGATIVE_INTEGER);
		setDatatype(vf, OWL.MINCARDINALITY, XMLSchema.NON_NEGATIVE_INTEGER);
		setDatatype(vf, OWL.MAXCARDINALITY, XMLSchema.NON_NEGATIVE_INTEGER);
		setMemberType(OWL.UNIONOF, OWL.CLASS);
		setMemberType(OWL.INTERSECTIONOF, OWL.CLASS);
	}

	private void setMemberType(URI pred, URI type) {
		for (Value list : match(null, pred, null).objects()) {
			if (list instanceof Resource) {
				RDFList members = new RDFList(manager, (Resource) list);
				for (Value member : members.asList()) {
					if (member instanceof Resource) {
						manager.add((Resource) member, RDF.TYPE, type);
					}
				}
			}
		}
	}

	private Map<String, URI> findOntologies() {
		Map<String, URI> ontologies = new HashMap<String, URI>();
		assignOrphansToTheirOntology();
		findNamespacesOfOntologies(ontologies);
		assignOrphansToNewOntology(ontologies);
		return ontologies;
	}

	private void assignOrphansToTheirOntology() {
		for (Statement st : match(null, RDF.TYPE, null)) {
			Resource subj = st.getSubject();
			if (!contains(subj, RDFS.ISDEFINEDBY, null)) {
				if (st.getContext() == null)
					continue;
				for (Resource ont : manager.match(null, RDF.TYPE, OWL.ONTOLOGY,
						st.getContext()).subjects()) {
					logger.debug("assigning {} {}", subj, ont);
					manager.add(subj, RDFS.ISDEFINEDBY, ont);
				}
			}
		}
	}

	private void findNamespacesOfOntologies(Map<String, URI> ontologies) {
		for (Resource subj : match(null, RDF.TYPE, OWL.ONTOLOGY).subjects()) {
			if (subj instanceof BNode)
				continue;
			URI ont = (URI) subj;
			logger.debug("found ontology {}", ont);
			ontologies.put(ont.toString(), ont);
			ontologies.put(ont.getNamespace(), ont);
			ontologies.put(ont.toString() + '#', ont);
			Set<String> spaces = new HashSet<String>();
			for (Resource bean : match(null, RDFS.ISDEFINEDBY, ont).subjects()) {
				if (bean instanceof URI)
					spaces.add(((URI) bean).getNamespace());
			}
			if (spaces.size() > 0) {
				for (String ns : spaces) {
					ontologies.put(ns, ont);
				}
			} else {
				ontologies.put(guessNamespace(ont), ont);
			}
		}
	}

	private void assignOrphansToNewOntology(Map<String, URI> ontologies) {
		for (Resource subj : match(null, RDF.TYPE, null).subjects()) {
			if (subj instanceof URI && !contains(subj, RDFS.ISDEFINEDBY, null)) {
				URI uri = (URI) subj;
				String ns = uri.getNamespace();
				URI ont = findOntology(ns, ontologies);
				logger.debug("assigning {} {}", uri, ont);
				manager.add(uri, RDFS.ISDEFINEDBY, ont);
			}
		}
		loop: for (Statement st : match(null, RDF.TYPE, null)) {
			Resource subj = st.getSubject();
			if (!(subj instanceof URI)
					&& !contains(subj, RDFS.ISDEFINEDBY, null)) {
				for (Resource peer : manager.match(null, RDF.TYPE, null,
						st.getContext()).subjects()) {
					for (Value ont : match(peer, RDFS.ISDEFINEDBY, null)
							.objects()) {
						logger.debug("assigning {} {}", subj, ont);
						manager.add(subj, RDFS.ISDEFINEDBY, ont);
						continue loop;
					}
				}
			}
		}
	}

	private String guessNamespace(URI URI) {
		String ns = URI.getNamespace();
		String local = URI.getLocalName();
		if (local.endsWith("#") || local.endsWith("/")) {
			return ns + local;
		}
		if (ns.endsWith("#")) {
			return ns;
		}
		return ns + local + "#";
	}

	private URI findOntology(String ns, Map<String, URI> ontologies) {
		if (ontologies.containsKey(ns)) {
			return ontologies.get(ns);
		}
		for (Map.Entry<String, URI> e : ontologies.entrySet()) {
			String key = e.getKey();
			if (key.indexOf('#') > 0
					&& ns.startsWith(key.substring(0, key.indexOf('#'))))
				return e.getValue();
		}
		URI URI = new URIImpl(ns);
		if (ns.endsWith("#")) {
			URI = new URIImpl(ns.substring(0, ns.length() - 1));
		}
		ontologies.put(ns, URI);
		return URI;
	}

	private void propagateSubClassType(Resource classDef) {
		for (Resource c : findClasses(Collections.singleton(classDef))) {
			if (c.equals(RDFS.DATATYPE))
				continue;
			for (Statement stmt : match(null, RDF.TYPE, c)) {
				Resource subj = stmt.getSubject();
				manager.add(subj, RDF.TYPE, classDef);
			}
		}
	}

	private Set<Resource> findClasses(Collection<Resource> classes) {
		Set<Resource> set = new HashSet<Resource>(classes);
		for (Resource c : classes) {
			for (Statement stmt : match(null, RDFS.SUBCLASSOF, c)) {
				Resource subj = stmt.getSubject();
				set.add(subj);
			}
		}
		if (set.size() > classes.size()) {
			return findClasses(set);
		} else {
			return set;
		}
	}

	private void symmetric(URI pred) {
		for (Statement stmt : match(null, pred, null)) {
			if (stmt.getObject() instanceof Resource) {
				Resource subj = (Resource) stmt.getObject();
				manager.add(subj, pred, stmt.getSubject());
			} else {
				logger.warn("Invalid statement {}", stmt);
			}
		}
	}

	private void setSubjectType(URI pred, Value obj, URI type) {
		for (Statement stmt : match(null, pred, obj)) {
			manager.add(stmt.getSubject(), RDF.TYPE, type);
		}
	}

	private void setObjectType(URI pred, URI type) {
		for (Statement st : match(null, pred, null)) {
			if (st.getObject() instanceof Resource) {
				Resource subj = (Resource) st.getObject();
				manager.add(subj, RDF.TYPE, type);
			} else {
				logger.warn("Invalid statement {}", st);
			}
		}
	}

	private void setDatatype(ValueFactory vf, URI pred, URI datatype) {
		for (Statement stmt : match(null, pred, null)) {
			String label = ((Literal) stmt.getObject()).getLabel();
			Literal literal = vf.createLiteral(label, datatype);
			manager.remove(stmt.getSubject(), pred, stmt.getObject());
			manager.add(stmt.getSubject(), pred, literal);
		}
	}

	private void checkPropertyDomains() {
		loop: for (Statement st : match(null, RDF.TYPE, RDF.PROPERTY)) {
			Resource p = st.getSubject();
			if (!contains(p, RDFS.DOMAIN, null)) {
				for (Value sup : match(p, RDFS.SUBPROPERTYOF, null).objects()) {
					for (Value obj : match(sup, RDFS.DOMAIN, null).objects()) {
						manager.add(p, RDFS.DOMAIN, obj);
						continue loop;
					}
				}
				manager.add(p, RDFS.DOMAIN, RDFS.RESOURCE);
				if (!contains(RDFS.RESOURCE, RDF.TYPE, OWL.CLASS)) {
					manager.add(RDFS.RESOURCE, RDF.TYPE, OWL.CLASS);
				}
			}
		}
	}

	private void checkPropertyRanges() {
		loop: for (Statement st : match(null, RDF.TYPE, RDF.PROPERTY)) {
			Resource p = st.getSubject();
			if (!contains(p, RDFS.RANGE, null)) {
				for (Value sup : match(p, RDFS.SUBPROPERTYOF, null).objects()) {
					for (Value obj : match(sup, RDFS.RANGE, null).objects()) {
						manager.add(p, RDFS.RANGE, obj);
						continue loop;
					}
				}
				manager.add(p, RDFS.RANGE, RDFS.RESOURCE);
			}
		}
	}

	private void distributeSubMessage() {
		boolean changed = false;
		for (Resource msg : match(null, RDFS.SUBCLASSOF, MSG.MESSAGE)
				.subjects()) {
			for (Resource sub : match(null, RDFS.SUBCLASSOF, msg).subjects()) {
				if (!contains(sub, RDFS.SUBCLASSOF, MSG.MESSAGE)) {
					manager.add(sub, RDFS.SUBCLASSOF, MSG.MESSAGE);
					changed = true;
				}
			}
		}
		for (Resource msg : match(null, RDFS.SUBCLASSOF, OBJ.MESSAGE)
				.subjects()) {
			for (Resource sub : match(null, RDFS.SUBCLASSOF, msg).subjects()) {
				if (!contains(sub, RDFS.SUBCLASSOF, OBJ.MESSAGE)) {
					manager.add(sub, RDFS.SUBCLASSOF, OBJ.MESSAGE);
					changed = true;
				}
			}
		}
		if (changed) {
			distributeSubMessage();
		}
	}

	private void checkMessageTargets() {
		for (Resource msg : match(null, RDFS.SUBCLASSOF, MSG.MESSAGE)
				.subjects()) {
			getOrAddTargetRestriction(msg);
		}
		for (Resource msg : match(null, RDFS.SUBCLASSOF, OBJ.MESSAGE)
				.subjects()) {
			getOrAddTargetRestriction(msg);
		}
	}

	private Value getOrAddTargetRestriction(Resource msg) {
		for (Value res : match(msg, RDFS.SUBCLASSOF, null).objects()) {
			if (contains(res, OWL.ONPROPERTY, MSG.TARGET)
					|| contains(res, OWL.ONPROPERTY, OBJ.TARGET)) {
				return res;
			}
		}
		for (Value sup : match(msg, RDFS.SUBCLASSOF, null).objects()) {
			if (sup instanceof URI) {
				Value res = getOrAddTargetRestriction((URI) sup);
				if (res != null) {
					manager.add(msg, RDFS.SUBCLASSOF, res);
					return res;
				}
			}
		}
		ValueFactory vf = getValueFactory();
		BNode res = vf.createBNode();
		manager.add(msg, RDFS.SUBCLASSOF, res);
		manager.add(res, RDF.TYPE, OWL.RESTRICTION);
		manager.add(res, OWL.ONPROPERTY, MSG.TARGET);
		manager.add(res, OWL.ALLVALUESFROM, RDFS.RESOURCE);
		manager.add(RDFS.RESOURCE, RDF.TYPE, OWL.CLASS);
		return res;
	}

	private void addPrecedesToSubClasses() {
		for (Resource msg : match(null, RDFS.SUBCLASSOF, MSG.MESSAGE).subjects()) {
			for (Value of : match(msg, RDFS.SUBCLASSOF, null).objects()) {
				if (!MSG.MESSAGE.equals(of) && !OBJ.MESSAGE.equals(of) && of instanceof URI) {
					manager.add(msg, MSG.PRECEDES, of);
				}
			}
		}
		for (Resource msg : match(null, RDFS.SUBCLASSOF, OBJ.MESSAGE).subjects()) {
			for (Value of : match(msg, RDFS.SUBCLASSOF, null).objects()) {
				if (!MSG.MESSAGE.equals(of) && !OBJ.MESSAGE.equals(of) && of instanceof URI) {
					manager.add(msg, MSG.PRECEDES, of);
				}
			}
		}
	}

	private ValueFactory getValueFactory() {
		return ValueFactoryImpl.getInstance();
	}

	private void hasValueFromList() {
		ValueFactory vf = getValueFactory();
		for (Statement st : match(null, OWL.HASVALUE, null)) {
			Value obj = st.getObject();
			BNode node = vf.createBNode();
			manager.add(st.getSubject(), OWL.ALLVALUESFROM, node);
			manager.add(node, RDF.TYPE, OWL.CLASS);
			BNode list = vf.createBNode();
			manager.add(node, OWL.ONEOF, list);
			manager.add(list, RDF.TYPE, RDF.LIST);
			manager.add(list, RDF.FIRST, obj);
			manager.add(list, RDF.REST, RDF.NIL);
			for (Value type : match(obj, RDF.TYPE, null).objects()) {
				manager.add(node, RDFS.SUBCLASSOF, type);
			}
		}
	}

	private void subClassOneOf() {
		Set<Value> common = null;
		for (Resource subj : match(null, OWL.ONEOF, null).subjects()) {
			for (Value of : new RDFList(manager, match(subj, OWL.ONEOF, null)
					.objectResource()).asList()) {
				if (contains(of, RDF.TYPE, null)) {
					Set<Value> supers = new HashSet<Value>();
					Set<Value> types = match(of, RDF.TYPE, null).objects();
					for (Value type : types) {
						if (type instanceof Resource) {
							supers.addAll(findSuperClasses((Resource) type));
						}
					}
					if (common == null) {
						common = new HashSet<Value>(supers);
					} else {
						common.retainAll(supers);
					}
				} else {
					common = Collections.emptySet();
				}
			}
			if (common != null) {
				for (Value s : common) {
					manager.add(subj, RDFS.SUBCLASSOF, s);
				}
			}
		}
	}

	private void subClassIntersectionOf() {
		for (Resource subj : match(null, OWL.INTERSECTIONOF, null).subjects()) {
			for (Value of : match(subj, OWL.INTERSECTIONOF, null).objects()) {
				if (of instanceof Resource) {
					RDFList list = new RDFList(manager, (Resource) of);
					for (Value member : list.asList()) {
						manager.add(subj, RDFS.SUBCLASSOF, member);
					}
				}
			}
		}
	}

	private void renameAnonymousClasses() {
		for (Resource res : match(null, RDF.TYPE, OWL.CLASS).subjects()) {
			if (res instanceof URI)
				continue;
			// if not already moved
			nameAnonymous(res);
		}
	}

	private URI nameAnonymous(Resource clazz) {
		for (Value eq : match(clazz, OWL.EQUIVALENTCLASS, null).objects()) {
			if (eq instanceof URI) {
				rename(clazz, (URI) eq);
				return (URI) eq;
			}
		}
		Resource unionOf = match(clazz, OWL.UNIONOF, null).objectResource();
		if (unionOf != null) {
			return renameClass("", clazz, "Or", new RDFList(manager, unionOf)
					.asList());
		}
		Resource intersectionOf = match(clazz, OWL.INTERSECTIONOF, null)
				.objectResource();
		if (intersectionOf != null) {
			return renameClass("", clazz, "And", new RDFList(manager,
					intersectionOf).asList());
		}
		Resource oneOf = match(clazz, OWL.ONEOF, null).objectResource();
		if (oneOf != null) {
			return renameClass("Is", clazz, "Or", new RDFList(manager, oneOf)
					.asList());
		}
		Resource complement = match(clazz, OWL.COMPLEMENTOF, null)
				.objectResource();
		if (complement != null) {
			URI comp = complement instanceof URI ? (URI) complement : null;
			if (comp == null) {
				comp = nameAnonymous(complement);
				if (comp == null)
					return null;
			}
			String name = "Not" + comp.getLocalName();
			URI uri = new URIImpl(comp.getNamespace() + name);
			rename(clazz, uri);
			return uri;
		}
		if (contains(clazz, MSG.MATCHING, null)) {
			return renameClass("", clazz, "Or", match(clazz, MSG.MATCHING, null)
					.objects());
		} else if (contains(clazz, OBJ.MATCHES, null)) {
			return renameClass("", clazz, "Or", match(clazz, OBJ.MATCHES, null)
					.objects());
		}
		return null;
	}

	private void mergeDuplicateRestrictions() {
		Model model = match(null, OWL.ONPROPERTY, null);
		for (Statement st : model) {
			Value property = st.getObject();
			for (Resource r2 : model.filter(null, null, property).subjects()) {
				Resource r1 = st.getSubject();
				if (!r1.equals(r2)) {
					if (equivalent(r1, r2, 10)) {
						manager.add(r1, OWL.EQUIVALENTCLASS, r2);
						manager.add(r2, OWL.EQUIVALENTCLASS, r1);
					}
				}
			}
		}
	}

	private boolean equivalent(Value v1, Value v2, int depth) {
		if (depth < 0)
			return false;
		if (v1.equals(v2))
			return true;
		if (v1 instanceof Literal || v2 instanceof Literal)
			return false;
		Resource r1 = (Resource) v1;
		Resource r2 = (Resource) v2;
		if (contains(r1, OWL.EQUIVALENTCLASS, r2))
			return true;
		if (!equivalentObjects(r1, r2, OWL.ONPROPERTY, depth - 1))
			return false;
		if (equivalentObjects(r1, r2, OWL.HASVALUE, depth - 1))
			return true;
		if (equivalentObjects(r1, r2, OWL.ALLVALUESFROM, depth - 1))
			return true;
		if (equivalentObjects(r1, r2, OWL.SOMEVALUESFROM, depth - 1))
			return true;
		return false;
	}

	private boolean equivalentObjects(Resource r1, Resource r2, URI pred,
			int depth) {
		Set<Value> s1 = match(r1, pred, null).objects();
		Set<Value> s2 = match(r2, pred, null).objects();
		if (s1.isEmpty() || s2.isEmpty())
			return false;
		for (Value v1 : s1) {
			boolean equivalent = false;
			for (Value v2 : s2) {
				if (equivalent(v1, v2, depth - 1)) {
					equivalent = true;
					break;
				}
			}
			if (!equivalent)
				return false;
		}
		return true;
	}

	private void distributeEquivalentClasses() {
		for (Resource subj : match(null, RDF.TYPE, OWL.CLASS).subjects()) {
			for (Value equiv : match(subj, OWL.EQUIVALENTCLASS, null).objects()) {
				for (Value v : match(equiv, OWL.EQUIVALENTCLASS, null)
						.objects()) {
					manager.add(subj, OWL.EQUIVALENTCLASS, v);
				}
			}
			manager.remove(subj, OWL.EQUIVALENTCLASS, subj);
		}
		for (Resource subj : match(null, RDF.TYPE, OWL.CLASS).subjects()) {
			if (!(subj instanceof URI))
				continue;
			for (Value e : match(subj, OWL.EQUIVALENTCLASS, null).objects()) {
				for (Value d : match(e, OWL.DISJOINTWITH, null).objects()) {
					manager.add(subj, OWL.DISJOINTWITH, d);
				}
				if (contains(e, OWL.INTERSECTIONOF, null)) {
					Resource cinter = match(subj, OWL.INTERSECTIONOF, null)
							.objectResource();
					Resource inter = match(e, OWL.INTERSECTIONOF, null)
							.objectResource();
					if (cinter == null) {
						manager.add(subj, OWL.INTERSECTIONOF, inter);
					} else if (!inter.equals(cinter)) {
						new RDFList(manager, cinter).addAllOthers(new RDFList(
								manager, inter));
					}
				}
				if (contains(e, OWL.ONEOF, null)) {
					Resource co = match(subj, OWL.ONEOF, null).objectResource();
					Resource eo = match(e, OWL.ONEOF, null).objectResource();
					if (co == null) {
						manager.add(subj, OWL.ONEOF, match(e, OWL.ONEOF, null)
								.objectResource());
					} else if (!eo.equals(co)) {
						new RDFList(manager, co).addAllOthers(new RDFList(
								manager, eo));
					}
				}
				if (contains(e, OWL.UNIONOF, null)) {
					for (Value elist : match(e, OWL.UNIONOF, null).objects()) {
						if (!contains(subj, OWL.UNIONOF, null)) {
							manager.add(subj, OWL.UNIONOF, elist);
						} else if (!contains(subj, OWL.UNIONOF, elist)) {
							for (Value clist : match(subj, OWL.UNIONOF, null)
									.objects()) {
								new RDFList(manager, (Resource) clist)
										.addAllOthers(new RDFList(manager,
												(Resource) elist));
							}
						}
					}
				}
				if (contains(e, OWL.COMPLEMENTOF, null)) {
					if (!contains(subj, OWL.COMPLEMENTOF, null)) {
						Resource comp = match(e, OWL.COMPLEMENTOF, null)
								.objectResource();
						manager.add(subj, OWL.COMPLEMENTOF, comp);
					}
				}
				if (contains(e, OWL.DISJOINTWITH, null)) {
					for (Value d : match(e, OWL.DISJOINTWITH, null).objects()) {
						manager.add(subj, OWL.DISJOINTWITH, d);
					}
				}
				if (contains(e, RDFS.SUBCLASSOF, null)) {
					for (Value d : match(e, RDFS.SUBCLASSOF, null).objects()) {
						manager.add(subj, RDFS.SUBCLASSOF, d);
					}
				}
				if (contains(e, RDF.TYPE, OWL.RESTRICTION)) {
					manager.add(subj, RDFS.SUBCLASSOF, e);
				}
			}
		}
	}

	private void mergeUnionClasses() {
		for (Resource subj : match(null, RDF.TYPE, OWL.CLASS).subjects()) {
			List<Value> unionOf = new ArrayList<Value>();
			for (Value obj : match(subj, OWL.UNIONOF, null).objects()) {
				if (obj instanceof Resource) {
					List<? extends Value> list = new RDFList(manager,
							(Resource) obj).asList();
					list.removeAll(unionOf);
					unionOf.addAll(list);
				}
			}
			if (!unionOf.isEmpty()) {
				Set<URI> common = findCommonSupers(unionOf);
				if (common.contains(subj)) {
					// if union contains itself then remove it
					manager.remove(subj, OWL.UNIONOF, null);
					continue;
				} else if (findCommon(common, unionOf) != null) {
					// if union includes the common super class then fold
					// together
					URI sup = findCommon(common, unionOf);
					manager.remove(subj, OWL.UNIONOF, null);
					rename(subj, sup);
					continue;
				}
				for (URI c : common) {
					manager.add(subj, RDFS.SUBCLASSOF, c);
				}
				for (Value ofValue : unionOf) {
					if (contains(ofValue, RDF.TYPE, RDFS.DATATYPE)
							&& ofValue instanceof URI) {
						// don't use anonymous class for datatypes
						rename(subj, (URI) ofValue);
					} else {
						manager.add((Resource) ofValue, RDFS.SUBCLASSOF, subj);
					}
				}
			}
		}
	}

	private URI findCommon(Set<URI> common, Collection<? extends Value> unionOf) {
		URI result = null;
		for (Value e : unionOf) {
			if (common.contains(e)) {
				result = (URI) e;
			}
		}
		return result;
	}

	private Set<URI> findCommonSupers(List<? extends Value> unionOf) {
		Set<? extends Value> common = null;
		for (Value of : unionOf) {
			if (of instanceof Resource) {
				Set<Value> supers = findSuperClasses((Resource) of);
				if (common == null) {
					common = new HashSet<Value>(supers);
				} else {
					common.retainAll(supers);
				}
			}
		}
		if (common == null)
			return Collections.emptySet();
		Iterator<? extends Value> iter = common.iterator();
		while (iter.hasNext()) {
			if (!(iter.next() instanceof URI)) {
				iter.remove();
			}
		}
		return (Set<URI>) common;
	}

	private Set<Value> findSuperClasses(Resource of) {
		HashSet<Value> set = new HashSet<Value>();
		set.add(of);
		return findSuperClasses(of, set);
	}

	private Set<Value> findSuperClasses(Resource of, Set<Value> supers) {
		Set<Value> parent = match(of, RDFS.SUBCLASSOF, null).objects();
		if (supers.addAll(parent)) {
			for (Value s : parent) {
				if (s instanceof Resource) {
					findSuperClasses((Resource) s, supers);
				}
			}
		}
		return supers;
	}

	private URI renameClass(String prefix, Resource clazz, String and,
			Collection<? extends Value> list) {
		String namespace = null;
		Set<String> names = new TreeSet<String>();
		Set<String> others = new TreeSet<String>();
		for (Value of : list) {
			URI uri = null;
			if (of instanceof URI) {
				uri = (URI) of;
			} else if (of instanceof Literal) {
				String label = of.stringValue();
				StringBuilder sb = new StringBuilder();
				if (!label.contains(":")) {
					sb.append(getMatchNamespace(clazz));
				}
				if (label.startsWith("*")) {
					sb.append(label.replace("*", "Star"));
				} else if (label.endsWith("*")) {
					sb.append(label, 0, label.length() - 1);
				} else {
					sb.append(label);
				}
				if (label.startsWith("/")) {
					sb.append("Path");
				}
				if (label.endsWith("*")) {
					sb.append("Prefix");
				} else if (label.startsWith("*")) {
					sb.append("Suffix");
				}
				uri = new URIImpl(sb.toString());
			} else if (contains(of, RDF.TYPE, OWL.CLASS)) {
				uri = nameAnonymous((Resource) of);
			}
			if (uri != null && (namespace == null || commonNS.contains(namespace))) {
				namespace = uri.getNamespace();
			}
			if (uri == null) {
				others.add(of.stringValue());
			} else if (uri.getLocalName().length() > 0) {
				names.add(uri.getLocalName());
			} else {
				String str = uri.stringValue();
				Matcher m = Pattern.compile("\\b[a-zA-Z]\\w*\\b").matcher(str);
				while (m.find()) {
					str = m.group();
				}
				names.add(str);
			}
		}
		if (names.isEmpty())
			return null;
		StringBuilder sb = new StringBuilder();
		sb.append(prefix);
		for (String localPart : names) {
			sb.append(initcap(localPart));
			sb.append(and);
		}
		for (String localPart : others) {
			sb.append(initcap(localPart));
			sb.append(and);
		}
		sb.setLength(sb.length() - and.length());
		URIImpl dest = new URIImpl(namespace + sb.toString());
		rename(clazz, dest);
		return dest;
	}

	private CharSequence getMatchNamespace(Resource clazz) {
		for (Value ont : match(clazz, RDFS.ISDEFINEDBY, null).objects()) {
			if (ont instanceof URI) {
				return getMatchNamespace((URI) ont);
			}
		}
		for (Value ctx : match(clazz, null, null).contexts()) {
			if (ctx instanceof URI) {
				return getMatchNamespace((URI) ctx);
			}
		}
		// this shouldn't happen, but just in case
		return "urn:matches:";
	}

	private CharSequence getMatchNamespace(URI ontology) {
		StringBuilder sb = new StringBuilder();
		ParsedURI parsed = new ParsedURI(ontology.stringValue());
		if (parsed.getScheme() != null) {
			sb.append(parsed.getScheme());
			sb.append(':');
		}
		if (parsed.isOpaque()) {
			if (parsed.getSchemeSpecificPart() != null) {
				sb.append(parsed.getSchemeSpecificPart());
			}
		} else {
			if (parsed.getAuthority() != null) {
				sb.append("//");
				sb.append(parsed.getAuthority());
			}
			sb.append(parsed.getPath());
		}
		return sb;
	}

	private void rename(Resource orig, URI dest) {
		if (contains(dest, RDF.TYPE, OWL.CLASS)) {
			logger.debug("merging {} {}", orig, dest);
		} else {
			logger.debug("renaming {} {}", orig, dest);
			manager.add(dest, RDF.TYPE, OWL.CLASS);
			if (!contains(orig, RDFS.ISDEFINEDBY, null)) {
				URI ont = findOntology(dest.getNamespace(), ontologies);
				manager.add(dest, RDFS.ISDEFINEDBY, ont);
			}
			anonymousClasses.add(dest);
		}
		for (Statement stmt : match(orig, null, null)) {
			manager.add(dest, stmt.getPredicate(), stmt.getObject());
		}
		manager.remove(orig, null, null);
		for (Statement stmt : match(null, null, orig)) {
			manager.add(stmt.getSubject(), stmt.getPredicate(), dest);
		}
		manager.remove((Resource) null, null, orig);
	}

	private String initcap(String str) {
		if (str.length() < 2)
			return str.toUpperCase();
		return str.substring(0, 1).toUpperCase() + str.substring(1);
	}

	private void renameDeprecatedNamespaces() {
		renameAnnotation(OBJ.NAMESPACE, MSG.NAMESPACE, "precedes",
				"triggeredBy");
		String http = "http://www.openrdf.org/rdf/2009/httpobject#";
		renameAnnotation(http, MSG.NAMESPACE, "header", "rel", "title", "type",
				"method", "realm", "transform", "expect");
	}

	private void renameAnnotation(String from, String to,
			String... part) {
		for (String local : part) {
			URI before = getValueFactory().createURI(from, local);
			URI after = getValueFactory().createURI(to, local);
			for (Statement st : manager.match(null, before, null)) {
				manager.remove(st.getSubject(), before, st.getObject());
				manager.add(st.getSubject(), after, st.getObject());
			}
		}
	}
}
