/*
 * Copyright (c) 2012 3 Round Stones Inc., Some rights reserved.
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
package org.openrdf.sail.optimistic;

import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;
import org.openrdf.query.Dataset;
import org.openrdf.query.algebra.UpdateExpr;
import org.openrdf.sail.SailConnection;
import org.openrdf.sail.SailException;

/**
 * {@link SailConnection} with support for explicitly starting a transaction.
 * 
 * @author James Leigh
 * 
 */
public interface TransactionalSailConnection extends SailConnection {

	/**
	 * If {@link #begin()} has been called successfully and no exception has
	 * been thrown from {@link #prepare()} has been thrown (if called since) and
	 * {@link #commit()} and {@link #rollback()} have not been called since.
	 */
	boolean isActive();

	/**
	 * Starts a transaction when method returns without exception.
	 * 
	 * @throws SailException
	 *             the transaction is not open
	 */
	void begin() throws SailException;

	/**
	 * Checks that this transaction is consistent.
	 * 
	 * @throws SailException
	 *             the transaction has been aborted
	 */
	void prepare() throws SailException;

	/**
	 * Makes the changes in this transaction viewable to other connections. This
	 * transaction is no longer active when this method returns.
	 */
	void commit() throws SailException;

	/**
	 * Resets any uncommitted changes in this transaction. This transaction is
	 * no longer active when this method returns.
	 */
	void rollback() throws SailException;

	/**
	 * Adds a statement to the store within the context of an update operation.
	 * 
	 * @param updateExpr
	 *            The expression that produced this statement
	 * @param dataset
	 *            The dataset that was used to execute the update, or
	 *            <tt>null</tt> if the Sail's default dataset was used.
	 * @param bindings
	 *            A set of input parameters that was used to execute. The keys
	 *            reference variable names that were be bound to the value they
	 *            map to.
	 * @param subj
	 *            The subject of the statement to add.
	 * @param pred
	 *            The predicate of the statement to add.
	 * @param obj
	 *            The object of the statement to add.
	 * @param contexts
	 *            The context(s) to add the statement to. Note that this
	 *            parameter is a vararg and as such is optional. If no contexts
	 *            are specified, a context-less statement will be added.
	 * @throws SailException
	 *             If the statement could not be added.
	 * @throws IllegalStateException
	 *             If the connection has been closed.
	 */
	public void executeInsert(UpdateExpr updateExpr, Dataset dataset,
			BindingSet bindings, Resource subj, URI pred, Value obj,
			Resource... contexts) throws SailException;

	/**
	 * Removes the triple matching the specified subject, predicate and object
	 * from the repository within the context of an update operation. An empty
	 * contexts indicates a wildcard.
	 * 
	 * @param updateExpr
	 *            The expression that produced this statement
	 * @param dataset
	 *            The dataset that was used to execute the update, or
	 *            <tt>null</tt> if the Sail's default dataset was used.
	 * @param bindings
	 *            A set of input parameters that was used to execute. The keys
	 *            reference variable names that were be bound to the value they
	 *            map to.
	 * @param subj
	 *            The subject of the statement that should be removed, or
	 *            <tt>null</tt> to indicate a wildcard.
	 * @param pred
	 *            The predicate of the statement that should be removed, or
	 *            <tt>null</tt> to indicate a wildcard.
	 * @param obj
	 *            The object of the statement that should be removed , or
	 *            <tt>null</tt> to indicate a wildcard. *
	 * @param contexts
	 *            The context(s) from which to remove the statement. Note that
	 *            this parameter is a vararg and as such is optional. If no
	 *            contexts are specified the method operates on the entire
	 *            repository. A <tt>null</tt> value can be used to match
	 *            context-less statements.
	 * @throws SailException
	 *             If the statement could not be removed.
	 * @throws IllegalStateException
	 *             If the connection has been closed.
	 */
	public void executeDelete(UpdateExpr updateExpr, Dataset dataset,
			BindingSet bindings, Resource subj, URI pred, Value obj,
			Resource... contexts) throws SailException;

}