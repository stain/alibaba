package org.openrdf.alibaba.decor.support;

import org.openrdf.alibaba.decor.EncodingBehaviour;
import org.openrdf.alibaba.vocabulary.ALI;
import org.openrdf.elmo.annotations.oneOf;

@oneOf(ALI.NS + "none")
public class NoEncodingSupport implements EncodingBehaviour {

	public String encode(String value) {
		return value;
	}

	public String decode(String value) {
		return value;
	}

}
