package org.openrdf.http.object.util;

import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.http.Header;

public class HTTPDateFormat extends DateFormat {
	private static final long serialVersionUID = -2636174153773598968L;
	/** Date format pattern used to generate the header in RFC 1123 format. */
	private static final String HTTP_RESPONSE_DATE_HEADER = "EEE, dd MMM yyyy HH:mm:ss zzz";
	private static ThreadLocal<SimpleDateFormat> simple = new ThreadLocal<SimpleDateFormat>() {
		@Override
		protected SimpleDateFormat initialValue() {
			SimpleDateFormat format = new SimpleDateFormat(
					HTTP_RESPONSE_DATE_HEADER, Locale.US);
			format.setTimeZone(TimeZone.getTimeZone("GMT"));
			return format;
		}
	};
	private long format = 0;
	private String parse = "Thu, 01 Jan 1970 00:00:00 GMT";

	public synchronized String format(long date) {
		if (format == date)
			return parse;
		return parse = simple.get().format(format = date);
	}

	public long parseHeader(Header hd) {
		if (hd == null)
			return System.currentTimeMillis() / 1000 * 1000;
		return parseDate(hd.getValue());
	}

	public long parseDate(String source) {
		if (source == null)
			return System.currentTimeMillis() / 1000 * 1000;
		synchronized (this) {
			if (source.equals(parse))
				return format;
		}
		try {
			return simple.get().parse(source).getTime();
		} catch (ParseException e) {
			return System.currentTimeMillis() / 1000 * 1000;
		}
	}

	public Date parse(String source) {
		if (source == null)
			return new Date(System.currentTimeMillis() / 1000 * 1000);
		synchronized (this) {
			if (source.equals(parse))
				return new Date(format);
		}
		try {
			return simple.get().parse(source);
		} catch (ParseException e) {
			return new Date(System.currentTimeMillis() / 1000 * 1000);
		}
	}

	@Override
	public StringBuffer format(Date date, StringBuffer toAppendTo,
			FieldPosition fieldPosition) {
		return simple.get().format(date, toAppendTo, fieldPosition);
	}

	@Override
	public Date parse(String source, ParsePosition pos) {
		return simple.get().parse(source, pos);
	}

}
