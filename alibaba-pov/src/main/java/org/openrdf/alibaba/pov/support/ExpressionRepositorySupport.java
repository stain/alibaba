package org.openrdf.alibaba.pov.support;

import java.util.HashSet;
import java.util.Set;

import org.openrdf.alibaba.pov.Expression;
import org.openrdf.alibaba.pov.ExpressionRepository;
import org.openrdf.alibaba.pov.ExpressionRepositoryBehaviour;
import org.openrdf.alibaba.pov.base.RepositoryBase;
import org.openrdf.alibaba.vocabulary.POV;
import org.openrdf.elmo.annotations.rdf;

@rdf(POV.NS + "ExpressionRepository")
public class ExpressionRepositorySupport extends RepositoryBase<Expression>
		implements ExpressionRepositoryBehaviour {
	private ExpressionRepository repository;

	public ExpressionRepositorySupport(ExpressionRepository repository) {
		super(repository.getPovRegisteredExpressions());
		this.repository = repository;
	}

	public Set<Expression> findByNames(Set<String> names) {
		Set<Expression> result = new HashSet<Expression>(names.size());
		for (Expression expr : this) {
			if (names.contains(expr.getPovName())) {
				result.add(expr);
			}
		}
		if (result.isEmpty()) {
			Expression de = repository.getPovDefaultExpression();
			if (de != null) {
				result.add(de);
			}
		}
		return result;
	}

}
