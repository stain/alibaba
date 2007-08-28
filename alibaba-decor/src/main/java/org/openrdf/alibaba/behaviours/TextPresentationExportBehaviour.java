package org.openrdf.alibaba.behaviours;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import org.openrdf.alibaba.concepts.Intent;
import org.openrdf.alibaba.exceptions.AlibabaException;
import org.openrdf.elmo.Entity;

public interface TextPresentationExportBehaviour {
	public abstract void exportPresentation(Intent intention, Entity target,
			Map<String, String> filter, String orderBy, PrintWriter writer)
			throws AlibabaException, IOException;
}