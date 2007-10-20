package org.openrdf.alibaba.pov;

import org.openrdf.elmo.annotations.rdf;

/** Description of how sequential property values should be shown. */
@rdf("http://www.openrdf.org/rdf/2007/09/point-of-view#SeqDisplay")
public interface SeqDisplay extends Display, AltDisplayOrBagDisplayOrNestedPropertyDisplayOrSeqDisplay {

}
