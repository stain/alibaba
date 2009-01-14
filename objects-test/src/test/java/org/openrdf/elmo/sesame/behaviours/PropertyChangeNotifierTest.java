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
package org.openrdf.elmo.sesame.behaviours;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Iterator;

import javax.xml.namespace.QName;

import junit.framework.TestCase;

import org.openrdf.elmo.ElmoManager;
import org.openrdf.elmo.ElmoModule;
import org.openrdf.elmo.annotations.rdf;
import org.openrdf.elmo.sesame.SesameManager;
import org.openrdf.elmo.sesame.SesameManagerFactory;
import org.openrdf.elmo.sesame.concepts.Channel;
import org.openrdf.elmo.sesame.concepts.Person;
import org.openrdf.elmo.sesame.roles.PropertyChangeNotifier;
import org.openrdf.repository.Repository;
import org.openrdf.repository.event.base.NotifyingRepositoryWrapper;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.memory.MemoryStore;

public class PropertyChangeNotifierTest extends TestCase {

	private ElmoManager manager;

	int fireCount;

	private PropertyChangeListener sc = new PropertyChangeListener() {
		public void propertyChange(PropertyChangeEvent evt) {
			fireCount++;
		}
	};

	private SesameManagerFactory factory;

	@Override
	protected void setUp() throws Exception {
		Repository repository = new SailRepository(new MemoryStore());
		repository = new NotifyingRepositoryWrapper(repository, true);
		repository.initialize();
		ElmoModule module = new ElmoModule();
		module.addBehaviour(PropertyChangeNotifierSupport.class, "http://www.w3.org/2000/01/rdf-schema#Resource");
		factory = new SesameManagerFactory(module, repository);
		manager = factory.createElmoManager();
		fireCount = 0;
	}

	@Override
	protected void tearDown() throws Exception {
		manager.close();
		factory.close();
		factory.getRepository().shutDown();
	}

	public void testSingleModification() {
		Channel bean = manager.create(Channel.class);
		assertTrue(bean instanceof PropertyChangeNotifier);
		assertEquals(0, fireCount);
		((PropertyChangeNotifier) bean).addPropertyChangeListener(sc);
		assertEquals(0, fireCount);
		assertNull(bean.getRssTitle());
		assertEquals(0, fireCount);
		bean.setRssTitle("title");
		assertEquals(1, fireCount);
	}

	public void testMultipleModification() {
		Channel bean = manager.create(Channel.class);
		assertTrue(bean instanceof PropertyChangeNotifier);
		assertEquals(0, fireCount);
		((PropertyChangeNotifier) bean).addPropertyChangeListener(sc);
		assertEquals(0, fireCount);
		assertNull(bean.getRssTitle());
		assertEquals(0, fireCount);
		manager.getTransaction().begin();
		bean.setRssTitle("title");
		bean.setRssDescription("desc");
		assertEquals(0, fireCount);
		manager.getTransaction().commit();
		assertEquals(1, fireCount);
	}

	public void testSetModification() {
		Person bean = manager.create(Person.class);
		assertTrue(bean instanceof PropertyChangeNotifier);
		((PropertyChangeNotifier) bean).addPropertyChangeListener(sc);
		assertEquals(0, fireCount);
		bean.getFoafNames().add("John");
		assertEquals(1, fireCount);
	}

	public void testIteratorModification() {
		Person bean = manager.create(Person.class);
		bean.getFoafNames().add("John");
		assertTrue(bean instanceof PropertyChangeNotifier);
		((PropertyChangeNotifier) bean).addPropertyChangeListener(sc);
		assertEquals(0, fireCount);
		Iterator<Object> iter = bean.getFoafNames().iterator();
		iter.next();
		iter.remove();
		assertEquals(1, fireCount);
	}

	public void testRefresh() {
		Person bean = manager.create(Person.class);
		bean.getFoafNames().add("John");
		assertTrue(bean instanceof PropertyChangeNotifier);
		((PropertyChangeNotifier) bean).addPropertyChangeListener(sc);
		assertEquals(0, fireCount);
		manager.refresh(bean);
		assertEquals(1, fireCount);
	}

	public void testNotifyAllListeners() {
		Person bean = manager.create(Person.class);
		bean.getFoafNames().add("John");
		assertTrue(bean instanceof PropertyChangeNotifier);
		((PropertyChangeNotifier) bean).addPropertyChangeListener(sc);
		assertEquals(0, fireCount);
		PropertyChangeNotifierSupport.notifyAllListenersOf((SesameManager) manager);
		assertEquals(1, fireCount);
	}

	@rdf("urn:people:Me")
	public interface Me {
		@rdf("urn:people:name")
		String getName();
		void setName(String name);
	}

	public void testMerge() throws Exception {
		ElmoManager manager = createElmoManager();
		Me me = manager.designate(new QName("urn:people:", "me"), Me.class);
		((PropertyChangeNotifier) me).addPropertyChangeListener(sc);
		ElmoManager m2 = createElmoManager();
		Me me2 = m2.designate(new QName("urn:people:", "me"), Me.class);
		me2.setName("James");
		manager.merge(me2);
		assertEquals(1, fireCount);
	}

	private ElmoManager createElmoManager() {
		ElmoModule module = new ElmoModule();
		module.addBehaviour(PropertyChangeNotifierSupport.class, "urn:people:Me");
		module.addConcept(Me.class);
		SesameManagerFactory factory = new SesameManagerFactory(module);
		return factory.createElmoManager();
	}

}
