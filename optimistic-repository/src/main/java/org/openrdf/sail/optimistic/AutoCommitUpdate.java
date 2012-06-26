/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 2007-2008.
 * Copyright 3 Round Stones Inc. (http://3roundstones.com/) (c) 2012.
 *
 * Licensed under the Aduna BSD-style license.
 */
package org.openrdf.sail.optimistic;

import java.net.URL;
import java.util.List;
import java.util.Map;

import org.openrdf.model.Resource;
import org.openrdf.model.Value;
import org.openrdf.query.Dataset;
import org.openrdf.query.UpdateExecutionException;
import org.openrdf.query.algebra.Load;
import org.openrdf.query.algebra.UpdateExpr;
import org.openrdf.query.impl.FallbackDataset;
import org.openrdf.query.parser.ParsedUpdate;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepositoryConnection;
import org.openrdf.repository.sail.SailUpdate;
import org.openrdf.sail.SailConnection;
import org.openrdf.sail.SailException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class AutoCommitUpdate extends SailUpdate {

	private final Logger logger = LoggerFactory.getLogger(AutoCommitUpdate.class);

	private final ParsedUpdate parsedUpdate;

	private final AutoCommitRepositoryConnection con;

	protected AutoCommitUpdate(ParsedUpdate parsedUpdate, AutoCommitRepositoryConnection con) {
		super(parsedUpdate, con);
		this.parsedUpdate = parsedUpdate;
		this.con = con;
	}

	protected AutoCommitRepositoryConnection getConnection() {
		return con;
	}

	public void execute()
		throws UpdateExecutionException
	{

		List<UpdateExpr> updateExprs = parsedUpdate.getUpdateExprs();
		Map<UpdateExpr, Dataset> datasetMapping = parsedUpdate.getDatasetMapping();

		for (UpdateExpr updateExpr : updateExprs) {
			// LOAD is handled at the Repository API level because it requires
			// access to the Rio parser.
			if (updateExpr instanceof Load) {

				Load load = (Load)updateExpr;

				Value source = load.getSource().getValue();
				Value graph = load.getGraph() != null ? load.getGraph().getValue() : null;

				SailRepositoryConnection conn = getConnection();
				try {
					URL sourceURL = new URL(source.stringValue());

					if (graph == null) {
						conn.add(sourceURL, source.stringValue(), null);
					}
					else {
						conn.add(sourceURL, source.stringValue(), null, (Resource)graph);
					}
				}
				catch (Exception e) {
					logger.warn("exception during update execution: ", e);
					if (!load.isSilent()) {
						throw new UpdateExecutionException(e);
					}
				}
			}
			else {
				// pass update operation to the SAIL.
				SailConnection conn = getConnection().getSailConnection();

				// explicitly set dataset on the SailUpdate takes precedence over declaration in the update itself.
				Dataset activeDataset = FallbackDataset.fallback(dataset, datasetMapping.get(updateExpr));

				try {
					getConnection().autoBegin();
					try {
						conn.executeUpdate(updateExpr, activeDataset, getBindings(), true);
						getConnection().autoCommit();
					} catch (RepositoryException e) {
						getConnection().autoRollback();
						throw e;
					} catch (RuntimeException e) {
						getConnection().autoRollback();
						throw e;
					} catch (Error e) {
						getConnection().autoRollback();
						throw e;
					}
				}
				catch (SailException e) {
					logger.warn("exception during update execution: ", e);
					if (!updateExpr.isSilent()) {
						throw new UpdateExecutionException(e);
					}
				}
				catch (RepositoryException e) {
					logger.warn("exception during update execution: ", e);
					if (!updateExpr.isSilent()) {
						throw new UpdateExecutionException(e);
					}
				}
			}
		}
	}
}