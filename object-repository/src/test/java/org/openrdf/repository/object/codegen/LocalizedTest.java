package org.openrdf.repository.object.codegen;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectFactory;
import org.openrdf.repository.object.ObjectRepository;
import org.openrdf.repository.object.base.CodeGenTestCase;
import org.openrdf.repository.object.config.ObjectRepositoryFactory;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.memory.MemoryStore;

public class LocalizedTest extends CodeGenTestCase {

	public void testLocalized() throws Exception {
		addRdfSource("/ontologies/localized-ontology.owl");
		ObjectRepositoryFactory ofm = new ObjectRepositoryFactory();
		ObjectRepository repo = ofm.getRepository(converter);
		repo.setDelegate(new SailRepository(new MemoryStore()));
		repo.setDataDir(targetDir);
		repo.initialize();
		ObjectConnection manager = repo.getConnection();
		ClassLoader cl = manager.getObjectFactory().getClassLoader();
		Class<?> Document = Class.forName("local.Document", true, cl);
		Method getSubject = Document.getMethod("getLocalSubject");
		ObjectFactory of = manager.getObjectFactory();
		Object document = manager.addDesignation(of.createObject(), Document);
		manager.setLanguage("en");
		((Set)getSubject.invoke(document)).add("user");
		((Set)getSubject.invoke(document)).add("guide");
		manager.setLanguage("fr");
		assertEquals(new HashSet(Arrays.asList("user", "guide")), getSubject.invoke(document));
		((Set)getSubject.invoke(document)).add("guide");
		((Set)getSubject.invoke(document)).add("utilisateur");
		assertEquals(new HashSet(Arrays.asList("guide", "utilisateur")), getSubject.invoke(document));
		manager.setLanguage("en");
		assertEquals(new HashSet(Arrays.asList("user", "guide")), getSubject.invoke(document));
		manager.close();
		repo.shutDown();
	}

	public void testFunctionalLocalized() throws Exception {
		addRdfSource("/ontologies/localized-ontology.owl");
		ObjectRepositoryFactory ofm = new ObjectRepositoryFactory();
		ObjectRepository repo = ofm.getRepository(converter);
		repo.setDelegate(new SailRepository(new MemoryStore()));
		repo.setDataDir(targetDir);
		repo.initialize();
		ObjectConnection manager = repo.getConnection();
		ClassLoader cl = manager.getObjectFactory().getClassLoader();
		Class<?> Document = Class.forName("local.Document", true, cl);
		Method setTitle = Document.getMethod("setLocalTitle", String.class);
		Method getTitle = Document.getMethod("getLocalTitle");
		ObjectFactory of = manager.getObjectFactory();
		Object document = manager.addDesignation(of.createObject(), Document);
		manager.setLanguage("en");
		setTitle.invoke(document, "User Guide");
		manager.setLanguage("fr");
		assertEquals("User Guide", getTitle.invoke(document));
		setTitle.invoke(document, "Guide de l'Utilisateur");
		assertEquals("Guide de l'Utilisateur", getTitle.invoke(document));
		manager.setLanguage("en");
		assertEquals("User Guide", getTitle.invoke(document));
		manager.close();
		repo.shutDown();
	}
}
