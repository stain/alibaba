package org.openrdf.repository.object;

import junit.framework.Test;

import org.openrdf.model.URI;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.repository.object.annotations.complementOf;
import org.openrdf.repository.object.annotations.intersectionOf;
import org.openrdf.repository.object.annotations.rdf;
import org.openrdf.repository.object.base.ObjectRepositoryTestCase;
import org.openrdf.repository.object.base.RepositoryTestCase;

public class MergeTest extends ObjectRepositoryTestCase {

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
		con.addType(con.getObject(name), BigCompany.class);
		con.addObject(name, new SmallCompanyImpl(name));
		Company company = (Company) con.getObject(name);
		assertTrue(company instanceof BigCompany);
	}

	public void setUp() throws Exception {
		config.addConcept(Company.class);
		config.addConcept(BigCompany.class);
		super.setUp();
	}

}
