package org.openrdf.repository.object.compiler.source;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openrdf.model.vocabulary.OWL;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.repository.object.compiler.JavaNameResolver;
import org.openrdf.repository.object.compiler.model.RDFClass;
import org.openrdf.repository.object.compiler.model.RDFProperty;
import org.openrdf.repository.object.exceptions.BehaviourException;
import org.openrdf.repository.object.exceptions.ObjectStoreConfigException;
import org.openrdf.repository.object.managers.helpers.XSLTOptimizer;
import org.openrdf.repository.object.xslt.XSLTransformer;

public class JavaXSLTBuilder extends JavaMessageBuilder {
	private static Set<String> parameterTypes;
	static {
		Set<String> set = new HashSet<String>();
		for (Method method : XSLTransformer.class.getMethods()) {
			if ("with".equals(method.getName())
					&& method.getParameterTypes().length == 2) {
				set.add(method.getParameterTypes()[1].getName());
			}
		}
		parameterTypes = Collections.unmodifiableSet(set);
	}

	public JavaXSLTBuilder(File source, JavaNameResolver resolver)
			throws FileNotFoundException {
		super(source, resolver);
	}

	public JavaMessageBuilder xslt(RDFClass msg, RDFClass property, String xslt,
			Map<String, String> namespaces) throws ObjectStoreConfigException {
		XSLTOptimizer optimizer = new XSLTOptimizer();
		RDFProperty resp = msg.getResponseProperty();
		String base = property.getURI().stringValue();
		String field = "xslt" + Math.abs(base.hashCode());
		staticField(imports(optimizer.getFieldType()), field, optimizer
				.getFieldConstructor(xslt, base));
		JavaMethodBuilder out = message(msg, xslt == null);
		if (xslt != null) {
			String rangeClassName = getRangeClassName(msg, resp);
			String input = null;
			String inputName = null;
			out.code("try {\n\t\t\t");
			Map<String, String> parameters = new HashMap<String, String>();
			parameters.put("this", getBindingValue("this", false, false, false)
					+ ".stringValue()");
			List<RDFProperty> msgParameters = msg.getParameters();
			int inputIdx = -1;
			for (int i = msgParameters.size() - 1; i >= 0; i--) {
				RDFProperty param = msgParameters.get(i);
				if (resolver.getExplicitMemberName(param.getURI()) == null) {
					String range = getRangeClassName(msg, param);
					if (optimizer.isKnownInputType(range)
							|| !param.isA(OWL.DATATYPEPROPERTY)) {
						inputIdx = i;
						input = range;
						inputName = resolver.getMemberName(param.getURI());
						break;
					}
				}
			}
			for (int i = 0, n = msgParameters.size(); i < n; i++) {
				if (i == inputIdx)
					continue;
				RDFProperty param = msgParameters.get(i);
				if (msg.isFunctional(param)) {
					String name = resolver.getMemberName(param.getURI());
					String range = getRangeClassName(msg, param);
					boolean datatype = msg.getRange(param).isDatatype();
					boolean primitive = !getRangeObjectClassName(msg, param)
							.equals(range);
					boolean bool = range.equals("boolean");
					if (parameterTypes.contains(range)) {
						parameters.put(name, name);
					} else {
						parameters.put(name, getBindingValue(name, datatype,
								primitive, bool)
								+ ".stringValue()");
					}
				} else {
					// TODO handle plural parameterTypes
					throw new ObjectStoreConfigException(
							"All parameterTypes of xslt methods must be functional: "
									+ property.getURI());
				}
			}
			String call = optimizer.implementXSLT(field, input, inputName,
					parameters, rangeClassName);
			out.code("return ").code(call).code(";");
			out.code("\n\t\t} catch(");
			out.code(out.imports(RuntimeException.class)).code(" e) {\n");
			out.code("\t\t\tthrow e;");
			out.code("\n\t\t} catch(");
			out.code(out.imports(Exception.class)).code(" e) {\n");
			out.code("\t\t\tthrow new ");
			out.code(out.imports(BehaviourException.class)).code("(e, ");
			out.string(String.valueOf(property.getURI())).code(");\n");
			out.code("\t\t}\n");
		}
		out.end();
		return this;
	}

	private String getBindingValue(String name, boolean datatype,
			boolean primitive, boolean bool) {
		StringBuilder out = new StringBuilder();
		if (bool || primitive) {
			out
					.append("getObjectConnection().getValueFactory().createLiteral(");
			out.append(name);
			out.append(")");
		} else if (datatype) {
			out.append(name).append(" == null ? null : ");
			out
					.append("getObjectConnection().getObjectFactory().createLiteral(");
			out.append(name);
			out.append(")");
		} else {
			out.append(name).append(" == null ? null : ");
			out.append("((");
			out.append(RDFObject.class.getName()).append(")");
			out.append(name);
			out.append(").getResource()");
		}
		return out.toString();
	}

}
