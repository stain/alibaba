/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 2008-2009.
 *
 * Licensed under the Aduna BSD-style license.
 */
package org.openrdf.sail.federation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.sail.SailConnection;
import org.openrdf.sail.SailException;
import org.openrdf.sail.helpers.SailBase;

/**
 * Union multiple (possibly remote) Repositories into a single RDF store.
 * 
 * @author James Leigh
 * @author Arjohn Kampman
 */
public class Federation extends SailBase implements Executor {

	private final List<Repository> members = new ArrayList<Repository>();

	private final ExecutorService executor = Executors.newCachedThreadPool();

	private PrefixHashSet localPropertySpace;

	private boolean distinct;

	private boolean readOnly;

	public ValueFactory getValueFactory() {
		return ValueFactoryImpl.getInstance();
	}

	public boolean isWritable() throws SailException {
		return !isReadOnly();
	}

	public void addMember(Repository member) {
		members.add(member);
	}

	/**
	 * @return PrefixHashSet or null
	 */
	public PrefixHashSet getLocalPropertySpace() {
		return localPropertySpace;
	}

	public void setLocalPropertySpace(Collection<String> localPropertySpace) {
		if (localPropertySpace.isEmpty()) {
			this.localPropertySpace = null;
		}
		else {
			this.localPropertySpace = new PrefixHashSet(localPropertySpace);
		}
	}

	public boolean isDistinct() {
		return distinct;
	}

	public void setDistinct(boolean distinct) {
		this.distinct = distinct;
	}

	public boolean isReadOnly() {
		return readOnly;
	}

	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}

	public void initialize()
		throws SailException
	{
		super.initialize();
		for (Repository member : members) {
			try {
				member.initialize();
			} catch (RepositoryException e) {
				throw new SailException(e);
			}
		}
	}

	@Override
	protected void shutDownInternal()
		throws SailException
	{
		for (Repository member : members) {
			try {
				member.shutDown();
			} catch (RepositoryException e) {
				throw new SailException(e);
			}
		}
		executor.shutdown();
	}

	public void execute(Runnable command) {
		executor.execute(command);
	}

	@Override
	protected SailConnection getConnectionInternal()
		throws SailException
	{
		List<RepositoryConnection> connections = new ArrayList<RepositoryConnection>(members.size());
		try {
			for (Repository member : members) {
				connections.add(member.getConnection());
			}

			if (readOnly) {
				return new ReadOnlyConnection(this, connections);
			}
			else {
				return new WritableConnection(this, connections);

			}
		}
		catch (RepositoryException e) {
			closeAll(connections);
			throw new SailException(e);
		}
		catch (RuntimeException e) {
			closeAll(connections);
			throw e;
		}
	}

	private void closeAll(Iterable<RepositoryConnection> connections) {
		for (RepositoryConnection con : connections) {
			try {
				con.close();
			}
			catch (RepositoryException e) {
				logger.error(e.getMessage(), e);
			}
		}
	}
}
