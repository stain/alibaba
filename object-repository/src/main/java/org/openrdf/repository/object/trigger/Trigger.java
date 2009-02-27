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
package org.openrdf.repository.object.trigger;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.object.annotations.triggeredBy;
import org.openrdf.repository.object.managers.PropertyMapper;

public class Trigger {

	private String predicate;

	private Class<?> declaredIn;

	private String methodName;

	private String sparql;

	public Trigger(Method method, PropertyMapper mapper) {
		this.predicate = method.getAnnotation(triggeredBy.class).value();
		this.declaredIn = method.getDeclaringClass();
		this.methodName = method.getName();
		sparql = buildQuery(mapper);
	}

	public String toString() {
		return methodName;
	}

	public Class<?> getDeclaredIn() {
		return declaredIn;
	}

	public String getMethodName() {
		return methodName;
	}

	public String getSparqlQuery() {
		return sparql;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((methodName == null) ? 0 : methodName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Trigger other = (Trigger) obj;
		if (methodName == null) {
			if (other.methodName != null)
				return false;
		} else if (!methodName.equals(other.methodName))
			return false;
		return true;
	}

	private String buildQuery(PropertyMapper mapper) {
		Map<String, String> subjectProperties = mapper
				.findEagerProperties(declaredIn);
		if (subjectProperties == null) {
			subjectProperties = new HashMap<String, String>();
			subjectProperties.put("class", RDF.TYPE.stringValue());
		}
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT ?_");
		for (String name : subjectProperties.keySet()) {
			sb.append(" ?__").append(name);
		}
		sb.append("\nWHERE { ");
		sb.append(" ?_ <").append(predicate).append("> ?obj ");
		for (String name : subjectProperties.keySet()) {
			String pred = subjectProperties.get(name);
			sb.append("\nOPTIONAL {").append(" ?_ <");
			sb.append(pred);
			sb.append("> ?__").append(name).append(" } ");
		}
		sb.append(" } ");
		return sb.toString();
	}

}
