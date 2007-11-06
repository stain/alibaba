package org.openrdf.alibaba.decor.support;

import org.openrdf.alibaba.decor.EncodingBehaviour;
import org.openrdf.alibaba.vocabulary.ALI;
import org.openrdf.elmo.annotations.oneOf;

@oneOf(ALI.NS + "javascript-encoding")
public class JavaScriptEncodingSupport implements EncodingBehaviour {

	public String encode(String value) {
		if (value == null)
			return null;
		return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("'", "\\'");
	}

	public String decode(String value) {
		if (value == null)
			return null;
		return value.replace("\\'", "'").replace("\\\"", "\"").replace("\\\\", "\\");
	}

}