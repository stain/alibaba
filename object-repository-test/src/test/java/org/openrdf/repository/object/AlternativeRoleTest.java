package org.openrdf.repository.object;

import junit.framework.Test;

import org.openrdf.model.URI;
import org.openrdf.model.URIFactory;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.repository.object.annotations.complementOf;
import org.openrdf.repository.object.annotations.intersectionOf;
import org.openrdf.repository.object.annotations.oneOf;
import org.openrdf.repository.object.annotations.rdf;
import org.openrdf.repository.object.base.ObjectRepositoryTestCase;

public class AlternativeRoleTest extends ObjectRepositoryTestCase {

	public static Test suite() throws Exception {
		return ObjectRepositoryTestCase.suite(AlternativeRoleTest.class);
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
		config.addConcept(Animal.class);
		config.addConcept(Pet.class);
		config.addConcept(Wild.class);
		config.addConcept(Trained.class);
		config.addConcept(Dog.class);
		config.addConcept(Cat.class);
		config.addConcept(Horse.class);
		config.addBehaviour(PetSupport.class);
		config.addBehaviour(WildSupport.class);
		config.addConcept(CUJO.class);
		config.addBehaviour(CujoSupport.class);
		config.addBehaviour(TrainedSupport.class);
		config.addConcept(NotTrained.class);
		config.addBehaviour(NotTrainedSupport.class);
		config.addBehaviour(DogSupport.class, new URIImpl((NS + "Dog")));
		config.addConcept(PetCat.class);
		config.addBehaviour(PetCatSupport.class);
		config.addConcept(WildCat.class);
		config.addBehaviour(WildCatSupport.class);
		config.addBehaviour(HorseSupport.class, new URIImpl((NS + "Horse")));
		config.addConcept(TrainedHorse.class);
		config.addBehaviour(TrainedHorseSupport.class);
		config.addConcept(NotRidable.class);
		config.addBehaviour(NotRidableSupport.class);
		super.setUp();
	}

	public void testAnimals() throws Exception {
		con.addType(con.getObject(TOBY), Pet.class);
		con.addType(con.getObject(SANDY), Pet.class);
		con.addType(con.getObject(CUJO), Pet.class);

		con.addType(con.getObject(TOBY), Trained.class);
		con.addType(con.getObject(TRIGGER), Trained.class);

		con.addType(con.getObject(TOBY), Dog.class);
		con.addType(con.getObject(LYCAON), Dog.class);
		con.addType(con.getObject(CUJO), Dog.class);
		con.addType(con.getObject(SANDY), Cat.class);
		con.addType(con.getObject(LINGRA), Cat.class);
		con.addType(con.getObject(TRIGGER), Horse.class);
		con.addType(con.getObject(MUSTANG), Horse.class);

		Animal toby = (Animal) con.getObject(TOBY);
		Animal lycaon = (Animal) con.getObject(LYCAON);
		Animal cujo = (Animal) con.getObject(CUJO);
		Animal sandy = (Animal) con.getObject(SANDY);
		Animal lingra = (Animal) con.getObject(LINGRA);
		Animal trigger = (Animal) con.getObject(TRIGGER);
		Animal mustang = (Animal) con.getObject(MUSTANG);

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
