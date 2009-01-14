/*
 * Copyright (c) 2007, James Leigh All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution. 
 * - Neither the name of the openrdf.org nor the names of its contributors may
 *   be used to endorse or promote products derived from this software without
 *   specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 */
package org.openrdf.elmo.sesame;

import java.net.URL;

import javax.naming.InitialContext;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.xml.namespace.QName;

import junit.framework.TestCase;

import org.openrdf.elmo.sesame.concepts.Person;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.repository.sail.SailRepositoryConnection;
import org.openrdf.rio.RDFFormat;
import org.openrdf.sail.memory.MemoryStore;

public class PersistenceProviderTest extends TestCase {

	private static final String BASE = "http://emlo.openrdf.org/model/ElmoSessionTest/";
	private EntityManager manager;
	private EntityManagerFactory factory;

	public void testLoad() throws Exception {
		assertNotNull(manager);
		Person person = manager.find(Person.class, new QName(BASE, "jbroeks"));
		assertNotNull(person);
		assertEquals(27, person.getFoafKnows().size());
	}

	public void testClassPath() throws Exception {
		Person person = manager.find(Person.class, new QName(BASE, "jbroeks"));
		for (Class<?> face : person.getClass().getInterfaces()) {
			if (face.getSimpleName().equals("TestPerson"))
				return;
		}
		fail("did not load jar-file");
	}

	public void testJarFile() throws Exception {
		Person person = manager.find(Person.class, new QName(BASE, "jbroeks"));
		for (Class<?> face : person.getClass().getInterfaces()) {
			if (face.getSimpleName().equals("TestPerson2"))
				return;
		}
		fail("did not scan jar-file");
	}

	@Override
	public void runBare() throws Throwable {
		synchronized (Persistence.class) {
			super.runBare();
		}
	}

	@Override
	protected void setUp() throws Exception {
		SailRepository repo = new SailRepository(new MemoryStore());
		repo.initialize();
		SailRepositoryConnection con = repo.getConnection();
		try {
			ClassLoader cl = Thread.currentThread().getContextClassLoader();
			URL url = cl.getResource("testcases/sesame-foaf.rdf");
			con.add(url, "", RDFFormat.RDFXML, con.getValueFactory().createURI(url.toExternalForm()));
		} finally {
			con.close();
		}
		new InitialContext().addToEnvironment("java:comp/env/repositories/test", repo);
		factory = Persistence.createEntityManagerFactory("test");
		manager = factory.createEntityManager();
	}

	@Override
	protected void tearDown() throws Exception {
		manager.close();
		factory.close();
	}
}
