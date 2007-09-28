package org.openrdf.alibaba.decor;

import java.io.IOException;

import org.openrdf.alibaba.decor.helpers.Context;
import org.openrdf.alibaba.exceptions.AlibabaException;
import org.openrdf.alibaba.pov.Intent;
import org.openrdf.elmo.Entity;

public interface TextPresentationExportBehaviour {
	public abstract void exportPresentation(Intent intention, Entity target,
			Context ctx) throws AlibabaException, IOException;
}