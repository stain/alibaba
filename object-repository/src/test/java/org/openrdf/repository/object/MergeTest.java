package org.openrdf.repository.object;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import junit.framework.Test;

import org.openrdf.model.URI;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.repository.object.AlternativeRoleTest.complementOf;
import org.openrdf.repository.object.annotations.rdf;
import org.openrdf.repository.object.base.ObjectRepositoryTestCase;
import org.openrdf.repository.object.base.RepositoryTestCase;

public class MergeTest extends ObjectRepositoryTestCase {

	public static Test suite() throws Exception {
		return RepositoryTestCase.suite(MergeTest.class);
	}

	@rdf(OWL.NAMESPACE + "complementOf")
	@Retention(RetentionPolicy.RUNTIME)
	@Target( { ElementType.TYPE })
	public @interface complementOf {
		Class<?> value();
	}

	@rdf(OWL.NAMESPACE+"intersectionOf")
	@Retention(RetentionPolicy.RUNTIME)
	@Target( { ElementType.TYPE })
	public @interface intersectionOf {
		Class<?>[] value();
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
		con.addDesignation(con.getObject(name), BigCompany.class);
		con.addObject(name, new SmallCompanyImpl(name));
		Company company = (Company) con.getObject(name);
		assertTrue(company instanceof BigCompany);
	}

	public void setUp() throws Exception {
		config.addAnnotation(complementOf.class);
		config.addAnnotation(intersectionOf.class);
		config.addConcept(Company.class);
		config.addConcept(BigCompany.class);
		super.setUp();
	}

}
