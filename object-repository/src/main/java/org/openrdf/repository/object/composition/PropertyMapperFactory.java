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
package org.openrdf.repository.object.composition;

import static org.openrdf.repository.object.composition.helpers.PropertySet.ADD_ALL;
import static org.openrdf.repository.object.composition.helpers.PropertySet.ADD_SINGLE;
import static org.openrdf.repository.object.composition.helpers.PropertySet.GET_ALL;
import static org.openrdf.repository.object.composition.helpers.PropertySet.GET_SINGLE;
import static org.openrdf.repository.object.composition.helpers.PropertySet.SET_ALL;
import static org.openrdf.repository.object.composition.helpers.PropertySet.SET_SINGLE;
import static org.openrdf.repository.object.composition.helpers.PropertySetFactory.GET_NAME;
import static org.openrdf.repository.object.traits.PropertyConsumer.USE;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.openrdf.query.BindingSet;
import org.openrdf.repository.object.composition.helpers.AbstractBehaviourProvider;
import org.openrdf.repository.object.composition.helpers.PropertySet;
import org.openrdf.repository.object.composition.helpers.PropertySetFactory;
import org.openrdf.repository.object.exceptions.ObjectStoreConfigException;
import org.openrdf.repository.object.traits.Mergeable;
import org.openrdf.repository.object.traits.PropertyConsumer;
import org.openrdf.repository.object.traits.Refreshable;

/**
 * Properties that have the rdf or localname annotation are replaced with
 * getters and setters that access the Sesame Repository directly.
 * 
 * @author James Leigh
 * 
 */
public class PropertyMapperFactory extends AbstractBehaviourProvider {

	public static String getMapperClassNameFor(Class<?> concept) {
		String suffix = PropertyMapperFactory.class.getSimpleName().replaceAll("Factory$", "");
		return CLASS_PREFIX + concept.getName() + suffix;
	}

	public static String getLoadedMethod(Field field) throws Exception {
		return "_$isLoaded_" + getPropertyName(field);
	}

	public static String getReadMethod(Field field) throws Exception {
		return "_$get_" + getPropertyName(field);
	}

	public static String getWriteMethod(Field field) throws Exception {
		return "_$set_" + getPropertyName(field);
	}

	public static Class<?> getClassType(Method method) {
		return method.getReturnType();
	}

	public static Class<?> getContentClassType(Method method) {
		Type[] types = getTypeArguments(method);
		if (types == null || types.length != 1)
			return null;
		if (types[0] instanceof Class)
			return (Class) types[0];
		if (types[0] instanceof ParameterizedType)
			return (Class) ((ParameterizedType) types[0]).getRawType();
		return null;
	}

	public static Type[] getTypeArguments(Method method) {
		Type type = method.getGenericReturnType();
		if (type instanceof ParameterizedType)
			return ((ParameterizedType) type).getActualTypeArguments();
		return null;
	}

	public static Type[] getContentTypeArguments(Method method) {
		Type[] types = getTypeArguments(method);
		if (types == null || types.length != 1)
			return null;
		if (types[0] instanceof ParameterizedType)
			return ((ParameterizedType) types[0]).getActualTypeArguments();
		return null;
	}

	private static String getPropertyName(Field f) {
		int code = f.getDeclaringClass().getName().hashCode();
		return f.getName() + Integer.toHexString(code);
	}

	private static final String PROPERTY_SUFFIX = "Property";

	private static final String FACTORY_SUFFIX = "Factory";

	private Class<?> propertyFactoryClass = PropertySetFactory.class;

	protected boolean isEnhanceable(Class<?> concept)
			throws ObjectStoreConfigException {
		if (concept.equals(Object.class))
			return false;
		if (!properties.findProperties(concept).isEmpty())
			return true;
		if (!properties.findFields(concept).isEmpty())
			return true;
		return false;
	}

	@Override
	protected String getJavaClassName(Class<?> concept) {
		return getMapperClassNameFor(concept);
	}

	protected void enhance(ClassTemplate cc, Class<?> concept) throws Exception {
		cc.addInterface(Mergeable.class);
		cc.addInterface(Refreshable.class);
		cc.addInterface(PropertyConsumer.class);
		for (Field field : properties.findFields(concept)) {
			implementProperty(field, cc);
		}
		for (PropertyDescriptor pd : properties.findProperties(concept)) {
			overrideMethod(pd, cc);
		}
		overrideMergeMethod(cc, concept);
		overrideRefreshMethod(cc, concept);
		overrideConsumeMethod(cc, concept);
	}

	private void overrideMergeMethod(ClassTemplate cc, Class<?> concept)
			throws Exception {
		Method merge = Mergeable.class.getMethod("merge", Object.class);
		CodeBuilder sb = cc.overrideMethod(merge, false);
		sb.code("if($1 instanceof ").code(concept.getName());
		sb.code("){\n");
		for (PropertyDescriptor pd : properties.findProperties(concept)) {
			Method method = pd.getReadMethod();
			String property = pd.getName();
			Class<?> type = pd.getPropertyType();
			String ref = "((" + concept.getName() + ") $1)." + method.getName()
					+ "()";
			mergeProperty(concept, sb, property, type, ref);
		}
		int count = 0;
		for (Field f : properties.findFields(concept)) {
			String property = getPropertyName(f);
			Class<?> type = f.getType();
			String ref;
			if (Modifier.isPublic(f.getModifiers())) {
				ref = "((" + concept.getName() + ") $1)." + f.getName();
			} else {
				String fieldVar = f.getName() + "Field" + ++count;
				sb.declareObject(Field.class, fieldVar);
				sb.insert(f.getDeclaringClass());
				sb.code(".getDeclaredField(\"");
				sb.code(f.getName()).code("\")").semi();
				sb.code(fieldVar).code(".setAccessible(true)").semi();
				StringBuilder s = new StringBuilder();
				s.append(fieldVar).append(".get");
				if (f.getType().isPrimitive()) {
					String tname = f.getType().getName();
					s.append(tname.substring(0, 1).toUpperCase());
					s.append(tname.substring(1));
				}
				s.append("($1)");
				String fieldValue = f.getName() + "FieldValue" + ++count;
				sb.declareObject(f.getType(), fieldValue);
				if (f.getType().isPrimitive()) {
					sb.code(s.toString()).semi();
				} else {
					sb.castObject(f.getType()).code(s.toString()).semi();
				}
				ref = fieldValue;
			}
			mergeProperty(concept, sb, property, type, ref);
		}
		sb.code("}").end();
	}

	private void mergeProperty(Class<?> concept, CodeBuilder sb,
			String property, Class<?> type, String ref) throws Exception {
		String field = getPropertyField(property);
		String factory = getFactoryField(property);
		if (type.isPrimitive()) {
			appendNullCheck(sb, field, factory);
			appendPersistCode(sb, field, type, ref);
		} else {
			String var = property + "Var";
			sb.declareObject(type, var);
			sb.code(ref);
			sb.code(";\n");
			// array != null does not seem to work in javassist
			sb.code("if(");
			sb.codeInstanceof(var, type);
			sb.code("){");
			appendNullCheck(sb, field, factory);
			appendPersistCode(sb, field, type, var);
			sb.code("}\n");
		}
	}

	private void overrideRefreshMethod(ClassTemplate cc, Class<?> concept)
			throws Exception {
		Method refresh = Refreshable.class.getMethod("refresh");
		CodeBuilder sb = cc.overrideMethod(refresh, false);
		for (String field : getPropertySetFieldNames(cc)) {
			sb.code("if (").code(field).code(" != null) {");
			sb.code(field).code(".").code(refresh.getName()).code("($$);}\n");
		}
		sb.end();
	}

	private void overrideConsumeMethod(ClassTemplate cc, Class<?> concept)
			throws Exception {
		Method method = PropertyConsumer.class.getMethod(USE, String.class,
				List.class);
		CodeBuilder sb = cc.overrideMethod(method, false);
		for (String field : getPropertySetFieldNames(cc)) {
			String factory = getFactoryFieldUsingPropertyField(field);
			String var = "binding_" + field;
			sb.declareObject(String.class, var).code("$1 + \"_\" + ");
			sb.code(factory).code(".").code(GET_NAME).code("()").semi();
			sb.code("if((").castObject(BindingSet.class).code("$2.get(0)");
			sb.code(").hasBinding(").code(var).code(")) {\n");
			appendNullCheck(sb, field, factory);
			sb.code("if(").codeInstanceof(field, PropertyConsumer.class);
			sb.code("){ (").castObject(PropertyConsumer.class).code(field);
			sb.code(").").code(method.getName());
			sb.code("(").code(var).code(", $2)").semi();
			sb.code("}}\n");
		}
		sb.end();
	}

	private String getFactoryFieldUsingPropertyField(String field) {
		return field.substring(0, field.length() - PROPERTY_SUFFIX.length())
				+ FACTORY_SUFFIX;
	}

	private Collection<String> getPropertySetFieldNames(ClassTemplate cc) {
		Collection<String> fieldNames = new ArrayList<String>();
		for (String field : cc.getDeclaredFieldNames()) {
			if (field.endsWith(PROPERTY_SUFFIX)) {
				fieldNames.add(field);
			}
		}
		return fieldNames;
	}

	private void overrideMethod(PropertyDescriptor pd, ClassTemplate cc)
			throws Exception {
		Method method = pd.getReadMethod();
		String property = pd.getName();
		Method setter = pd.getWriteMethod();
		Class<?> type = method.getReturnType();
		String field = createPropertyField(property, cc);
		String factory = createFactoryField(pd, cc);
		CodeBuilder body = cc.overrideMethod(method, false);
		appendNullCheck(body, field, factory);
		appendGetterMethod(body, field, type, cc);
		body.end();
		if (setter != null) {
			body = cc.overrideMethod(setter, false);
			appendNullCheck(body, field, factory);
			appendSetterMethod(body, field, type, "$1");
			body.end();
		}
	}

	private void implementProperty(Field f, ClassTemplate cc) throws Exception {
		String property = getPropertyName(f);
		String field = createPropertyField(property, cc);
		Class<?> type = f.getType();
		String factory = createFactoryField(f, cc);
		cc.createField(Boolean.TYPE, field + "Loaded");
		CodeBuilder body = cc.createPrivateMethod(Boolean.TYPE, getLoadedMethod(f));
		body.code("return ").code(field + "Loaded").semi();
		body.end();
		body = cc.createPrivateMethod(type, getReadMethod(f));
		body.assign(field + "Loaded").code("true").semi();
		appendNullCheck(body, field, factory);
		appendGetterMethod(body, field, type, cc);
		body.end();
		body = cc.createPrivateMethod(Void.TYPE, getWriteMethod(f), type);
		appendNullCheck(body, field, factory);
		appendSetterMethod(body, field, type, "$1");
		body.end();
	}

	private String createPropertyField(String property, ClassTemplate cc)
			throws Exception {
		String fieldName = getPropertyField(property);
		cc.createField(PropertySet.class, fieldName);
		return fieldName;
	}

	private String getPropertyField(String property) {
		return "_$" + property + PROPERTY_SUFFIX;
	}

	private String createFactoryField(PropertyDescriptor pd, ClassTemplate cc)
			throws Exception {
		Method method = pd.getReadMethod();
		Method setter = pd.getWriteMethod();
		String property = pd.getName();
		Class<?> class1 = PropertyDescriptor.class;
		Class<?> type = PropertySetFactory.class;
		String fieldName = getFactoryField(property);
		CodeBuilder code = cc.assignStaticField(type, fieldName);
		code.code("new ").code(propertyFactoryClass.getName()).code("(");
		code.construct(class1, property, method, setter).code(",");
		code.insert(properties.findPredicate(pd));
		code.code(")").end();
		return fieldName;
	}

	private String createFactoryField(Field field, ClassTemplate cc)
			throws Exception {
		Class<?> dc = field.getDeclaringClass();
		Class<?> type = PropertySetFactory.class;
		String fieldName = getFactoryField(getPropertyName(field));
		CodeBuilder code = cc.assignStaticField(type, fieldName);
		code.code("new ").code(propertyFactoryClass.getName()).code("(");
		code.insert(dc).code(".getDeclaredField(");
		code.insert(field.getName()).code("),");
		code.insert(properties.findPredicate(field));
		code.code(")").end();
		return fieldName;
	}

	private String getFactoryField(String property) {
		return "_$" + property + FACTORY_SUFFIX;
	}

	private boolean isCollection(Class<?> type) throws Exception {
		return Set.class.equals(type);
	}

	private CodeBuilder appendNullCheck(CodeBuilder body, String field,
			String propertyFactory) throws Exception {
		body.code("if (").code(field).code(" == null) {");
		body.assign(field).code(propertyFactory);
		body.code(".").code(PropertySetFactory.CREATE);
		body.code("(").code(BEAN_FIELD_NAME).code(");}");
		return body;
	}

	private CodeBuilder appendGetterMethod(CodeBuilder body, String field,
			Class<?> type, ClassTemplate cc) throws Exception {
		if (isCollection(type)) {
			body.code("return ").code(field);
			body.code(".").code(GET_ALL);
			body.code("();");
		} else if (type.isPrimitive()) {
			body.declareWrapper(type, "result");
			body.castObject(type).code(field);
			body.code(".").code(GET_SINGLE);
			body.code("();");
			body.code("if (result != null) ");
			body.code("return result.").code(type.getName()).code("Value();");
			if (Boolean.TYPE.equals(type)) {
				body.code("return false;");
			} else {
				body.code("return ($r) 0;");
			}
		} else {
			body.code("try {");
			body.code("return ");
			body.castObject(type).code(field);
			body.code(".").code(GET_SINGLE);
			body.code("();");
			body.code("} catch (java.lang.ClassCastException exc) {");
			body.code("throw new java.lang.ClassCastException(");
			body.code(field).code(".").code(GET_SINGLE);
			body.code("().toString() + \" cannot be cast to ");
			body.code(type.getName()).code("\");");
			body.code("}");
		}
		return body;
	}

	private CodeBuilder appendPersistCode(CodeBuilder body, String field,
			Class<?> type, String ref) throws Exception {
		body.code(field).code(".");
		if (isCollection(type)) {
			body.code(ADD_ALL).code("(").code(ref).code(");");
		} else {
			body.code(ADD_SINGLE).code("(").codeObject(ref, type).code(");");
		}
		return body;
	}

	private CodeBuilder appendSetterMethod(CodeBuilder body, String field,
			Class<?> type, String ref) throws Exception {
		body.code(field).code(".");
		if (isCollection(type)) {
			body.code(SET_ALL).code("(").code(ref).code(");");
		} else {
			body.code(SET_SINGLE).code("(").codeObject(ref, type).code(");");
		}
		return body;
	}
}
