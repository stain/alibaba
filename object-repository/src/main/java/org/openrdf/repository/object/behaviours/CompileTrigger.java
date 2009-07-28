package org.openrdf.repository.object.behaviours;

import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.repository.object.annotations.triggeredBy;

public abstract class CompileTrigger implements RDFObject {
	private static final String RDFSNS = RDFS.NAMESPACE;
	private static final String OWLNS = OWL.NAMESPACE;

	@triggeredBy( { OWLNS + "imports", OWLNS + "complementOf",
			OWLNS + "intersectionOf", OWLNS + "oneOf", OWLNS + "onProperty",
			OWLNS + "unionOf", RDFSNS + "domain", RDFSNS + "range",
			RDFSNS + "subClassOf", RDFSNS + "subPropertyOf" })
	public void schemaChanged() {
		ObjectConnection con = getObjectConnection();
		con.getRepository().compileAfter(con);
	}
}
