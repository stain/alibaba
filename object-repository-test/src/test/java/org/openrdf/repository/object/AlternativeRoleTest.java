package org.openrdf.repository.object;

import junit.framework.Test;

import org.openrdf.model.URI;
import org.openrdf.model.URIFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.repository.object.annotations.complementOf;
import org.openrdf.repository.object.annotations.intersectionOf;
import org.openrdf.repository.object.annotations.oneOf;
import org.openrdf.repository.object.annotations.rdf;
import org.openrdf.repository.object.base.ElmoManagerTestCase;

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

	@complementOf(Pet.class)
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

	public static class PetSupport implements Pet {
		public Friendly isFriendly() {
			return Friendly.FRIENDLY;
		}
	}

	@oneOf({NS + "cujo"})
	public static interface CUJO extends Pet  {
	}

	public static class CujoSupport extends PetSupport implements CUJO  {
		@Override
		public Friendly isFriendly() {
			return Friendly.NOT_FRIENDLY;
		}
	}

	public static class WildSupport implements Wild {
		public Friendly isFriendly() {
			return Friendly.NOT_FRIENDLY;
		}
	}

	public static class TrainedSupport implements Trained {
		public Behaves isBehaved() {
			return Behaves.BEHAVES;
		}
	}

	@complementOf(Trained.class)
	public interface NotTrained {
	}

	public static class NotTrainedSupport implements NotTrained {
		public Behaves isBehaved() {
			return Behaves.DOES_NOT_BEHAVE;
		}
	}

	public static class DogSupport {
		public String disturb() {
			return "Bark!";
		}
	}

	@intersectionOf( { Cat.class, Pet.class })
	public interface PetCat {
	}

	public static class PetCatSupport implements PetCat {
		public String disturb() {
			return "Meow";
		}
	}

	@intersectionOf( { Cat.class, Wild.class })
	public interface WildCat {
	}

	public static class WildCatSupport implements WildCat {
		public String disturb() {
			return "Hiss";
		}
	}

	public static class HorseSupport {
		public String disturb() {
			return "Neigh";
		}
	}

	@intersectionOf( { Horse.class, Trained.class })
	public static interface TrainedHorse extends Horse, Trained {
	}

	public static abstract class TrainedHorseSupport implements TrainedHorse {
		public Ridable isRidable() {
			return Ridable.RIDABLE;
		}
	}

	@complementOf(TrainedHorse.class)
	public interface NotRidable {
	}

	public static class NotRidableSupport implements NotRidable {
		public Ridable isRidable() {
			return Ridable.NOT_RIDABLE;
		}
	}

	private static final String NS = "http://www.example.com/rdf/2007/";

	private static final URIFactory vf = ValueFactoryImpl.getInstance();

	private static final URI TOBY = vf.createURI(NS, "toby");

	private static final URI LYCAON = vf.createURI(NS, "lycaon");

	private static final URI CUJO = vf.createURI(NS, "cujo");

	private static final URI SANDY = vf.createURI(NS, "sandy");

	private static final URI LINGRA = vf.createURI(NS, "lingra");

	private static final URI TRIGGER = vf.createURI(NS, "trigger");

	private static final URI MUSTANG = vf.createURI(NS, "mustang");

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
		module.addConcept(CUJO.class);
		module.addBehaviour(CujoSupport.class);
		module.addBehaviour(TrainedSupport.class);
		module.addConcept(NotTrained.class);
		module.addBehaviour(NotTrainedSupport.class);
		module.addBehaviour(DogSupport.class, NS + "Dog");
		module.addConcept(PetCat.class);
		module.addBehaviour(PetCatSupport.class);
		module.addConcept(WildCat.class);
		module.addBehaviour(WildCatSupport.class);
		module.addBehaviour(HorseSupport.class, NS + "Horse");
		module.addConcept(TrainedHorse.class);
		module.addBehaviour(TrainedHorseSupport.class);
		module.addConcept(NotRidable.class);
		module.addBehaviour(NotRidableSupport.class);
		super.setUp();
	}

	public void testAnimals() throws Exception {
		manager.designate(manager.find(TOBY), Pet.class);
		manager.designate(manager.find(SANDY), Pet.class);
		manager.designate(manager.find(CUJO), Pet.class);

		manager.designate(manager.find(TOBY), Trained.class);
		manager.designate(manager.find(TRIGGER), Trained.class);

		manager.designate(manager.find(TOBY), Dog.class);
		manager.designate(manager.find(LYCAON), Dog.class);
		manager.designate(manager.find(CUJO), Dog.class);
		manager.designate(manager.find(SANDY), Cat.class);
		manager.designate(manager.find(LINGRA), Cat.class);
		manager.designate(manager.find(TRIGGER), Horse.class);
		manager.designate(manager.find(MUSTANG), Horse.class);

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
