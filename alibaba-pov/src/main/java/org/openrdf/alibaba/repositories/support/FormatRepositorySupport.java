package org.openrdf.alibaba.repositories.support;

import javax.xml.namespace.QName;

import org.openrdf.alibaba.concepts.Format;
import org.openrdf.alibaba.concepts.FormatRepository;
import org.openrdf.alibaba.repositories.FormatRepositoryBehaviour;
import org.openrdf.alibaba.repositories.base.RepositoryBase;
import org.openrdf.alibaba.vocabulary.ALI;
import org.openrdf.alibaba.vocabulary.POV;
import org.openrdf.elmo.ElmoManager;
import org.openrdf.elmo.annotations.rdf;

@rdf(POV.NS + "FormatRepository")
public class FormatRepositorySupport extends RepositoryBase<Format> implements FormatRepositoryBehaviour {
	private ElmoManager manager;

	public FormatRepositorySupport(FormatRepository repository) {
		super(repository.getPovRegisteredFormats());
		manager = repository.getElmoManager();
	}

	public Format findFormat(QName qname) {
		return (Format) manager.find(qname);
	}

	public Format findFormatFor(Object value) {
		// TODO Auto-generated method stub
		return (Format) manager.find(ALI.NONE);
	}
}
