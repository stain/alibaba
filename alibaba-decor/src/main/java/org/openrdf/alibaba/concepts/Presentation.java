package org.openrdf.alibaba.concepts;

import java.util.Set;

import org.openrdf.alibaba.behaviours.PresentationBehaviour;
import org.openrdf.concepts.owl.Thing;
import org.openrdf.elmo.annotations.rdf;

/** Presentation of representations. */
@rdf("http://www.openrdf.org/rdf/2007/08/point-of-view#Presentation")
public interface Presentation extends Thing, PresentationOrRepresentation, PresentationBehaviour {


	/** Set of accept strings that can be used with this presentation. */
	@rdf("http://www.openrdf.org/rdf/2007/08/point-of-view#accept")
	public abstract Set<String> getPovAccepts();

	/** Set of accept strings that can be used with this presentation. */
	public abstract void setPovAccepts(Set<String> value);


	/** The content-type this presentation generates. */
	@rdf("http://www.openrdf.org/rdf/2007/08/point-of-view#contentType")
	public abstract String getPovContentType();

	/** The content-type this presentation generates. */
	public abstract void setPovContentType(String value);


	/** Method used to serialize values. */
	@rdf("http://www.openrdf.org/rdf/2007/08/point-of-view#encoding")
	public abstract Encoding getPovEncoding();

	/** Method used to serialize values. */
	public abstract void setPovEncoding(Encoding value);


	/** The format repository used to lookup formats. */
	@rdf("http://www.openrdf.org/rdf/2007/08/point-of-view#formats")
	public abstract FormatRepository getPovFormats();

	/** The format repository used to lookup formats. */
	public abstract void setPovFormats(FormatRepository value);


	/** The perspective factory used to create missing perspectives. */
	@rdf("http://www.openrdf.org/rdf/2007/08/point-of-view#perspectiveFactory")
	public abstract PerspectiveFactory getPovPerspectiveFactory();

	/** The perspective factory used to create missing perspectives. */
	public abstract void setPovPerspectiveFactory(PerspectiveFactory value);


	/** The perspective repository used to lookup perspectives. */
	@rdf("http://www.openrdf.org/rdf/2007/08/point-of-view#perspectives")
	public abstract PerspectiveRepository getPovPerspectives();

	/** The perspective repository used to lookup perspectives. */
	public abstract void setPovPerspectives(PerspectiveRepository value);


	/** The representation repository used to lookup representations. */
	@rdf("http://www.openrdf.org/rdf/2007/08/point-of-view#representations")
	public abstract RepresentationRepository getPovRepresentations();

	/** The representation repository used to lookup representations. */
	public abstract void setPovRepresentations(RepresentationRepository value);


	/** The search pattern repository used to lookup search patterns. */
	@rdf("http://www.openrdf.org/rdf/2007/08/point-of-view#searchPatterns")
	public abstract SearchPatternRepository getPovSearchPatterns();

	/** The search pattern repository used to lookup search patterns. */
	public abstract void setPovSearchPatterns(SearchPatternRepository value);

}
