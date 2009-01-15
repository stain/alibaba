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
package org.openrdf.repository.object.composition;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.repository.object.annotations.inverseOf;
import org.openrdf.repository.object.annotations.localized;
import org.openrdf.repository.object.annotations.rdf;
import org.openrdf.repository.object.composition.helpers.CachedPropertySet;
import org.openrdf.repository.object.composition.helpers.InversePropertySet;
import org.openrdf.repository.object.composition.helpers.LocalizedPropertySet;
import org.openrdf.repository.object.composition.helpers.PropertySetModifier;
import org.openrdf.repository.object.composition.helpers.PropertySet;
import org.openrdf.repository.object.composition.helpers.UnmodifiableProperty;

/**
 * Creates {@link PropertySet} objects for a given predicate.
 * 
 * @author James Leigh
 * 
 * @param <E>
 *            property type
 */
public class PropertySetFactory<E> {
	private static ValueFactory vf = new ValueFactoryImpl();

	private URI predicate;

	private boolean inverse;

	private boolean localized;

	private boolean readOnly;

	private PropertySetModifier inferencer;

	public static ValueFactory getValueFactory() {
		return vf;
	}

	public URI getPredicate() {
		return predicate;
	}

	public PropertySetFactory<E> setUri(String uri) {
		predicate = vf.createURI(uri);
		inferencer = getPropertyChanger();
		return this;
	}

	public PropertySetFactory<E> setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
		return this;
	}

	public PropertySetFactory<E> setField(Field field) {
		String uri;
		rdf rdf = field.getAnnotation(rdf.class);
		inverseOf inv = field.getAnnotation(inverseOf.class);
		if (rdf != null && rdf.value() != null) {
			uri = rdf.value();
			inverse = false;
		} else if (inv != null && inv.value() != null) {
			uri = inv.value();
			inverse = true;
		} else {
			throw new IllegalArgumentException("Field has no Elmo annotations");
		}
		predicate = vf.createURI(uri);
		localized = field.isAnnotationPresent(localized.class);
		inferencer = getPropertyChanger();
		return this;
	}

	public PropertySetFactory<E> setPropertyDescriptor(
			PropertyDescriptor property) {
		String uri;
		Method getter = property.getReadMethod();
		rdf rdf = getter.getAnnotation(rdf.class);
		inverseOf inv = getter.getAnnotation(inverseOf.class);
		if (rdf != null && rdf.value() != null) {
			uri = rdf.value();
			inverse = false;
		} else if (inv != null && inv.value() != null) {
			uri = inv.value();
			inverse = true;
		} else {
			throw new IllegalArgumentException(
					"Property has no Elmo annotations on the getter method");
		}
		predicate = vf.createURI(uri);
		localized = getter.isAnnotationPresent(localized.class);
		Method setter = property.getWriteMethod();
		readOnly = setter == null;
		inferencer = getPropertyChanger();
		return this;
	}

	public PropertySet<E> createElmoProperty(Object bean) {
		assert bean instanceof RDFObject : bean;
		CachedPropertySet<E> property = createSesameProperty((RDFObject) bean);
		if (readOnly)
			return new UnmodifiableProperty<E>(property);
		return property;
	}

	protected PropertySetModifier getPropertyChanger() {
		return new PropertySetModifier(predicate);
	}

	@SuppressWarnings("unchecked")
	private CachedPropertySet<E> createSesameProperty(RDFObject bean) {
		if (localized)
			return (CachedPropertySet<E>) new LocalizedPropertySet(bean,
					inferencer);
		if (inverse)
			return new InversePropertySet<E>(bean, inferencer);
		return new CachedPropertySet<E>(bean, inferencer);
	}

}
