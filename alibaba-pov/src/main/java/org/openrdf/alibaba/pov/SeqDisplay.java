package org.openrdf.alibaba.pov;

import org.openrdf.elmo.annotations.disjointWith;
import org.openrdf.elmo.annotations.rdf;

/** Description of how sequencial property values should be shown. */
@rdf("http://www.openrdf.org/rdf/2007/09/point-of-view#SeqDisplay")
@disjointWith({AltDisplay.class, BagDisplay.class, PropertyDisplay.class, QNameDisplay.class})
public interface SeqDisplay extends Display, CollectionDisplay {

}
