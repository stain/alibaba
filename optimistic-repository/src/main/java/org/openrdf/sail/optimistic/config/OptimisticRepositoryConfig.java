package org.openrdf.sail.optimistic.config;

import org.openrdf.model.Graph;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.util.GraphUtil;
import org.openrdf.model.util.GraphUtilException;
import org.openrdf.repository.config.RepositoryConfigException;
import org.openrdf.repository.sail.config.SailRepositoryConfig;
import org.openrdf.sail.config.SailImplConfig;

public class OptimisticRepositoryConfig extends SailRepositoryConfig {
	public static final URI SNAPSHOT = new URIImpl(
			"http://www.openrdf.org/config/repository/optimistic#snapshot");

	public static final URI SERIALIZABLE = new URIImpl(
			"http://www.openrdf.org/config/repository/optimistic#serializable");

	private boolean snapshot;
	private boolean serializable;

	public OptimisticRepositoryConfig() {
		setType(OptimisticFactory.REPOSITORY_TYPE);
	}

	public OptimisticRepositoryConfig(SailImplConfig sailImplConfig) {
		this();
		setSailImplConfig(sailImplConfig);
	}

	public boolean isSnapshot() {
		return snapshot;
	}

	public void setSnapshot(boolean snapshot) {
		this.snapshot = snapshot;
	}

	public boolean isSerializable() {
		return serializable;
	}

	public void setSerializable(boolean serializable) {
		this.serializable = serializable;
	}

	@Override
	public Resource export(Graph graph) {
		Resource subj = super.export(graph);
		ValueFactory vf = graph.getValueFactory();
		if (snapshot) {
			graph.add(subj, SNAPSHOT, vf.createLiteral(true));
		}
		if (serializable) {
			graph.add(subj, SERIALIZABLE, vf.createLiteral(true));
		}
		return subj;
	}

	@Override
	public void parse(Graph graph, Resource subj)
			throws RepositoryConfigException {
		super.parse(graph, subj);
		try {
			Literal lit = GraphUtil.getOptionalObjectLiteral(graph, subj,
					SNAPSHOT);
			if (lit != null) {
				setSnapshot(lit.booleanValue());
			}
			lit = GraphUtil.getOptionalObjectLiteral(graph, subj, SERIALIZABLE);
			if (lit != null) {
				setSerializable(lit.booleanValue());
			}
		} catch (GraphUtilException e) {
			throw new RepositoryConfigException(e.getMessage(), e);
		}
	}

}
