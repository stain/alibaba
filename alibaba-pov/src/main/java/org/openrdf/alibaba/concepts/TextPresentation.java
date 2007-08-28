package org.openrdf.alibaba.concepts;

import org.openrdf.alibaba.behaviours.TextPresentationBehaviour;
import org.openrdf.elmo.annotations.rdf;

/** Text based presentation of representations. */
@rdf("http://www.openrdf.org/rdf/2007/08/point-of-view#TextPresentation")
public interface TextPresentation extends Presentation, TextPresentationBehaviour {

}
