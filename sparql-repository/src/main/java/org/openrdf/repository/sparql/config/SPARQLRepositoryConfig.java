package org.openrdf.repository.sparql.config;

import org.openrdf.model.Graph;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.util.GraphUtil;
import org.openrdf.model.util.GraphUtilException;
import org.openrdf.repository.config.RepositoryConfigException;
import org.openrdf.repository.config.RepositoryImplConfigBase;

/**
 * @author James Leigh
 */
public class SPARQLRepositoryConfig extends RepositoryImplConfigBase {

	public static final URI ENDPOINT_URL = new URIImpl(
			"http://www.openrdf.org/config/repository/sparql#endpointurl");

	private String url;

	public SPARQLRepositoryConfig() {
		super(SPARQLRepositoryFactory.REPOSITORY_TYPE);
	}

	public SPARQLRepositoryConfig(String url) {
		setURL(url);
	}

	public String getURL() {
		return url;
	}

	public void setURL(String url) {
		this.url = url;
	}

	@Override
	public void validate() throws RepositoryConfigException {
		super.validate();
		if (url == null) {
			throw new RepositoryConfigException(
					"No URL specified for SPARQL repository");
		}
	}

	@Override
	public Resource export(Graph graph) {
		Resource implNode = super.export(graph);

		if (url != null) {
			graph.add(implNode, ENDPOINT_URL, graph.getValueFactory()
					.createURI(url));
		}

		return implNode;
	}

	@Override
	public void parse(Graph graph, Resource implNode)
			throws RepositoryConfigException {
		super.parse(graph, implNode);

		try {
			URI uri = GraphUtil.getOptionalObjectURI(graph, implNode,
					ENDPOINT_URL);
			if (uri != null) {
				setURL(uri.toString());
			}
		} catch (GraphUtilException e) {
			throw new RepositoryConfigException(e.getMessage(), e);
		}
	}
}
