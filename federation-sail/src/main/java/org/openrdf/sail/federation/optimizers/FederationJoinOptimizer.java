/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 2008-2009.
 *
 * Licensed under the Aduna BSD-style license.
 */
package org.openrdf.sail.federation.optimizers;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;
import org.openrdf.query.Dataset;
import org.openrdf.query.algebra.EmptySet;
import org.openrdf.query.algebra.Join;
import org.openrdf.query.algebra.LeftJoin;
import org.openrdf.query.algebra.QueryModelNode;
import org.openrdf.query.algebra.StatementPattern;
import org.openrdf.query.algebra.TupleExpr;
import org.openrdf.query.algebra.UnaryTupleOperator;
import org.openrdf.query.algebra.Union;
import org.openrdf.query.algebra.Var;
import org.openrdf.query.algebra.evaluation.QueryOptimizer;
import org.openrdf.query.algebra.helpers.QueryModelVisitorBase;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.sail.federation.PrefixHashSet;
import org.openrdf.sail.federation.algebra.OwnedTupleExpr;

/**
 * Search for Join, LeftJoin, and Union arguments that can be evaluated in a
 * single member.
 * 
 * @author James Leigh
 */
public class FederationJoinOptimizer extends QueryModelVisitorBase<RepositoryException> implements QueryOptimizer {

	private final Collection<? extends RepositoryConnection> members;

	private final PrefixHashSet localSpace;

	private final boolean distinct;

	public FederationJoinOptimizer(Collection<? extends RepositoryConnection> members, boolean distinct,
			PrefixHashSet localSpace)
	{
		this.members = members;
		this.localSpace = localSpace;
		this.distinct = distinct;
	}

	public void optimize(TupleExpr query, Dataset dataset,
			BindingSet bindings)
	{
		try {
			query.visit(this);
		} catch (RepositoryException e) {
			throw new UndeclaredThrowableException(e);
		}
	}

	@Override
	public void meet(Join node)
		throws RepositoryException
	{
		super.meet(node);
		List<Owned<TupleExpr>> ows = new ArrayList<Owned<TupleExpr>>();
		List<LocalJoin> vars = new ArrayList<LocalJoin>();
		for (TupleExpr arg : new TupleExpr[] { node.getLeftArg(),
				node.getRightArg() }) {
			RepositoryConnection member = getSingleOwner(arg);
			int idx = ows.size() - 1;
			if (ows.size() > 0 && ows.get(idx).getOwner() == member) {
				TupleExpr join = ows.get(idx).getOperation();
				join = new Join(join, arg.clone());
				ows.get(idx).setOperation(join);
			}
			else {
				ows.add(new Owned<TupleExpr>(member, arg.clone()));
			}
		}
		for (TupleExpr arg : new TupleExpr[] { node.getLeftArg(),
				node.getRightArg() }) {
			Var subj = getLocalSubject(arg);
			LocalJoin local = findLocalJoin(subj, vars);
			if (local != null) {
				local.setJoin(new Join(local.getJoin(), arg.clone()));
			}
			else {
				vars.add(new LocalJoin(subj, arg.clone()));
			}
		}
		addOwners(node, ows, vars);
	}

	@Override
	public void meet(LeftJoin node)
		throws RepositoryException
	{
		super.meet(node);
		Var leftSubject = getLocalSubject(node.getLeftArg());
		Var rightSubject = getLocalSubject(node.getRightArg());
		// if local then left and right can be combined
		boolean local = leftSubject != null && leftSubject.equals(rightSubject);
		RepositoryConnection leftOwner = getSingleOwner(node.getLeftArg());
		RepositoryConnection rightOwner = getSingleOwner(node.getRightArg());
		addOwners(node, leftOwner, rightOwner, local);
	}

	@Override
	public void meet(Union node)
		throws RepositoryException
	{
		super.meet(node);
		List<Owned<TupleExpr>> ows = new ArrayList<Owned<TupleExpr>>();
		for (TupleExpr arg : new TupleExpr[] { node.getLeftArg(),
				node.getRightArg() }) {
			RepositoryConnection member = getSingleOwner(arg);
			int idx = ows.size() - 1;
			if (ows.size() > 0 && ows.get(idx).getOwner() == member) {
				TupleExpr union = ows.get(idx).getOperation();
				union = new Union(union, arg.clone());
				ows.get(idx).setOperation(union);
			}
			else {
				ows.add(new Owned<TupleExpr>(member, arg.clone()));
			}
		}
		addOwners(node, ows);
	}

	@Override
	protected void meetUnaryTupleOperator(UnaryTupleOperator node)
		throws RepositoryException
	{
		super.meetUnaryTupleOperator(node);
		RepositoryConnection owner = getSingleOwner(node.getArg());
		if (owner != null) {
			node.replaceWith(new OwnedTupleExpr(owner, node.clone()));
		}
	}

	private static class Owned<O> {

		private RepositoryConnection owner;

		private O operation;

		public Owned(RepositoryConnection owner, O operation) {
			this.owner = owner;
			this.operation = operation;
		}

		public RepositoryConnection getOwner() {
			return owner;
		}

		public O getOperation() {
			return operation;
		}

		public void setOperation(O operation) {
			this.operation = operation;
		}

		@Override
		public String toString() {
			return owner + "=" + operation;
		}
	}

	private static class LocalJoin {

		private Var var;

		private TupleExpr join;

		public LocalJoin(Var key, TupleExpr value) {
			this.var = key;
			this.join = value;
		}

		public Var getVar() {
			return var;
		}

		public TupleExpr getJoin() {
			return join;
		}

		public void setJoin(TupleExpr join) {
			this.join = join;
		}

		@Override
		public String toString() {
			return var + "=" + join;
		}
	}

	private class OwnerScanner extends QueryModelVisitorBase<RepositoryException> {

		private boolean shared;

		private RepositoryConnection owner;

		/**
		 * If the argument can be sent to a single member.
		 */
		public RepositoryConnection getSingleOwner(TupleExpr arg)
			throws RepositoryException
		{
			boolean pre_shared = shared;
			RepositoryConnection pre_owner = owner;
			try {
				shared = false;
				owner = null;
				arg.visit(this);
				return owner;
			}
			finally {
				// restore
				shared = pre_shared;
				owner = pre_owner;
			}
		}

		@Override
		public void meet(StatementPattern node)
			throws RepositoryException
		{
			super.meet(node);
			Resource subj = (Resource)node.getSubjectVar().getValue();
			URI pred = (URI)node.getPredicateVar().getValue();
			Value obj = node.getObjectVar().getValue();
			Resource[] ctx = getContexts(node.getContextVar());
			RepositoryConnection member = getSingleOwner(subj, pred, obj, ctx);
			if (member == null) {
				multipleOwners();
			}
			else {
				usedBy(member);
			}
		}

		@Override
		public void meetOther(QueryModelNode node)
			throws RepositoryException
		{
			if (node instanceof OwnedTupleExpr) {
				meetOwnedTupleExpr((OwnedTupleExpr)node);
			}
			else {
				super.meetOther(node);
			}
		}

		private void meetOwnedTupleExpr(OwnedTupleExpr node)
			throws RepositoryException
		{
			usedBy(node.getOwner());
		}

		private Resource[] getContexts(Var var) {
			if (var == null || !var.hasValue()) {
				return new Resource[0];
			}
			return new Resource[] { (Resource)var.getValue() };
		}

		private RepositoryConnection getSingleOwner(Resource subj, URI pred, Value obj, Resource[] ctx)
			throws RepositoryException
		{
			RepositoryConnection o = null;
			for (RepositoryConnection member : members) {
				if (member.hasStatement(subj, pred, obj, true, ctx)) {
					if (o == null) {
						o = member;
					}
					else if (o != member) {
						return null;
					}
				}
			}
			return o;
		}

		private void usedBy(RepositoryConnection member) {
			if (!shared && owner == null) {
				// first owner sensitive element
				owner = member;
			}
			else if (owner != member) {
				multipleOwners();
			}
		}

		private void multipleOwners() {
			owner = null;
			shared = true;
		}

	}

	private class LocalScanner extends QueryModelVisitorBase<RepositoryException> {

		private boolean local;

		private Var relative;

		/**
		 * If the argument can be sent as a group to the members.
		 */
		public Var getLocalSubject(TupleExpr arg)
			throws RepositoryException
		{
			boolean local_stack = local;
			Var relative_stack = relative;
			try {
				local = true;
				relative = null;
				arg.visit(this);
				return relative;
			}
			finally {
				// restore
				local = local_stack;
				relative = relative_stack;
			}
		}

		@Override
		public void meet(StatementPattern node)
			throws RepositoryException
		{
			super.meet(node);
			URI pred = (URI)node.getPredicateVar().getValue();
			if (pred != null && localSpace != null && localSpace.match(pred.stringValue())) {
				local(node.getSubjectVar());
			}
			else {
				notLocal();
			}
		}

		private void local(Var subj) {
			if (local && relative == null) {
				relative = subj;
			}
			else if (!subj.equals(relative)) {
				notLocal();
			}
		}

		private void notLocal() {
			local = false;
			relative = null;
		}

	}

	/**
	 * If two basic graph patterns have the same subject and can be run on the
	 * same member, we can change the order.
	 */
	private LocalJoin findLocalJoin(Var subj, List<LocalJoin> vars) {
		if (vars.size() > 0 && vars.get(vars.size() - 1).getVar() == subj) {
			return vars.get(vars.size() - 1);
		}
		for (LocalJoin local : vars) {
			if (subj != null && subj.equals(local.getVar())) {
				return local;
			}
		}
		return null;
	}

	/**
	 * If the argument can be sent to a single member.
	 */
	private RepositoryConnection getSingleOwner(TupleExpr arg)
		throws RepositoryException
	{
		return new OwnerScanner().getSingleOwner(arg);
	}

	/**
	 * If the argument can be sent as a group to the members.
	 */
	private Var getLocalSubject(TupleExpr arg)
		throws RepositoryException
	{
		return new LocalScanner().getLocalSubject(arg);
	}

	private void addOwners(Join node, List<Owned<TupleExpr>> ows, List<LocalJoin> vars)
		throws RepositoryException
	{
		boolean local = false;
		if (vars.size() > 1 || vars.size() == 1 && vars.get(0).getVar() != null) {
			for (LocalJoin e : vars) {
				if (e.getVar() != null && e.getJoin() instanceof Join) {
					local = true;
					break;
				}
			}
		}
		if (ows.size() == 1) {
			RepositoryConnection o = ows.get(0).getOwner();
			if (o == null) {
				// every element has multiple owners
				if (local) {
					TupleExpr replacement = null;
					for (LocalJoin e : vars) {
						if (distinct || e.getVar() != null) {
							TupleExpr union = null;
							for (RepositoryConnection member : members) {
								TupleExpr arg = new OwnedTupleExpr(member, e.getJoin().clone());
								union = union == null ? arg : new Union(union, arg);
							}
							replacement = replacement == null ? union : new Join(replacement, union);
						}
						else {
							TupleExpr join = e.getJoin();
							replacement = replacement == null ? join : new Join(replacement, join);
						}
					}
					node.replaceWith(replacement);
				}
			}
			else {
				// every element is used by the same owner
				node.replaceWith(new OwnedTupleExpr(o, node.clone()));
			}
		}
		else if (local) {
			TupleExpr replacement = null;
			for (LocalJoin v : vars) {
				Var var = v.getVar();
				TupleExpr j = v.getJoin();
				if (var == null) {
					// each of these arguments could be anywhere
					replacement = replacement == null ? j : new Join(replacement, j);
				}
				else {
					boolean multipleOwners = false;
					RepositoryConnection owner = null;
					for (TupleExpr expr : getJoinArgs(j)) {
						RepositoryConnection o = getSingleOwner(expr);
						if (owner == null) {
							owner = o;
						}
						else if (o != null && owner != o) {
							multipleOwners = true;
							owner = null;
							break;
						}
					}
					if (multipleOwners) {
						// no subject exists on multiple members,
						// but in the same member
						node.replaceWith(new EmptySet());
						return;
					}
					else if (owner == null) {
						// these arguments might exist in any member,
						// but they will only join with the same member.
						TupleExpr union = null;
						for (RepositoryConnection member : members) {
							OwnedTupleExpr arg = new OwnedTupleExpr(member, j.clone());
							union = union == null ? arg : new Union(union, arg);
						}
						replacement = replacement == null ? union : new Join(replacement, union);
					}
					else {
						// there is only one member that can join all these arguments
						OwnedTupleExpr arg = new OwnedTupleExpr(owner, j);
						replacement = replacement == null ? arg : new Join(replacement, arg);
					}
				}
			}
			node.replaceWith(replacement);
		}
		else {
			TupleExpr replacement = null;
			for (Owned<TupleExpr> e : ows) {
				RepositoryConnection o = e.getOwner();
				TupleExpr join = e.getOperation();
				if (o == null) {
					// multiple owners
					for (TupleExpr arg : getJoinArgs(join)) {
						replacement = replacement == null ? arg : new Join(replacement, arg);
					}
				}
				else {
					OwnedTupleExpr arg = new OwnedTupleExpr(o, join);
					replacement = replacement == null ? arg : new Join(replacement, arg);
				}
			}
			node.replaceWith(replacement);
		}
	}

	private List<TupleExpr> getJoinArgs(TupleExpr j) {
		List<TupleExpr> list = new ArrayList<TupleExpr>();
		return getJoinArgs(j, list);
	}

	private List<TupleExpr> getJoinArgs(TupleExpr j, List<TupleExpr> list) {
		if (j instanceof Join) {
			getJoinArgs(((Join) j).getLeftArg(), list);
			getJoinArgs(((Join) j).getRightArg(), list);
			return list;
		} else {
			list.add(j);
			return list;
		}
	}

	private void addOwners(LeftJoin node, RepositoryConnection leftOwner, RepositoryConnection rightOwner,
			boolean local)
	{
		if (leftOwner == null && rightOwner == null) {
			if (local) {
				TupleExpr union = null;
				for (RepositoryConnection member : members) {
					OwnedTupleExpr arg = new OwnedTupleExpr(member, node.clone());
					union = union == null ? arg : new Union(union, arg);
				}
				node.replaceWith(union);
			}
		}
		else if (leftOwner == rightOwner) {
			node.replaceWith(new OwnedTupleExpr(leftOwner, node.clone()));
		}
		else {
			if (local) {
				if (rightOwner == null) {
					node.replaceWith(new OwnedTupleExpr(leftOwner, node.clone()));
				}
				else if (leftOwner == null) {
					TupleExpr union = null;
					for (RepositoryConnection member : members) {
						if (rightOwner == member) {
							OwnedTupleExpr arg = new OwnedTupleExpr(member, node.clone());
							union = union == null ? arg : new Union(union, arg);
						}
						else {
							OwnedTupleExpr arg = new OwnedTupleExpr(member, node.getLeftArg().clone());
							union = union == null ? arg : new Union(union, arg);
						}
					}
					node.replaceWith(union);
				}
				else {
					node.replaceWith(new OwnedTupleExpr(leftOwner, node.getLeftArg().clone()));
				}
			}
			else {
				if (leftOwner != null) {
					node.getLeftArg().replaceWith(new OwnedTupleExpr(leftOwner, node.getLeftArg().clone()));
				}
				if (rightOwner != null) {
					node.getRightArg().replaceWith(new OwnedTupleExpr(rightOwner, node.getRightArg().clone()));
				}
			}
		}
	}

	private void addOwners(Union node, List<Owned<TupleExpr>> ows) {
		if (ows.size() == 1) {
			RepositoryConnection o = ows.get(0).getOwner();
			if (o != null) {
				// every element is used by the same owner
				node.replaceWith(new OwnedTupleExpr(o, node.clone()));
			}
		}
		else {
			TupleExpr replacement = null;
			for (Owned<TupleExpr> e : ows) {
				RepositoryConnection o = e.getOwner();
				TupleExpr union = e.getOperation();
				if (o == null) {
					// multiple owners
					for (TupleExpr arg : getUnionArgs(union)) {
						replacement = replacement == null ? arg.clone() : new Union(replacement, arg.clone());
					}
				}
				else {
					OwnedTupleExpr arg = new OwnedTupleExpr(o, union);
					replacement = replacement == null ? arg : new Union(replacement, arg);
				}
			}
			node.replaceWith(replacement);
		}
	}

	private List<TupleExpr> getUnionArgs(TupleExpr union) {
		return getUnionArgs(union, new ArrayList<TupleExpr>());
	}

	private List<TupleExpr> getUnionArgs(TupleExpr union,
			ArrayList<TupleExpr> list) {
		if (union instanceof Union) {
			getUnionArgs(((Union) union).getLeftArg(), list);
			getUnionArgs(((Union) union).getLeftArg(), list);
			return list;
		} else {
			list.add(union);
			return list;
		}
	}

}
