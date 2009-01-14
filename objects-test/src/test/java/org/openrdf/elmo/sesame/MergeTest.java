package org.openrdf.elmo.sesame;

import javax.xml.namespace.QName;

import junit.framework.Test;

import org.openrdf.elmo.annotations.complementOf;
import org.openrdf.elmo.annotations.intersectionOf;
import org.openrdf.elmo.annotations.rdf;
import org.openrdf.elmo.sesame.base.ElmoManagerTestCase;
import org.openrdf.elmo.sesame.base.RepositoryTestCase;

public class MergeTest extends ElmoManagerTestCase {

	public static Test suite() throws Exception {
		return RepositoryTestCase.suite(MergeTest.class);
	}

	@rdf("urn:test:Company")
	public interface Company {}
	@rdf("urn:test:BigCompany")
	public interface BigCompany extends Company {}
	@complementOf(BigCompany.class)
	public interface NotBigCompany {}
	@intersectionOf({Company.class, NotBigCompany.class})
	public interface SmallCompany extends Company, NotBigCompany {}

	public class SmallCompanyImpl implements SmallCompany {
		private QName name;

		public SmallCompanyImpl(QName name) {
			this.name = name;
		}

		public QName getQName() {
			return name;
		}
	}

	public void testComplexMerge() throws Exception {
		QName name = new QName("urn:test:", "comp");
		manager.designate(name, BigCompany.class);
		SmallCompany company = manager.merge(new SmallCompanyImpl(name));
		assertTrue(company instanceof SmallCompany);
	}

	public void setUp() throws Exception {
		module.addConcept(Company.class);
		module.addConcept(BigCompany.class);
		module.addConcept(NotBigCompany.class);
		module.addConcept(SmallCompany.class);
		super.setUp();
	}

}
