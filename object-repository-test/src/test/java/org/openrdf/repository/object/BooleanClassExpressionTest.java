package org.openrdf.repository.object;

import junit.framework.Test;

import org.openrdf.repository.object.annotations.complementOf;
import org.openrdf.repository.object.annotations.intersectionOf;
import org.openrdf.repository.object.annotations.rdf;
import org.openrdf.repository.object.base.ObjectRepositoryTestCase;

public class BooleanClassExpressionTest extends ObjectRepositoryTestCase {
	public static Test suite() throws Exception {
		return ObjectRepositoryTestCase.suite(BooleanClassExpressionTest.class);
	}

	public static final String NS = "urn:test:";

	@rdf(NS + "Customer")
	public interface Customer {
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

	public void testDesignateCustomer() throws Exception {
		Object customer = con.addType(con.getObjectFactory().createBlankObject(), Customer.class);
		assertTrue(customer instanceof Customer);
		assertFalse(customer instanceof BigCustomer);
		assertTrue(customer instanceof NotBigCustomer);
		assertTrue(customer instanceof SmallCustomer);
	}

	public void testDesignateBigCustomer() throws Exception {
		Object customer = con.addType(con.getObjectFactory().createBlankObject(), BigCustomer.class);
		assertTrue(customer instanceof Customer);
		assertTrue(customer instanceof BigCustomer);
		assertFalse(customer instanceof NotBigCustomer);
		assertFalse(customer instanceof SmallCustomer);
	}

	public void testDesignateNotBigCustomer() throws Exception {
		Object customer = con.addType(con.getObjectFactory().createBlankObject(), NotBigCustomer.class);
		assertFalse(customer instanceof Customer);
		assertFalse(customer instanceof BigCustomer);
		assertTrue(customer instanceof NotBigCustomer);
		assertFalse(customer instanceof SmallCustomer);
	}

	public void testDesignateBigNotBig() throws Exception {
		Object customer = con.addType(con.getObjectFactory().createBlankObject(), BigCustomer.class);
		customer = con.addType(customer, NotBigCustomer.class);
		assertFalse(customer instanceof Customer);
		assertFalse(customer instanceof BigCustomer);
		assertTrue(customer instanceof NotBigCustomer);
		assertFalse(customer instanceof SmallCustomer);
	}

	public void testDesignateCustomerBigNotBig() throws Exception {
		Object customer = con.getObjectFactory().createBlankObject();
		customer = con.addType(customer, BigCustomer.class);
		customer = con.addType(customer, SmallCustomer.class);
		assertTrue(customer instanceof Customer);
		assertFalse(customer instanceof BigCustomer);
		assertTrue(customer instanceof NotBigCustomer);
		assertTrue(customer instanceof SmallCustomer);
	}

	public void testDesignateCustomerRemoveNotBig() throws Exception {
		RDFObject node = con.getObjectFactory().createBlankObject();
		Object customer = con.addType(node, Customer.class);
		customer = con.removeType(customer, NotBigCustomer.class);
		assertTrue(customer instanceof Customer);
		assertTrue(customer instanceof BigCustomer);
		assertFalse(customer instanceof NotBigCustomer);
		assertFalse(customer instanceof SmallCustomer);
	}

	public void testDesignateSmallCustomer() throws Exception {
		Object customer = con.addType(con.getObjectFactory().createBlankObject(), SmallCustomer.class);
		assertTrue(customer instanceof Customer);
		assertFalse(customer instanceof BigCustomer);
		assertTrue(customer instanceof NotBigCustomer);
		assertTrue(customer instanceof SmallCustomer);
	}

	@Override
	protected void setUp() throws Exception {
		config.addConcept(Customer.class);
		config.addConcept(BigCustomer.class);
		config.addConcept(NotBigCustomer.class);
		config.addConcept(SmallCustomer.class);
		super.setUp();
	}

}
