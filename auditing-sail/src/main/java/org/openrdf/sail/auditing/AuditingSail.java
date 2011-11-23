/*
 * Copyright (c) 2009, James Leigh All rights reserved.
 * Copyright (c) 2011 Talis Inc., Some rights reserved.
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
package org.openrdf.sail.auditing;

import info.aduna.iteration.CloseableIteration;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.Duration;

import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.sail.Sail;
import org.openrdf.sail.SailConnection;
import org.openrdf.sail.SailException;
import org.openrdf.sail.auditing.vocabulary.Audit;
import org.openrdf.sail.helpers.SailWrapper;

/**
 * Saves triples to a unique graph when no graph is specified.
 */
public class AuditingSail extends SailWrapper {
	private static final String prefix = "t"
			+ Long.toHexString(System.currentTimeMillis()) + "x";
	private static final AtomicLong seq = new AtomicLong(0);
	private String ns;
	private boolean archiving;
	private int maxArchive;
	private int minRecent;
	private int maxRecent;
	private Duration purgeAfter;
	private Queue<Resource> recent = null;
	private final Set<Resource> predecessors = new HashSet<Resource>();
	private long nextPurge = Long.MAX_VALUE;

	public AuditingSail() {
		super();
	}

	public AuditingSail(Sail baseSail) {
		super(baseSail);
	}

	public String getNamespace() {
		return ns;
	}

	public void setNamespace(String ns) {
		this.ns = ns;
	}

	public boolean isArchiving() {
		return archiving;
	}

	public void setArchiving(boolean archiving) {
		this.archiving = archiving;
	}

	public int getMaxArchive() {
		return maxArchive;
	}

	public void setMaxArchive(int maxArchive) {
		this.maxArchive = maxArchive;
	}

	public int getMinRecent() {
		return minRecent;
	}

	public void setMinRecent(int minRecent) {
		this.minRecent = minRecent;
	}

	public int getMaxRecent() {
		return maxRecent;
	}

	public void setMaxRecent(int maxRecent) {
		this.maxRecent = maxRecent;
		if (maxRecent > 0 && recent == null) {
			recent = new ArrayDeque<Resource>(maxRecent + 1);
		} else if (maxRecent <= 0 && recent != null) {
			recent = null;
		}
	}

	public Duration getPurgeAfter() {
		return purgeAfter;
	}

	public void setPurgeAfter(Duration purgeAfter) {
		this.purgeAfter = purgeAfter;
	}

	@Override
	public void initialize() throws SailException {
		super.initialize();
		if (ns == null) {
			RuntimeMXBean bean = ManagementFactory.getRuntimeMXBean();
			ns = "urn:trx:" + bean.getName() + ":";
		}
		SailConnection con = super.getConnection();
		try {
			Set<Resource> set = loadRecent(con);
			if (recent != null) {
				recent.addAll(set);
			}
			removePredecessorsFrom(set, con);
			this.predecessors.addAll(set);
			if (purgeAfter != null) {
				long now = System.currentTimeMillis();
				purgeObsolete(now, con);
			}
			con.commit();
		} finally {
			con.close();
		}
	}

	@Override
	public void shutDown() throws SailException {
		if (recent == null && !predecessors.isEmpty()) {
			SailConnection con = super.getConnection();
			try {
				// record predecessors
				con.removeStatements(null, RDF.TYPE, Audit.RECENT);
				for (Resource trx : predecessors) {
					con.addStatement(trx, RDF.TYPE, Audit.RECENT);
				}
				con.commit();
			} finally {
				con.close();
			}
		}
		super.shutDown();
	}

	@Override
	public SailConnection getConnection() throws SailException {
		try {
			return new AuditingConnection(this, super.getConnection(), getPredecessors());
		} catch (DatatypeConfigurationException e) {
			throw new SailException(e);
		}
	}

	public URI nextTransaction() {
		return getValueFactory().createURI(ns, prefix + seq.getAndIncrement());
	}

	public String toString() {
		return String.valueOf(getDataDir());
	}

	protected Set<Resource> getPredecessors() {
		synchronized (predecessors) {
			return new HashSet<Resource>(predecessors);
		}
	}

	void recent(URI trx, SailConnection con) throws SailException {
		if (recent != null) {
			synchronized (this.predecessors) {
				int size = predecessors.size();
				if (recent.size() >= maxRecent && recent.size() > size) {
					while ((recent.size() >= minRecent || recent.size() >= maxRecent)
							&& recent.size() > size) {
						Resource old = recent.poll();
						if (old == null)
							break;
						if (predecessors.contains(old)) {
							// old has not yet been succeeded
							recent.add(old);
						} else {
							con.removeStatements(old, RDF.TYPE, Audit.RECENT);
						}
					}
				}
				recent.add(trx);
			}
			con.addStatement(trx, RDF.TYPE, Audit.RECENT, trx);
		}
	}

	void committed(URI trx, Set<Resource> set) throws SailException {
		synchronized (this.predecessors) {
			this.predecessors.removeAll(set);
			this.predecessors.add(trx);
		}
		if (purgeAfter != null) {
			long now = System.currentTimeMillis();
			if (nextPurge < Long.MAX_VALUE && now >= nextPurge) {
				SailConnection con = super.getConnection();
				try {
					purgeObsolete(now, con);
					con.commit();
				} finally {
					con.close();
				}
			}
		}
	}

	private Set<Resource> loadRecent(SailConnection con) throws SailException {
		Set<Resource> set = new HashSet<Resource>();
		CloseableIteration<? extends Statement, SailException> stmts;
		stmts = con.getStatements(null, RDF.TYPE, Audit.RECENT, true);
		try {
			while (stmts.hasNext()) {
				set.add(stmts.next().getSubject());
			}
		} finally {
			stmts.close();
		}
		return set;
	}

	private void removePredecessorsFrom(Set<Resource> set, SailConnection con) throws SailException {
		CloseableIteration<? extends Statement, SailException> stmts;
		// trim predecessors to a minimal set
		for (Resource predecessor : new ArrayList<Resource>(set)) {
			stmts = con.getStatements(predecessor, Audit.PREDECESSOR, null, true);
			try {
				while (stmts.hasNext()) {
					Value trx = stmts.next().getObject();
					set.remove(trx);
				}
			} finally {
				stmts.close();
			}
		}
	}

	private void purgeObsolete(long now, SailConnection con)
			throws SailException {
		Date earlier = new Date(now);
		purgeAfter.negate().addTo(earlier);
		purgeEarlier(earlier.getTime(), con);
		Date next = new Date(now);
		purgeAfter.addTo(next);
		nextPurge = next.getTime();
	}

	private void purgeEarlier(long earlier, SailConnection con)
			throws SailException {
		CloseableIteration<? extends Statement, SailException> stmts;
		stmts = con.getStatements(null, RDF.TYPE, Audit.OBSOLETE, true);
		try {
			while (stmts.hasNext()) {
				Resource trx = stmts.next().getSubject();
				long time = getCommitTime(trx, con);
				if (time < earlier) {
					con.removeStatements(null, null, null, trx);
					con.removeStatements(trx, RDF.TYPE, Audit.OBSOLETE);
				}
			}
		} finally {
			stmts.close();
		}
	}

	private long getCommitTime(Resource trx, SailConnection con)
			throws SailException {
		CloseableIteration<? extends Statement, SailException> c;
		c = con.getStatements(trx, Audit.COMMITTED_ON, null, false);
		try {
			if (c.hasNext()) {
				Value lit = c.next().getObject();
				if (lit instanceof Literal) {
					return ((Literal) lit).calendarValue().toGregorianCalendar().getTimeInMillis();
				}
			}
		} catch (IllegalArgumentException e) {
			// bad data
			return -1;
		} finally {
			c.close();
		}
		return -1;
	}
}
