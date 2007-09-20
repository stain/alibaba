package org.openrdf.alibaba.pov;

import org.openrdf.elmo.annotations.disjointWith;
import org.openrdf.elmo.annotations.rdf;

/** Description of how the qualified name should be shown. */
@rdf("http://www.openrdf.org/rdf/2007/09/point-of-view#QNameDisplay")
@disjointWith({AltDisplay.class, BagDisplay.class, PropertyDisplay.class, SeqDisplay.class})
public interface QNameDisplay extends Display {

}
