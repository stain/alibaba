package org.openrdf.alibaba.concepts;

import java.util.Set;
import org.openrdf.concepts.owl.Thing;
import org.openrdf.elmo.annotations.rdf;

/** A format that can be used to represent resources. */
@rdf("http://www.openrdf.org/rdf/2007/09/decor#Representation")
public interface Representation extends Thing, PresentationOrRepresentation {


	/** The layouts that are supported by this Representation. */
	@rdf("http://www.openrdf.org/rdf/2007/09/decor#conformsTo")
	public abstract Set<Layout> getPovConformsTos();

	/** The layouts that are supported by this Representation. */
	public abstract void setPovConformsTos(Set<Layout> value);


	/** Decoration around the displays. */
	@rdf("http://www.openrdf.org/rdf/2007/09/decor#displayDecoration")
	public abstract Decoration getPovDisplayDecoration();

	/** Decoration around the displays. */
	public abstract void setPovDisplayDecoration(Decoration value);


	/** Decoration around literal display values. */
	@rdf("http://www.openrdf.org/rdf/2007/09/decor#literalDecoration")
	public abstract Decoration getPovLiteralDecoration();

	/** Decoration around literal display values. */
	public abstract void setPovLiteralDecoration(Decoration value);


	/** Decoration around perspective display values. */
	@rdf("http://www.openrdf.org/rdf/2007/09/decor#perspectiveDecoration")
	public abstract Decoration getPovPerspectiveDecoration();

	/** Decoration around perspective display values. */
	public abstract void setPovPerspectiveDecoration(Decoration value);


	/** Decoration around search display values. */
	@rdf("http://www.openrdf.org/rdf/2007/09/decor#searchDecoration")
	public abstract Decoration getPovSearchDecoration();

	/** Decoration around search display values. */
	public abstract void setPovSearchDecoration(Decoration value);

}
