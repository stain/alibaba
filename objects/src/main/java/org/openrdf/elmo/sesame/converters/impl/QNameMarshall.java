package org.openrdf.elmo.sesame.converters.impl;

import static javax.xml.XMLConstants.NULL_NS_URI;

import javax.xml.namespace.QName;

import org.openrdf.elmo.sesame.converters.Marshall;
import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.XMLSchema;

public class QNameMarshall  implements Marshall<QName> {
	private ValueFactory vf;

	public QNameMarshall(ValueFactory vf) {
		this.vf = vf;
	}

	public String getJavaClassName() {
		return QName.class.getName();
	}

	public URI getDatatype() {
		return XMLSchema.QNAME;
	}

	public void setDatatype(URI datatype) {
		if (!datatype.equals(XMLSchema.QNAME))
			throw new IllegalArgumentException(datatype.toString());
	}

	public QName deserialize(Literal literal) {
		String label = literal.getLabel();
		int idx = label.indexOf(':');
		if (label.charAt(0) == '{' || idx < 0)
			return QName.valueOf(label);
		String prefix = label.substring(0, idx);
		return new QName(NULL_NS_URI, label.substring(idx + 1), prefix);
	}

	public Literal serialize(QName object) {
		if (object.getPrefix().length() == 0)
			return vf.createLiteral(object.toString());
		StringBuilder label = new StringBuilder();
		label.append(object.getPrefix());
		label.append(":");
		label.append(object.getLocalPart());
		return vf.createLiteral(label.toString());
	}

}
