package org.openrdf.elmo.sesame;

import javax.xml.namespace.QName;

import junit.framework.Test;

import org.openrdf.elmo.annotations.complementOf;
import org.openrdf.elmo.annotations.intersectionOf;
import org.openrdf.elmo.annotations.oneOf;
import org.openrdf.elmo.annotations.rdf;
import org.openrdf.elmo.sesame.base.ElmoManagerTestCase;

public class AlternativeRoleTest extends ElmoManagerTestCase {

	public static Test suite() throws Exception {
		return ElmoManagerTestCase.suite(AlternativeRoleTest.class);
	}

	public enum Friendly { FRIENDLY, NOT_FRIENDLY }
	public enum Ridable { RIDABLE, NOT_RIDABLE }
	public enum Behaves { BEHAVES, DOES_NOT_BEHAVE }

	@rdf(NS + "Animal")
	public static interface Animal {
		String disturb();

		Friendly isFriendly();

		Ridable isRidable();

		Behaves isBehaved();
	}

	@rdf(NS + "Pet")
	public static interface Pet {
	}

	@rdf(NS + "Wild")
	public static interface Wild {
	}

	@rdf(NS + "Trained")
	public static interface Trained {
	}

	@rdf(NS + "Dog")
	public static interface Dog extends Animal {
	}

	@rdf(NS + "Cat")
	public static interface Cat extends Animal {
	}

	@rdf(NS + "Horse")
	public static interface Horse extends Animal {
	}

	@rdf(NS + "Pet")
	public static class PetSupport implements Pet {
		public Friendly isFriendly() {
			return Friendly.FRIENDLY;
		}
	}

	@oneOf({NS + "cujo"})
	public static class CujoSupport extends PetSupport implements Pet  {
		@Override
		public Friendly isFriendly() {
			return Friendly.NOT_FRIENDLY;
		}
	}

	@complementOf(Pet.class)
	public static class WildSupport implements Wild {
		public Friendly isFriendly() {
			return Friendly.NOT_FRIENDLY;
		}
	}

	@rdf(NS + "Trained")
	public static class TrainedSupport implements Trained {
		public Behaves isBehaved() {
			return Behaves.BEHAVES;
		}
	}

	@complementOf(Trained.class)
	public static class NotTrainedSupport {
		public Behaves isBehaved() {
			return Behaves.DOES_NOT_BEHAVE;
		}
	}

	@rdf(NS + "Dog")
	public static class DogSupport {
		public String disturb() {
			return "Bark!";
		}
	}

	@intersectionOf( { Cat.class, Pet.class })
	public static class PetCatSupport {
		public String disturb() {
			return "Meow";
		}
	}

	@intersectionOf( { Cat.class, Wild.class })
	public static class WildCatSupport {
		public String disturb() {
			return "Hiss";
		}
	}

	@rdf(NS + "Horse")
	public static class HorseSupport {
		public String disturb() {
			return "Neigh";
		}
	}

	@intersectionOf( { Horse.class, Trained.class })
	public static interface TrainedHorse extends Horse, Trained {
	}

	@intersectionOf( { Horse.class, Trained.class })
	public static class TrainedHorseSupport {
		public Ridable isRidable() {
			return Ridable.RIDABLE;
		}
	}

	@complementOf(TrainedHorse.class)
	public static class NotRidableSupport {
		public Ridable isRidable() {
			return Ridable.NOT_RIDABLE;
		}
	}

	private static final String NS = "http://www.example.com/rdf/2007/";

	private static final QName TOBY = new QName(NS, "toby");

	private static final QName LYCAON = new QName(NS, "lycaon");

	private static final QName CUJO = new QName(NS, "cujo");

	private static final QName SANDY = new QName(NS, "sandy");

	private static final QName LINGRA = new QName(NS, "lingra");

	private static final QName TRIGGER = new QName(NS, "trigger");

	private static final QName MUSTANG = new QName(NS, "mustang");

	@Override
	protected void setUp() throws Exception {
		module.addConcept(Animal.class);
		module.addConcept(Pet.class);
		module.addConcept(Wild.class);
		module.addConcept(Trained.class);
		module.addConcept(Dog.class);
		module.addConcept(Cat.class);
		module.addConcept(Horse.class);
		module.addBehaviour(PetSupport.class);
		module.addBehaviour(WildSupport.class);
		module.addBehaviour(CujoSupport.class);
		module.addBehaviour(TrainedSupport.class);
		module.addBehaviour(NotTrainedSupport.class);
		module.addBehaviour(DogSupport.class);
		module.addBehaviour(PetCatSupport.class);
		module.addBehaviour(WildCatSupport.class);
		module.addBehaviour(HorseSupport.class);
		module.addConcept(TrainedHorse.class);
		module.addBehaviour(TrainedHorseSupport.class);
		module.addBehaviour(NotRidableSupport.class);
		super.setUp();
	}

	public void testAnimals() {
		manager.designate(TOBY, Pet.class);
		manager.designate(SANDY, Pet.class);
		manager.designate(CUJO, Pet.class);

		manager.designate(TOBY, Trained.class);
		manager.designate(TRIGGER, Trained.class);

		manager.designate(TOBY, Dog.class);
		manager.designate(LYCAON, Dog.class);
		manager.designate(CUJO, Dog.class);
		manager.designate(SANDY, Cat.class);
		manager.designate(LINGRA, Cat.class);
		manager.designate(TRIGGER, Horse.class);
		manager.designate(MUSTANG, Horse.class);

		Animal toby = (Animal) manager.find(TOBY);
		Animal lycaon = (Animal) manager.find(LYCAON);
		Animal cujo = (Animal) manager.find(CUJO);
		Animal sandy = (Animal) manager.find(SANDY);
		Animal lingra = (Animal) manager.find(LINGRA);
		Animal trigger = (Animal) manager.find(TRIGGER);
		Animal mustang = (Animal) manager.find(MUSTANG);

		assertEquals(Friendly.FRIENDLY, toby.isFriendly());
		assertEquals(Friendly.NOT_FRIENDLY, lycaon.isFriendly());
		assertEquals(Friendly.NOT_FRIENDLY, cujo.isFriendly());
		assertEquals(Friendly.FRIENDLY, sandy.isFriendly());
		assertEquals(Friendly.NOT_FRIENDLY, lingra.isFriendly());
		assertEquals(Friendly.NOT_FRIENDLY, trigger.isFriendly());
		assertEquals(Friendly.NOT_FRIENDLY, mustang.isFriendly());

		assertEquals("Bark!", toby.disturb());
		assertEquals("Bark!", lycaon.disturb());
		assertEquals("Bark!", cujo.disturb());
		assertEquals("Meow", sandy.disturb());
		assertEquals("Hiss", lingra.disturb());
		assertEquals("Neigh", trigger.disturb());
		assertEquals("Neigh", mustang.disturb());

		assertEquals(Behaves.BEHAVES, toby.isBehaved());
		assertEquals(Behaves.DOES_NOT_BEHAVE, lycaon.isBehaved());
		assertEquals(Behaves.DOES_NOT_BEHAVE, cujo.isBehaved());
		assertEquals(Behaves.DOES_NOT_BEHAVE, sandy.isBehaved());
		assertEquals(Behaves.DOES_NOT_BEHAVE, lingra.isBehaved());
		assertEquals(Behaves.BEHAVES, trigger.isBehaved());
		assertEquals(Behaves.DOES_NOT_BEHAVE, mustang.isBehaved());

		assertEquals(Ridable.NOT_RIDABLE, toby.isRidable());
		assertEquals(Ridable.NOT_RIDABLE, lycaon.isRidable());
		assertEquals(Ridable.NOT_RIDABLE, cujo.isRidable());
		assertEquals(Ridable.NOT_RIDABLE, sandy.isRidable());
		assertEquals(Ridable.NOT_RIDABLE, lingra.isRidable());
		assertEquals(Ridable.RIDABLE, trigger.isRidable());
		assertEquals(Ridable.NOT_RIDABLE, mustang.isRidable());

	}
}
