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
package org.openrdf.elmo.sesame.writers;

import java.io.StringWriter;

import javax.xml.namespace.QName;

import junit.framework.Test;

import org.openrdf.elmo.ElmoManager;
import org.openrdf.elmo.ElmoModule;
import org.openrdf.elmo.sesame.SesameManagerFactory;
import org.openrdf.elmo.sesame.base.RepositoryTestCase;
import org.openrdf.elmo.sesame.concepts.Channel;
import org.openrdf.elmo.sesame.concepts.DcResource;
import org.openrdf.elmo.sesame.concepts.Item;
import org.openrdf.elmo.sesame.concepts.Seq;
import org.openrdf.elmo.sesame.roles.SesameEntity;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.rio.rss.RssWriter;

public class RssWriterTest extends RepositoryTestCase {
	public static Test suite() throws Exception {
		return RepositoryTestCase.suite(RssWriterTest.class);
	}

	private SesameManagerFactory factory;

	private ElmoManager manager;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		factory = new SesameManagerFactory(new ElmoModule(), repository);
		this.manager = factory.createElmoManager();
	}

	@Override
	protected void tearDown() throws Exception {
		manager.close();
		factory.close();
		super.tearDown();
	}

	public void testOutput() throws Exception {

		Channel channel = createChannel();

		StringWriter writer = new StringWriter();
		RssWriter rss = new RssWriter(writer);
		RepositoryConnection conn = factory.getRepository().getConnection();
		rss.setConnection(conn);
		rss.startRDF();
		rss.printChannel(((SesameEntity) channel).getSesameResource());
		rss.endRDF();
		rss.close();
		conn.close();

		String output = writer.toString();
		assertTrue("output contains rdf:Description", output
				.indexOf("rdf:Description") < 0);
	}

	private Channel createChannel() {

		Channel channel = manager.designate(new QName("urn:",
				"channel"), Channel.class);
		channel.setRssTitle("tile");
		channel.getRssLinks().add("about:blank");
		channel.setRssDescription("description");
		((DcResource) channel).getDcSubjects().add("subject");

		Seq items = manager.create(Seq.class);
		channel.setRssItems(items);

		Item item = manager.designate(new QName("urn:", "item"), Item.class);
		item.setRssTitle("title");
		item.getRssLinks().add("about:blank");
		item.setRssDescription("description");
		items.add(item);

		return channel;
	}
}
