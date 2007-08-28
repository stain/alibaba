package org.openrdf.alibaba.behaviours.support;

import org.openrdf.alibaba.behaviours.PresentationBehaviour;
import org.openrdf.alibaba.concepts.Intent;
import org.openrdf.alibaba.concepts.Perspective;
import org.openrdf.alibaba.concepts.PerspectiveFactory;
import org.openrdf.alibaba.concepts.PerspectiveOrSearchPattern;
import org.openrdf.alibaba.concepts.PerspectiveRepository;
import org.openrdf.alibaba.concepts.Presentation;
import org.openrdf.alibaba.concepts.SearchPattern;
import org.openrdf.alibaba.concepts.SearchPatternRepository;
import org.openrdf.alibaba.vocabulary.POV;
import org.openrdf.concepts.rdfs.Class;
import org.openrdf.elmo.Entity;
import org.openrdf.elmo.annotations.rdf;

@rdf(POV.NS + "Presentation")
public class PresentationSupport implements PresentationBehaviour {
	private Presentation pres;

	public PresentationSupport(Presentation pres) {
		this.pres = pres;
	}

	public PerspectiveOrSearchPattern findPerspectiveOrSearchPattern(Intent intent, Entity target) {
		if (target instanceof Class) {
			SearchPatternRepository spr = pres.getPovSearchPatterns();
			SearchPattern sp = spr.findSearchPattern(intent, (Class) target);
			if (sp != null) {
				return sp;
			}
		}
		PerspectiveRepository repo = pres.getPovPerspectives();
		Perspective spec = repo.findPerspective(intent, target);
		if (spec == null) {
			spec = createPerspective(intent, target);
		}
		return spec;
	}

	private Perspective createPerspective(Intent intent, Entity target) {
		PerspectiveFactory factory = pres.getPovPerspectiveFactory();
		if (factory == null)
			return null;
		Perspective spec = factory.createPerspectiveFor(intent, target);
		PerspectiveRepository repo = pres.getPovPerspectives();
		repo.getPovRegisteredPerspectives().add(spec);
		return spec;
	}

}
