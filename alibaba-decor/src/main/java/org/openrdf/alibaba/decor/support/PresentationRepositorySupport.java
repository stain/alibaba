package org.openrdf.alibaba.decor.support;

import org.openrdf.alibaba.decor.Presentation;
import org.openrdf.alibaba.decor.PresentationRepository;
import org.openrdf.alibaba.decor.PresentationRepositoryBehaviour;
import org.openrdf.alibaba.pov.base.RepositoryBase;
import org.openrdf.alibaba.vocabulary.DCR;
import org.openrdf.elmo.annotations.rdf;

@rdf(DCR.NS + "PresentationRepository")
public class PresentationRepositorySupport extends RepositoryBase<Presentation> implements PresentationRepositoryBehaviour {
	public PresentationRepositorySupport(PresentationRepository repository) {
		super(repository.getPovRegisteredPresentations());
	}

	public Presentation findPresentation(String... accept) {
		Presentation result = null;
		for (String contentType : accept) {
			for (Presentation present : this) {
				if (present.getPovAccepts().contains(contentType)) {
					result = present;
				}
			}
			if (result != null)
				return result;
		}
		return null;
	}
}
