package org.openrdf.repository.object;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import junit.framework.Test;

import org.openrdf.model.vocabulary.OWL;
import org.openrdf.repository.object.annotations.iri;
import org.openrdf.repository.object.base.ObjectRepositoryTestCase;

public class BooleanClassExpressionTest extends ObjectRepositoryTestCase {
	private static final int BIG_CUSTOMER_SIZE = 100000;
	private static final int SMALL_CUSTOMER_SIZE = 100;

	public static Test suite() throws Exception {
		return ObjectRepositoryTestCase.suite(BooleanClassExpressionTest.class);
	}

	public static final String NS = "urn:test:";

	@iri(NS + "Customer")
	public interface Customer {
		int getCustomerSize();
	}

	@iri(NS + "BigCustomer")
	public interface BigCustomer extends Customer {
	}

	@iri(OWL.NAMESPACE + "complementOf")
	@Retention(RetentionPolicy.RUNTIME)
	@Target( { ElementType.TYPE })
	public @interface complementOf {
		Class<?> value();
	}

	@complementOf(BigCustomer.class)
	public interface NotBigCustomer {
	}

	@iri(OWL.NAMESPACE + "intersectionOf")
	@Retention(RetentionPolicy.RUNTIME)
	@Target( { ElementType.TYPE })
	public @interface intersectionOf {
		Class<?>[] value();
	}

	@intersectionOf( { Customer.class, NotBigCustomer.class })
	public interface SmallCustomer extends Customer {
	}

	public static abstract class BigCustomerSupport implements BigCustomer {
		public int getCustomerSize() {
			return BIG_CUSTOMER_SIZE;
		}
	}

	public static abstract class SmallCustomerSupport implements SmallCustomer {
		public int getCustomerSize() {
			return SMALL_CUSTOMER_SIZE;
		}
	}

	public void testDesignateCustomer() throws Exception {
		Customer customer = con.addDesignation(con.getObjectFactory()
				.createObject(), Customer.class);
		assertEquals(SMALL_CUSTOMER_SIZE, customer.getCustomerSize());
		assertFalse(customer instanceof BigCustomer);
		assertTrue(customer instanceof NotBigCustomer);
		assertTrue(customer instanceof SmallCustomer);
	}

	public void testDesignateBigCustomer() throws Exception {
		Customer customer = con.addDesignation(con.getObjectFactory()
				.createObject(), BigCustomer.class);
		assertEquals(BIG_CUSTOMER_SIZE, customer.getCustomerSize());
		assertTrue(customer instanceof BigCustomer);
		assertFalse(customer instanceof NotBigCustomer);
		assertFalse(customer instanceof SmallCustomer);
	}

	@Override
	protected void setUp() throws Exception {
		config.addAnnotation(complementOf.class);
		config.addAnnotation(intersectionOf.class);
		config.addConcept(Customer.class);
		config.addConcept(BigCustomer.class);
		config.addBehaviour(BigCustomerSupport.class);
		config.addBehaviour(SmallCustomerSupport.class);
		super.setUp();
	}

}
