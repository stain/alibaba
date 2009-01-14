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
package org.openrdf.elmo.sesame;

import java.util.Calendar;
import java.util.Collection;
import java.util.Currency;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.TimeZone;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import junit.framework.Test;

import org.openrdf.elmo.sesame.base.RepositoryTestCase;
import org.openrdf.elmo.sesame.helpers.PropertyChanger;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.LiteralImpl;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.model.vocabulary.XMLSchema;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.contextaware.ContextAwareConnection;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectRepository;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.repository.object.annotations.rdf;
import org.openrdf.repository.object.config.ObjectRepositoryConfig;
import org.openrdf.repository.object.config.ObjectRepositoryFactory;
import org.openrdf.repository.object.exceptions.ElmoConversionException;
import org.openrdf.repository.object.exceptions.ElmoPersistException;
import org.openrdf.rio.RDFFormat;
import org.openrdf.store.StoreException;

public class LiteralTest extends RepositoryTestCase {

	public static Test suite() throws Exception {
		return RepositoryTestCase.suite(LiteralTest.class);
	}

	private ObjectRepository factory;

	private ObjectConnection manager;

	private URI dateURI = new URIImpl("urn:aDate");

	@Override
	public void setUp() throws Exception {
		super.setUp();
		RepositoryConnection connection = repository.getConnection();
		// import RDF schema and datatype hierarchy
		connection.add(getClass().getResourceAsStream(
				"/testcases/schemas/rdfs-schema.rdf"), "", RDFFormat.RDFXML);
		connection.add(getClass().getResourceAsStream(
				"/testcases/schemas/owl-schema.rdf"), "", RDFFormat.RDFXML);
		connection.add(getClass().getResourceAsStream(
				"/testcases/schemas/xsd-datatypes.rdf"), "", RDFFormat.RDFXML);
		connection.close();
		ObjectRepositoryConfig module = new ObjectRepositoryConfig();
		module.addBehaviour(TestSupport.class, "urn:TestConcept");
		module.addConcept(TestConcept.class);
		module.addDatatype(SomeLiteral.class, "urn:SomeLiteral");
		factory = new ObjectRepositoryFactory().createRepository(module,
				repository);
		this.manager = factory.getConnection();
	}

	@Override
	protected void tearDown() throws Exception {
		manager.close();
		factory.shutDown();
		super.tearDown();
	}

	private ValueFactory getValueFactory() {
		return manager.getValueFactory();
	}

	public void testCalendar() throws Exception {
		TestConcept tester = manager.create(TestConcept.class);
		assertFalse(tester.equal(new Object(), new Object()));
		Calendar cal = Calendar.getInstance();
		cal.set(1970, 0, 1, 0, 0, 0);
		cal.set(Calendar.MILLISECOND, 0);
		tester.setADate(cal.getTime());
		Date date2 = tester.getADate();
		assertEquals(cal.getTime(), date2);
	}

	public void testDate() throws Exception {
		TestConcept tester = manager.create(TestConcept.class);
		Calendar cal = Calendar.getInstance();
		cal.set(1970, 0, 1, 0, 0, 0);
		cal.set(Calendar.MILLISECOND, 0);
		Date date = cal.getTime();
		tester.setADate(date);
		cal.setTime(tester.getADate());
		assertEquals(date, cal.getTime());
	}

	public void testDay() throws Exception {
		TestConcept tester = manager.create(TestConcept.class);
		Resource bNode = (Resource) manager.valueOf(tester);
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		cal.set(1970, 0, 1, 0, 0, 0);
		cal.set(Calendar.MILLISECOND, 0);
		Date date = cal.getTime();
		try {
			ContextAwareConnection conn = manager;
			conn.add(bNode, dateURI, getValueFactory().createLiteral(
					"1970-01-01Z", XMLSchema.DATE));
		} catch (StoreException e) {
			throw new ElmoPersistException(e);
		}
		cal.setTime(tester.getADate());
		assertEquals(date, cal.getTime());
	}

	public void testDateTimeS() throws Exception {
		TestConcept tester = manager.create(TestConcept.class);
		Resource bNode = (Resource) manager.valueOf(tester);
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		cal.set(2001, 6, 4, 12, 8, 56);
		cal.set(Calendar.MILLISECOND, 0);
		cal.set(Calendar.ZONE_OFFSET, -7 * 60 * 60 * 1000);
		try {
			ContextAwareConnection conn = manager;
			conn.add(bNode, dateURI, getValueFactory().createLiteral(
					"2001-07-04T12:08:56-07:00", XMLSchema.DATETIME));
		} catch (StoreException e) {
			throw new ElmoPersistException(e);
		}
		Date date = tester.getADate();
		assertEquals(cal.getTime(), date);
	}

	public void testDateTimeMS() throws Exception {
		TestConcept tester = manager.create(TestConcept.class);
		Resource bNode = (Resource) manager.valueOf(tester);
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		cal.set(2001, 6, 4, 12, 8, 56);
		cal.set(Calendar.MILLISECOND, 27);
		cal.set(Calendar.ZONE_OFFSET, -7 * 60 * 60 * 1000);
		try {
			Literal literal = getValueFactory().createLiteral(
					"2001-07-04T12:08:56.027-07:00", XMLSchema.DATETIME);
			manager.add(bNode, dateURI, literal);
		} catch (StoreException e) {
			throw new ElmoPersistException(e);
		}
		Date date = tester.getADate();
		assertEquals(cal.getTime(), date);
	}

	public void testInteger() throws Exception {
		TestConcept tester = manager.create(TestConcept.class);
		Integer integer = new Integer(72);
		tester.setInteger(integer);
		assertEquals(integer, tester.getInteger());
	}

	public void testCurrency() throws Exception {
		TestConcept tester = manager.create(TestConcept.class);
		Currency cur = Currency.getInstance("CAD");
		tester.setCurrency(cur);
		assertEquals(cur, tester.getCurrency());
	}

	public void testSomeLiteral() throws Exception {
		TestConcept tester = manager.create(TestConcept.class);
		SomeLiteral so = new SomeLiteral("blah");
		tester.setSomeLiteral(so);
		assertEquals(so, tester.getSomeLiteral());
	}

	public void testMixProperty() throws Exception {
		TestConcept tester = manager.create(TestConcept.class);
		Resource bNode = (Resource) manager.valueOf(tester);
		manager.add(getValueFactory().createURI("urn:SomeLiteral"),
				RDFS.SUBCLASSOF, RDFS.LITERAL);
		manager.add(bNode, RDFS.SEEALSO, new LiteralImpl("a string"));
		manager.add(bNode, RDFS.SEEALSO, new LiteralImpl("a literal object",
				new URIImpl("urn:SomeLiteral")));
		manager.add(bNode, RDFS.SEEALSO, new URIImpl("urn:aResourceTester"));
		manager.add(getValueFactory().createURI("urn:aResourceTester"),
				RDF.TYPE, new URIImpl("urn:TestConcept"));
		Collection<Object> col = new SesameProperty<Object>((RDFObject) tester,
				new PropertyChanger(RDFS.SEEALSO));
		int stringCount = 0;
		int someLiteralCount = 0;
		int resourceTestObjectCount = 0;
		Iterator<Object> it = col.iterator();
		try {
			while (it.hasNext()) {
				Object res = it.next();
				if (!(res instanceof RDFObject)) {
					if (res instanceof String) {
						stringCount++;
					} else if (res instanceof SomeLiteral) {
						someLiteralCount++;
					} else {
						fail("unknow literal: " + res.getClass());
					}
				} else {
					if (res instanceof TestConcept) {
						resourceTestObjectCount++;
					} else {
						fail("unknow resource: " + res.getClass());
					}
				}
			}
		} finally {
			manager.close(it);
		}
		assertEquals(1, resourceTestObjectCount);
		assertEquals(1, someLiteralCount);
		assertEquals(1, stringCount);
	}

	public void testEnums() throws Exception {
		TestConcept tester = manager.create(TestConcept.class);
		tester.setEnumLiteral(EnumLiteral.ONE);
		assertEquals(EnumLiteral.ONE, tester.getEnumLiteral());
	}

	public static enum EnumLiteral {
		ONE, TWO, THREE
	}

	public interface TestBehaviour {
		public Date getADate();

		public void setADate(Date date);

		public boolean equal(Object o1, Object o2);
	}

	@rdf("urn:TestConcept")
	public interface TestConcept extends TestBehaviour {
		@rdf("urn:integer")
		public Integer getInteger();

		public void setInteger(Integer integer);

		@rdf("urn:someLiteral")
		public SomeLiteral getSomeLiteral();

		public void setSomeLiteral(SomeLiteral someLiteral);

		@rdf("urn:enumLiteral")
		public EnumLiteral getEnumLiteral();

		public void setEnumLiteral(EnumLiteral value);

		@rdf("urn:currency")
		public Currency getCurrency();

		public void setCurrency(Currency currency);

		@rdf("urn:aDate")
		public abstract XMLGregorianCalendar getXMLGregorianCalendar();

		public abstract void setXMLGregorianCalendar(XMLGregorianCalendar date);
	}

	public static class TestSupport implements TestBehaviour {
		private TestConcept bean;

		public TestSupport(TestConcept bean) {
			this.bean = bean;
		}

		public Date getADate() {
			XMLGregorianCalendar xgc = bean.getXMLGregorianCalendar();
			if (xgc == null)
				return null;
			return xgc.toGregorianCalendar().getTime();
		}

		public void setADate(Date date) {
			DatatypeFactory factory;
			try {
				factory = DatatypeFactory.newInstance();
			} catch (DatatypeConfigurationException e) {
				throw new ElmoConversionException(e);
			}
			GregorianCalendar gc = new GregorianCalendar(0, 0, 0);
			gc.setTime(date);
			XMLGregorianCalendar xgc = factory.newXMLGregorianCalendar(gc);
			bean.setXMLGregorianCalendar(xgc);
		}

		public boolean equal(Object o1, Object o2) {
			return o1.equals(o2);
		}
	}

	public static class SomeLiteral {
		public String value;

		public SomeLiteral(CharSequence string) {
			value = string.toString();
		}

		@Override
		public String toString() {
			return value;
		}

		@Override
		public boolean equals(Object o) {
			return value.equals(o.toString());
		}
	}
}
