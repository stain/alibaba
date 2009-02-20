/*
 * Copyright (c) 2007, James Leigh All rights reserved.
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
package org.openrdf.elmo.codegen;

import info.aduna.iteration.CloseableIteration;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.namespace.QName;

import org.openrdf.concepts.owl.Class;
import org.openrdf.concepts.owl.Ontology;
import org.openrdf.concepts.owl.Thing;
import org.openrdf.concepts.rdf.List;
import org.openrdf.concepts.rdf.Property;
import org.openrdf.concepts.rdfs.Datatype;
import org.openrdf.elmo.Entity;
import org.openrdf.elmo.exceptions.ElmoIOException;
import org.openrdf.elmo.sesame.SesameManager;
import org.openrdf.elmo.sesame.SesameQuery;
import org.openrdf.elmo.sesame.roles.SesameEntity;
import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.Namespace;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.model.vocabulary.XMLSchema;
import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.contextaware.ContextAwareConnection;
import org.openrdf.rio.rdfxml.util.OrganizedRDFXMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Applies a series of rules against the ontology, making it easier to convert
 * into Java classes. This includes appling some OWL reasoning on properties,
 * renaming anonymous and foriegn classes.
 * 
 * @author James Leigh
 * 
 */
public class OwlNormalizer {
	private final Logger logger = LoggerFactory.getLogger(OwlNormalizer.class);

	private static final String PREFIX = "PREFIX rdf: <" + RDF.NAMESPACE
			+ "> PREFIX rdfs: <" + RDFS.NAMESPACE + "> PREFIX owl: <"
			+ OWL.NAMESPACE + "> ";

	private static final String FIND_PROPERTIES_WO_DOMAIN = PREFIX
			+ "SELECT ?prop "
			+ "WHERE {  { ?prop a rdf:Property } "
			+ "UNION { ?prop a owl:ObjectProperty } UNION { ?prop a owl:DatatypeProperty } "
			+ "UNION { ?prop a owl:DeprecatedProperty } UNION { ?prop a owl:AnnotationProperty } "
			+ "OPTIONAL { ?prop rdfs:domain ?domain } "
			+ "FILTER (!bound(?domain)) }";

	private static final String SELECT_DEFINED = PREFIX + "SELECT ?bean "
			+ "WHERE { ?bean rdfs:isDefinedBy ?ont "
			+ "FILTER ( ?bean != ?ont ) } ";

	private static final String FIND_ORPHANS_ONTOLOGY = PREFIX
			+ "SELECT DISTINCT ?bean ?ontology"
			+ " WHERE { GRAPH ?graph { ?bean a ?type. "
			+ " ?ontology a owl:Ontology "
			+ " OPTIONAL { ?bean rdfs:isDefinedBy ?ont }"
			+ " FILTER ( isURI(?bean) && ! bound(?ont) ) }}";

	private static final String SELECT_ORPHANS = PREFIX
			+ "SELECT DISTINCT ?bean" + " WHERE { ?bean rdf:type ?type"
			+ " OPTIONAL { ?bean rdfs:isDefinedBy ?ont }"
			+ " FILTER ( isURI(?bean) && ! bound(?ont) ) }";

	private static final String BEAN_DEFINED_BY = PREFIX
			+ "SELECT ?bean WHERE { ?bean rdfs:isDefinedBy ?ont }";

	private static final String SELECT_C_INTERSECTION_OF = PREFIX
			+ "SELECT ?c WHERE { ?c owl:intersectionOf ?l }";

	private static final String SELECT_C_ONE_OF = PREFIX
			+ "SELECT ?c WHERE { ?c owl:oneOf ?l }";

	private SesameManager manager;

	private Set<URI> anonymousClasses = new HashSet<URI>();

	private Map<URI, URI> aliases = new HashMap<URI, URI>();

	private Map<String, Ontology> ontologies;

	private Set<String> commonNS = new HashSet<String>(Arrays.asList(
			RDF.NAMESPACE, RDFS.NAMESPACE, OWL.NAMESPACE));

	private static final Pattern NS_PREFIX = Pattern
			.compile("^.*[/#](\\w+)[/#]?$");

	public void setSesameManager(SesameManager manager) {
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

	public void normalize() throws Exception {
		infer(manager.getConnection());
		ontologies = findOntologies();
		checkNamespacePrefixes();
		checkPropertyDomains();
		subClassIntersectionOf();
		subClassOneOf();
		renameAnonymousClasses();
		distributeEquivalentClasses();
		mergeUnionClasses();
		moveForiegnDomains(manager.getConnection());
		if (logger.isDebugEnabled()) {
			File file = File.createTempFile("normalized", ".rdf");
			manager.getConnection().export(new OrganizedRDFXMLWriter(file));
			logger.debug("Normalized RDF saved to {}", file);
		}
	}

	private void infer(ContextAwareConnection conn) throws RepositoryException {
		logger.debug("inferring");
		ValueFactory vf = conn.getRepository().getValueFactory();
		propagateSubClassType(conn, RDFS.CLASS);
		symmetric(conn, OWL.INVERSEOF);
		symmetric(conn, OWL.EQUIVALENTCLASS);
		symmetric(conn, OWL.EQUIVALENTPROPERTY);
		symmetric(conn, OWL.DISJOINTWITH);
		setSubjectType(conn, RDF.FIRST, null, RDF.LIST);
		setSubjectType(conn, RDF.REST, null, RDF.LIST);
		setSubjectType(conn, OWL.UNIONOF, null, OWL.CLASS);
		setSubjectType(conn, OWL.DISJOINTWITH, null, OWL.CLASS);
		setSubjectType(conn, OWL.COMPLEMENTOF, null, OWL.CLASS);
		setSubjectType(conn, OWL.EQUIVALENTCLASS, null, OWL.CLASS);
		setSubjectType(conn, OWL.INTERSECTIONOF, null, OWL.CLASS);
		setSubjectType(conn, RDF.TYPE, RDFS.CLASS, OWL.CLASS);
		setSubjectType(conn, RDF.TYPE, OWL.DEPRECATEDCLASS, OWL.CLASS);
		setObjectType(conn, RDFS.SUBCLASSOF, OWL.CLASS);
		setObjectType(conn, OWL.UNIONOF, RDF.LIST);
		setObjectType(conn, RDFS.ISDEFINEDBY, OWL.ONTOLOGY);
		setSubjectType(conn, OWL.INVERSEOF, null, OWL.OBJECTPROPERTY);
		setObjectType(conn, OWL.INVERSEOF, OWL.OBJECTPROPERTY);
		setSubjectType(conn, RDFS.RANGE, null, RDF.PROPERTY);
		setSubjectType(conn, RDFS.DOMAIN, null, RDF.PROPERTY);
		setObjectType(conn, RDFS.SUBPROPERTYOF, RDF.PROPERTY);
		setDatatype(vf, conn, OWL.CARDINALITY, XMLSchema.NON_NEGATIVE_INTEGER);
		setDatatype(vf, conn, OWL.MINCARDINALITY,
				XMLSchema.NON_NEGATIVE_INTEGER);
		setDatatype(vf, conn, OWL.MAXCARDINALITY,
				XMLSchema.NON_NEGATIVE_INTEGER);
	}

	private Map<String, Ontology> findOntologies()
			throws MalformedQueryException, RepositoryException,
			QueryEvaluationException {
		Map<String, Ontology> ontologies = new HashMap<String, Ontology>();
		ContextAwareConnection conn = manager.getConnection();
		assignOrphansToTheirOntology(conn, ontologies);
		findNamespacesOfOntologies(conn, ontologies);
		assignOrphansToNewOntology(conn, ontologies);
		return ontologies;
	}

	private void assignOrphansToTheirOntology(ContextAwareConnection conn,
			Map<String, Ontology> ontologies) throws MalformedQueryException,
			RepositoryException, QueryEvaluationException {
		TupleQuery query;
		query = conn.prepareTupleQuery(FIND_ORPHANS_ONTOLOGY);
		TupleQueryResult result = query.evaluate();
		try {
			while (result.hasNext()) {
				BindingSet tuple = result.next();
				URI uri = (URI) tuple.getValue("bean");
				
				if (!conn.hasStatement(uri, RDFS.ISDEFINEDBY, null)) {
					URI ontology = (URI) tuple.getValue("ontology");
					logger.debug("assigning {} {}", uri, ontology);
					conn.add(uri, RDFS.ISDEFINEDBY, ontology);
				}
			}
		} finally {
			result.close();
		}
	}

	private void findNamespacesOfOntologies(ContextAwareConnection conn,
			Map<String, Ontology> ontologies) throws MalformedQueryException,
			RepositoryException, QueryEvaluationException {
		TupleQuery query = conn.prepareTupleQuery(BEAN_DEFINED_BY);
		for (Ontology ont : manager.findAll(Ontology.class)) {
			logger.debug("found ontology {}", ont);
			ontologies.put(ont.toString(), ont);
			ontologies.put(ont.getQName().getNamespaceURI(), ont);
			ontologies.put(ont.toString() + '#', ont);
			Set<String> spaces = new HashSet<String>();
			query.setBinding("ont", ((SesameEntity) ont).getSesameResource());
			TupleQueryResult result = query.evaluate();
			try {
				while (result.hasNext()) {
					BindingSet tuple = result.next();
					Value bean = tuple.getBinding("bean").getValue();
					if (bean instanceof URI)
						spaces.add(((URI) bean).getNamespace());
				}
			} finally {
				result.close();
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

	private void assignOrphansToNewOntology(ContextAwareConnection conn,
			Map<String, Ontology> ontologies) throws MalformedQueryException,
			RepositoryException, QueryEvaluationException {
		TupleQuery query;
		query = conn.prepareTupleQuery(SELECT_ORPHANS);
		TupleQueryResult result = query.evaluate();
		try {
			while (result.hasNext()) {
				BindingSet tuple = result.next();
				URI uri = (URI) tuple.getValue("bean");
				Resource ontology = (Resource) tuple.getValue("ontology");
				if (true || ontology == null) {
					String ns = uri.getNamespace();
					Ontology ont = findOntology(ns, ontologies);
					ontology = ((SesameEntity) ont).getSesameResource();
				}
				logger.debug("assigning {} {}", uri, ontology);
				conn.add(uri, RDFS.ISDEFINEDBY, ontology);
			}
		} finally {
			result.close();
		}
	}

	private String guessNamespace(Entity bean) {
		QName qname = bean.getQName();
		String ns = qname.getNamespaceURI();
		String local = qname.getLocalPart();
		if (local.endsWith("#") || local.endsWith("/")) {
			return ns + local;
		}
		if (ns.endsWith("#")) {
			return ns;
		}
		return ns + local + "#";
	}

	private Ontology findOntology(String ns, Map<String, Ontology> ontologies) {
		if (ontologies.containsKey(ns)) {
			return ontologies.get(ns);
		}
		for (Map.Entry<String, Ontology> e : ontologies.entrySet()) {
			String key = e.getKey();
			if (key.indexOf('#') > 0
					&& ns.startsWith(key.substring(0, key.indexOf('#'))))
				return e.getValue();
		}
		QName qname = new QName(ns);
		if (ns.endsWith("#")) {
			qname = new QName(ns.substring(0, ns.length() - 1));
		}
		Ontology ont = manager.designate(qname, Ontology.class);
		ontologies.put(ns, ont);
		return ont;
	}

	private void propagateSubClassType(ContextAwareConnection conn,
			Resource classDef) throws RepositoryException {
		CloseableIteration<? extends Statement, RepositoryException> stmts;
		for (Resource c : findClasses(conn, Collections.singleton(classDef))) {
			if (c.equals(RDFS.DATATYPE))
				continue;
			stmts = conn.getStatements(null, RDF.TYPE, c);
			try {
				while (stmts.hasNext()) {
					Statement stmt = stmts.next();
					Resource subj = stmt.getSubject();
					conn.add(subj, RDF.TYPE, classDef);
				}
			} finally {
				stmts.close();
			}
		}
	}

	private Set<Resource> findClasses(ContextAwareConnection conn,
			Collection<Resource> classes) throws RepositoryException {
		CloseableIteration<? extends Statement, RepositoryException> stmts;
		Set<Resource> set = new HashSet<Resource>(classes);
		for (Resource c : classes) {
			stmts = conn.getStatements(null, RDFS.SUBCLASSOF, c);
			try {
				while (stmts.hasNext()) {
					Statement stmt = stmts.next();
					Resource subj = stmt.getSubject();
					set.add(subj);
				}
			} finally {
				stmts.close();
			}
		}
		if (set.size() > classes.size()) {
			return findClasses(conn, set);
		} else {
			return set;
		}
	}

	private void symmetric(ContextAwareConnection conn, URI pred)
			throws RepositoryException {
		CloseableIteration<? extends Statement, RepositoryException> stmts;
		stmts = conn.getStatements(null, pred, null);
		try {
			while (stmts.hasNext()) {
				Statement stmt = stmts.next();
				if (stmt.getObject() instanceof Resource) {
					Resource subj = (Resource) stmt.getObject();
					conn.add(subj, pred, stmt.getSubject());
				} else {
					logger.warn("Invalid statement {}", stmt);
				}
			}
		} finally {
			stmts.close();
		}
	}

	private void setSubjectType(ContextAwareConnection conn, URI pred,
			Value obj, URI type) throws RepositoryException {
		CloseableIteration<? extends Statement, RepositoryException> stmts;
		stmts = conn.getStatements(null, pred, obj);
		try {
			while (stmts.hasNext()) {
				conn.add(stmts.next().getSubject(), RDF.TYPE, type);
			}
		} finally {
			stmts.close();
		}
	}

	private void setObjectType(ContextAwareConnection conn, URI pred, URI type)
			throws RepositoryException {
		CloseableIteration<? extends Statement, RepositoryException> stmts;
		stmts = conn.getStatements(null, pred, null);
		try {
			while (stmts.hasNext()) {
				Statement st = stmts.next();
				if (st.getObject() instanceof Resource) {
					Resource subj = (Resource) st.getObject();
					conn.add(subj, RDF.TYPE, type);
				} else {
					logger.warn("Invalid statement {}", st);
				}
			}
		} finally {
			stmts.close();
		}
	}

	private void addBaseClass(Class base, java.lang.Class<Class> type)
			throws RepositoryException {
		SesameQuery query = manager.createQuery(BEAN_DEFINED_BY);
		for (Object ont : base.getRdfsIsDefinedBy()) {
			query.setParameter("ont", ont);
			for (Object o : query.getResultList()) {
				if (o instanceof Class && !o.equals(base)) {
					Class c = (Class) o;
					boolean isBase = true;
					for (org.openrdf.concepts.rdfs.Class e : c
							.getRdfsSubClassOf()) {
						if (e.getRdfsIsDefinedBy().contains(ont)) {
							isBase = false;
						}
					}
					if (isBase) {
						logger.debug("extending {} {}", c, base);
						c.getRdfsSubClassOf().add(base);
					}
				}
			}
		}
	}

	private void setDatatype(ValueFactory vf, ContextAwareConnection conn,
			URI pred, URI datatype) throws RepositoryException {
		CloseableIteration<? extends Statement, RepositoryException> stmts;
		stmts = conn.getStatements(null, pred, null);
		try {
			while (stmts.hasNext()) {
				Statement stmt = stmts.next();
				String label = ((Literal) stmt.getObject()).getLabel();
				Literal literal = vf.createLiteral(label, datatype);
				conn.remove(stmt);
				conn.add(stmt.getSubject(), stmt.getPredicate(), literal);
			}
		} finally {
			stmts.close();
		}
	}

	private void checkPropertyDomains() {
		SesameQuery query = manager.createQuery(FIND_PROPERTIES_WO_DOMAIN);
		java.util.List<Property> list = query.getResultList();
		for (Property p : list) {
			boolean found = false;
			for (Property sup : p.getRdfsSubPropertyOf()) {
				if (!sup.getRdfsDomains().isEmpty()) {
					found = true;
					p.getRdfsDomains().addAll(sup.getRdfsDomains());
				}
			}
			if (!found) {
				Class res = manager.designate(RDFS.RESOURCE, Class.class);
				p.getRdfsDomains().add(res);
			}
		}
	}

	private void moveForiegnDomains(ContextAwareConnection conn)
			throws RepositoryException {
		CloseableIteration<? extends Statement, RepositoryException> stmts;
		stmts = conn.getStatements(null, RDFS.DOMAIN, null);
		try {
			while (stmts.hasNext()) {
				Statement stmt = stmts.next();
				if (stmt.getSubject() instanceof URI
						&& stmt.getObject() instanceof URI) {
					URI subj = (URI) stmt.getSubject();
					URI obj = (URI) stmt.getObject();
					for (Map.Entry<String, Ontology> e : ontologies.entrySet()) {
						String ns = e.getKey();
						Ontology ont = e.getValue();
						if (isInOntology(conn, subj, ns, ont)
								&& !isInSameOntology(conn, subj, obj)) {
							URI nc = createLocalClass(obj, ont);
							logger.debug("moving {} {}", subj, nc);
							conn.remove(stmt);
							conn.add(subj, RDFS.DOMAIN, nc);
							conn.add(nc, RDF.TYPE, OWL.CLASS);
							conn.add(nc, RDFS.SUBCLASSOF, obj);
							conn.add(nc, RDFS.ISDEFINEDBY, ((SesameEntity) ont)
									.getSesameResource());
						}
					}
				}
			}
		} finally {
			stmts.close();
		}
	}

	private boolean isInOntology(ContextAwareConnection conn, URI subj,
			String ns, Ontology ont) throws RepositoryException {
		if (subj.getNamespace().equals(ns))
			return true;
		Resource ontURI = ((SesameEntity) ont).getSesameResource();
		return conn.hasStatement(subj, RDFS.ISDEFINEDBY, ontURI);
	}

	private boolean isInSameOntology(ContextAwareConnection conn, URI subj,
			URI obj) throws RepositoryException {
		if (subj.getNamespace().equals(obj.getNamespace()))
			return true;
		RepositoryResult<Statement> stmts = conn.getStatements(subj,
				RDFS.ISDEFINEDBY, null);
		try {
			while (stmts.hasNext()) {
				if (conn.hasStatement(obj, RDFS.ISDEFINEDBY, stmts.next()
						.getObject()))
					return true;
			}
		} finally {
			stmts.close();
		}
		return false;
	}

	private URI createLocalClass(URI obj, Ontology ont)
			throws RepositoryException {
		String localName = obj.getLocalName();
		ValueFactory vf = getValueFactory();
		String prefix = findPrefix(ont);
		if (prefix != null)
			localName = initcap(prefix) + initcap(localName);
		URI nc = vf.createURI(findNamespace(ont), localName);
		aliases.put(nc, obj);
		if (obj.equals(RDFS.RESOURCE)) {
			Class base = manager.designate(nc, Class.class);
			base.getRdfsIsDefinedBy().add(ont);
			addBaseClass(base, Class.class);
		}
		return nc;
	}

	private ValueFactory getValueFactory() {
		return manager.getConnection().getRepository().getValueFactory();
	}

	private String findPrefix(Ontology ont) throws RepositoryException {
		RepositoryResult<Namespace> spaces;
		spaces = manager.getConnection().getNamespaces();
		try {
			while (spaces.hasNext()) {
				Namespace next = spaces.next();
				if (next.getName().equals(ont.getQName().getNamespaceURI()))
					return next.getPrefix();
				for (Map.Entry<String, Ontology> e : ontologies.entrySet()) {
					if (e.getValue().equals(ont)
							&& next.getName().equals(e.getKey()))
						return next.getPrefix();
				}
			}
		} finally {
			spaces.close();
		}
		return null;
	}

	private String findNamespace(Ontology ont) throws RepositoryException {
		String prefix = findPrefix(ont);
		if (prefix != null) {
			String ns = manager.getConnection().getNamespace(prefix);
			if (ns.endsWith("#") || ns.endsWith("/") || ns.endsWith(":"))
				return ns;
			if (ns.contains("#"))
				return ns.substring(0, ns.indexOf('#') + 1);
			return ns + "#";
		}
		String ns = ont.toString();
		if (ns.contains("#"))
			return ns.substring(0, ns.indexOf('#') + 1);
		return ont.toString() + '#';
	}

	private void renameClass(ContextAwareConnection conn, URI obj, URI nc)
			throws RepositoryException {
		logger.debug("renaming {} {}", obj, nc);
		aliases.put(nc, obj);
		CloseableIteration<? extends Statement, RepositoryException> stmts;
		stmts = conn.getStatements(null, null, obj);
		try {
			while (stmts.hasNext()) {
				Statement stmt = stmts.next();
				if (isLocal(nc, stmt.getSubject())) {
					if (!stmt.getPredicate().equals(RDFS.RANGE)
							|| !stmt.getObject().equals(RDFS.RESOURCE)) {
						if (!stmt.getPredicate().equals(RDF.TYPE))
							conn.remove(stmt);
						conn.add(stmt.getSubject(), stmt.getPredicate(), nc);
					}
				}
			}
		} finally {
			stmts.close();
		}
		if (obj.equals(RDFS.RESOURCE)) {
			Class base = manager.designate(nc, Class.class);
			addBaseClass(base, Class.class);
		}
	}

	private boolean isLocal(Resource nc, Resource obj)
			throws RepositoryException {
		if (obj instanceof BNode)
			return true;
		if (nc instanceof BNode)
			return true;
		return isInSameOntology(manager.getConnection(), (URI) nc, (URI) obj);
	}

	private void subClassIntersectionOf() {
		SesameQuery query = manager.createQuery(SELECT_C_INTERSECTION_OF);
		java.util.List<Class> classes = query.getResultList();
		for (Class c : classes) {
			for (Class of : c.getOwlIntersectionOf()) {
				c.getRdfsSubClassOf().add(of);
			}
		}
	}

	private void subClassOneOf() {
		SesameQuery query = manager.createQuery(SELECT_C_ONE_OF);
		java.util.List<Class> classes = query.getResultList();
		for (Class c : classes) {
			java.util.List list = new ArrayList();
			for (Object of : c.getOwlOneOf()) {
				if (of instanceof Thing) {
					Set types = ((Thing) of).getRdfTypes();
					if (types.isEmpty()) {
						list.add(manager
								.find(new QName(OWL.NAMESPACE, "Thing")));
					} else {
						list.addAll(types);
					}
				}
			}
			for (Class s : findCommonSupers(list)) {
				c.getRdfsSubClassOf().add(s);
			}
		}
	}

	private void renameAnonymousClasses() throws RepositoryException {
		Iterable<? extends Entity> classes = manager.findAll(Class.class);
		for (Entity clazz : classes) {
			Resource res = ((SesameEntity) clazz).getSesameResource();
			if (res instanceof URI)
				continue;
			// if not already moved
			if (clazz instanceof Class)
				nameAnonymous((Class) clazz);
		}
	}

	private Class nameAnonymous(Class clazz) throws RepositoryException {
		List<Class> unionOf = clazz.getOwlUnionOf();
		if (unionOf != null) {
			return renameClass(clazz, "Or", unionOf);
		}
		List<Class> intersectionOf = clazz.getOwlIntersectionOf();
		if (intersectionOf != null) {
			return renameClass(clazz, "And", intersectionOf);
		}
		List oneOf = clazz.getOwlOneOf();
		if (oneOf != null) {
			boolean things = true;
			for (Object of : oneOf) {
				things &= of instanceof Thing;
			}
			if (things)
				return renameClass(clazz, "Or", oneOf);
		}
		Class complement = clazz.getOwlComplementOf();
		if (complement != null) {
			QName comp = complement.getQName();
			if (comp == null) {
				Class nc = nameAnonymous(complement);
				if (nc == null)
					return null;
				comp = nc.getQName();
			}
			String name = "Not" + comp.getLocalPart();
			QName qname = new QName(comp.getNamespaceURI(), name);
			return rename(clazz, qname);
		}
		return null;
	}

	private void distributeEquivalentClasses() {
		for (Class klass : manager.findAll(Class.class)) {
			for (Class equiv : klass.getOwlEquivalentClasses()) {
				klass.getOwlEquivalentClasses().addAll(
						equiv.getOwlEquivalentClasses());
			}
			klass.getOwlEquivalentClasses().remove(klass);
		}
		for (Class c : manager.findAll(Class.class)) {
			for (Class e : c.getOwlEquivalentClasses()) {
				c.getOwlDisjointWith().addAll(e.getOwlDisjointWith());
				List<Class> inter = e.getOwlIntersectionOf();
				if (inter != null) {
					if (c.getOwlIntersectionOf() == null) {
						c.setOwlIntersectionOf(inter);
					} else if (!inter.equals(c.getOwlIntersectionOf())) {
						java.util.List list = new ArrayList<Class>(inter);
						list.removeAll(c.getOwlIntersectionOf());
						c.getOwlIntersectionOf().addAll(list);
					}
				}
				if (e.getOwlOneOf() != null) {
					if (c.getOwlOneOf() == null) {
						c.setOwlOneOf(e.getOwlOneOf());
					} else if (!e.getOwlOneOf().equals(c.getOwlOneOf())) {
						java.util.List list = new ArrayList(e.getOwlOneOf());
						list.removeAll(c.getOwlOneOf());
						c.getOwlOneOf().addAll(list);
					}
				}
				if (e.getOwlUnionOf() != null) {
					if (c.getOwlUnionOf() == null) {
						c.setOwlUnionOf(e.getOwlUnionOf());
					} else if (!e.getOwlUnionOf().equals(c.getOwlUnionOf())) {
						java.util.List list = new ArrayList(e.getOwlUnionOf());
						list.removeAll(c.getOwlUnionOf());
						c.getOwlUnionOf().addAll(list);
					}
				}
				if (e.getOwlComplementOf() != null) {
					if (c.getOwlComplementOf() == null) {
						c.setOwlComplementOf(e.getOwlComplementOf());
					}
				}
				if (!e.getOwlDisjointWith().isEmpty()) {
					c.getOwlDisjointWith().addAll(e.getOwlDisjointWith());
				}
			}
		}
	}

	private void mergeUnionClasses() throws RepositoryException {
		Iterable<Class> classes = manager.findAll(Class.class);
		for (Class clazz : classes) {
			List<? extends Entity> unionOf = clazz.getOwlUnionOf();
			if (unionOf != null) {
				Collection<? extends Class> common = findCommonSupers(unionOf);
				if (common.contains(clazz)) {
					unionOf.clear();
					manager.remove(unionOf);
					continue;
				} else if (findCommon(common, unionOf) != null) {
					Class sup = findCommon(common, unionOf);
					unionOf.clear();
					manager.remove(unionOf);
					if (sup.getQName() == null) {
						sup = nameAnonymous(sup);
					}
					rename(clazz, sup.getQName());
					continue;
				}
				clazz.getRdfsSubClassOf().addAll(common);
				Resource clazzValue = ((SesameEntity) clazz)
						.getSesameResource();
				for (Entity bean : unionOf) {
					Resource ofValue = ((SesameEntity) bean)
							.getSesameResource();
					Class of = manager.designate(ofValue, Class.class);
					if (bean instanceof Datatype && bean.getQName() != null) {
						// don't use anonymous class for datatypes
						rename(clazz, bean.getQName());
					} else if (isLocal(clazzValue, ofValue)) {
						of.getRdfsSubClassOf().add(clazz);
					} else {
						Ontology ont = (Ontology) clazz.getRdfsIsDefinedBy()
								.toArray()[0];
						URI nc = createLocalClass((URI) ofValue, ont);
						ContextAwareConnection conn = manager.getConnection();
						conn.add(nc, RDF.TYPE, OWL.CLASS);
						conn.add(nc, RDFS.SUBCLASSOF, ofValue);
						conn.add(nc, RDFS.SUBCLASSOF, clazzValue);
						conn.add(nc, RDFS.ISDEFINEDBY, ((SesameEntity) ont)
								.getSesameResource());
						renameClass(conn, (URI) ofValue, nc);
					}
				}
			}
		}
	}

	private Class findCommon(Collection<? extends Class> common,
			List<? extends Entity> unionOf) {
		Class result = null;
		for (Entity e : unionOf) {
			if (common.contains(e)) {
				result = (Class) e;
			}
		}
		return result;
	}

	private Collection<? extends Class> findCommonSupers(java.util.List unionOf) {
		Set common = null;
		for (Object of : unionOf) {
			Set supers = findSuperClasses((Class) of);
			if (common == null) {
				common = new HashSet(supers);
			} else {
				common.retainAll(supers);
			}
		}
		if (common == null)
			return Collections.emptySet();
		Iterator<Entity> iter = common.iterator();
		while (iter.hasNext()) {
			if (iter.next().getQName() == null) {
				iter.remove();
			}
		}
		return common;
	}

	private Set findSuperClasses(Class of) {
		HashSet set = new HashSet();
		set.add(of);
		return findSuperClasses(of, set);
	}

	private Set findSuperClasses(Class of, Set supers) {
		if (supers.addAll(of.getRdfsSubClassOf())) {
			for (Object s : of.getRdfsSubClassOf()) {
				findSuperClasses((Class) s, supers);
			}
		}
		return supers;
	}

	private Class renameClass(Class clazz, String and,
			List<? extends Entity> list) throws RepositoryException {
		String namespace = null;
		Set<String> names = new TreeSet<String>();
		for (Entity of : list) {
			QName qname = of.getQName();
			if (qname == null) {
				if (!(of instanceof Class))
					return null;
				Class nc = nameAnonymous((Class) of);
				if (nc == null)
					return null;
				qname = nc.getQName();
			}
			if (namespace == null || commonNS.contains(namespace)) {
				namespace = qname.getNamespaceURI();
			}
			names.add(qname.getLocalPart());
		}
		StringBuilder sb = new StringBuilder();
		for (String localPart : names) {
			sb.append(initcap(localPart));
			sb.append(and);
		}
		sb.setLength(sb.length() - and.length());
		return rename(clazz, new QName(namespace, sb.toString()));
	}

	private Class rename(Class clazz, QName qname) throws RepositoryException {
		logger.debug("renaming {} {}", clazz, qname);
		Class copy = manager.designate(qname, clazz.getClass());
		Ontology ont = findOntology(qname.getNamespaceURI(), ontologies);
		copy.getRdfsIsDefinedBy().add(ont);
		ContextAwareConnection conn = manager.getConnection();
		CloseableIteration<? extends Statement, RepositoryException> stmts;
		Resource orig = ((SesameEntity) clazz).getSesameResource();
		URI dest = (URI) ((SesameEntity) copy).getSesameResource();
		anonymousClasses.add(dest);
		stmts = conn.getStatements(orig, null, null);
		try {
			while (stmts.hasNext()) {
				Statement stmt = stmts.next();
				conn.add(dest, stmt.getPredicate(), stmt.getObject());
			}
		} finally {
			stmts.close();
		}
		conn.remove(orig, null, null);
		stmts = conn.getStatements(null, null, orig);
		try {
			while (stmts.hasNext()) {
				Statement stmt = stmts.next();
				conn.add(stmt.getSubject(), stmt.getPredicate(), dest);
			}
		} finally {
			stmts.close();
		}
		conn.remove((Resource) null, null, orig);
		return copy;
	}

	private String initcap(String str) {
		return str.substring(0, 1).toUpperCase() + str.substring(1);
	}

	private void checkNamespacePrefixes() throws Exception {
		ContextAwareConnection conn;
		conn = manager.getConnection();
		TupleQuery query = conn.prepareTupleQuery(SELECT_DEFINED);
		TupleQueryResult result = query.evaluate();
		try {
			while (result.hasNext()) {
				BindingSet tuple = result.next();
				Value value = tuple.getBinding("bean").getValue();
				if (value instanceof BNode)
					continue;
				String ns = ((URI) value).getNamespace();
				String prefix = getPrefix(ns);
				if (prefix == null) {
					Matcher matcher = NS_PREFIX.matcher(ns);
					if (matcher.find()) {
						prefix = matcher.group(1);
						logger.debug("creating prefix {} {}", prefix, ns);
						conn.setNamespace(prefix, ns);
					}
				}
			}
		} finally {
			result.close();
		}
	}

	private String getPrefix(String namespace) {
		CloseableIteration<? extends Namespace, RepositoryException> namespaces = null;
		try {
			try {
				ContextAwareConnection conn;
				conn = manager.getConnection();
				namespaces = conn.getNamespaces();
				while (namespaces.hasNext()) {
					Namespace ns = namespaces.next();
					if (namespace.equals(ns.getName()))
						return ns.getPrefix();
				}
				return null;
			} finally {
				if (namespaces != null)
					namespaces.close();
			}
		} catch (RepositoryException e) {
			throw new ElmoIOException(e);
		}
	}
}