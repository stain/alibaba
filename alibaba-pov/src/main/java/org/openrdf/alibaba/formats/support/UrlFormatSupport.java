package org.openrdf.alibaba.formats.support;

import javax.xml.namespace.QName;

import org.openrdf.alibaba.concepts.Format;
import org.openrdf.alibaba.exceptions.AlibabaException;
import org.openrdf.alibaba.formats.FormatBehaviour;
import org.openrdf.alibaba.vocabulary.ALI;
import org.openrdf.elmo.ElmoManager;
import org.openrdf.elmo.Entity;
import org.openrdf.elmo.annotations.oneOf;

/** Describes how an embeded resource should appear. */
@oneOf(ALI.NS + "url")
public class UrlFormatSupport implements FormatBehaviour {
	private ElmoManager manager;

	public UrlFormatSupport(Format format) {
		super();
		this.manager = format.getElmoManager();
	}

	public String format(Object value) {
		QName qname = ((Entity) value).getQName();
		return qname.getNamespaceURI() + qname.getLocalPart();
	}

	public Object parse(String source) throws AlibabaException {
		return manager.find(new QName(source));
	}
}
