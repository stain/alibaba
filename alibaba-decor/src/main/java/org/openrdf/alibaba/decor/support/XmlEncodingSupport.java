package org.openrdf.alibaba.decor.support;

import org.openrdf.alibaba.decor.EncodingBehaviour;
import org.openrdf.alibaba.vocabulary.ALI;
import org.openrdf.elmo.annotations.oneOf;

@oneOf(ALI.NS + "xml-encoding")
public class XmlEncodingSupport implements EncodingBehaviour {
	private String[] decoded = new String[]{"&","<", ">", "\"", "'"};

	private String[] encoded = new String[]{"&amp;", "&lt;", "&gt;", "&quot;", "&apos;"};

	public String encode(String value) {
		if (value == null)
			return null;
		String result = value;
		for (int i=0; i<decoded.length; i++) {
			result = value.replace(decoded[i], encoded[i]);
		}
		return result;
	}

	public String decode(String value) {
		if (value == null)
			return null;
		String result = value;
		for (int i=0; i<encoded.length; i++) {
			result = value.replace(encoded[i], decoded[i]);
		}
		return result;
	}

}
