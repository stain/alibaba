package org.openrdf.alibaba.decor.support;

import org.openrdf.alibaba.decor.EncodingBehaviour;
import org.openrdf.alibaba.vocabulary.ALI;
import org.openrdf.elmo.annotations.oneOf;

/**
 * Encodes a String into a value that is save to include in a JavaScript String
 * literal. The encoded value does not include opening and closing quotes.
 * 
 * @author James Leigh
 * 
 */
@oneOf(ALI.NS + "javascript-encoding")
public class JavaScriptEncodingSupport implements EncodingBehaviour {

	public String encode(String value) {
		if (value == null)
			return null;
		String enc = value.replace("\\", "\\\\");
		return enc.replace("\"", "\\\"").replace("'", "\\'");
	}

	public String decode(String value) {
		if (value == null)
			return null;
		String dec = value.replace("\\'", "'").replace("\\\"", "\"");
		return dec.replace("\\\\", "\\");
	}

}
