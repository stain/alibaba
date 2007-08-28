package org.openrdf.alibaba.behaviours;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;

import org.openrdf.alibaba.concepts.Intent;
import org.openrdf.alibaba.exceptions.AlibabaException;
import org.openrdf.elmo.Entity;

public interface TextPresentationImportBehaviour {
	public abstract void importPresentation(Intent intention, Entity target,
			Map<String, String> filter, String orderBy, BufferedReader reader)
			throws AlibabaException, IOException;
}