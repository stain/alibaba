package org.openrdf.script.base;

import org.openrdf.script.ast.ASTAnd;
import org.openrdf.script.ast.ASTAssignment;
import org.openrdf.script.ast.ASTBaseDecl;
import org.openrdf.script.ast.ASTBasicGraphPattern;
import org.openrdf.script.ast.ASTBindingSet;
import org.openrdf.script.ast.ASTBindingVar;
import org.openrdf.script.ast.ASTBlankNode;
import org.openrdf.script.ast.ASTBlockExpression;
import org.openrdf.script.ast.ASTBody;
import org.openrdf.script.ast.ASTBooleanLiteral;
import org.openrdf.script.ast.ASTBoundVar;
import org.openrdf.script.ast.ASTConditionalAndExpression;
import org.openrdf.script.ast.ASTConditionalOrExpression;
import org.openrdf.script.ast.ASTControl;
import org.openrdf.script.ast.ASTEnsure;
import org.openrdf.script.ast.ASTFilter;
import org.openrdf.script.ast.ASTFunction;
import org.openrdf.script.ast.ASTGraphGraphPattern;
import org.openrdf.script.ast.ASTGraphPattern;
import org.openrdf.script.ast.ASTGroupOrUnionGraphPattern;
import org.openrdf.script.ast.ASTIRI;
import org.openrdf.script.ast.ASTKeyword;
import org.openrdf.script.ast.ASTMessage;
import org.openrdf.script.ast.ASTMessageList;
import org.openrdf.script.ast.ASTNumericLiteralNegative;
import org.openrdf.script.ast.ASTNumericLiteralPositive;
import org.openrdf.script.ast.ASTNumericLiteralUnsigned;
import org.openrdf.script.ast.ASTObject;
import org.openrdf.script.ast.ASTObjectList;
import org.openrdf.script.ast.ASTOptionalGraphPattern;
import org.openrdf.script.ast.ASTOr;
import org.openrdf.script.ast.ASTPrefixDecl;
import org.openrdf.script.ast.ASTPrefixedName;
import org.openrdf.script.ast.ASTPropertyList;
import org.openrdf.script.ast.ASTRDFLiteral;
import org.openrdf.script.ast.ASTRelationalExpression;
import org.openrdf.script.ast.ASTRescue;
import org.openrdf.script.ast.ASTResourceTemplate;
import org.openrdf.script.ast.ASTReverseTraversal;
import org.openrdf.script.ast.ASTStatements;
import org.openrdf.script.ast.ASTString;
import org.openrdf.script.ast.ASTTraversal;
import org.openrdf.script.ast.ASTTriplesSameSubject;
import org.openrdf.script.ast.ASTUnaryExpression;
import org.openrdf.script.ast.ASTValue;
import org.openrdf.script.ast.ASTValueList;
import org.openrdf.script.ast.ASTVerb;
import org.openrdf.script.ast.SimpleNode;
import org.openrdf.script.ast.SyntaxTreeBuilderVisitor;

public class SyntaxTreeVisitorBase implements SyntaxTreeBuilderVisitor {

	public Object visit(ASTBindingSet node, Object data) {
		return visitChildren(node, data);
	}

	public Object visit(ASTBlockExpression node, Object data) {
		return visitChildren(node, data);
	}

	private Object visitChildren(SimpleNode node, Object data) {
		Object result = data;
		for (int i = 0, n = node.jjtGetNumChildren(); i < n; ++i) {
			Object o = node.jjtGetChild(i).jjtAccept(this, data);
			if (o != data) {
				assert result == data;
				result = o;
			}
		}
		return result;
	}

	public Object visit(ASTConditionalAndExpression node, Object data) {
		return visitChildren(node, data);
	}

	public Object visit(ASTConditionalOrExpression node, Object data) {
		return visitChildren(node, data);
	}

	public Object visit(ASTGraphPattern node, Object data) {
		return visitChildren(node, data);
	}

	public Object visit(ASTKeyword node, Object data) {
		return visitChildren(node, data);
	}

	public Object visit(ASTMessageList node, Object data) {
		return visitChildren(node, data);
	}

	public Object visit(ASTRelationalExpression node, Object data) {
		return visitChildren(node, data);
	}

	public Object visit(ASTResourceTemplate node, Object data) {
		return visitChildren(node, data);
	}

	public Object visit(ASTValue node, Object data) {
		return visitChildren(node, data);
	}

	public Object visit(SimpleNode node, Object data) {
		return visitChildren(node, data);
	}

	public Object visit(ASTBody node, Object data) {
		return visitChildren(node, data);
	}

	public Object visit(ASTBaseDecl node, Object data) {
		return visitChildren(node, data);
	}

	public Object visit(ASTPrefixDecl node, Object data) {
		return visitChildren(node, data);
	}

	public Object visit(ASTAssignment node, Object data) {
		return visitChildren(node, data);
	}

	public Object visit(ASTBasicGraphPattern node, Object data) {
		return visitChildren(node, data);
	}

	public Object visit(ASTOptionalGraphPattern node, Object data) {
		return visitChildren(node, data);
	}

	public Object visit(ASTGraphGraphPattern node, Object data) {
		return visitChildren(node, data);
	}

	public Object visit(ASTGroupOrUnionGraphPattern node, Object data) {
		return visitChildren(node, data);
	}

	public Object visit(ASTFilter node, Object data) {
		return visitChildren(node, data);
	}

	public Object visit(ASTOr node, Object data) {
		return visitChildren(node, data);
	}

	public Object visit(ASTAnd node, Object data) {
		return visitChildren(node, data);
	}

	public Object visit(ASTUnaryExpression node, Object data) {
		return visitChildren(node, data);
	}

	public Object visit(ASTTriplesSameSubject node, Object data) {
		return visitChildren(node, data);
	}

	public Object visit(ASTObject node, Object data) {
		return visitChildren(node, data);
	}

	public Object visit(ASTPropertyList node, Object data) {
		return visitChildren(node, data);
	}

	public Object visit(ASTObjectList node, Object data) {
		return visitChildren(node, data);
	}

	public Object visit(ASTVerb node, Object data) {
		return visitChildren(node, data);
	}

	public Object visit(ASTBindingVar node, Object data) {
		return visitChildren(node, data);
	}

	public Object visit(ASTBoundVar node, Object data) {
		return visitChildren(node, data);
	}

	public Object visit(ASTTraversal node, Object data) {
		return visitChildren(node, data);
	}

	public Object visit(ASTReverseTraversal node, Object data) {
		return visitChildren(node, data);
	}

	public Object visit(ASTMessage node, Object data) {
		return visitChildren(node, data);
	}

	public Object visit(ASTBlankNode node, Object data) {
		return visitChildren(node, data);
	}

	public Object visit(ASTIRI node, Object data) {
		return visitChildren(node, data);
	}

	public Object visit(ASTPrefixedName node, Object data) {
		return visitChildren(node, data);
	}

	public Object visit(ASTRDFLiteral node, Object data) {
		return visitChildren(node, data);
	}

	public Object visit(ASTNumericLiteralUnsigned node, Object data) {
		return visitChildren(node, data);
	}

	public Object visit(ASTNumericLiteralPositive node, Object data) {
		return visitChildren(node, data);
	}

	public Object visit(ASTNumericLiteralNegative node, Object data) {
		return visitChildren(node, data);
	}

	public Object visit(ASTBooleanLiteral node, Object data) {
		return visitChildren(node, data);
	}

	public Object visit(ASTString node, Object data) {
		return visitChildren(node, data);
	}

	public Object visit(ASTRescue node, Object data) {
		return visitChildren(node, data);
	}

	public Object visit(ASTEnsure node, Object data) {
		return visitChildren(node, data);
	}

	public Object visit(ASTControl node, Object data) {
		return visitChildren(node, data);
	}

	public Object visit(ASTFunction node, Object data) {
		return visitChildren(node, data);
	}

	public Object visit(ASTValueList node, Object data) {
		return visitChildren(node, data);
	}

	public Object visit(ASTStatements node, Object data) {
		return visitChildren(node, data);
	}

}
