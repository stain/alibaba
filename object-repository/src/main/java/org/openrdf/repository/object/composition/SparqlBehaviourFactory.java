package org.openrdf.repository.object.composition;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.openrdf.repository.object.annotations.rdf;
import org.openrdf.repository.object.annotations.sparql;
import org.openrdf.repository.object.exceptions.BehaviourException;
import org.openrdf.repository.object.exceptions.ObjectStoreConfigException;
import org.openrdf.repository.object.managers.helpers.ObjectQueryOptimizer;

public class SparqlBehaviourFactory extends BehaviourFactory {

	private static final String CLASS_PREFIX = "object.behaviours.";

	@Override
	protected String getJavaClassName(Class<?> concept) {
		String cn = concept.getName();
		return CLASS_PREFIX + cn + "Sparql";
	}

	@Override
	protected boolean isEnhanceable(Class<?> concept)
			throws ObjectStoreConfigException {
		for (Method m : concept.getDeclaredMethods()) {
			if (m.isAnnotationPresent(sparql.class))
				return true;
		}
		return false;
	}

	@Override
	protected void enhance(ClassTemplate cc, Class<?> concept) throws Exception {
		for (Method m : concept.getDeclaredMethods()) {
			if (m.isAnnotationPresent(sparql.class)) {
				enhance(cc, m);
			}
		}
	}

	private void enhance(ClassTemplate cc, Method m) throws Exception {
		String[] sparql = m.getAnnotation(sparql.class).value();
		assert sparql.length == 1;
		String base;
		if (m.getDeclaringClass().isAnnotationPresent(rdf.class)) {
			base = m.getDeclaringClass().getAnnotation(rdf.class).value();
		} else {
			base = "java:" + m.getDeclaringClass().getName();
		}
		int argc = m.getParameterTypes().length;
		List<String> args = new ArrayList<String>(argc);
		for (int i = 1; i <= argc; i++) {
			args.add("$" + i);
		}
		CodeBuilder out = cc.overrideMethod(m, m.isBridge());
		out.code("try {\n");
		ObjectQueryOptimizer oqo = new ObjectQueryOptimizer();
		String str = oqo.implementQuery(sparql[0], base, m, args, properties);
		out.code(str);
		out.code("\n} catch(");
		out.code(Exception.class.getName()).code(" e) {");
		out.code("throw new ");
		out.code(BehaviourException.class.getName()).code("(e);");
		out.code("}");
		out.end();
	}

}
