package org.openrdf.alibaba.formats.support;

import javax.xml.namespace.QName;

import org.openrdf.alibaba.exceptions.AlibabaException;
import org.openrdf.alibaba.formats.FormatBehaviour;
import org.openrdf.alibaba.vocabulary.ALI;
import org.openrdf.elmo.annotations.oneOf;

/** Describes how an embedded resource should appear. */
@oneOf(ALI.NS + "qualified-name")
public class QualifiedNameFormatSupport implements FormatBehaviour {

	public String format(Object value) {
		if (value == null) {
			return "?";
		}
		assert value instanceof QName : value;
		QName qname = (QName) value;
		return qname.getPrefix() + ':' + qname.getLocalPart();
	}

	public Object parse(String source) throws AlibabaException {
		String[] qname = source.split(":", 2);
		return new QName("", qname[1], qname[0]);
	}
}
