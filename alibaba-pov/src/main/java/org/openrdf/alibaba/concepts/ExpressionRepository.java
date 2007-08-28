package org.openrdf.alibaba.concepts;

import java.util.Set;

import org.openrdf.alibaba.repositories.ExpressionRepositoryBehaviour;
import org.openrdf.concepts.owl.Thing;
import org.openrdf.elmo.annotations.rdf;

/** Repository of Expressions. */
@rdf("http://www.openrdf.org/rdf/2007/08/point-of-view#ExpressionRepository")
public interface ExpressionRepository extends Thing, ExpressionRepositoryBehaviour {


	/** A default expression. */
	@rdf("http://www.openrdf.org/rdf/2007/08/point-of-view#defaultExpression")
	public abstract Expression getPovDefaultExpression();

	/** A default expression. */
	public abstract void setPovDefaultExpression(Expression value);


	/** Set of expressions. */
	@rdf("http://www.openrdf.org/rdf/2007/08/point-of-view#registeredExpression")
	public abstract Set<Expression> getPovRegisteredExpressions();

	/** Set of expressions. */
	public abstract void setPovRegisteredExpressions(Set<Expression> value);

}
