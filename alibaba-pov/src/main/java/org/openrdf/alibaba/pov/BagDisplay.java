package org.openrdf.alibaba.pov;

import org.openrdf.elmo.annotations.rdf;

/** Description of how a collection of property value should be shown. */
@rdf("http://www.openrdf.org/rdf/2007/09/point-of-view#BagDisplay")
public interface BagDisplay extends Display, AltDisplayOrBagDisplayOrSeqDisplay {

}
