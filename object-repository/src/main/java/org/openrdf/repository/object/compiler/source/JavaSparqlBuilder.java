package org.openrdf.repository.object.compiler.source;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openrdf.model.URI;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.repository.object.compiler.JavaNameResolver;
import org.openrdf.repository.object.compiler.model.RDFClass;
import org.openrdf.repository.object.compiler.model.RDFProperty;
import org.openrdf.repository.object.exceptions.BehaviourException;
import org.openrdf.repository.object.exceptions.ObjectStoreConfigException;
import org.openrdf.repository.object.managers.helpers.SPARQLQueryOptimizer;

public class JavaSparqlBuilder extends JavaMessageBuilder {
	private Pattern startsWithPrefix = Pattern.compile("\\s*PREFIX\\s.*",
			Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

	public JavaSparqlBuilder(File source, JavaNameResolver resolver)
			throws FileNotFoundException {
		super(source, resolver);
	}

	public JavaMessageBuilder sparql(RDFClass msg, RDFClass property, String sparql,
			Map<String, String> namespaces) throws ObjectStoreConfigException {
		RDFProperty resp = msg.getResponseProperty();
		JavaMethodBuilder out = message(msg, sparql == null);
		if (sparql != null) {
			String range = getRangeObjectClassName(msg, resp);
			String rangeClassName = getRangeClassName(msg, resp);
			RDFClass range2 = msg.getRange(resp);
			Map<String, String> eager = null;
			if (!range2.isDatatype()) {
				eager = new HashMap<String, String>();
				for (RDFProperty prop : range2
						.getFunctionalDatatypeProperties()) {
					URI p = resolver.getType(prop.getURI());
					String name = resolver.getMemberName(p);
					eager.put(name, p.stringValue());
				}
			}
			String qry = prefixQueryString(sparql, namespaces);
			out.code("try {\n\t\t\t");
			String base = property.getURI().stringValue();
			boolean functional = msg.isFunctional(resp);
			Map<String, String> parameters = new HashMap<String, String>();
			for (RDFProperty param : msg.getParameters()) {
				if (msg.isFunctional(param)) {
					String name = resolver.getMemberName(param.getURI());
					boolean datatype = msg.getRange(param).isDatatype();
					boolean primitive = !getRangeObjectClassName(msg, param)
							.equals(getRangeClassName(msg, param));
					boolean bool = getRangeClassName(msg, param).equals(
							"boolean");
					parameters.put(name, getBindingValue(name, datatype,
							primitive, bool));
				} else {
					// TODO handle plural parameterTypes
					throw new ObjectStoreConfigException(
							"All parameterTypes of sparql methods must be functional: "
									+ property.getURI());
				}
			}
			out.code(new SPARQLQueryOptimizer().implementQuery(qry, base,
					eager, range, rangeClassName, functional, parameters));
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

	private String prefixQueryString(String sparql,
			Map<String, String> namespaces) {
		if (startsWithPrefix.matcher(sparql).matches())
			return sparql;
		String regex = "[pP][rR][eE][fF][iI][xX]\\s+";
		StringBuilder sb = new StringBuilder(256 + sparql.length());
		for (String prefix : namespaces.keySet()) {
			String pattern = regex + prefix + "\\s*:";
			Matcher m = Pattern.compile(pattern).matcher(sparql);
			if (sparql.contains(prefix) && !m.find()) {
				sb.append("PREFIX ").append(prefix).append(":<");
				sb.append(namespaces.get(prefix)).append("> ");
			}
		}
		return sb.append(sparql).toString();
	}

}
