package org.openrdf.alibaba.decor;

import java.io.IOException;

import org.openrdf.alibaba.decor.helpers.Context;
import org.openrdf.alibaba.exceptions.AlibabaException;
import org.openrdf.alibaba.pov.PerspectiveOrSearchPattern;
import org.openrdf.elmo.Entity;

public interface TextPresentationExportBehaviour {
	public abstract void exportPresentation(PerspectiveOrSearchPattern spec,
			Entity target, Context ctx) throws AlibabaException, IOException;

	public abstract void exportRepresentation(PerspectiveOrSearchPattern spec,
			Context context) throws AlibabaException, IOException;
}