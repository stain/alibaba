package org.openrdf.repository.object.advisers;

import java.io.StringReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.openrdf.annotations.Bind;
import org.openrdf.annotations.Iri;
import org.openrdf.annotations.Sparql;
import org.openrdf.repository.object.advice.Advice;
import org.openrdf.repository.object.advice.AdviceFactory;
import org.openrdf.repository.object.advice.AdviceProvider;
import org.openrdf.repository.object.advisers.helpers.SparqlEvaluator;

public class SparqlAdviceFactory implements AdviceFactory, AdviceProvider {

	public AdviceFactory getAdviserFactory(Class<?> annotationType) {
		if (Sparql.class.equals(annotationType))
			return this;
		return null;
	}

	public Advice createAdviser(Method m) {
		String base = getBaseIri(m);
		String sparql = getSparqlQuery(m);
		SparqlEvaluator evaluator = new SparqlEvaluator(new StringReader(sparql), base, true);
		Annotation[][] anns = m.getParameterAnnotations();
		String[][] bindingNames = new String[anns.length][];
		for (int i=0; i<anns.length; i++) {
			bindingNames[i] = new String[0];
			for (Annotation ann : anns[i]) {
				if (Bind.class.equals(ann.annotationType())) {
					bindingNames[i] = ((Bind) ann).value();
				}
			}
		}
		return new SparqlAdvice(evaluator, bindingNames);
	}

	private String getSparqlQuery(Method m) {
		return m.getAnnotation(Sparql.class).value();
	}

	private String getBaseIri(Method m) {
		String base;
		if (m.getDeclaringClass().isAnnotationPresent(Iri.class)) {
			base = m.getDeclaringClass().getAnnotation(Iri.class).value();
		} else {
			base = "java:" + m.getDeclaringClass().getName();
		}
		return base;
	}

}
