package org.openrdf.alibaba.formats.support;

import java.util.Locale;

import org.openrdf.alibaba.formats.Format;
import org.openrdf.alibaba.formats.FormatBehaviour;
import org.openrdf.alibaba.vocabulary.ALI;
import org.openrdf.elmo.annotations.oneOf;

/** Literal value is converted to lower-case. */
@oneOf(ALI.NS + "lowercase")
public class LowercaseFormatSupport implements FormatBehaviour {
	private Locale locale;

	public LowercaseFormatSupport(Format bean) {
		Locale locale = bean.getElmoManager().getLocale();
		this.locale = locale == null ? Locale.getDefault() : locale;
	}

	public String format(Object value) {
		return value.toString().toLowerCase(locale);
	}

	public Object parse(String source) {
		return source;
	}
}
