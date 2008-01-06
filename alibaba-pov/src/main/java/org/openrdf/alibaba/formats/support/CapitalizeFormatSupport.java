package org.openrdf.alibaba.formats.support;

import java.util.Locale;

import org.openrdf.alibaba.formats.Format;
import org.openrdf.alibaba.formats.FormatBehaviour;
import org.openrdf.alibaba.vocabulary.ALI;
import org.openrdf.elmo.annotations.oneOf;

/** Every character at the beginning of a word is capitalized. */
@oneOf(ALI.NS + "capitalize")
public class CapitalizeFormatSupport implements FormatBehaviour {
	private Locale locale;

	public CapitalizeFormatSupport(Format bean) {
		Locale locale = bean.getElmoManager().getLocale();
		this.locale = locale == null ? Locale.getDefault() : locale;
	}

	public String format(Object value) {
		return capitalize(value.toString());
	}

	public Object parse(String source) {
		return source;
	}

	private String capitalize(String orig) {
		StringBuffer sb = new StringBuffer(orig.length());
		for (int i=0,n=orig.length(); i<n; i++) {
			char chr = orig.charAt(i);
			if (i == 0 || Character.isWhitespace(orig.charAt(i - 1))) {
				String upper = String.valueOf(chr).toUpperCase(locale);
				sb.append(upper);
			} else {
				sb.append(chr);
			}
		}
		return sb.toString();
	}
}
