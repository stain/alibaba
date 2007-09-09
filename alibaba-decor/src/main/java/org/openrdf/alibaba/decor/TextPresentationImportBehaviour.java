package org.openrdf.alibaba.decor;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;

import org.openrdf.alibaba.exceptions.AlibabaException;
import org.openrdf.alibaba.pov.Intent;
import org.openrdf.elmo.Entity;

public interface TextPresentationImportBehaviour {
	public abstract void importPresentation(Intent intention, Entity target,
			Map<String, String> filter, String orderBy, UrlResolver link,
			BufferedReader reader) throws AlibabaException, IOException;
}