package org.openrdf.alibaba.pov.support;

import java.util.Collections;
import java.util.Set;

import org.openrdf.alibaba.pov.Display;
import org.openrdf.alibaba.pov.Expression;
import org.openrdf.alibaba.pov.OrderByExpression;
import org.openrdf.alibaba.pov.OrderByRepository;
import org.openrdf.alibaba.pov.OrderByRepositoryBehaviour;
import org.openrdf.alibaba.vocabulary.POV;
import org.openrdf.elmo.ElmoManager;
import org.openrdf.elmo.ElmoQuery;
import org.openrdf.elmo.annotations.rdf;

/**
 * Support class for accessing ORDER BY {@link Expression}s.
 * 
 * @author James Leigh
 *
 */
@rdf(POV.NS + "OrderByRepository")
public class OrderByRepositorySupport extends
		ExpressionRepositorySupport implements
		OrderByRepositoryBehaviour {
	private static final String PREFIX = "PREFIX pov: <" + POV.NS + ">\n";

	private static final String SELECT_ORDER_BY_ASC = PREFIX
			+ "SELECT ?expression "
			+ "WHERE { ?repository pov:registeredExpression ?expression . "
			+ " ?expression pov:ascending ?display }";

	private static final String SELECT_ORDER_BY_DESC = PREFIX
			+ "SELECT ?expression "
			+ "WHERE { ?repository pov:registeredExpression ?expression ."
			+ " ?expression pov:descending ?display }";

	private OrderByRepository repository;

	private ElmoManager manager;

	public OrderByRepositorySupport(
			OrderByRepository repository) {
		super(repository);
		this.repository = repository;
		manager = repository.getElmoManager();
	}

	public Expression findByName(String name) {
		Set<Expression> set = findByNames(Collections.singleton(name));
		if (set.isEmpty())
			return null;
		return set.iterator().next();
	}

	public OrderByExpression findAscending(Display display) {
		ElmoQuery query = manager.createQuery(SELECT_ORDER_BY_ASC);
		query.setParameter("display", display);
		query.setParameter("repository", repository);
		try {
			for (Object o : query.getResultList()) {
				return (OrderByExpression) o;
			}
		} finally {
			query.close();
		}
		return null;
	}

	public OrderByExpression findDescending(Display display) {
		ElmoQuery query = manager.createQuery(SELECT_ORDER_BY_DESC);
		query.setParameter("display", display);
		query.setParameter("repository", repository);
		try {
			for (Object o : query.getResultList()) {
				return (OrderByExpression) o;
			}
		} finally {
			query.close();
		}
		return null;
	}

}
