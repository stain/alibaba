package org.openrdf.alibaba.pov.support;

import javax.xml.namespace.QName;

import org.openrdf.alibaba.core.base.RepositoryBase;
import org.openrdf.alibaba.pov.Intent;
import org.openrdf.alibaba.pov.Perspective;
import org.openrdf.alibaba.pov.PerspectiveRepository;
import org.openrdf.alibaba.pov.PerspectiveRepositoryBehaviour;
import org.openrdf.alibaba.vocabulary.POV;
import org.openrdf.elmo.ElmoManager;
import org.openrdf.elmo.ElmoQuery;
import org.openrdf.elmo.Entity;
import org.openrdf.elmo.annotations.rdf;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDF;

@rdf(POV.NS + "PerspectiveRepository")
public class PerspectiveRepositorySupport extends RepositoryBase<Perspective> implements
		PerspectiveRepositoryBehaviour {
	private static final String SELECT_PERSPECTIVE;
	static {
		StringBuilder sb = new StringBuilder();
		sb.append("PREFIX rdf:<").append(RDF.NAMESPACE).append(">\n");
		sb.append("PREFIX owl:<").append(OWL.NAMESPACE).append(">\n");
		sb.append("PREFIX pov:<").append(POV.NS).append(">\n");
		sb.append("SELECT ?perspective WHERE {");
		sb.append(" ?target rdf:type ?domain .");
		sb.append(" ?perspective pov:represents ?domain .");
		sb.append(" ?repository pov:registeredPerspective ?perspective .");
		sb.append(" ?perspective pov:purpose ?intention .");
		sb.append("}");
		SELECT_PERSPECTIVE = sb.toString();
	}

	private PerspectiveRepository repository;

	private ElmoManager manager;

	public PerspectiveRepositorySupport(PerspectiveRepository repository) {
		super(repository.getPovRegisteredPerspectives());
		this.repository = repository;
		manager = repository.getElmoManager();
	}

	public Perspective findPerspective(QName qname) {
		return (Perspective) manager.find(qname);
	}

	public Perspective findPerspective(Intent intention, Entity target) {
		ElmoQuery query = manager.createQuery(SELECT_PERSPECTIVE);
		query.setParameter("repository", repository);
		query.setParameter("target", target);
		query.setParameter("intention", intention);
		try {
			for (Object bean : query.getResultList()) {
				return (Perspective) bean;
			}
		} finally {
			query.close();
		}
		return null;
	}
}
