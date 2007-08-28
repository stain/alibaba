package org.openrdf.alibaba.concepts;

import java.util.Set;
import org.openrdf.elmo.annotations.rdf;

public interface PresentationOrRepresentation  {


	/** The decoration used when rendering the presentation or representation */
	@rdf("http://www.openrdf.org/rdf/2007/08/point-of-view#decoration")
	public abstract Decoration getPovDecoration();

	/** The decoration used when rendering the presentation or representation */
	public abstract void setPovDecoration(Decoration value);


	/** This intention is matched against the purpose. */
	@rdf("http://www.openrdf.org/rdf/2007/08/point-of-view#intention")
	public abstract Set<Intent> getPovIntentions();

	/** This intention is matched against the purpose. */
	public abstract void setPovIntentions(Set<Intent> value);

}
