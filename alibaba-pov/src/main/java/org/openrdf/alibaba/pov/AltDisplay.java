package org.openrdf.alibaba.pov;

import org.openrdf.elmo.annotations.disjointWith;
import org.openrdf.elmo.annotations.rdf;

/** Description of how alternative property values should be shown. */
@rdf("http://www.openrdf.org/rdf/2007/09/point-of-view#AltDisplay")
@disjointWith({BagDisplay.class, PropertyDisplay.class, QNameDisplay.class, SeqDisplay.class})
public interface AltDisplay extends Display, CollectionDisplay {

}
