package org.openrdf.sail.optimistic;

import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.repository.sail.SailRepositoryConnection;
import org.openrdf.sail.Sail;
import org.openrdf.sail.SailException;

public class OptimisticRepository extends SailRepository {
	private OptimisticSail sail;

	public OptimisticRepository(Sail sail) {
		super(new OptimisticSail(sail));
		this.sail = (OptimisticSail) getSail();
	}

	@Override
	public SailRepositoryConnection getConnection() throws RepositoryException {
		try {
			OptimisticConnection con = sail.getConnection();
			return new AutoCommitRepositoryConnection(this, con);
		} catch (SailException e) {
			throw new RepositoryException(e);
		}
	}

}
