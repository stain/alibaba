package org.openrdf.alibaba.decor.support;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.codehaus.groovy.runtime.InvokerHelper;
import org.openrdf.alibaba.decor.DecorationBehaviour;
import org.openrdf.alibaba.decor.TemplateDecoration;
import org.openrdf.alibaba.exceptions.AlibabaException;
import org.openrdf.alibaba.exceptions.InternalServerErrorException;
import org.openrdf.alibaba.vocabulary.DCR;
import org.openrdf.elmo.annotations.rdf;

/**
 * Dynamic Groovy Template decoration support.
 * 
 * @author James Leigh
 *
 */
@rdf(DCR.NS + "TemplateDecoration")
public class TemplateDecorationSupport extends TextDecorationSupport implements
		DecorationBehaviour {
	private static ConcurrentMap<String, Class<? extends Script>> scripts = new ConcurrentHashMap<String, Class<? extends Script>>();

	private CharArrayWriter buffer = new CharArrayWriter();

	public TemplateDecorationSupport(TemplateDecoration decoration) {
		super(decoration);
	}

	@Override
	protected void print(String text, Map<String, ?> bindings)
			throws AlibabaException, IOException {
		if (text != null) {
			try {
				Class<? extends Script> sc = getScript(text);
				Map<String, Object> map = new HashMap<String, Object>(bindings);
				Binding binding = new Binding(map);
				Script script = InvokerHelper.createScript(sc, binding);
				script.run();
			} catch (RuntimeException e) {
				throw e;
			} catch (Exception e) {
				throw new InternalServerErrorException(e);
			}
		}
	}

	@Override
	protected String interpret(String text, Map<String, ?> bindings)
			throws AlibabaException, IOException {
		Object old = bindings.get("out");
		buffer.reset();
		PrintWriter out = new PrintWriter(buffer);
		((Map<String, Object>) bindings).put("out", out);
		print(text, bindings);
		((Map<String, Object>) bindings).put("out", old);
		out.flush();
		return buffer.toString();
	}

	private Class<? extends Script> getScript(String text) throws IOException {
		Class<? extends Script> script = scripts.get(text);
		if (script == null) {
			script = createScript(text);
			Class<? extends Script> o = scripts.putIfAbsent(text, script);
			if (o != null)
				return o;
		}
		return script;
	}

	private Class<? extends Script> createScript(String text)
			throws IOException {
		GroovyShell shell = new GroovyShell(GroovyShell.class.getClassLoader());
		return shell.parse(parse(text)).getClass();
	}

	private String parse(String text) throws IOException {
		Reader reader = new StringReader(text);
		StringWriter sw = new StringWriter();
		sw.write("def toString(o) { if (o == null) return \"\"; return o.toString(); }\n");
		sw.write("out.print(\"");
		int c;
		while ((c = reader.read()) != -1) {
			if (c == '<') {
				reader.mark(1);
				c = reader.read();
				if (c != '%') {
					sw.write('<');
					reader.reset();
				} else {
					reader.mark(1);
					c = reader.read();
					if (c == '=') {
						expression(reader, sw);
					} else {
						reader.reset();
						code(reader, sw);
					}
				}
				continue;
			}
			if (c == '\"') {
				sw.write('\\');
			}
			if (c == '\n' || c == '\r') {
				if (c == '\r') {
					reader.mark(1);
					c = reader.read();
					if (c != '\n') {
						reader.reset();
					}
				}
				sw.write("\\n\");\nout.print(\"");
				continue;
			}
			sw.write(c);
		}
		sw.write("\");\n");
		return sw.toString();
	}

	private void expression(Reader reader, StringWriter sw) throws IOException {
		sw.write("\");out.print(presentation.getPovEncoding().encode(toString(");
		int c;
		while ((c = reader.read()) != -1) {
			if (c == '%') {
				c = reader.read();
				if (c != '>') {
					sw.write('%');
				} else {
					break;
				}
			}
			if (c != '\n' && c != '\r') {
				sw.write(c);
			}
		}
		sw.write(")));\nout.print(\"");
	}

	private void code(Reader reader, StringWriter sw) throws IOException {
		sw.write("\");");
		int c;
		while ((c = reader.read()) != -1) {
			if (c == '%') {
				c = reader.read();
				if (c != '>') {
					sw.write('%');
				} else {
					break;
				}
			}
			sw.write(c);
		}
		sw.write(";\nout.print(\"");
	}

}
