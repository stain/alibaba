package org.openrdf.elmo.sesame;

import java.util.Set;

import junit.framework.Test;

import org.openrdf.elmo.sesame.base.ElmoManagerTestCase;
import org.openrdf.elmo.sesame.concepts.ClassConcept;
import org.openrdf.elmo.sesame.concepts.Property;
import org.openrdf.model.URI;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.repository.object.annotations.rdf;

public class DesignationTest extends ElmoManagerTestCase {

	public static Test suite() throws Exception {
		return ElmoManagerTestCase.suite(DesignationTest.class);
	}

	@rdf("http://www.w3.org/2000/01/rdf-schema#Resource")
	public interface Resource {
		/** The subject is an instance of a class. */
		@rdf("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")
		public abstract Set<ClassConcept> getRdfTypes();

		/** The subject is an instance of a class. */
		public abstract void setRdfTypes(Set<ClassConcept> value);
	}

	@Override
	protected void setUp() throws Exception {
		module.addConcept(Resource.class);
		super.setUp();
	}

	public void testDesignateEntity() throws Exception {
		URI name = ValueFactoryImpl.getInstance().createURI("urn:resource");
		Resource resource = (Resource) manager.find(name);
		assertEquals(0, resource.getRdfTypes().size());
		Property prop = manager.designate(resource, Property.class);
		assertEquals(1, prop.getRdfTypes().size());
		resource = (Resource) manager.removeDesignation(prop, Property.class);
		assertTrue(!(resource instanceof Property));
		assertEquals(0, resource.getRdfTypes().size());
	}

}
