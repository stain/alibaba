package org.openrdf.repository.object.codegen;

import info.aduna.iteration.Iterations;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectFactory;
import org.openrdf.repository.object.ObjectRepository;
import org.openrdf.repository.object.annotations.parameterTypes;
import org.openrdf.repository.object.base.CodeGenTestCase;
import org.openrdf.repository.object.config.ObjectRepositoryFactory;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.memory.MemoryStore;

public class SparqlMethodTest extends CodeGenTestCase {

	@parameterTypes({boolean.class})
	public void testFriends() throws Exception {
		addRdfSource("/ontologies/rdfs-schema.rdf");
		addRdfSource("/ontologies/owl-schema.rdf");
		addRdfSource("/ontologies/object-ontology.owl");
		addRdfSource("/ontologies/sparql-ontology.ttl");
		ObjectRepositoryFactory ofm = new ObjectRepositoryFactory();
		ObjectRepository repo = ofm.getRepository(converter);
		repo.setDelegate(new SailRepository(new MemoryStore()));
		repo.setDataDir(targetDir);
		repo.initialize();
		ObjectConnection con = repo.getConnection();
		// vocabulary
		ObjectFactory of = con.getObjectFactory();
		ClassLoader cl = of.getClassLoader();
		Class<?> Person = Class.forName("foaf.Person", true, cl);
		Method getFoafName = Person.getMethod("getFoafName");
		Method setFoafName = Person.getMethod("setFoafName", String.class);
		Method setFoafLiving = Person.getMethod("setFoafLiving", Boolean.TYPE);
		Method setFoafFriends = Person.getMethod("setFoafFriends", Set.class);
		Method foafGetFriendByName = Person.getMethod("foafGetFriendByName", String.class);
		Method foafGetFriendForName = Person.getMethod("foafGetFriendForName", String.class);
		Method foafGetFOAFs = Person.getMethod("foafGetFOAFs");
		Method foafGetFriendNames = Person.getMethod("foafGetFriendNames");
		Method foafHasFriends = Person.getMethod("foafHasFriends");
		Method foafHasLivingFriends = Person.getMethod("foafHasLivingFriends", Boolean.TYPE);
		Method foafGetFriendsAndName = Person.getMethod("foafGetFriendsAndName");
		Method foafGetFriendNetwork = Person.getMethod("foafGetFriendNetwork");
		Method foafGetFriendTuple = Person.getMethod("foafGetFriendTuple");
		// test data
		Object me = con.addDesignation(of.createObject(), Person);
		setFoafName.invoke(me, "james");
		Object megan = con.addDesignation(of.createObject(), Person);
		setFoafName.invoke(megan, "megan");
		setFoafLiving.invoke(megan, true);
		setFoafFriends.invoke(me, Collections.singleton(megan));
		Object jen = con.addDesignation(of.createObject(), Person);
		setFoafName.invoke(jen, "jen");
		setFoafFriends.invoke(megan, Collections.singleton(jen));
		// test sparql methods
		assertEquals("jen", getFoafName.invoke(((Set)foafGetFOAFs.invoke(me)).iterator().next()));
		assertEquals("megan", getFoafName.invoke(foafGetFriendByName.invoke(me, "megan")));
		assertEquals("megan", getFoafName.invoke(foafGetFriendForName.invoke(me, "megan")));
		assertEquals("megan", getFoafName.invoke(foafGetFriendForName.invoke(me, "jen")));
		assertEquals(Collections.singleton("megan"), foafGetFriendNames.invoke(me));
		assertEquals(Boolean.TRUE, foafHasFriends.invoke(me));
		assertEquals(Boolean.TRUE, foafHasLivingFriends.invoke(me, true));
		Set result = (Set) foafGetFriendsAndName.invoke(me);
		Object[] row = (Object[]) result.iterator().next();
		assertEquals(Arrays.asList(new Object[]{megan, "megan"}), Arrays.asList(row));
		assertEquals(1, Iterations.asSet((GraphQueryResult)foafGetFriendNetwork.invoke(me)).size());
		assertEquals(1, Iterations.asSet((TupleQueryResult)foafGetFriendTuple.invoke(me)).size());
	}

}
