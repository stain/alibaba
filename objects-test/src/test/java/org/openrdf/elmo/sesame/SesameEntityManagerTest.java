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

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import junit.framework.TestCase;

import org.openrdf.elmo.sesame.concepts.Person;
import org.openrdf.elmo.sesame.foaf.PersonImpl;

public class SesameEntityManagerTest extends TestCase {

	private EntityManagerFactory factory;

	private EntityManager manager;

	@Override
	public void runBare() throws Throwable {
		synchronized(Persistence.class) {
			super.runBare();
		}
	}

	@Override
	protected void setUp() throws Exception {
		factory = Persistence.createEntityManagerFactory("test");
		manager = factory.createEntityManager();
	}

	@Override
	protected void tearDown() throws Exception {
		manager.close();
		factory.close();
	}

	public void testFindNew() throws Exception {
		Person person = new PersonImpl();
		person = manager.merge(person);
		assertNotNull(person);
		person.getFoafNames().add("my name");
		assertEquals(1, person.getFoafNames().size());
		assertEquals("my name", person.getFoafNames().iterator().next());
	}

	public void testMerge() throws Exception {
		Person person = new PersonImpl();
		person.getFoafNames().add("my name");
		person = manager.merge(person);
		assertFalse(person instanceof PersonImpl);
		assertEquals(1, person.getFoafNames().size());
		assertEquals("my name", person.getFoafNames().iterator().next());
	}

	public void testAutoMerge() throws Exception {
		Person person = new PersonImpl();
		person.getFoafNames().add("my name");
		person = manager.merge(person);
		Person friend = new PersonImpl();
		friend.getFoafNames().add("my friend's name");
		person.getFoafKnows().add(friend);
		friend = (Person) person.getFoafKnows().toArray()[0];
		assertFalse(friend instanceof PersonImpl);
		assertEquals(1, friend.getFoafNames().size());
		assertEquals("my friend's name", friend.getFoafNames().iterator().next());
	} 

	public void testRecursiveMerge() throws Exception {
		Person person = new PersonImpl();
		person.getFoafNames().add("my name");
		Person friend = new PersonImpl();
		friend.getFoafNames().add("my friend's name");
		friend.getFoafKnows().add(person);
		person.getFoafKnows().add(friend);
		person = manager.merge(person);
		assertFalse(person instanceof PersonImpl);
		assertEquals(1, person.getFoafNames().size());
		assertEquals("my name", person.getFoafNames().iterator().next());
	}
}
