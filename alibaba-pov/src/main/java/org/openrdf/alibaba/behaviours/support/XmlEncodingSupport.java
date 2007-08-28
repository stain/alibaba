package org.openrdf.alibaba.behaviours.support;

import org.openrdf.alibaba.behaviours.EncodingBehaviour;
import org.openrdf.alibaba.vocabulary.ALI;
import org.openrdf.elmo.annotations.oneOf;

@oneOf(ALI.NS + "xml-encoding")
public class XmlEncodingSupport implements EncodingBehaviour {

	public String encode(String value) {
		if (value == null)
			return null;
		return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	public String decode(String value) {
		if (value == null)
			return null;
		return value.replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&");
	}

}
