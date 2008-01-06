package org.openrdf.alibaba.pov;

import java.util.Set;
import org.openrdf.concepts.owl.Thing;
import org.openrdf.elmo.annotations.rdf;

/** Repository of {@link Expression}s. */
@rdf("http://www.openrdf.org/rdf/2007/09/point-of-view#ExpressionRepository")
public interface ExpressionRepository extends Thing, ExpressionRepositoryBehaviour {


	/** A default expression. */
	@rdf("http://www.openrdf.org/rdf/2007/09/point-of-view#defaultExpression")
	public abstract Expression getPovDefaultExpression();

	/** A default expression. */
	public abstract void setPovDefaultExpression(Expression value);


	/** Set of expressions. */
	@rdf("http://www.openrdf.org/rdf/2007/09/point-of-view#registeredExpression")
	public abstract Set<Expression> getPovRegisteredExpressions();

	/** Set of expressions. */
	public abstract void setPovRegisteredExpressions(Set<Expression> value);

}
