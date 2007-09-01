package org.openrdf.alibaba.decor;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import org.openrdf.alibaba.exceptions.AlibabaException;
import org.openrdf.alibaba.pov.Intent;
import org.openrdf.elmo.Entity;

public interface TextPresentationExportBehaviour {
	public abstract void exportPresentation(Intent intention, Entity target,
			Map<String, String> filter, String orderBy, PrintWriter writer)
			throws AlibabaException, IOException;
}