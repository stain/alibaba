/*
 * Copyright (c) 2010, Zepheira LLC, Some rights reserved.
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
package org.openrdf.sail.optimistic.helpers;

import java.util.HashSet;
import java.util.Set;

import org.openrdf.model.Model;
import org.openrdf.model.impl.MemoryOverflowModel;
import org.openrdf.sail.optimistic.exceptions.ConcurrencyException;

/**
 * Changeset that appeared, but was not observed during a transaction and all
 * the subsequentObservations that were performed since the changeset appeared.
 * 
 * @author James Leigh
 * 
 */
public class InconsistentChange {
	private final MemoryOverflowModel added;
	private final MemoryOverflowModel removed;
	private final ConcurrencyException inconsistency;
	private final Set<EvaluateOperation> subsequentObservations = new HashSet<EvaluateOperation>();

	public InconsistentChange(MemoryOverflowModel added, MemoryOverflowModel removed, ConcurrencyException inconsistency) {
		this.added = added.open();
		this.removed = removed.open();
		this.inconsistency = inconsistency;
	}

	public Model getAdded() {
		return added;
	}

	public Model getRemoved() {
		return removed;
	}

	public ConcurrencyException getInconsistency() {
		return inconsistency;
	}

	public Set<EvaluateOperation> getSubsequentObservations() {
		return subsequentObservations;
	}

	public void addObservation(EvaluateOperation op) {
		subsequentObservations.add(op);
	}

	public void release() {
		added.release();
		removed.release();
	}
}
