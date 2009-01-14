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
package org.openrdf.elmo.sesame;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.openrdf.elmo.ElmoProperty;
import org.openrdf.elmo.ElmoPropertyFactory;
import org.openrdf.elmo.annotations.inverseOf;
import org.openrdf.elmo.annotations.localized;
import org.openrdf.elmo.annotations.rdf;
import org.openrdf.elmo.impl.UnmodifiableElmoProperty;
import org.openrdf.elmo.sesame.helpers.PropertyChanger;
import org.openrdf.elmo.sesame.roles.SesameEntity;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;

/**
 * Creates {@link ElmoProperty} objects for a given predicate.
 * 
 * @author James Leigh
 * 
 * @param <E>
 *            property type
 */
public class SesamePropertyFactory<E> implements ElmoPropertyFactory<E> {
	private static ValueFactory vf = new ValueFactoryImpl();

	private URI predicate;

	private boolean inverse;

	private boolean localized;

	private boolean readOnly;

	private PropertyChanger inferencer;

	public static ValueFactory getValueFactory() {
		return vf;
	}

	public URI getPredicate() {
		return predicate;
	}

	public SesamePropertyFactory<E> setUri(String uri) {
		predicate = vf.createURI(uri);
		inferencer = getPropertyChanger();
		return this;
	}

	public SesamePropertyFactory<E> setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
		return this;
	}

	public SesamePropertyFactory<E> setField(Field field) {
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

	public SesamePropertyFactory<E> setPropertyDescriptor(
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

	public ElmoProperty<E> createElmoProperty(Object bean) {
		assert bean instanceof SesameEntity : bean;
		SesameProperty<E> property = createSesameProperty((SesameEntity) bean);
		if (readOnly)
			return new UnmodifiableElmoProperty<E>(property);
		return property;
	}

	protected PropertyChanger getPropertyChanger() {
		return new PropertyChanger(predicate);
	}

	@SuppressWarnings("unchecked")
	private SesameProperty<E> createSesameProperty(SesameEntity bean) {
		if (localized)
			return (SesameProperty<E>) new LocalizedSesameProperty(bean,
					inferencer);
		if (inverse)
			return new InverseSesameProperty<E>(bean, inferencer);
		return new SesameProperty<E>(bean, inferencer);
	}

}
