package org.openrdf.alibaba.decor;

import java.util.Set;

import org.openrdf.concepts.owl.Thing;
import org.openrdf.elmo.annotations.rdf;

/** Presentation of representations. */
@rdf("http://www.openrdf.org/rdf/2007/09/decor#Presentation")
public interface Presentation extends Thing, PresentationOrRepresentation {


	/** Set of accept strings that can be used with this presentation. */
	@rdf("http://www.openrdf.org/rdf/2007/09/decor#accept")
	public abstract Set<String> getPovAccepts();

	/** Set of accept strings that can be used with this presentation. */
	public abstract void setPovAccepts(Set<String> value);


	/** The content-type this presentation generates. */
	@rdf("http://www.openrdf.org/rdf/2007/09/decor#contentType")
	public abstract String getPovContentType();

	/** The content-type this presentation generates. */
	public abstract void setPovContentType(String value);


	/** Method used to serialize values. */
	@rdf("http://www.openrdf.org/rdf/2007/09/decor#encoding")
	public abstract Encoding getPovEncoding();

	/** Method used to serialize values. */
	public abstract void setPovEncoding(Encoding value);


	/** The representation repository used to lookup representations. */
	@rdf("http://www.openrdf.org/rdf/2007/09/decor#representations")
	public abstract RepresentationRepository getPovRepresentations();

	/** The representation repository used to lookup representations. */
	public abstract void setPovRepresentations(RepresentationRepository value);

}
