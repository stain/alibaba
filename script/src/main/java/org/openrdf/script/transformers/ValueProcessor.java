package org.openrdf.script.transformers;

import info.aduna.net.ParsedURI;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.datatypes.XMLDatatypeUtil;
import org.openrdf.script.ast.ASTBaseDecl;
import org.openrdf.script.ast.ASTIRI;
import org.openrdf.script.ast.ASTKeyword;
import org.openrdf.script.ast.ASTKeywordDecl;
import org.openrdf.script.ast.ASTPrefixDecl;
import org.openrdf.script.ast.ASTRDFLiteral;
import org.openrdf.script.ast.ASTString;
import org.openrdf.script.ast.SimpleNode;
import org.openrdf.script.vocabulary.Script;

public class ValueProcessor {

	private static final String NS = Script.NAMESPACE;

	private static final String[] DEFAULT_KEYWORDS = { "a", "base", "prefix",
			"keywords", "do", "rescue", "ensure", "end", "optional", "graph",
			"union", "filter", "true", "false", "using", "insert", "delete",
			"where", "into", "from", "if", "then", "super", "regex", "else",
			"case", "for", "loop", "order", "asc", "desc", "while", "begin",
			"try", "ask", "return" };

	private Set<String> keywords = new HashSet<String>(Arrays
			.asList(DEFAULT_KEYWORDS));

	private ParsedURI baseURI;

	private Map<String, String> namespaces = new HashMap<String, String>();

	private ValueFactory vf;

	public ValueProcessor(ValueFactory vf) {
		this.vf = vf;
	}

	public void setKeywords(String... keywords) {
		this.keywords.clear();
		for (String keyword : keywords) {
			this.keywords.add(keyword);
		}
	}

	public void setBase(String baseURI) {
		if (baseURI == null) {
			this.baseURI = null;
		} else {
			this.baseURI = new ParsedURI(baseURI);
		}
	}

	public void setNamespaces(Map<String, String> namespaces) {
		this.namespaces.putAll(namespaces);
	}

	public void setKeywords(ASTKeywordDecl node) {
		String[] result = new String[node.jjtGetNumChildren()];
		for (int i = 0, n = node.jjtGetNumChildren(); i < n; ++i) {
			ASTKeyword o = (ASTKeyword) node.jjtGetChild(i);
			result[i] = o.jjtGetFirstToken().image;
		}
		setKeywords(result);
	}

	public void setBase(ASTBaseDecl node) {
		ASTIRI child = (ASTIRI) node.jjtGetChild(0);
		String iri = child.jjtGetFirstToken().image;
		assert iri.startsWith("<") && iri.endsWith(">");
		setBase(iri.substring(1, iri.length() - 1));
	}

	public void setNamespace(ASTPrefixDecl node) {
		String prefix = node.jjtGetValue().toString();
		assert prefix.endsWith(":");
		prefix = prefix.substring(0, prefix.length() - 1);
		ASTIRI iri = (ASTIRI) node.jjtGetChild(0);
		namespaces.put(prefix, createURI(iri).stringValue());
	}

	public URI createURI(SimpleNode node) {
		String token = node.jjtGetFirstToken().image;
		if (token.startsWith("<") && token.endsWith(">")) {
			token = token.substring(1, token.length() - 1);
			if (baseURI != null) {
				token = baseURI.resolve(token).toString();
			}
			return vf.createURI(token);
		} else if (token.contains(":")) {
			int colonIdx = token.indexOf(':');
			String prefix = token.substring(0, colonIdx);
			String localName = token.substring(colonIdx + 1);

			String namespace = namespaces.get(prefix);
			if (namespace == null) {
				throw new AssertionError("QName '" + token
						+ "' uses an undefined prefix");
			}
			return vf.createURI(namespace, localName);
		} else if (token.startsWith("@")) {
			return vf.createURI(NS, token.substring(1));
		} else if (keywords.contains(token)) {
			return vf.createURI(NS, token);
		} else {
			String namespace = namespaces.get("");
			if (namespace == null) {
				throw new AssertionError("No default namespace");
			}
			return vf.createURI(namespace, token);
		}
	}

	public Literal createLiteral(SimpleNode node) {
		if (node instanceof ASTRDFLiteral) {
			ASTString str = (ASTString) node.jjtGetChild(0);
			String stringValue = extract(str.jjtGetFirstToken().image);
			if (node.jjtGetNumChildren() == 1) {
				String lang = node.jjtGetFirstToken().image;
				assert lang.startsWith("@");
				return vf.createLiteral(stringValue, lang.substring(1));
			}
			URI datatype = createURI((SimpleNode) node.jjtGetChild(1));
			return vf.createLiteral(stringValue, datatype);

		}
		String value = node.jjtGetFirstToken().image;
		return createLiteral(value);
	}

	private Literal createLiteral(String value) throws AssertionError {
		if (Boolean.TRUE.toString().equals(value))
			return vf.createLiteral(true);
		if (Boolean.FALSE.toString().equals(value))
			return vf.createLiteral(false);
		if (value.startsWith("'") || value.startsWith("\"")) {
			return vf.createLiteral(extract(value));
		}
		if (value.contains("e") || value.contains("E")) {
			return vf.createLiteral(XMLDatatypeUtil.parseDouble(value));
		}
		if (value.contains(".")) {
			return vf.createLiteral(XMLDatatypeUtil.parseDecimal(value));
		}
		return vf.createLiteral(XMLDatatypeUtil.parseInteger(value));
	}

	private String extract(String value) throws AssertionError {
		if (value.length() < 2)
			throw new AssertionError();
		if (value.startsWith("\"\"\"") || value.startsWith("'''"))
			return decodeString(value.substring(3, value.length() - 3));
		return decodeString(value.substring(1, value.length() - 1));
	}

	/**
	 * Decodes an encoded SPARQL string. Any \-escape sequences are substituted
	 * with their decoded value.
	 * 
	 * @param s
	 *            An encoded SPARQL string.
	 * @return The decoded string.
	 * @exception IllegalArgumentException
	 *                If the supplied string is not a correctly encoded SPARQL
	 *                string.
	 */
	public static String decodeString(String s) {
		int backSlashIdx = s.indexOf('\\');

		if (backSlashIdx == -1) {
			// No escaped characters found
			return s;
		}

		int startIdx = 0;
		int sLength = s.length();
		StringBuilder sb = new StringBuilder(sLength);

		while (backSlashIdx != -1) {
			sb.append(s.substring(startIdx, backSlashIdx));

			if (backSlashIdx + 1 >= sLength) {
				throw new IllegalArgumentException("Unescaped backslash in: "
						+ s);
			}

			char c = s.charAt(backSlashIdx + 1);

			if (c == 't') {
				sb.append('\t');
				startIdx = backSlashIdx + 2;
			} else if (c == 'n') {
				sb.append('\n');
				startIdx = backSlashIdx + 2;
			} else if (c == 'r') {
				sb.append('\r');
				startIdx = backSlashIdx + 2;
			} else if (c == 'b') {
				sb.append('\b');
				startIdx = backSlashIdx + 2;
			} else if (c == 'f') {
				sb.append('\f');
				startIdx = backSlashIdx + 2;
			} else if (c == '"') {
				sb.append('"');
				startIdx = backSlashIdx + 2;
			} else if (c == '\'') {
				sb.append('\'');
				startIdx = backSlashIdx + 2;
			} else if (c == '\\') {
				sb.append('\\');
				startIdx = backSlashIdx + 2;
			} else {
				throw new IllegalArgumentException("Unescaped backslash in: "
						+ s);
			}

			backSlashIdx = s.indexOf('\\', startIdx);
		}

		sb.append(s.substring(startIdx));

		return sb.toString();
	}

}
