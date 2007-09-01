package org.openrdf.alibaba.pov;

import java.util.Set;
import org.openrdf.concepts.owl.Thing;
import org.openrdf.elmo.annotations.rdf;

/** The propose or intent in which a specific persective might be appropriate. */
@rdf("http://www.openrdf.org/rdf/2007/09/point-of-view#Intent")
public interface Intent extends Thing {


	/** This more sepecific intent complies with the broader intent given. */
	@rdf("http://www.openrdf.org/rdf/2007/09/point-of-view#aim")
	public abstract Set<Intent> getPovAims();

	/** This more sepecific intent complies with the broader intent given. */
	public abstract void setPovAims(Set<Intent> value);

}
