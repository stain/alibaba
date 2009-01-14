package org.openrdf.elmo.sesame;

import javax.xml.namespace.QName;

import junit.framework.Test;

import org.openrdf.elmo.sesame.base.ElmoManagerTestCase;
import org.openrdf.elmo.sesame.base.RepositoryTestCase;
import org.openrdf.repository.object.annotations.complementOf;
import org.openrdf.repository.object.annotations.intersectionOf;
import org.openrdf.repository.object.annotations.rdf;

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
		Class<?>[] concepts = {};
		manager.designate(manager.find(name), BigCompany.class, concepts);
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
