package org.openrdf.alibaba.decor;

import org.openrdf.alibaba.factories.PerspectiveFactory;
import org.openrdf.alibaba.formats.FormatRepository;
import org.openrdf.alibaba.pov.Intent;
import org.openrdf.alibaba.pov.PerspectiveRepository;
import org.openrdf.alibaba.pov.SearchPatternRepository;
import org.openrdf.concepts.owl.Thing;
import org.openrdf.elmo.annotations.rdf;

/** Handles presentation requests. */
@rdf("http://www.openrdf.org/rdf/2007/09/decor#PresentationService")
public interface PresentationService extends Thing, PresentationServiceBehaviour {


	/** The presentation repository used to lookup presentations. */
	@rdf("http://www.openrdf.org/rdf/2007/09/decor#presentations")
	public abstract PresentationRepository getPovPresentations();

	/** The presentation repository used to lookup presentations. */
	public abstract void setPovPresentations(PresentationRepository value);


	/** If no explicit intetion, this intention is matched against the purpose. */
	@rdf("http://www.openrdf.org/rdf/2007/09/decor#defaultIntention")
	public abstract Intent getPovDefaultIntention();

	/** If no explicit intetion, this intention is matched against the purpose. */
	public abstract void setPovDefaultIntention(Intent value);


	/** The format repository used to lookup formats. */
	@rdf("http://www.openrdf.org/rdf/2007/09/decor#formats")
	public abstract FormatRepository getPovFormats();

	/** The format repository used to lookup formats. */
	public abstract void setPovFormats(FormatRepository value);


	/** The perspective factory used to create missing perspectives. */
	@rdf("http://www.openrdf.org/rdf/2007/09/decor#perspectiveFactory")
	public abstract PerspectiveFactory getPovPerspectiveFactory();

	/** The perspective factory used to create missing perspectives. */
	public abstract void setPovPerspectiveFactory(PerspectiveFactory value);


	/** The perspective repository used to lookup perspectives. */
	@rdf("http://www.openrdf.org/rdf/2007/09/decor#perspectives")
	public abstract PerspectiveRepository getPovPerspectives();

	/** The perspective repository used to lookup perspectives. */
	public abstract void setPovPerspectives(PerspectiveRepository value);


	/** The search pattern repository used to lookup search patterns. */
	@rdf("http://www.openrdf.org/rdf/2007/09/decor#searchPatterns")
	public abstract SearchPatternRepository getPovSearchPatterns();

	/** The search pattern repository used to lookup search patterns. */
	public abstract void setPovSearchPatterns(SearchPatternRepository value);

}
