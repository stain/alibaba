package org.openrdf.model.impl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.openrdf.model.Model;
import org.openrdf.model.Namespace;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.util.ModelException;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;

public class RepositoryModel extends AbstractModel {
	private final RepositoryConnection con;

	public RepositoryModel(RepositoryConnection con) {
		this.con = con;
	}

	public int size() {
		try {
			return (int) con.size();
		} catch (RepositoryException e) {
			throw new ModelException(e);
		}
	}

	public boolean isEmpty() {
		try {
			return !con.hasStatement(null, null, null, false);
		} catch (RepositoryException e) {
			throw new ModelException(e);
		}
	}

	public Map<String, String> getNamespaces() {
		Map<String, String> map = new HashMap<String, String>();
		try {
			RepositoryResult<Namespace> spaces = con.getNamespaces();
			try {
				while (spaces.hasNext()) {
					Namespace ns = spaces.next();
					map.put(ns.getPrefix(), ns.getName());
				}
			} finally {
				spaces.close();
			}
			return map;
		} catch (RepositoryException e) {
			throw new ModelException(e);
		}
	}

	public String getNamespace(String prefix) {
		try {
			return con.getNamespace(prefix);
		} catch (RepositoryException e) {
			throw new ModelException(e);
		}
	}

	public String setNamespace(String prefix, String name) {
		try {
			String ret = con.getNamespace(prefix);
			con.setNamespace(prefix, name);
			return ret;
		} catch (RepositoryException e) {
			throw new ModelException(e);
		}
	}

	public String removeNamespace(String prefix) {
		try {
			String ret = con.getNamespace(prefix);
			con.removeNamespace(prefix);
			return ret;
		} catch (RepositoryException e) {
			throw new ModelException(e);
		}
	}

	public boolean contains(Resource subj, URI pred, Value obj,
			Resource... contexts) {
		try {
			return con.hasStatement(subj, pred, obj, false, contexts);
		} catch (RepositoryException e) {
			throw new ModelException(e);
		}
	}

	public boolean add(Resource subj, URI pred, Value obj, Resource... contexts) {
		try {
			if (contains(subj, pred, obj, contexts))
				return false;
			con.add(subj, pred, obj, contexts);
			return true;
		} catch (RepositoryException e) {
			throw new ModelException(e);
		}
	}

	public boolean clear(Resource... contexts) {
		try {
			if (contains(null, null, null, contexts)) {
				con.clear(contexts);
				return true;
			}
		} catch (RepositoryException e) {
			throw new ModelException(e);
		}
		return false;
	}

	public boolean remove(Resource subj, URI pred, Value obj,
			Resource... contexts) {
		try {
			if (contains(subj, pred, obj, contexts)) {
				con.remove(subj, pred, obj, contexts);
				return true;
			}
		} catch (RepositoryException e) {
			throw new ModelException(e);
		}
		return false;
	}

	@Override
	public Iterator<Statement> iterator() {
		try {
			final RepositoryResult<Statement> stmts = con.getStatements(null, null, null, false);
			return new Iterator<Statement>() {
				private Statement last;

				public boolean hasNext() {
					try {
						if (stmts.hasNext())
							return true;
						stmts.close();
						return false;
					} catch (RepositoryException e) {
						throw new ModelException(e);
					}
				}

				public Statement next() {
					try {
						last = stmts.next();
						if (last == null) {
							stmts.close();
						}
						return last;
					} catch (RepositoryException e) {
						throw new ModelException(e);
					}
				}

				public void remove() {
					if (last == null)
						throw new IllegalStateException("next() not yet called");
					RepositoryModel.this.remove(last);
					last = null;
				}
			};
		} catch (RepositoryException e) {
			throw new ModelException(e);
		}
	}

	public Model filter(final Resource subj, final URI pred, final Value obj,
			final Resource... contexts) {
		return new FilteredModel(this, subj, pred, obj, contexts) {

			@Override
			public Iterator<Statement> iterator() {
				try {
					final RepositoryResult<Statement> stmts = con.getStatements(subj, pred, obj, false, contexts);
					return new Iterator<Statement>() {
						private Statement last;

						public boolean hasNext() {
							try {
								if (stmts.hasNext())
									return true;
								stmts.close();
								return false;
							} catch (RepositoryException e) {
								throw new ModelException(e);
							}
						}

						public Statement next() {
							try {
								last = stmts.next();
								if (last == null) {
									stmts.close();
								}
								return last;
							} catch (RepositoryException e) {
								throw new ModelException(e);
							}
						}

						public void remove() {
							if (last == null)
								throw new IllegalStateException("next() not yet called");
							RepositoryModel.this.remove(last);
							last = null;
						}
					};
				} catch (RepositoryException e) {
					throw new ModelException(e);
				}
			}

			@Override
			protected void removeFilteredIteration(Iterator<Statement> iter,
					Resource subj, URI ppred, Value obj, Resource... contexts) {
				try {
					con.remove(subj, pred, obj, contexts);
				} catch (RepositoryException e) {
					throw new ModelException(e);
				}
			}
		};
	}

	@Override
	protected void removeIteration(Iterator<Statement> iter, Resource subj,
			URI pred, Value obj, Resource... contexts) {
		try {
			con.remove(subj, pred, obj, contexts);
		} catch (RepositoryException e) {
			throw new ModelException(e);
		}
	}

}
