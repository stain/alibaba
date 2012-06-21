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
import org.openrdf.model.impl.MemoryOverflowModel;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.query.BindingSet;
import org.openrdf.query.Dataset;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.algebra.StatementPattern;
import org.openrdf.query.algebra.UpdateExpr;
import org.openrdf.query.algebra.Var;
import org.openrdf.sail.SailConnection;
import org.openrdf.sail.SailException;
import org.openrdf.sail.auditing.helpers.BasicGraphPatternVisitor;
import org.openrdf.sail.auditing.vocabulary.Audit;
import org.openrdf.sail.helpers.SailConnectionWrapper;
import org.openrdf.sail.helpers.SailUpdateExecutor;

/**
 * Intercepts the add and remove operations and add a revision to each resource.
 */
public class AuditingConnection extends SailConnectionWrapper {
	private static final String AUDIT_2012 = "http://www.openrdf.org/rdf/2012/auditing#";
	private static final String PROV = "http://www.w3.org/ns/prov#";
	private static final String WAS_INFORMED_BY = PROV + "wasInformedBy";
	private static final String QUALIFIED_USAGE = PROV + "qualifiedUsage";
	private static final String ENTITY = PROV + "entity";
	private static final String GENERATED_BY = PROV + "wasGeneratedBy";
	private static final String CHANGED = AUDIT_2012 + "changed";
	private static int MAX_REVISED = 1024;
	private final AuditingSail sail;
	private URI trx;
	private final DatatypeFactory factory;
	private final ValueFactory vf;
	private final Map<Resource, Boolean> revised = new LinkedHashMap<Resource, Boolean>(
			128, 0.75f, true) {
		private static final long serialVersionUID = 1863694012435196527L;

		protected boolean removeEldestEntry(Entry<Resource, Boolean> eldest) {
			return size() > MAX_REVISED;
		}
	};
	private final Set<Resource> modified = new HashSet<Resource>();
	private final MemoryOverflowModel metadata = new MemoryOverflowModel();
	private final List<Statement> arch = new ArrayList<Statement>();
	private Set<? extends Resource> predecessors;
	private final URI currentTrx;
	private final URI informedBy;
	private final URI qualifiedUsage;
	private final URI usedEntity;
	private final URI changed;
	private final URI subject;
	private final URI predicate;
	private final URI object;

	public AuditingConnection(AuditingSail sail, SailConnection wrappedCon,
			Set<Resource> predecessors) throws DatatypeConfigurationException {
		super(wrappedCon);
		this.sail = sail;
		factory = DatatypeFactory.newInstance();
		vf = sail.getValueFactory();
		currentTrx = vf.createURI(CURRENT_TRX.stringValue());
		this.predecessors = predecessors;
		informedBy = vf.createURI(WAS_INFORMED_BY);
		qualifiedUsage = vf.createURI(QUALIFIED_USAGE);
		usedEntity = vf.createURI(ENTITY);
		changed = vf.createURI(CHANGED);
		subject = vf.createURI(RDF.SUBJECT.stringValue());
		predicate = vf.createURI(RDF.PREDICATE.stringValue());
		object = vf.createURI(RDF.OBJECT.stringValue());
	}

	@Override
	public SailConnection getWrappedConnection() {
		return super.getWrappedConnection();
	}

	@Override
	public void close() throws SailException {
		super.close();
		metadata.release();
	}

	public synchronized URI getTransactionURI() throws SailException {
		return getTrx();
	}

	@Override
	public void executeUpdate(UpdateExpr updateExpr, Dataset ds,
			BindingSet bindings, boolean includeInferred) throws SailException {
		SailConnection remover = this;
		final URI activity = ds == null ? null : ds.getDefaultInsertGraph();
		if (activity != null) {
			final URI entity = getOperationEntity(updateExpr, ds, bindings);
			remover = new SailConnectionWrapper(this) {
				public void removeStatements(Resource subj, URI pred,
						Value obj, Resource... ctx) throws SailException {
					removeInforming(activity, entity, subj, pred, obj, ctx);
				}
			};
		}
		SailUpdateExecutor executor = new SailUpdateExecutor(remover, vf, false);
		executor.executeUpdate(updateExpr, ds, bindings, includeInferred);
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
		if (contexts == null || contexts.length == 0 || contexts.length == 1
				&& contexts[0] == null) {
			addRevision(subj);
			super.addStatement(subj, pred, obj, getTrx());
		} else if (contexts.length == 1) {
			if (contexts[0].equals(trx)) {
				addRevision(subj);
			}
			super.addStatement(subj, pred, obj, contexts);
			Resource ctx = contexts[0];
			if (isURI(ctx) && !ctx.equals(trx) && modified.add(ctx)) {
				addMetadata(currentTrx, MODIFIED, ctx, currentTrx);
			}
		} else {
			for (Resource ctx : contexts) {
				if (ctx == null || ctx.equals(trx)) {
					addRevision(subj);
					break;
				}
			}
			super.addStatement(subj, pred, obj, contexts);
			for (Resource ctx : contexts) {
				if (isURI(ctx) && !ctx.equals(trx) && modified.add(ctx)) {
					addMetadata(currentTrx, MODIFIED, ctx, currentTrx);
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
				addMetadata(h, REVISION, currentTrx, currentTrx);
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
				String ns = pred.getNamespace();
				if (Audit.NAMESPACE.equals(ns) || PROV.equals(ns)
						|| AUDIT_2012.equals(ns))
					continue;
				if (RDF.SUBJECT.equals(pred) || RDF.PREDICATE.equals(pred)
						|| RDF.OBJECT.equals(pred))
					continue;
				if (RDF.TYPE.equals(pred) && obj instanceof URI) {
					ns = ((URI) obj).getNamespace();
					if (Audit.NAMESPACE.equals(ns) || PROV.equals(ns)
							|| AUDIT_2012.equals(ns)
							|| RDF.NAMESPACE.equals(ns))
						continue;
				}
				return false;
			}
		} finally {
			stmts.close();
		}
		return true;
	}

	private URI getOperationEntity(UpdateExpr updateExpr, Dataset dataset, BindingSet bindings) {
		if (dataset == null)
			return null;
		URI activity = dataset.getDefaultInsertGraph();
		if (activity == null || activity.stringValue().indexOf('#') >= 0)
			return null;
		final Set<Var> subjects = new HashSet<Var>();
		final Set<Var> objects = new HashSet<Var>();
		try {
			updateExpr.visit(new BasicGraphPatternVisitor() {
				public void meet(StatementPattern node) {
					subjects.add(node.getSubjectVar());
					objects.add(node.getObjectVar());
				}
			});
		} catch (QueryEvaluationException e) {
			throw new AssertionError(e);
		}
		URI entity = null;
		for (Var var : subjects) {
			Value subj = var.getValue();
			if (subj == null) {
				subj = bindings.getValue(var.getName());
			}
			if (subj instanceof URI) {
				if (entity == null) {
					entity = entity((URI) subj);
				} else if (!entity.equals(entity((URI) subj))) {
					return null;
				}
			} else if (!objects.contains(var)) {
				return null;
			}
		}
		return entity;
	}

	private void removeInforming(URI activity, URI entity, Resource subj, URI pred,
			Value obj, Resource... contexts) throws SailException {
		if (contexts != null && contexts.length == 0) {
			CloseableIteration<? extends Statement, SailException> stmts;
			stmts = super.getStatements(subj, pred, obj, false, contexts);
			try {
				while (stmts.hasNext()) {
					Statement st = stmts.next();
					Resource ctx = st.getContext();
					subj = st.getSubject();
					pred = st.getPredicate();
					obj = st.getObject();
					removeInformingGraph(activity, entity, subj, pred, obj, ctx);
				}
			} finally {
				stmts.close();
			}
		} else if (contexts == null) {
			removeInformingGraph(activity, entity, subj, pred, obj, null);
		} else {
			for (Resource ctx : contexts) {
				removeInformingGraph(activity, entity, subj, pred, obj, ctx);
			}
		}
	}

	private void removeInformingGraph(URI activity, URI entity, Resource subj,
			URI pred, Value obj, Resource ctx) throws SailException {
		reify(activity, entity, subj, pred, obj, ctx);
		super.removeStatements(subj, pred, obj, ctx);
	}

	private void reify(URI activity, URI entity, Resource subj, URI pred,
			Value obj, Resource ctx) throws SailException {
		String ns = activity.stringValue();
		if (ctx instanceof URI) {
			super.addStatement(activity, informedBy, ctx, activity);
		}
		if (entity == null || GENERATED_BY.equals(pred.stringValue()))
			return;
		URI operation = vf.createURI(ns + "#" + hash(ctx, entity));
		if (ctx instanceof URI) {
			super.addStatement(ctx, qualifiedUsage, operation, activity);
		}
		super.addStatement(activity, qualifiedUsage, operation, activity);
		super.addStatement(operation, usedEntity, entity, activity);
		Resource node = vf.createBNode();
		super.addStatement(operation, changed, node, activity);
		super.addStatement(node, subject, subj, activity);
		super.addStatement(node, predicate, pred, activity);
		super.addStatement(node, object, obj, activity);
	}

	private String hash(Resource ctx, Resource entity) {
		return Integer.toHexString(31 * ctx.hashCode() + entity.hashCode());
	}

	private URI entity(URI subject) {
		URI entity = subject;
		int hash = entity.stringValue().indexOf('#');
		if (hash > 0) {
			entity = vf.createURI(entity.stringValue().substring(0, hash));
		}
		return entity;
	}
}
