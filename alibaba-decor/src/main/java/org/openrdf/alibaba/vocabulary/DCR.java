package org.openrdf.alibaba.vocabulary;

/**
 * Ontology for defining presentation of display perspectives.
 * PresentationService is the primary access point for import/export requests.
 * Presentation combined with a Perspective or SearchPattern describes a
 * complete content response. Representation describes a single resource within
 * the content response. Decoration is used around display values.
 * 
 * @author James Leigh
 * 
 */
public class DCR {
	/** http://www.openrdf.org/rdf/2007/09/decor# */
	public static final String NS = "http://www.openrdf.org/rdf/2007/09/decor#";

	private DCR() {
		// prevent construction
	}
}
