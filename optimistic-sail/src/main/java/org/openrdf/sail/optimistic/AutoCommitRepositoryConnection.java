package org.openrdf.sail.optimistic;

import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.repository.sail.SailRepositoryConnection;
import org.openrdf.sail.SailException;
import org.openrdf.sail.optimistic.exceptions.ConcurrencyException;
import org.openrdf.sail.optimistic.exceptions.ConcurrencySailException;

public class AutoCommitRepositoryConnection extends SailRepositoryConnection {
	private boolean active;
	private OptimisticConnection sail;

	protected AutoCommitRepositoryConnection(SailRepository repository,
			OptimisticConnection sail) {
		super(repository, sail);
		this.sail = sail;
	}

	@Override
	protected void addWithoutCommit(Resource subject, URI predicate,
			Value object, Resource... contexts) throws RepositoryException {
		autoBegin();
		super.addWithoutCommit(subject, predicate, object, contexts);
	}

	@Override
	protected void removeWithoutCommit(Resource subject, URI predicate,
			Value object, Resource... contexts) throws RepositoryException {
		autoBegin();
		super.removeWithoutCommit(subject, predicate, object, contexts);
	}

	@Override
	public void clear(Resource... contexts) throws RepositoryException {
		autoBegin();
		super.clear(contexts);
	}

	@Override
	public void clearNamespaces() throws RepositoryException {
		autoBegin();
		super.clearNamespaces();
	}

	@Override
	public void removeNamespace(String prefix) throws RepositoryException {
		autoBegin();
		super.removeNamespace(prefix);
	}

	@Override
	public void setNamespace(String prefix, String name)
			throws RepositoryException {
		autoBegin();
		super.setNamespace(prefix, name);
	}

	@Override
	public void close() throws RepositoryException {
		if (!isAutoCommit()) {
			try {
				sail.rollback();
			} catch (SailException e) {
				throw new RepositoryException(e);
			}
		}
		super.close();
	}

	@Override
	public void commit() throws RepositoryException {
		try {
			sail.commit();
			sail.begin();
		} catch (ConcurrencySailException e) {
			throw new ConcurrencyException(e);
		} catch (SailException e) {
			throw new RepositoryException(e);
		}
	}

	@Override
	public void rollback() throws RepositoryException {
		super.rollback();
		try {
			sail.begin();
		} catch (SailException e) {
			throw new RepositoryException(e);
		}
	}

	@Override
	public void setAutoCommit(boolean autoCommit) throws RepositoryException {
		if (isAutoCommit() != autoCommit) {
			super.setAutoCommit(autoCommit);
			if (autoCommit) {
				try {
					sail.commit();
				} catch (ConcurrencySailException e) {
					throw new ConcurrencyException(e);
				} catch (SailException e) {
					throw new RepositoryException(e);
				}
			} else {
				try {
					sail.begin();
				} catch (SailException e) {
					throw new RepositoryException(e);
				}
			}
		}
	}

	@Override
	protected void autoCommit() throws RepositoryException {
		if (isAutoCommit()) {
			active = false;
			commit();
		}
	}

	private void autoBegin() throws RepositoryException {
		if (isAutoCommit() && !active) {
			active = true;
			try {
				sail.begin();
			} catch (SailException e) {
				throw new RepositoryException(e);
			}
		}
	}

}
