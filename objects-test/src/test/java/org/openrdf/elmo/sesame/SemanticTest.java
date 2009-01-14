package org.openrdf.elmo.sesame;

import java.util.Locale;

import javax.xml.namespace.QName;

import junit.framework.Test;

import org.openrdf.elmo.ElmoModule;
import org.openrdf.elmo.annotations.disjointWith;
import org.openrdf.elmo.annotations.inverseOf;
import org.openrdf.elmo.annotations.localized;
import org.openrdf.elmo.annotations.rdf;
import org.openrdf.elmo.sesame.base.RepositoryTestCase;
import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.repository.contextaware.ContextAwareConnection;

public class SemanticTest extends RepositoryTestCase {
	private static final String NS = "http://www.example.com/rdf/2007/";

	public static Test suite() throws Exception {
		return RepositoryTestCase.suite(SemanticTest.class);
	}

	private SesameManagerFactory factory;

	private ValueFactory vf;

	private SesameManager manager;

	private ContextAwareConnection conn;

	@rdf(NS + "Role1")
	public static interface Role1A {
	}

	@rdf(NS + "Role1")
	public static interface Role1B {
	}

	@rdf(NS + "Role2")
	public static interface Role2 {
	}

	@rdf({NS + "Role3", NS + "Role2" })
	public static interface Role3 {
	}

	@rdf(NS + "Role4")
	@disjointWith( { Role3.class })
	public static interface Role4 {
	}

	@rdf(NS + "Concept1")
	public static interface Concept1 {
		@rdf({NS + "p1", NS + "p2"})
		public String getP1();
		public void setP1(String value);
	}

	@rdf(NS + "Concept2")
	public static interface Concept2 {
		@rdf(NS + "p1")
		@inverseOf({NS + "p2"})
		public Concept2 getP1();
		public void setP1(Concept2 value);
		@inverseOf(value=NS + "p1")
		public Concept2 getP2();
		public void setP2(Concept2 value);
	}

	@rdf(NS + "Concept3")
	public static interface Concept3 {
		@localized
		@rdf(NS + "p1")
		public String getP1();
		public void setP1(String value);
	}

	public void testClassRdf() throws Exception {
		Object bean = manager.create(Role1A.class);
		assertTrue(bean instanceof Role1B);

	}

	public void testClassEquivalent() throws Exception {
		Object bean = manager.create(Role2.class);
		assertTrue(bean instanceof Role3);

	}

	public void testClassDisjointWith() throws Exception {
		QName id = new QName(NS, "ID1");
		manager.designate(id, Role3.class);
		boolean ea = false;
		assert ea = true;
		if (ea) {
			try {
				manager.designate(id, Role4.class);
				fail();
			} catch (AssertionError e) {
			}
		}

	}

	public void testPropertyReadRdf() throws Exception {
		QName id = new QName(NS, "ID1");
		URI subj = vf.createURI(NS + "ID1");
		URI pred = vf.createURI(NS + "p1");
		Literal obj = vf.createLiteral("v1");
		conn.add(subj, pred, obj);
		Concept1 bean = manager.designate(id, Concept1.class);
		assertEquals("v1", bean.getP1());
	}

	public void testPropertyWriteRdf() throws Exception {
		QName id = new QName(NS, "ID1");
		Concept1 bean = manager.designate(id, Concept1.class);
		bean.setP1("v1");
		URI subj = vf.createURI(NS + "ID1");
		URI pred = vf.createURI(NS + "p1");
		Literal obj = vf.createLiteral("v1");
		assertTrue(conn.hasStatement(subj, pred, obj));
	}

	public void testPropertyReadInverseRdf() throws Exception {
		QName id1 = new QName(NS, "ID1");
		QName id2 = new QName(NS, "ID2");
		URI subj = vf.createURI(NS + "ID2");
		URI pred = vf.createURI(NS + "p1");
		URI obj = vf.createURI(NS + "ID1");
		conn.add(subj, pred, obj);
		Concept2 bean1 = manager.designate(id1, Concept2.class);
		Concept2 bean2 = manager.designate(id2, Concept2.class);
		assertEquals(bean2, bean1.getP2());
	}

	public void testPropertyWriteInverseRdf() throws Exception {
		QName id1 = new QName(NS, "ID1");
		QName id2 = new QName(NS, "ID2");
		Concept2 bean1 = manager.designate(id1, Concept2.class);
		Concept2 bean2 = manager.designate(id2, Concept2.class);
		bean1.setP1(bean2);
		assertEquals(bean1, bean2.getP2());
	}

	public void testPropertyReadLocalized() throws Exception {
		manager.close();
		manager = factory.createElmoManager(Locale.ENGLISH);
		conn = manager.getConnection();
		QName id = new QName(NS, "ID1");
		URI subj = vf.createURI(NS + "ID1");
		URI pred = vf.createURI(NS + "p1");
		Literal obj = vf.createLiteral("v1", "en");
		conn.add(subj, pred, obj);
		Concept3 bean = manager.designate(id, Concept3.class);
		assertEquals("v1", bean.getP1());
	}

	public void testPropertyWriteLocalized() throws Exception {
		manager.close();
		manager = factory.createElmoManager(Locale.ENGLISH);
		conn = manager.getConnection();
		QName id = new QName(NS, "ID1");
		Concept3 bean = manager.designate(id, Concept3.class);
		bean.setP1("v1");
		URI subj = vf.createURI(NS + "ID1");
		URI pred = vf.createURI(NS + "p1");
		Literal obj = vf.createLiteral("v1", "en");
		assertTrue(conn.hasStatement(subj, pred, obj));
	}

	public void testPropertyEquivalent() throws Exception {
		QName id = new QName(NS, "ID1");
		Concept1 bean = manager.designate(id, Concept1.class);
		bean.setP1("v1");
		URI subj = vf.createURI(NS + "ID1");
		URI pred = vf.createURI(NS + "p2");
		Literal obj = vf.createLiteral("v1");
		assertTrue(conn.hasStatement(subj, pred, obj));
	}

	public void testPropertyInverseOf() throws Exception {
		QName id = new QName(NS, "ID1");
		Concept1 bean = manager.designate(id, Concept1.class);
		bean.setP1("v1");
		URI subj = vf.createURI(NS + "ID1");
		URI pred = vf.createURI(NS + "p2");
		Literal obj = vf.createLiteral("v1");
		assertTrue(conn.hasStatement(subj, pred, obj));
	}

	public void testPropertyOneOf() throws Exception {
		QName id1 = new QName(NS, "ID1");
		QName id2 = new QName(NS, "ID2");
		Concept2 bean1 = manager.designate(id1, Concept2.class);
		Concept2 bean2 = manager.designate(id2, Concept2.class);
		bean1.setP1(bean2);
		URI subj = vf.createURI(NS + "ID2");
		URI pred = vf.createURI(NS + "p2");
		URI obj = vf.createURI(NS + "ID1");
		assertTrue(conn.hasStatement(subj, pred, obj));
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		vf = repository.getValueFactory();
		ElmoModule module = new ElmoModule();
		module.addConcept(Role1A.class);
		module.addConcept(Role1B.class);
		module.addConcept(Role2.class);
		module.addConcept(Role3.class);
		module.addConcept(Role4.class);
		module.addConcept(Concept1.class);
		module.addConcept(Concept2.class);
		module.addConcept(Concept3.class);
		factory = new SesameManagerFactory(module, repository);
		factory.setInferencingEnabled(true);
		manager = factory.createElmoManager();
		conn = manager.getConnection();
	}

	@Override
	protected void tearDown() throws Exception {
		manager.close();
		factory.close();
		super.tearDown();
	}
}
