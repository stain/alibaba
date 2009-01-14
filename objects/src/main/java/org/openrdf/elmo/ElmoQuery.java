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
package org.openrdf.elmo;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import javax.xml.namespace.QName;

/**
 * Interface used to bind and evaluate a query
 * 
 * @author James Leigh
 */
public interface ElmoQuery {

	/**
	 * Closes any open results from this query.
	 */
	public abstract void close();

	/**
	 * Evaluates the query and returns an iterator over the result.
	 * 
	 * @return Iterator over the result of the query.
	 */
	public abstract Iterator<?> evaluate();

	/**
	 * Evaluates the query and return the first result.
	 * 
	 * @return The first result from the query.
	 */
	public abstract Object getSingleResult();

	/**
	 * Evaluates the query and returns the results disconnected from the query.
	 * 
	 * @return The results from the query.
	 */
	public abstract List getResultList();

	/**
	 * Skips to the <code>startPosition</code> of the results.
	 * 
	 * @param startPosition
	 */
	public abstract ElmoQuery setFirstResult(int startPosition);

	/**
	 * Terminates the result list after reading <code>maxResult</code>
	 * 
	 * @param maxResult
	 */
	public abstract ElmoQuery setMaxResults(int maxResult);

	/**
	 * Assigns a concept to the given name.
	 * 
	 * @param name
	 *            Name of the variable to bind to.
	 * @param concept
	 *            Registered concept.
	 */
	public abstract ElmoQuery setType(String name, Class<?> concept);

	/**
	 * Assigns entity by name to the given parameter name.
	 * 
	 * @param name
	 *            Name of the variable to bind to.
	 * @param qname
	 *            Name of the entity.
	 */
	public abstract ElmoQuery setQName(String name, QName qname);

	/**
	 * Binds a literal with no type and a language of <code>locale</code>.
	 * 
	 * @param name
	 *            Name of the variable to bind to.
	 * @param label
	 * @param locale
	 */
	public abstract ElmoQuery setParameter(String name, String label,
			Locale locale);

	/**
	 * Assigns an entity or literal to the given name.
	 * 
	 * @param name
	 *            Name of the variable to bind to.
	 * @param value
	 *            managed entity or literal.
	 */
	public abstract ElmoQuery setParameter(String name, Object value);
}
