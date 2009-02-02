package org.openrdf.script.transformers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.openrdf.script.ast.ASTBaseDecl;
import org.openrdf.script.ast.ASTBody;
import org.openrdf.script.ast.ASTBooleanLiteral;
import org.openrdf.script.ast.ASTControl;
import org.openrdf.script.ast.ASTIRI;
import org.openrdf.script.ast.ASTNumericLiteralNegative;
import org.openrdf.script.ast.ASTNumericLiteralPositive;
import org.openrdf.script.ast.ASTNumericLiteralUnsigned;
import org.openrdf.script.ast.ASTPrefixDecl;
import org.openrdf.script.ast.ASTPrefixedName;
import org.openrdf.script.ast.ASTRDFLiteral;
import org.openrdf.script.ast.ASTStatements;
import org.openrdf.script.ast.ASTString;
import org.openrdf.model.URI;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.query.algebra.QueryModelNode;
import org.openrdf.query.algebra.TupleExpr;
import org.openrdf.query.algebra.ValueConstant;
import org.openrdf.script.base.SyntaxTreeVisitorBase;
import org.openrdf.script.model.Body;
import org.openrdf.script.model.ControlNode;

public class CodeTransformer extends SyntaxTreeVisitorBase {
	private ValueProcessor vf = new ValueProcessor(ValueFactoryImpl
			.getInstance());

	public void setBase(String baseURI) {
		vf.setBase(baseURI);
	}

	public void setNamespaces(Map<String, String> namespaces) {
		vf.setNamespaces(namespaces);
	}

	public Body transform(ASTBody tree) {
		return (Body) tree.jjtAccept(this, null);
	}

	@Override
	public Object visit(ASTBaseDecl node, Object nil) {
		vf.setBase(node);
		return nil;
	}

	@Override
	public Object visit(ASTPrefixDecl node, Object nil) {
		vf.setNamespace(node);
		return nil;
	}

	@Override
	public Object visit(ASTBody node, Object nil) {
		Object statements = super.visit(node, nil);
		if (statements != nil)
			return new Body(((List<QueryModelNode>) statements));
		return new Body(Collections.EMPTY_LIST);
	}

	@Override
	public Object visit(ASTStatements node, Object nil) {
		int size = node.jjtGetNumChildren();
		List<QueryModelNode> list = new ArrayList<QueryModelNode>(size);
		for (int i = 0, n = size; i < n; ++i) {
			Object expr = node.jjtGetChild(i).jjtAccept(this, nil);
			list.add((QueryModelNode) expr);
		}
		return list;
	}

	@Override
	public Object visit(ASTControl node, Object nil) {
		ValueConstant iri = (ValueConstant) node.jjtGetChild(0).jjtAccept(this,
				nil);
		TupleExpr expr = (TupleExpr) node.jjtGetChild(1)
				.jjtAccept(this, nil);
		ControlNode control = new ControlNode((URI) iri.getValue(), expr);
		for (int i = 2, n = node.jjtGetNumChildren(); i < n; i++) {
			iri = (ValueConstant) node.jjtGetChild(0).jjtAccept(this, nil);
			expr = (TupleExpr) node.jjtGetChild(1).jjtAccept(this, nil);
			control.addParameter((URI) iri.getValue(), expr);
		}
		return control;
	}

	@Override
	public Object visit(ASTIRI node, Object nil) {
		return new ValueConstant(vf.createURI(node));
	}

	@Override
	public Object visit(ASTPrefixedName node, Object nil) {
		return new ValueConstant(vf.createURI(node));
	}

	@Override
	public Object visit(ASTString node, Object nil) {
		return new ValueConstant(vf.createLiteral(node));
	}

	@Override
	public Object visit(ASTBooleanLiteral node, Object nil) {
		return new ValueConstant(vf.createLiteral(node));
	}

	@Override
	public Object visit(ASTNumericLiteralUnsigned node, Object nil) {
		return new ValueConstant(vf.createLiteral(node));
	}

	@Override
	public Object visit(ASTNumericLiteralNegative node, Object nil) {
		return new ValueConstant(vf.createLiteral(node));
	}

	@Override
	public Object visit(ASTNumericLiteralPositive node, Object nil) {
		return new ValueConstant(vf.createLiteral(node));
	}

	@Override
	public Object visit(ASTRDFLiteral node, Object nil) {
		return new ValueConstant(vf.createLiteral(node));
	}

}
