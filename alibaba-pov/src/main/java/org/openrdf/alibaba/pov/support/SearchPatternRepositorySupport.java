package org.openrdf.alibaba.pov.support;

import javax.xml.namespace.QName;

import org.openrdf.alibaba.core.base.RepositoryBase;
import org.openrdf.alibaba.pov.Intent;
import org.openrdf.alibaba.pov.SearchPattern;
import org.openrdf.alibaba.pov.SearchPatternRepository;
import org.openrdf.alibaba.pov.SearchPatternRepositoryBehaviour;
import org.openrdf.alibaba.vocabulary.POV;
import org.openrdf.concepts.rdfs.Class;
import org.openrdf.elmo.ElmoManager;
import org.openrdf.elmo.ElmoQuery;
import org.openrdf.elmo.annotations.rdf;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDF;

@rdf(POV.NS + "SearchPatternRepository")
public class SearchPatternRepositorySupport extends RepositoryBase<SearchPattern> implements
		SearchPatternRepositoryBehaviour {
	private static final String SELECT_SEARCH_PATTERN;
	static {
		StringBuilder sb = new StringBuilder();
		sb.append("PREFIX rdf:<").append(RDF.NAMESPACE).append(">\n");
		sb.append("PREFIX owl:<").append(OWL.NAMESPACE).append(">\n");
		sb.append("PREFIX pov:<").append(POV.NS).append(">\n");
		sb.append("SELECT ?pattern WHERE {");
		sb.append(" ?pattern pov:represents ?type .");
		sb.append(" ?repository pov:registeredSearchPattern ?pattern .");
		sb.append(" ?pattern pov:purpose ?intent .");
		sb.append("}");
		SELECT_SEARCH_PATTERN = sb.toString();
	}

	private SearchPatternRepository repository;

	private ElmoManager manager;

	public SearchPatternRepositorySupport(SearchPatternRepository repository) {
		super(repository.getPovRegisteredSearchPatterns());
		this.repository = repository;
		manager = repository.getElmoManager();
	}

	public SearchPattern findSearchPattern(QName qname) {
		return (SearchPattern) manager.find(qname);
	}

	public SearchPattern findSearchPattern(Intent intent, Class type) {
		ElmoQuery query = manager.createQuery(SELECT_SEARCH_PATTERN);
		query.setParameter("repository", repository);
		query.setParameter("type", type);
		query.setParameter("intent", intent);
		try {
			for (Object bean : query.getResultList()) {
				return (SearchPattern) bean;
			}
		} finally {
			query.close();
		}
		return null;
	}

}
