package org.openrdf.alibaba.formats.support;

import javax.xml.namespace.QName;

import org.openrdf.alibaba.core.base.RepositoryBase;
import org.openrdf.alibaba.formats.Format;
import org.openrdf.alibaba.formats.FormatRepository;
import org.openrdf.alibaba.formats.FormatRepositoryBehaviour;
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
}
