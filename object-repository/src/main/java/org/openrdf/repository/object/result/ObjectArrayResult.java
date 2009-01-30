/*
 * Copyright (c) 2007-2009, James Leigh All rights reserved.
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
package org.openrdf.repository.object.result;

import java.util.List;

import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectResult;
import org.openrdf.result.TupleResult;
import org.openrdf.store.StoreException;

/**
 * Converts the repository result into an array of Objects.
 * 
 * @author James Leigh
 * 
 */
public class ObjectArrayResult extends ObjectIterator<BindingSet, Object>
		implements ObjectResult {

	private List<String> bindings;

	private ObjectConnection manager;

	public ObjectArrayResult(ObjectConnection manager, TupleResult result, List<String> bindings)
			throws StoreException {
		super(result);
		this.bindings = bindings;
		this.manager = manager;
	}

	@Override
	protected Object[] convert(BindingSet sol) throws StoreException {
		Object[] result = new Object[bindings.size()];
		for (int i = 0; i < result.length; i++) {
			Value value = sol.getValue(bindings.get(i));
			if (value == null) {
				result[i] = null;
			} else if (value instanceof Resource) {
				result[i] = manager.getObject((Resource) value);
			} else {
				result[i] = manager.getObjectFactory().createObject(
						((Literal) value));
			}
		}
		return result;
	}

	public List<String> getBindingNames() throws StoreException {
		return bindings;
	}

}
