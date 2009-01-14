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
package org.openrdf.elmo.sesame;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;

import org.openrdf.elmo.annotations.inverseOf;
import org.openrdf.elmo.annotations.rdf;
import org.openrdf.elmo.sesame.helpers.PropertyChanger;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.store.StoreException;
import org.openrdf.repository.contextaware.ContextAwareConnection;

/**
 * Extends {@link SesamePropertyFactory} by creating
 * {@link org.openrdf.elmo.ElmoProperty} that update the equivalent and
 * inverseOf predicates.`
 * 
 * @author James Leigh
 * 
 * @param <E>
 *            the property type
 */
public class InferencingPropertyFactory<E> extends SesamePropertyFactory<E> {

	private URI[] equivalent;

	private URI[] inverseOf;

	@Override
	public SesamePropertyFactory<E> setPropertyDescriptor(
			PropertyDescriptor property) {
		Method getter = property.getReadMethod();
		if (getter != null) {
			inverseOf ann = getter.getAnnotation(inverseOf.class);
			if (getter.isAnnotationPresent(rdf.class)) {
				String[] eq = getter.getAnnotation(rdf.class).value();
				if (eq.length > 1) { // first one is main predicate
					equivalent = new URI[eq.length - 1];
					for (int i = 1; i < eq.length; i++) {
						equivalent[i - 1] = getValueFactory().createURI(eq[i]);
					}
				}
				if (getter.isAnnotationPresent(inverseOf.class)) {
					String[] inv = ann.value();
					inverseOf = new URI[inv.length];
					for (int i = 0; i < inv.length; i++) {
						inverseOf[i] = getValueFactory().createURI(inv[i]);
					}
				}
			} else if (getter.isAnnotationPresent(inverseOf.class)) {
				String[] inv = ann.value();
				if (inv.length > 1) { // first one is main predicate
					inverseOf = new URI[inv.length - 1];
					for (int i = 1; i < inv.length; i++) {
						inverseOf[i - 1] = getValueFactory().createURI(inv[i]);
					}
				}
			}
		}
		return super.setPropertyDescriptor(property);
	}

	@Override
	protected PropertyChanger getPropertyChanger() {
		final URI pred = getPredicate();
		final URI[] equivalent = this.equivalent;
		final URI[] inverseOf = this.inverseOf;
		if (equivalent == null && inverseOf == null)
			return super.getPropertyChanger();
		return new PropertyChanger(pred, getOneOf()) {
			@Override
			public void add(ContextAwareConnection conn, Resource subj,
					Value obj) throws StoreException {
				super.add(conn, subj, obj);
				if (equivalent != null) {
					for (URI eq : equivalent) {
						conn.add(subj, eq, obj);
					}
				}
				if (inverseOf != null) {
					for (URI inv : inverseOf) {
						conn.add((Resource) obj, inv, subj);
					}
				}
			}

			@Override
			public void remove(ContextAwareConnection conn, Resource subj,
					Value obj) throws StoreException {
				super.remove(conn, subj, obj);
				if (equivalent != null) {
					for (URI eq : equivalent) {
						conn.removeMatch(subj, eq, obj);
					}
				}
				if (inverseOf != null) {
					for (URI inv : inverseOf) {
						conn.removeMatch((Resource) obj, inv, subj);
					}
				}
			}

		};
	}

}
