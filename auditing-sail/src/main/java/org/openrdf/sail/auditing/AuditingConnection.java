/*
 * Copyright (c) 2009-2010, James Leigh All rights reserved.
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

import static org.openrdf.sail.auditing.vocabulary.Audit.COMMITTED_ON;
import static org.openrdf.sail.auditing.vocabulary.Audit.CONTAINED;
import static org.openrdf.sail.auditing.vocabulary.Audit.CONTRIBUTED;
import static org.openrdf.sail.auditing.vocabulary.Audit.CURRENT_TRX;
import static org.openrdf.sail.auditing.vocabulary.Audit.MODIFIED;
import static org.openrdf.sail.auditing.vocabulary.Audit.PREDECESSOR;
import static org.openrdf.sail.auditing.vocabulary.Audit.REVISION;
import static org.openrdf.sail.auditing.vocabulary.Audit.TRANSACTION;
import info.aduna.iteration.CloseableIteration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.query.BindingSet;
import org.openrdf.query.Dataset;
import org.openrdf.query.algebra.UpdateExpr;
import org.openrdf.sail.SailConnection;
import org.openrdf.sail.SailException;
import org.openrdf.sail.auditing.vocabulary.Audit;
import org.openrdf.sail.helpers.SailConnectionWrapper;
import org.openrdf.sail.helpers.SailUpdateExecutor;

/**
 * Intercepts the add and remove operations and add a revision to each resource.
 */
public class AuditingConnection extends SailConnectionWrapper {
	private static int MAX_REVISED = 1024;
	private AuditingSail sail;
	private URI trx;
	private DatatypeFactory factory;
	private ValueFactory vf;
	private Map<Resource, Boolean> revised = new LinkedHashMap<Resource, Boolean>(
			128, 0.75f, true) {
		private static final long serialVersionUID = 1863694012435196527L;

		protected boolean removeEldestEntry(Entry<Resource, Boolean> eldest) {
			return size() > MAX_REVISED;
		}
	};
	private Set<Resource> modified = new HashSet<Resource>();
	private List<Statement> metadata = new ArrayList<Statement>();
	private List<Statement> arch = new ArrayList<Statement>();
	private Set<? extends Resource> predecessors;
	private URI currentTrx;

	public AuditingConnection(AuditingSail sail, SailConnection wrappedCon,
			Set<Resource> predecessors) throws DatatypeConfigurationException {
		super(wrappedCon);
		this.sail = sail;
		factory = DatatypeFactory.newInstance();
		vf = sail.getValueFactory();
		currentTrx = vf.createURI(CURRENT_TRX.stringValue());
		this.predecessors = predecessors;
	}

	@Override
	public SailConnection getWrappedConnection() {
		return super.getWrappedConnection();
	}

	public synchronized URI getTransactionURI() throws SailException {
		return getTrx();
	}

	@Override
	public void executeUpdate(UpdateExpr updateExpr, Dataset dataset,
			BindingSet bindings, boolean includeInferred) throws SailException {
		SailUpdateExecutor executor = new SailUpdateExecutor(this, sail.getValueFactory(), false);
		executor.executeUpdate(updateExpr, dataset, bindings, includeInferred);
	}

	@Override
	public synchronized void addStatement(Resource subj, URI pred, Value obj,
			Resource... contexts) throws SailException {
		flushArchive();
		if (subj.equals(currentTrx) || obj.equals(currentTrx) && !Audit.REVISION.equals(pred)) {
			if (contexts == null) {
				addMetadata(subj, pred, obj, null);
			} else if (contexts.length == 1) {
				addMetadata(subj, pred, obj, contexts[0]);
			} else {
				for (Resource ctx : contexts) {
					addMetadata(subj, pred, obj, ctx);
				}
			}
		} else {
			storeStatement(subj, pred, obj, contexts);
		}
	}

	@Override
	public synchronized void removeStatements(Resource subj, URI pred,
			Value obj, Resource... contexts) throws SailException {
		if (sail.isArchiving()) {
			CloseableIteration<? extends Statement, SailException> stmts;
			stmts = super.getStatements(subj, pred, obj, false, contexts);
			try {
				while (stmts.hasNext()) {
					Statement st = stmts.next();
					Resource s = st.getSubject();
					URI p = st.getPredicate();
					Value o = st.getObject();
					Resource ctx = st.getContext();
					removeRevision(s, p);
					if (ctx instanceof URI && !ctx.equals(trx)) {
						if (modified.add(ctx)) {
							super.addStatement(getTrx(), MODIFIED, ctx,
									getTrx());
						}
						BNode node = vf.createBNode();
						super.addStatement(ctx, CONTAINED, node, getTrx());
						super.addStatement(node, RDF.SUBJECT, s, getTrx());
						super.addStatement(node, RDF.PREDICATE, p, getTrx());
						super.addStatement(node, RDF.OBJECT, o, getTrx());
					}
				}
			} finally {
				stmts.close();
			}
			super.removeStatements(subj, pred, obj, contexts);
		} else {
			if (sail.getMaxArchive() > 0 && arch.size() <= sail.getMaxArchive()) {
				CloseableIteration<? extends Statement, SailException> stmts;
				stmts = super.getStatements(subj, pred, obj, false, contexts);
				try {
					int maxArchive = sail.getMaxArchive();
					while (stmts.hasNext() && arch.size() <= maxArchive) {
						Statement st = stmts.next();
						Resource ctx = st.getContext();
						if (ctx instanceof URI && !ctx.equals(trx)) {
							arch.add(st);
						}
					}
				} finally {
					stmts.close();
				}
			}
			super.removeStatements(subj, pred, obj, contexts);
			removeRevision(subj, pred);
			if (contexts != null && contexts.length > 0) {
				for (Resource ctx : contexts) {
					if (ctx != null && modified.add(ctx)) {
						addMetadata(currentTrx, MODIFIED, ctx, currentTrx);
					}
				}
			}
		}
	}

	@Override
	public synchronized void commit() throws SailException {
		flushArchive();
		if (trx != null) {
			for (Statement st : arch) {
				Resource ctx = st.getContext();
				if (ctx instanceof URI) {
					modified.add(ctx);
				}
			}
			for (Resource ctx : modified) {
				if (isObsolete(ctx)) {
					super.addStatement(ctx, RDF.TYPE, Audit.OBSOLETE, trx);
				}
			}
			GregorianCalendar cal = new GregorianCalendar();
			XMLGregorianCalendar xgc = factory.newXMLGregorianCalendar(cal);
			Literal now = vf.createLiteral(xgc);
			super.addStatement(trx, RDF.TYPE, TRANSACTION, trx);
			super.addStatement(trx, COMMITTED_ON, now, trx);
			for (Resource predecessor : predecessors) {
				super.addStatement(trx, PREDECESSOR, predecessor, trx);
			}
			sail.recent(trx, getWrappedConnection());
		}
		super.commit();
		metadata.clear();
		revised.clear();
		modified.clear();
		arch.clear();
		if (trx != null) {
			sail.committed(trx, predecessors);
			predecessors = Collections.singleton(trx);
			trx = null;
		}
	}

	@Override
	public synchronized void rollback() throws SailException {
		trx = null;
		metadata.clear();
		revised.clear();
		modified.clear();
		arch.clear();
		super.rollback();
	}

	public String toString() {
		if (trx != null)
			return trx.stringValue();
		return super.toString();
	}

	private URI getTrx() throws SailException {
		if (trx == null) {
			trx = sail.nextTransaction();
			synchronized (metadata) {
				for (Statement st : metadata) {
					storeStatement(st.getSubject(), st.getPredicate(), st
							.getObject(), st.getContext());
				}
				metadata.clear();
			}
		}
		return trx;
	}

	private void flushArchive() throws SailException {
		if (arch.size() <= sail.getMaxArchive()) {
			for (Statement st : arch) {
				Resource s = st.getSubject();
				URI p = st.getPredicate();
				Value o = st.getObject();
				Resource ctx = st.getContext();
				removeRevision(s, p);
				BNode node = vf.createBNode();
				super.addStatement(ctx, CONTAINED, node, getTrx());
				super.addStatement(node, RDF.SUBJECT, s, getTrx());
				super.addStatement(node, RDF.PREDICATE, p, getTrx());
				super.addStatement(node, RDF.OBJECT, o, getTrx());
				if (ctx instanceof URI && modified.add(ctx)) {
					super.addStatement(getTrx(), MODIFIED, ctx, getTrx());
				}
			}
			arch.clear();
		}
	}

	private void addMetadata(Resource subj, URI pred, Value obj,
			Resource context) throws SailException {
		if (trx == null) {
			synchronized (metadata) {
				metadata.add(vf.createStatement(subj, pred, obj, context));
			}
		} else {
			storeStatement(subj, pred, obj, context);
		}
	}

	private void storeStatement(Resource subj, URI pred, Value obj,
			Resource... contexts) throws SailException {
		if (subj.equals(currentTrx)) {
			subj = getTrx();
		}
		if (obj.equals(currentTrx)) {
			obj = getTrx();
		}
		if (contexts != null && contexts.length == 1) {
			if (currentTrx.equals(contexts[0])) {
				contexts[0] = getTrx();
			}
		} else if (contexts != null) {
			for (int i = 0; i < contexts.length; i++) {
				if (currentTrx.equals(contexts[i])) {
					contexts[i] = getTrx();
				}
			}
		}
		addRevision(subj);
		if (contexts == null || contexts.length == 0 || contexts.length == 1
				&& contexts[0] == null) {
			super.addStatement(subj, pred, obj, getTrx());
		} else if (contexts.length == 1) {
			super.addStatement(subj, pred, obj, contexts);
			Resource ctx = contexts[0];
			if (isURI(ctx) && !ctx.equals(trx) && modified.add(ctx)) {
				super.addStatement(getTrx(), MODIFIED, ctx, getTrx());
			}
		} else {
			super.addStatement(subj, pred, obj, contexts);
			for (Resource ctx : contexts) {
				if (isURI(ctx) && !ctx.equals(trx) && modified.add(ctx)) {
					super.addStatement(getTrx(), MODIFIED, ctx, getTrx());
				}
			}
		}
	}

	private boolean addRevision(Resource subj) throws SailException {
		if (subj instanceof URI) {
			Resource h = getContainerURI(subj);
			Boolean b = revised.get(h);
			if (b != null && b)
				return false;
			revised.put(h, Boolean.TRUE);
			if (!subj.equals(trx)) {
				removeAllRevisions(subj);
				super.addStatement(h, REVISION, getTrx(), getTrx());
				if (b == null) {
					super.addStatement(getTrx(), CONTRIBUTED, h, getTrx());
				}
				return true;
			}
		}
		return false;
	}

	private boolean removeRevision(Resource subj, URI pred) throws SailException {
		if (subj instanceof URI) {
			Resource h = getContainerURI(subj);
			if (revised.containsKey(h))
				return false;
			revised.put(h, Boolean.TRUE);
			if (pred != null && !REVISION.equals(pred)) {
				removeAllRevisions(subj);
				super.addStatement(h, REVISION, getTrx(), getTrx());
			} else {
				URI uri = (URI) subj;
				String ns = uri.getNamespace();
				if (ns.charAt(ns.length() - 1) != '#') {
					if (trx != null) {
						super.removeStatements(subj, REVISION, trx, trx);
					}
					revised.put(subj, Boolean.FALSE);
				}
			}
			addMetadata(currentTrx, CONTRIBUTED, h, currentTrx);
			return true;
		}
		return false;
	}

	private void removeAllRevisions(Resource subj) throws SailException {
		CloseableIteration<? extends Statement, SailException> stmts;
		Resource s = getContainerURI(subj);
		stmts = super.getStatements(s, REVISION, null, true);
		try {
			while (stmts.hasNext()) {
				Statement st = stmts.next();
				Resource ctx = st.getContext();
				if (ctx instanceof URI && modified.add(ctx)) {
					super.addStatement(getTrx(), MODIFIED, ctx, getTrx());
				}
				super.removeStatements(s, REVISION, ctx);
			}
		} finally {
			stmts.close();
		}
	}

	private boolean isURI(Resource s) {
		return s instanceof URI;
	}

	private Resource getContainerURI(Resource subj) {
		if (subj instanceof URI) {
			URI uri = (URI) subj;
			String ns = uri.getNamespace();
			if (ns.charAt(ns.length() - 1) == '#')
				return vf.createURI(ns.substring(0, ns.length() - 1));
		}
		return subj;
	}

	private boolean isObsolete(Resource ctx) throws SailException {
		CloseableIteration<? extends Statement, SailException> stmts;
		stmts = super.getStatements(null, null, null, true, ctx);
		try {
			while (stmts.hasNext()) {
				Statement st = stmts.next();
				URI pred = st.getPredicate();
				Value obj = st.getObject();
				if (RDF.SUBJECT.equals(pred) || RDF.PREDICATE.equals(pred)
						|| RDF.OBJECT.equals(pred))
					continue;
				if (Audit.COMMITTED_ON.equals(pred)
						|| Audit.CONTAINED.equals(pred)
						|| Audit.MODIFIED.equals(pred)
						|| Audit.PREDECESSOR.equals(pred)
						|| Audit.CONTRIBUTED.equals(pred))
					continue;
				if (RDF.TYPE.equals(pred)) {
					if (Audit.TRANSACTION.equals(obj)
							|| Audit.RECENT.equals(obj)
							|| Audit.OBSOLETE.equals(obj)
							|| RDF.STATEMENT.equals(obj))
						continue;
				}
				return false;
			}
		} finally {
			stmts.close();
		}
		return true;
	}
}
