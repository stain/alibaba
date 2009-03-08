package org.openrdf.repository.object;

import junit.framework.Test;

import org.openrdf.repository.object.annotations.complementOf;
import org.openrdf.repository.object.annotations.intersectionOf;
import org.openrdf.repository.object.annotations.rdf;
import org.openrdf.repository.object.base.ObjectRepositoryTestCase;

public class BooleanClassExpressionTest extends ObjectRepositoryTestCase {
	private static final int BIG_CUSTOMER_SIZE = 100000;
	private static final int SMALL_CUSTOMER_SIZE = 100;

	public static Test suite() throws Exception {
		return ObjectRepositoryTestCase.suite(BooleanClassExpressionTest.class);
	}

	public static final String NS = "urn:test:";

	@rdf(NS + "Customer")
	public interface Customer {
		int getCustomerSize();
	}

	@rdf(NS + "BigCustomer")
	public interface BigCustomer extends Customer {
	}

	@complementOf(BigCustomer.class)
	public interface NotBigCustomer {
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
		Customer customer = con.addType(con.getObjectFactory().createBlankObject(), Customer.class);
		assertEquals(SMALL_CUSTOMER_SIZE, customer.getCustomerSize());
		assertFalse(customer instanceof BigCustomer);
		assertTrue(customer instanceof NotBigCustomer);
		assertTrue(customer instanceof SmallCustomer);
	}

	public void testDesignateBigCustomer() throws Exception {
		Customer customer = con.addType(con.getObjectFactory().createBlankObject(), BigCustomer.class);
		assertEquals(BIG_CUSTOMER_SIZE, customer.getCustomerSize());
		assertTrue(customer instanceof BigCustomer);
		assertFalse(customer instanceof NotBigCustomer);
		assertFalse(customer instanceof SmallCustomer);
	}

	@Override
	protected void setUp() throws Exception {
		config.addConcept(Customer.class);
		config.addConcept(BigCustomer.class);
		config.addBehaviour(BigCustomerSupport.class);
		config.addBehaviour(SmallCustomerSupport.class);
		super.setUp();
	}

}
