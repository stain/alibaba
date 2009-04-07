package org.openrdf.repository.object;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import junit.framework.Test;

import org.openrdf.model.vocabulary.OWL;
import org.openrdf.repository.object.annotations.rdf;
import org.openrdf.repository.object.base.ObjectRepositoryTestCase;
import org.openrdf.repository.object.base.RepositoryTestCase;

public class UnionOfTest extends ObjectRepositoryTestCase {

	public static Test suite() throws Exception {
		return RepositoryTestCase.suite(UnionOfTest.class);
	}

	@rdf(OWL.NAMESPACE + "unionOf")
	@Retention(RetentionPolicy.RUNTIME)
	@Target( { ElementType.TYPE })
	public @interface unionOf {
		Class<?>[] value();
	}

	@unionOf( { Car.class, Truck.class })
	public interface CarOrTruck {
		String start();
	}

	@rdf("urn:test:Car")
	public interface Car extends CarOrTruck {
	}

	@rdf("urn:test:Truck")
	public interface Truck extends CarOrTruck {
	};

	public static class Engine implements CarOrTruck {

		public String start() {
			return "vroom";
		}
	}

	@Override
	public void setUp() throws Exception {
		config.addAnnotation(unionOf.class);
		config.addConcept(Car.class);
		config.addConcept(Truck.class);
		config.addBehaviour(Engine.class);
		super.setUp();
	}

	public void testUnioOfBehaviour() throws Exception {
		Car car = con.addType(of.createBlankObject(), Car.class);
		assertEquals("vroom", car.start());
	}

}
