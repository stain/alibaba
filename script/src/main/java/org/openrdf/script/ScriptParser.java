package org.openrdf.script;

import java.util.Map;

import org.openrdf.script.ast.ASTBody;
import org.openrdf.script.ast.ParseException;
import org.openrdf.script.ast.SyntaxTreeBuilder;
import org.openrdf.script.ast.TokenMgrError;
import org.openrdf.script.model.Body;
import org.openrdf.script.transformers.CodeTransformer;

public class ScriptParser {

	public Body parse(String code) throws TokenMgrError, ParseException {
		return parse(code, null, null);
	}

	public Body parse(String code, String base,
			Map<String, String> namespaces) throws TokenMgrError, ParseException {
		ASTBody tree = SyntaxTreeBuilder.parse(code);
		CodeTransformer transformer = new CodeTransformer();
		if (base != null) {
			transformer.setBase(base);
		}
		if (namespaces != null) {
			transformer.setNamespaces(namespaces);
		}
		return transformer.transform(tree);
	}
}
