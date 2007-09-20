package org.openrdf.alibaba.formats.support;

import javax.xml.namespace.QName;

import org.openrdf.alibaba.exceptions.AlibabaException;
import org.openrdf.alibaba.formats.FormatBehaviour;
import org.openrdf.alibaba.vocabulary.ALI;
import org.openrdf.elmo.annotations.oneOf;

/** Describes how an embedded resource should appear. */
@oneOf(ALI.NS + "url")
public class UrlFormatSupport implements FormatBehaviour {

	public String format(Object value) {
		assert value instanceof QName : value;
		QName qname = (QName) value;
		return qname.getNamespaceURI() + qname.getLocalPart();
	}

	public Object parse(String source) throws AlibabaException {
		return new QName(source);
	}
}
