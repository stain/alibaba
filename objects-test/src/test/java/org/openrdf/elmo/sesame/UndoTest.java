package org.openrdf.elmo.sesame;

import java.util.Collections;
import java.util.Iterator;

import junit.framework.Test;

import org.openrdf.elmo.ElmoManager;
import org.openrdf.elmo.ElmoModule;
import org.openrdf.elmo.Memento;
import org.openrdf.elmo.sesame.base.RepositoryTestCase;
import org.openrdf.elmo.sesame.concepts.Person;
import org.openrdf.repository.event.base.NotifyingRepositoryWrapper;

public class UndoTest extends RepositoryTestCase {

	public static Test suite() throws Exception {
		return RepositoryTestCase.suite(UndoTest.class);
	}

	private SesameManagerFactory factory;

	private ElmoManager manager;

	public UndoTest() {
	}

	public UndoTest(String arg0) {
		super(arg0);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		repository = new NotifyingRepositoryWrapper(repository, true);
		factory = new SesameManagerFactory(new ElmoModule(), repository);
		this.manager = factory.createElmoManager();
	}

	@Override
	protected void tearDown() throws Exception {
		if (manager.getTransaction().isActive())
			manager.getTransaction().rollback();
		manager.close();
		factory.close();
		super.tearDown();
	}

	public void testUndoCreate() throws Exception {
		Memento memento = manager.createMemento();
		manager.create(Person.class);
		Iterable<Person> query = manager.findAll(Person.class);
		Iterator<Person> iter = query.iterator();
		assertTrue(iter.hasNext());
		manager.undoMemento(memento);
		query = manager.findAll(Person.class);
		iter = query.iterator();
		assertFalse(iter.hasNext());
	}

	public void testUndoProperty() throws Exception {
		Person person = manager.create(Person.class);
		Memento memento = manager.createMemento();
		person.setFoafNames(Collections.singleton((Object) "John"));
		int size = 0;
		for (Person p : manager.findAll(Person.class)) {
			size++;
			assertEquals(Collections.singleton("John"), p.getFoafNames());
		}
		assertEquals(1, size);
		manager.undoMemento(memento);
		size = 0;
		for (Person p : manager.findAll(Person.class)) {
			size++;
			assertEquals(0, p.getFoafNames().size());
		}
		assertEquals(1, size);
	}

	public void testUndoCommitted() throws Exception {
		Iterable<Person> query;
		Iterator<Person> iter = null;
		Memento memento = manager.createMemento();
		manager.getTransaction().begin();
		manager.create(Person.class);
		try {
			query = manager.findAll(Person.class);
			iter = query.iterator();
			assertTrue(iter.hasNext());
		} finally {
			manager.close(iter);
		}
		manager.undoMemento(memento);
		try {
			query = manager.findAll(Person.class);
			iter = query.iterator();
			assertFalse(iter.hasNext());
		} finally {
			manager.close(iter);
		}
	}

	public void testUndoNotCommitted() throws Exception {
		Person person = manager.create(Person.class);
		manager.getTransaction().begin();
		Memento memento = manager.createMemento();
		person.setFoafNames(Collections.singleton((Object) "John"));
		manager.undoMemento(memento);
		manager.getTransaction().commit();
		int size = 0;
		for (Person p : manager.findAll(Person.class)) {
			size++;
			assertEquals(0, p.getFoafNames().size());
		}
		assertEquals(1, size);
	}

	public void testUndoRefresh() throws Exception {
		Person person = manager.create(Person.class);
		manager.getTransaction().begin();
		Memento memento = manager.createMemento();
		person.setFoafNames(Collections.singleton((Object) "John"));
		manager.getTransaction().rollback();
		manager.getTransaction().begin();
		manager.refresh(person);
		int size = 0;
		for (Person p : manager.findAll(Person.class)) {
			size++;
			assertEquals(0, p.getFoafNames().size());
		}
		assertEquals(1, size);
		manager.undoMemento(memento);
		manager.getTransaction().commit();
		size = 0;
		for (Person p : manager.findAll(Person.class)) {
			size++;
			assertEquals(0, p.getFoafNames().size());
		}
		assertEquals(1, size);
	}

	public void testUndoRemoveAdd() throws Exception {
		Person person = manager.create(Person.class);
		person.setFoafNames(Collections.singleton((Object) "John"));
		manager.getTransaction().begin();
		person.getFoafNames().clear();
		Memento memento = manager.createMemento();
		person.setFoafNames(Collections.singleton((Object) "John"));
		manager.getTransaction().commit();
		manager.getTransaction().begin();
		int size = 0;
		for (Person p : manager.findAll(Person.class)) {
			size++;
			assertEquals(1, p.getFoafNames().size());
		}
		assertEquals(1, size);
		manager.undoMemento(memento);
		manager.getTransaction().commit();
		size = 0;
		for (Person p : manager.findAll(Person.class)) {
			size++;
			assertEquals(0, p.getFoafNames().size());
		}
		assertEquals(1, size);
	}

	public void testUndoRemove() throws Exception {
		Person person = manager.create(Person.class);
		person.setFoafNames(Collections.singleton((Object) "John"));
		Memento memento = manager.createMemento();
		manager.remove(person);
		int size = 0;
		for (Person p : manager.findAll(Person.class)) {
			size++;
		}
		assertEquals(0, size);
		manager.undoMemento(memento);
		size = 0;
		for (Person p : manager.findAll(Person.class)) {
			size++;
			assertEquals(1, p.getFoafNames().size());
		}
		assertEquals(1, size);
	}

}
