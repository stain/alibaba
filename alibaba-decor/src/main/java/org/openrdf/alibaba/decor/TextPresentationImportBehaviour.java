package org.openrdf.alibaba.decor;

import java.io.IOException;

import org.openrdf.alibaba.decor.helpers.Context;
import org.openrdf.alibaba.exceptions.AlibabaException;
import org.openrdf.alibaba.pov.PerspectiveOrSearchPattern;
import org.openrdf.elmo.Entity;

public interface TextPresentationImportBehaviour {
	public abstract void importPresentation(PerspectiveOrSearchPattern spec,
			Entity target, Context ctx) throws AlibabaException, IOException;
}