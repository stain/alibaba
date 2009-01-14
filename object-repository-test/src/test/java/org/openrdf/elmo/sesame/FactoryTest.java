package org.openrdf.elmo.sesame;

import javax.xml.namespace.QName;

import junit.framework.Test;

import org.openrdf.elmo.sesame.base.ElmoManagerTestCase;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.repository.object.annotations.factory;
import org.openrdf.repository.object.annotations.oneOf;
import org.openrdf.repository.object.annotations.rdf;

public class FactoryTest extends ElmoManagerTestCase {

	private static final String NS = "urn:test:";

	public static Test suite() throws Exception {
		return ElmoManagerTestCase.suite(FactoryTest.class);
	}
	@rdf(RDFS.NAMESPACE + "Resource")
	public interface HereConcept {
		public abstract boolean isHere();
	}
	public static class HereFactory {
		public static HereFactory getInstance() {
			return new HereFactory();
		}
		private HereFactory() {
		}
		@factory
		public HereConcept createConcept(HereConcept entity) {
			((RDFObject) entity).getQName();
			return new HereConcept() {
				public boolean isHere() {
					return true;
				}
			};
		}
	}
	@oneOf({NS + "root", NS + "boot"})
	public interface NameConcept {
		public abstract String getName();
	}
	@oneOf(NS + "root")
	public static class RootNameFactory {
		@factory
		public NameConcept createConcept() {
			return new NameConcept() {
				public String getName() {
					return "root";
				}
			};
		}
	}
	@oneOf(NS + "boot")
	public static class BootNameFactory {
		@factory
		public NameConcept createConcept() {
			return new NameConcept() {
				public String getName() {
					return "boot";
				}
			};
		}
	}

	@rdf("urn:test:Bank")
	public interface Bank {
		String getBankName();
	}
	@rdf("urn:test:Bank")
	public interface BankAddress {
		String getBankAddress();
	}

	public static class BankFactory {
		@factory
		public Bank createBank() {
			return new Bank() {
				public String getBankName() {
					return "Rambert Trust";
				}
			};
		}
	}

	public static class BankAddressSupport implements BankAddress {
		private Bank bank;

		public BankAddressSupport(Bank bank) {
			super();
			this.bank = bank;
		}

		public String getBankAddress() {
			return bank.getBankName().replace("Trust", "Street");
		}
	}

	public static class BankAddressFactory {
		@factory
		public BankAddressSupport createBankAddress(final Bank bank) {
			return new BankAddressSupport(bank);
		}
	}

	public void testGlobalFactory() throws Exception {
		HereConcept entity = (HereConcept) manager.find(new QName(NS, "root"));
		assertTrue(entity.isHere());
	}

	public void testOneOfFactory() throws Exception {
		NameConcept root = (NameConcept) manager.find(new QName(NS, "root"));
		assertEquals("root", root.getName());
		NameConcept boot = (NameConcept) manager.find(new QName(NS, "boot"));
		assertEquals("boot", boot.getName());
		RDFObject hoot =  manager.find(new QName(NS, "hoot"));
		assertFalse(hoot instanceof NameConcept);
	}

	public void testBankName() throws Exception {
		Bank bank = manager.create(Bank.class);
		assertNotNull(bank.getBankName());
	}

	public void testBankAddress() throws Exception {
		BankAddress bank = (BankAddress) manager.create(Bank.class);
		assertNotNull(bank.getBankAddress());
	}

	@Override
	protected void setUp() throws Exception {
		module.addConcept(HereConcept.class);
		module.addConcept(NameConcept.class);
		module.addFactory(HereFactory.class);
		module.addFactory(RootNameFactory.class);
		module.addFactory(BootNameFactory.class);
		module.addConcept(Bank.class);
		module.addConcept(BankAddress.class);
		module.addFactory(BankFactory.class);
		module.addBehaviour(BankAddressSupport.class);
		module.addFactory(BankAddressFactory.class);
		super.setUp();
	}

}
