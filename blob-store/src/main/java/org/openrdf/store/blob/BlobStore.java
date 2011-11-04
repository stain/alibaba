/*
 * Copyright (c) 2011, 3 Round Stones Inc. Some rights reserved.
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
package org.openrdf.store.blob;

import java.io.IOException;

/**
 * Managements {@link BlobTransaction}.
 * 
 * @author James Leigh
 * 
 */
public interface BlobStore {

	/**
	 * The number of transactions to persist in this store.
	 */
	int getMaxHistoryLength();

	/**
	 * If supported by the store, the number of transactions persisted can be
	 * increased or decreased.
	 */
	void setMaxHistoryLength(int maxHistory);

	/**
	 * Recent transaction identifiers that have previously made changes to the
	 * blob store. The number of identifiers returned is limited by the
	 * {@link #getMaxHistoryLength()} result.
	 */
	String[] getHistory() throws IOException;

	/**
	 * Open a read-only transaction from the history.
	 * 
	 * @param iri
	 *            IRI to identify transaction
	 * @return an existing BlobTransaction
	 * @throws IllegalArgumentException
	 *             if the iri is not in the recent history.
	 */
	BlobTransaction reopen(String iri) throws IOException;

	/**
	 * Create a new transaction with the given unique identifier.
	 * BlobTransaction returned from this method using an iri from the history
	 * have undefined consequences.
	 * 
	 * @param iri
	 *            IRI to uniquely identify transaction
	 * @return a new or existing BlobTransaction
	 */
	BlobTransaction open(String iri) throws IOException;

	/**
	 * Remove all blobs from all transactions and all the history.
	 */
	boolean erase() throws IOException;
}
