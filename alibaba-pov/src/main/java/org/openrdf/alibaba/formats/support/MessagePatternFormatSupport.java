package org.openrdf.alibaba.formats.support;

import java.text.FieldPosition;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.GregorianCalendar;
import java.util.Locale;

import javax.xml.datatype.XMLGregorianCalendar;

import org.openrdf.alibaba.exceptions.BadRequestException;
import org.openrdf.alibaba.exceptions.AlibabaException;
import org.openrdf.alibaba.formats.FormatBehaviour;
import org.openrdf.alibaba.formats.MessagePatternFormat;
import org.openrdf.alibaba.vocabulary.POV;
import org.openrdf.elmo.annotations.rdf;

@rdf(POV.NS + "MessagePatternFormat")
public class MessagePatternFormatSupport implements FormatBehaviour {
	private MessageFormat formatter;

	public MessagePatternFormatSupport(MessagePatternFormat bean) {
		MessagePatternFormat format = bean;
		String pattern = format.getPovPattern();
		Locale locale = bean.getElmoManager().getLocale();
		if (locale == null) {
			locale = Locale.getDefault();
		}
		formatter = new MessageFormat(pattern, locale);
	}

	public String format(Object value) {
		if (value instanceof Object[])
			return formatMessage((Object[]) value);
		return formatMessage(new Object[]{value});
	}

	public Object parse(String source) throws AlibabaException {
		try {
			Object[] parsed = parseMessage(source);
			if (parsed == null || parsed.length == 0)
				return null;
			if (parsed.length == 1)
				return parsed[0];
			return parsed;
		} catch (ParseException e) {
			throw new BadRequestException(e);
		}
	}

	protected String formatMessage(Object[] values) {
		for (int i = 0; i < values.length; i++) {
			if (values[i] instanceof XMLGregorianCalendar) {
				XMLGregorianCalendar xcal = (XMLGregorianCalendar) values[i];
				values[i] = xcal.toGregorianCalendar().getTime();
			}
			if (values[i] instanceof GregorianCalendar) {
				GregorianCalendar cal = (GregorianCalendar) values[i];
				values[i] = cal.getTime();
			}
		}
		StringBuffer sb = new StringBuffer();
		formatter.format(values, sb, new FieldPosition(0));
		return sb.toString();
	}

	protected Object[] parseMessage(String source) throws ParseException {
		return formatter.parse(source);
	}

}
