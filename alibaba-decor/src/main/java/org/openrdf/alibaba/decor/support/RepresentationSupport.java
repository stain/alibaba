package org.openrdf.alibaba.decor.support;

import org.openrdf.alibaba.decor.Decoration;
import org.openrdf.alibaba.decor.Representation;
import org.openrdf.alibaba.decor.RepresentationBehaviour;
import org.openrdf.alibaba.pov.Display;
import org.openrdf.alibaba.pov.FunctionalDisplay;
import org.openrdf.alibaba.pov.Perspective;
import org.openrdf.alibaba.pov.SearchPattern;
import org.openrdf.alibaba.vocabulary.DCR;
import org.openrdf.elmo.annotations.rdf;

@rdf(DCR.NS + "Representation")
public class RepresentationSupport implements RepresentationBehaviour {
	private Representation rep;

	public RepresentationSupport(Representation rep) {
		this.rep = rep;
	}

	public Decoration findDecorationFor(Display display) {
		Perspective perspective = display.getPovPerspective();
		SearchPattern searchPattern = display.getPovSearchPattern();
		if (display instanceof FunctionalDisplay) {
			if (perspective != null) {
				Decoration decor = rep.getPovFunctionalPerspectiveDecoration();
				if (decor != null)
					return decor;
			} else if (searchPattern != null) {
				Decoration decor = rep.getPovFunctionalSearchDecoration();
				if (decor != null)
					return decor;
			} else {
				Decoration decor = rep.getPovFunctionalLiteralDecoration();
				if (decor != null)
					return decor;
			}
		}
		if (perspective != null)
			return rep.getPovPerspectiveDecoration();
		if (searchPattern != null)
			return rep.getPovSearchDecoration();
		return rep.getPovLiteralDecoration();
	}

}
