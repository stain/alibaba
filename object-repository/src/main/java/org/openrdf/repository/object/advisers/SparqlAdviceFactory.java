package org.openrdf.repository.object.advisers;

import java.io.IOException;
import java.io.StringReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.URI;
import java.net.URL;
import java.util.regex.Pattern;

import org.openrdf.annotations.Bind;
import org.openrdf.annotations.Iri;
import org.openrdf.annotations.Sparql;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.repository.object.advice.Advice;
import org.openrdf.repository.object.advice.AdviceFactory;
import org.openrdf.repository.object.advice.AdviceProvider;
import org.openrdf.repository.object.advisers.helpers.SparqlEvaluator;

public class SparqlAdviceFactory implements AdviceFactory, AdviceProvider {
	private static final Pattern NOT_URI = Pattern.compile("\\s|\\}|\\]|\\>|\"");

	public AdviceFactory getAdviserFactory(Class<?> annotationType) {
		if (Sparql.class.equals(annotationType))
			return this;
		return null;
	}

	public Advice createAdvice(Method m) {
		SparqlEvaluator evaluator = createSparqlEvaluator(m);
		Type rtype = m.getGenericReturnType();
		Type[] ptypes = m.getGenericParameterTypes();
		Annotation[][] panns = m.getParameterAnnotations();
		String[][] bindingNames = getBindingNames(panns);
		String[] defaults = getDefaultValues(panns);
		return new SparqlAdvice(evaluator, rtype, ptypes, bindingNames, defaults);
	}

	private SparqlEvaluator createSparqlEvaluator(Method m) {
		String systemId = getSystemId(m);
		String sparql = getSparqlQuery(m);
		try {
			if (NOT_URI.matcher(sparql).find())
				return new SparqlEvaluator(new StringReader(sparql), systemId, true);
			if (URI.create(sparql).isAbsolute())
				return new SparqlEvaluator(sparql, true);
			URL url = m.getDeclaringClass().getResource(sparql);
			if (url != null)
				return new SparqlEvaluator(url.toExternalForm(), true);
			String uri = URI.create(systemId).resolve(sparql).toASCIIString();
			return new SparqlEvaluator(uri, true);
		} catch (IOException e) {
			throw new ExceptionInInitializerError(e);
		} catch (MalformedQueryException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	private String getSparqlQuery(Method m) {
		return m.getAnnotation(Sparql.class).value();
	}

	private String getSystemId(Method m) {
		if (m.isAnnotationPresent(Iri.class))
			return m.getAnnotation(Iri.class).value();
		Class<?> dclass = m.getDeclaringClass();
		String mame = m.getName();
		if (dclass.isAnnotationPresent(Iri.class)) {
			String url = dclass.getAnnotation(Iri.class).value();
			if (url.indexOf('#') >= 0)
				return url.substring(0, url.indexOf('#') + 1) + mame;
			return url + "#" + mame;
		}
		String name = dclass.getSimpleName() + ".class";
		URL url = dclass.getResource(name);
		if (url != null)
			return url.toExternalForm() + "#" + mame;
		return "java:" + dclass.getName() + "#" + mame;
	}

	private String[][] getBindingNames(Annotation[][] anns) {
		String[][] bindingNames = new String[anns.length][];
		loop: for (int i=0; i<anns.length; i++) {
			bindingNames[i] = new String[0];
			for (Annotation ann : anns[i]) {
				if (Bind.class.equals(ann.annotationType())) {
					bindingNames[i] = ((Bind) ann).value();
					continue loop;
				} else if (Iri.class.equals(ann.annotationType())) {
					bindingNames[i] = new String[] { local(((Iri) ann).value()) };
				}
			}
		}
		return bindingNames;
	}

	private String local(String iri) {
		String string = iri;
		if (string.lastIndexOf('#') >= 0) {
			string = string.substring(string.lastIndexOf('#') + 1);
		}
		if (string.lastIndexOf('?') >= 0) {
			string = string.substring(string.lastIndexOf('?') + 1);
		}
		if (string.lastIndexOf('/') >= 0) {
			string = string.substring(string.lastIndexOf('/') + 1);
		}
		if (string.lastIndexOf(':') >= 0) {
			string = string.substring(string.lastIndexOf(':') + 1);
		}
		return string;
	}

	private String[] getDefaultValues(Annotation[][] anns) {
		String[] defaults = new String[anns.length];
		for (int i=0; i<anns.length; i++) {
			Object value = getDefaultValue(anns[i]);
			if (value != null) {
				defaults[i] = value.toString();
			}
		}
		return defaults;
	}

	private Object getDefaultValue(Annotation[] anns) {
		for (Annotation ann : anns) {
			for (Method m : ann.annotationType().getDeclaredMethods()) {
				Iri iri = m.getAnnotation(Iri.class);
				if (iri != null && OWL.HASVALUE.equals(iri.value()) && m.getParameterTypes().length == 0) {
					return invoke(m, ann);
				}
			}
		}
		return null;
	}

	private Object invoke(Method m, Object obj) {
		try {
			return m.invoke(obj);
		} catch (IllegalArgumentException e) {
			throw new AssertionError(e);
		} catch (IllegalAccessException e) {
			IllegalAccessError error = new IllegalAccessError(e.getMessage());
			error.initCause(e);
			throw error;
		} catch (InvocationTargetException e) {
			try {
				throw e.getCause();
			} catch (RuntimeException cause) {
				throw cause;
			} catch (Error cause) {
				throw cause;
			} catch (Throwable cause) {
				throw new UndeclaredThrowableException(cause);
			}
		}
	}

}
