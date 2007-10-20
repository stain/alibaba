package org.openrdf.alibaba.pov;

import org.openrdf.elmo.annotations.rdf;

/** Description of how nested property values should be shown. */
@rdf("http://www.openrdf.org/rdf/2007/09/point-of-view#NestedPropertyDisplay")
public interface NestedPropertyDisplay extends Display, AltDisplayOrBagDisplayOrNestedPropertyDisplayOrSeqDisplay {

}
