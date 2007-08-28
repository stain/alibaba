package org.openrdf.alibaba.concepts;

import java.util.Set;
import org.openrdf.concepts.rdf.Seq;
import org.openrdf.concepts.rdfs.Class;
import org.openrdf.elmo.annotations.rdf;

public interface PerspectiveOrSearchPattern  {


	/** List of all Displays which should be shown in this perspective. */
	@rdf("http://www.openrdf.org/rdf/2007/08/point-of-view#displays")
	public abstract Seq<Display> getPovDisplays();

	/** List of all Displays which should be shown in this perspective. */
	public abstract void setPovDisplays(Seq<Display> value);


	/** The layout that is or should be used. */
	@rdf("http://www.openrdf.org/rdf/2007/08/point-of-view#layout")
	public abstract Layout getPovLayout();

	/** The layout that is or should be used. */
	public abstract void setPovLayout(Layout value);


	/** A representation must be selected based on this purpose. */
	@rdf("http://www.openrdf.org/rdf/2007/08/point-of-view#purpose")
	public abstract Intent getPovPurpose();

	/** A representation must be selected based on this purpose. */
	public abstract void setPovPurpose(Intent value);


	/** Specifies what type of indiviuals this perspective can be used for. */
	@rdf("http://www.openrdf.org/rdf/2007/08/point-of-view#represents")
	public abstract Set<Class> getPovRepresents();

	/** Specifies what type of indiviuals this perspective can be used for. */
	public abstract void setPovRepresents(Set<Class> value);

}
