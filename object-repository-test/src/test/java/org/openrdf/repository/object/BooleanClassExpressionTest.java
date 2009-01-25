package org.openrdf.repository.object;

import junit.framework.Test;

import org.openrdf.repository.object.annotations.complementOf;
import org.openrdf.repository.object.annotations.intersectionOf;
import org.openrdf.repository.object.annotations.rdf;
import org.openrdf.repository.object.base.ElmoManagerTestCase;

public class BooleanClassExpressionTest extends ElmoManagerTestCase {
	public static Test suite() throws Exception {
		return ElmoManagerTestCase.suite(BooleanClassExpressionTest.class);
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
		Object customer = manager.addType(manager.getObjectFactory().createBlankObject(), Customer.class);
		assertTrue(customer instanceof Customer);
		assertFalse(customer instanceof BigCustomer);
		assertTrue(customer instanceof NotBigCustomer);
		assertTrue(customer instanceof SmallCustomer);
	}

	public void testDesignateBigCustomer() throws Exception {
		Object customer = manager.addType(manager.getObjectFactory().createBlankObject(), BigCustomer.class);
		assertTrue(customer instanceof Customer);
		assertTrue(customer instanceof BigCustomer);
		assertFalse(customer instanceof NotBigCustomer);
		assertFalse(customer instanceof SmallCustomer);
	}

	public void testDesignateNotBigCustomer() throws Exception {
		Object customer = manager.addType(manager.getObjectFactory().createBlankObject(), NotBigCustomer.class);
		assertFalse(customer instanceof Customer);
		assertFalse(customer instanceof BigCustomer);
		assertTrue(customer instanceof NotBigCustomer);
		assertFalse(customer instanceof SmallCustomer);
	}

	public void testDesignateBigNotBig() throws Exception {
		Object customer = manager.addType(manager.getObjectFactory().createBlankObject(), BigCustomer.class);
		customer = manager.addType(customer, NotBigCustomer.class);
		assertFalse(customer instanceof Customer);
		assertFalse(customer instanceof BigCustomer);
		assertTrue(customer instanceof NotBigCustomer);
		assertFalse(customer instanceof SmallCustomer);
	}

	public void testDesignateCustomerBigNotBig() throws Exception {
		Object customer = manager.getObjectFactory().createBlankObject();
		customer = manager.addType(customer, BigCustomer.class);
		customer = manager.addType(customer, SmallCustomer.class);
		assertTrue(customer instanceof Customer);
		assertFalse(customer instanceof BigCustomer);
		assertTrue(customer instanceof NotBigCustomer);
		assertTrue(customer instanceof SmallCustomer);
	}

	public void testDesignateCustomerRemoveNotBig() throws Exception {
		RDFObject node = manager.getObjectFactory().createBlankObject();
		Object customer = manager.addType(node, Customer.class);
		customer = manager.removeType(customer, NotBigCustomer.class);
		assertTrue(customer instanceof Customer);
		assertTrue(customer instanceof BigCustomer);
		assertFalse(customer instanceof NotBigCustomer);
		assertFalse(customer instanceof SmallCustomer);
	}

	public void testDesignateSmallCustomer() throws Exception {
		Object customer = manager.addType(manager.getObjectFactory().createBlankObject(), SmallCustomer.class);
		assertTrue(customer instanceof Customer);
		assertFalse(customer instanceof BigCustomer);
		assertTrue(customer instanceof NotBigCustomer);
		assertTrue(customer instanceof SmallCustomer);
	}

	@Override
	protected void setUp() throws Exception {
		module.addConcept(Customer.class);
		module.addConcept(BigCustomer.class);
		module.addConcept(NotBigCustomer.class);
		module.addConcept(SmallCustomer.class);
		super.setUp();
	}

}
