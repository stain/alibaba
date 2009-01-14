package org.openrdf.elmo.sesame;

import junit.framework.Test;

import org.openrdf.elmo.sesame.base.ElmoManagerTestCase;
import org.openrdf.repository.object.annotations.factory;
import org.openrdf.repository.object.annotations.rdf;

public class ConfectionorFactoryTest extends ElmoManagerTestCase {

	public static Test suite() throws Exception {
		return ElmoManagerTestCase.suite(ConfectionorFactoryTest.class);
	}

	@rdf("urn:test:Candy")
	public interface Candy {
		String taste();
	}

	@rdf("urn:test:Chips")
	public interface Chips {
		String taste();
	}

	@rdf("urn:test:Sweet")
	public interface Sweet {
	}

	@rdf("urn:test:Sour")
	public interface Sour {
	}

	
	@rdf("urn:test:Salty")
	public interface Salty {
	}

	public static class Confectioner {
		@factory
		public Candy createSweetCandy(Sweet sweet) {
			return new Candy() {
				public String taste() {
					return "sweet";
				}
			};
		}
		@factory
		public Candy createSourCandy(Sour sour) {
			return new Candy() {
				public String taste() {
					return "sour";
				}
			};
		}
		@factory
		public Chips createSaltyChips(Salty salty) {
			return new Chips() {
				public String taste() {
					return "salty";
				}
			};
		}
	}

	public void testSweetCandy() throws Exception {
		Candy candy = manager.create(Candy.class);
		candy = (Candy) manager.designate(candy, Sweet.class);
		assertFalse(candy instanceof Chips);
		assertEquals("sweet", candy.taste());
	}

	public void testSourCandy() throws Exception {
		Candy candy = manager.create(Candy.class);
		candy = (Candy) manager.designate(candy, Sour.class);
		assertFalse(candy instanceof Chips);
		assertEquals("sour", candy.taste());
		Chips chips = manager.create(Chips.class);
		chips = (Chips) manager.designate(chips, Salty.class);
		assertFalse(chips instanceof Candy);
		assertEquals("salty", chips.taste());
	}

	public void testSaltyChips() throws Exception {
		Chips chips = manager.create(Chips.class);
		chips = (Chips) manager.designate(chips, Salty.class);
		assertFalse(chips instanceof Candy);
		assertEquals("salty", chips.taste());
	}

	@Override
	protected void setUp() throws Exception {
		module.addConcept(Candy.class);
		module.addConcept(Chips.class);
		module.addConcept(Sweet.class);
		module.addConcept(Sour.class);
		module.addConcept(Salty.class);
		module.addFactory(Confectioner.class);
		super.setUp();
	}

}
