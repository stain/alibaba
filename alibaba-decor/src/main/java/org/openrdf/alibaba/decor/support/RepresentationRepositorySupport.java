package org.openrdf.alibaba.decor.support;

import org.openrdf.alibaba.core.base.RepositoryBase;
import org.openrdf.alibaba.decor.Representation;
import org.openrdf.alibaba.decor.RepresentationRepository;
import org.openrdf.alibaba.decor.RepresentationRepositoryBehaviour;
import org.openrdf.alibaba.formats.Layout;
import org.openrdf.alibaba.pov.Intent;
import org.openrdf.alibaba.vocabulary.DCR;
import org.openrdf.elmo.annotations.rdf;

@rdf(DCR.NS + "RepresentationRepository")
public class RepresentationRepositorySupport extends
		RepositoryBase<Representation> implements
		RepresentationRepositoryBehaviour {
	public RepresentationRepositorySupport(
			RepresentationRepository repository) {
		super(repository.getPovRegisteredRepresentations());
	}

	public Representation findRepresentation(Intent intent, Layout layout) {
		Representation result = null;
		for (Representation rep : this) {
			if (!rep.getPovConformsTos().contains(layout))
				continue;
			if (!rep.getPovIntentions().contains(intent))
				continue;
			result = rep;
		}
		return result;
	}

}
