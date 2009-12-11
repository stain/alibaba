/*
 * Copyright (c) 2009, James Leigh All rights reserved.
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
import static org.openrdf.sail.auditing.vocabulary.Audit.GRAPH;
import static org.openrdf.sail.auditing.vocabulary.Audit.LITERAL;
import static org.openrdf.sail.auditing.vocabulary.Audit.OBJECT;
import static org.openrdf.sail.auditing.vocabulary.Audit.PATTERN;
import static org.openrdf.sail.auditing.vocabulary.Audit.PREDICATE;
import static org.openrdf.sail.auditing.vocabulary.Audit.REMOVED;
import static org.openrdf.sail.auditing.vocabulary.Audit.REVISION;
import static org.openrdf.sail.auditing.vocabulary.Audit.SUBJECT;
import static org.openrdf.sail.auditing.vocabulary.Audit.*;

import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Set;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.sail.SailConnection;
import org.openrdf.sail.SailException;
import org.openrdf.sail.helpers.SailConnectionWrapper;

/**
 * Intercepts the add and remove operations and add a revision to each resource.
 */
public class AuditingConnection extends SailConnectionWrapper {
	private AuditingSail sail;
	private URI trx;
	private DatatypeFactory factory;
	private ValueFactory vf;
	private Set<Resource> revised = new HashSet<Resource>();
	private URI currentTrx;

	public AuditingConnection(AuditingSail sail, SailConnection wrappedCon)
			throws DatatypeConfigurationException {
		super(wrappedCon);
		this.sail = sail;
		factory = DatatypeFactory.newInstance();
		vf = sail.getValueFactory();
		currentTrx = vf.createURI(CURRENT_TRX.stringValue());
	}

	@Override
	public synchronized void addStatement(Resource subj, URI pred, Value obj,
			Resource... contexts) throws SailException {
		if (trx == null) {
			trx = sail.nextTransaction();
			super.addStatement(trx, RDF.TYPE, TRANSACTION);
		}
		if (currentTrx != null) {
			if (currentTrx.equals(subj)) {
				subj = trx;
			}
			if (currentTrx.equals(obj)) {
				obj = trx;
			}
			if (contexts != null) {
				for (int i = 0; i < contexts.length; i++) {
					if (currentTrx.equals(contexts[i])) {
						contexts[i] = trx;
					}
				}
			}
		}
		if (revised.add(subj) && !subj.equals(trx)) {
			super.removeStatements(subj, REVISION, null);
			super.addStatement(subj, REVISION, trx);
		}
		if (contexts == null || contexts.length == 0 || contexts.length == 1
				&& contexts[0] == null) {
			super.addStatement(subj, pred, obj, trx);
		} else {
			super.addStatement(subj, pred, obj, contexts);
		}
	}

	@Override
	public synchronized void removeStatements(Resource subj, URI pred,
			Value obj, Resource... contexts) throws SailException {
		if (sail.isArchiving()) {
			if (trx == null) {
				trx = sail.nextTransaction();
				super.addStatement(trx, RDF.TYPE, TRANSACTION);
			}
			BNode st = vf.createBNode();
			super.addStatement(trx, REMOVED, st);
			super.addStatement(st, RDF.TYPE, PATTERN);
			if (subj != null) {
				super.addStatement(st, SUBJECT, subj);
			}
			if (pred != null) {
				super.addStatement(st, PREDICATE, pred);
			}
			if (obj instanceof Resource) {
				super.addStatement(st, OBJECT, obj);
			} else if (obj instanceof Literal) {
				super.addStatement(st, LITERAL, obj);
			}
			if (contexts != null) {
				for (int i = 0; i < contexts.length; i++) {
					if (contexts[i] != null) {
						super.addStatement(st, GRAPH, contexts[i]);
					}
				}
			}
		}
		super.removeStatements(subj, pred, obj, contexts);
		if (subj != null && pred != null && revised.add(subj)) {
			if (trx == null && !pred.equals(REVISION)) {
				trx = sail.nextTransaction();
				super.addStatement(trx, RDF.TYPE, TRANSACTION);
			}
			super.removeStatements(subj, REVISION, null);
			if (pred.equals(REVISION)) {
				super.removeStatements(subj, REVISION, trx);
			} else {
				super.addStatement(subj, REVISION, trx);
			}
		} else if (subj != null && trx != null && REVISION.equals(pred)) {
			super.removeStatements(subj, REVISION, trx);
		}
	}

	@Override
	public synchronized void commit() throws SailException {
		if (trx != null) {
			GregorianCalendar cal = new GregorianCalendar();
			XMLGregorianCalendar xgc = factory.newXMLGregorianCalendar(cal);
			Literal now = vf.createLiteral(xgc);
			super.addStatement(trx, COMMITTED_ON, now);
		}
		super.commit();
		trx = null;
	}
}
