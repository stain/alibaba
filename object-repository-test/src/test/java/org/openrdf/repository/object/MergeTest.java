package org.openrdf.repository.object;

import junit.framework.Test;

import org.openrdf.model.URI;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.repository.object.annotations.complementOf;
import org.openrdf.repository.object.annotations.intersectionOf;
import org.openrdf.repository.object.annotations.rdf;
import org.openrdf.repository.object.base.ElmoManagerTestCase;
import org.openrdf.repository.object.base.RepositoryTestCase;

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
		private URI name;

		public SmallCompanyImpl(URI name) {
			this.name = name;
		}

		public URI getURI() {
			return name;
		}
	}

	public void testComplexMerge() throws Exception {
		URI name = ValueFactoryImpl.getInstance().createURI("urn:test:", "comp");
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
