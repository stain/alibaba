package org.openrdf.alibaba.pov;

import org.openrdf.elmo.annotations.rdf;

/** Description of how alternative property values should be shown. */
@rdf("http://www.openrdf.org/rdf/2007/09/point-of-view#AltDisplay")
public interface AltDisplay extends Display, AltDisplayOrBagDisplayOrNestedPropertyDisplayOrSeqDisplay {

}
