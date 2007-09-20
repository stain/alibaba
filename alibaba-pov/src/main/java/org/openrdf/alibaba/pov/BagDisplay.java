package org.openrdf.alibaba.pov;

import org.openrdf.elmo.annotations.disjointWith;
import org.openrdf.elmo.annotations.rdf;

/** Description of how a collection of property value should be shown. */
@rdf("http://www.openrdf.org/rdf/2007/09/point-of-view#BagDisplay")
@disjointWith({AltDisplay.class, PropertyDisplay.class, QNameDisplay.class, SeqDisplay.class})
public interface BagDisplay extends Display, CollectionDisplay {

}
