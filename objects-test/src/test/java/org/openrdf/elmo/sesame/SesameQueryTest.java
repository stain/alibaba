package org.openrdf.elmo.sesame;

import java.sql.Date;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import javax.persistence.TemporalType;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import junit.framework.Test;

import org.openrdf.elmo.ElmoModule;
import org.openrdf.elmo.annotations.rdf;
import org.openrdf.elmo.sesame.base.RepositoryTestCase;

public class SesameQueryTest extends RepositoryTestCase {
	private static final int OFFSET = TimeZone.getDefault().getOffset(new Date(2007, Calendar.NOVEMBER, 6).getTime()) / 1000 / 60;
	private static final String SELECT_BY_DATE = "SELECT ?c WHERE { ?c <http://example.org/rdf/date> ?d . FILTER (?d <= ?date) }";
	private static final String NS = "http://example.org/rdf/";

	@rdf(NS + "Concept")
	public interface Concept {
		@rdf(NS + "date")
		XMLGregorianCalendar getDate();
		void setDate(XMLGregorianCalendar xcal);
	}

	public static Test suite() throws Exception {
		return RepositoryTestCase.suite(SesameQueryTest.class);
	}

	private SesameManager manager;
	private DatatypeFactory data;

	public void testXmlCalendarZ() throws Exception {
		XMLGregorianCalendar xcal = data.newXMLGregorianCalendar();
		xcal.setYear(2007);
		xcal.setMonth(11);
		xcal.setDay(6);
		xcal.setTimezone(OFFSET);
		SesameQuery query = manager.createQuery(SELECT_BY_DATE);
		query.setParameter("date", xcal);
		List list = query.getResultList();
		assertEquals(7, list.size());
	}

	public void testXmlCalendar() throws Exception {
		XMLGregorianCalendar xcal = data.newXMLGregorianCalendar();
		xcal.setYear(2000);
		xcal.setMonth(11);
		xcal.setDay(6);
		SesameQuery query = manager.createQuery(SELECT_BY_DATE);
		query.setParameter("date", xcal);
		List list = query.getResultList();
		assertEquals(3, list.size());
	}

	public void testCalendar() throws Exception {
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.YEAR, 2007);
		cal.set(Calendar.MONTH, Calendar.NOVEMBER);
		cal.set(Calendar.DAY_OF_MONTH, 6);
		cal.setTimeZone(TimeZone.getDefault());
		SesameQuery query = manager.createQuery(SELECT_BY_DATE);
		query.setParameter("date", cal, TemporalType.DATE);
		List list = query.getResultList();
		assertEquals(7, list.size());
	}

	public void testDate() throws Exception {
		Date date = new Date(2000, Calendar.NOVEMBER, 6);
		SesameQuery query = manager.createQuery(SELECT_BY_DATE);
		query.setParameter("date", date, TemporalType.DATE);
		List list = query.getResultList();
		assertEquals(3, list.size());
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		ElmoModule module = new ElmoModule().addConcept(Concept.class);
		SesameManagerFactory factory = new SesameManagerFactory(module, repository);
		manager = factory.createElmoManager();
		data = DatatypeFactory.newInstance();
		for (int i=1;i<5;i++) {
			Concept concept = manager.designate(new QName(NS, "concept" + i), Concept.class);
			XMLGregorianCalendar xcal = data.newXMLGregorianCalendar();
			xcal.setYear(2000);
			xcal.setMonth(11);
			xcal.setDay(i*2);
			concept.setDate(xcal);
			concept = manager.designate(new QName(NS, "conceptZ" + i), Concept.class);
			xcal = data.newXMLGregorianCalendar();
			xcal.setYear(2007);
			xcal.setMonth(11);
			xcal.setDay(i*2);
			xcal.setTimezone(OFFSET);
			concept.setDate(xcal);
		}
	}

	@Override
	protected void tearDown() throws Exception {
		manager.close();
		super.tearDown();
	}

}
