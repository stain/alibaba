package org.openrdf.alibaba.formats.support;

import org.openrdf.alibaba.formats.FormatBehaviour;
import org.openrdf.alibaba.vocabulary.ALI;
import org.openrdf.elmo.annotations.oneOf;

/** The labels are displayed without transformation. */
@oneOf(ALI.NS + "none")
public class NoneFormatSupport implements FormatBehaviour {

	public String format(Object value) {
		return value.toString();
	}

	public Object parse(String source) {
		return source;
	}
}
