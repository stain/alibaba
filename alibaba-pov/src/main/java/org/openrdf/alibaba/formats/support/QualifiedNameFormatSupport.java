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
@oneOf(ALI.NS + "qualified-name")
public class QualifiedNameFormatSupport implements FormatBehaviour {
	private ElmoManager manager;

	public QualifiedNameFormatSupport(Format format) {
		super();
		this.manager = format.getElmoManager();
	}

	public String format(Object value) {
		QName qname = ((Entity) value).getQName();
		if (qname == null) {
			return value.toString();
		}
		return qname.getPrefix() + ':' + qname.getLocalPart();
	}

	public Object parse(String source) throws AlibabaException {
		String[] qname = source.split(":", 2);
		return manager.find(new QName("", qname[1], qname[0]));
	}
}
