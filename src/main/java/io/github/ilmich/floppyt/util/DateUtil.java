/*
MIT License

Copyright (c) 2020 Michele Zuccalà

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/
package io.github.ilmich.floppyt.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Pattern;

public class DateUtil {

	private final static Locale LOCALE = Locale.US;
	private final static TimeZone GMT_ZONE;
	private final static String RFC_1123_PATTERN = "EEE, dd MMM yyyy HH:mm:ss zzz";
	private final static DateFormat RFC_1123_FORMAT;

	/** Pattern to find digits only. */
	private final static Pattern DIGIT_PATTERN = Pattern.compile("^\\d+$");

	static {
		RFC_1123_FORMAT = new SimpleDateFormat(DateUtil.RFC_1123_PATTERN, DateUtil.LOCALE);
		GMT_ZONE = TimeZone.getTimeZone("GMT");
		DateUtil.RFC_1123_FORMAT.setTimeZone(DateUtil.GMT_ZONE);
	}

	public static String getCurrentAsString() {
		synchronized (DateUtil.RFC_1123_FORMAT) {
			return DateUtil.RFC_1123_FORMAT.format(new Date());
		}
	}

	/**
	 * Translate a given date <code>String</code> in the <em>RFC 1123</em> format to
	 * a <code>long</code> representing the number of milliseconds since epoch.
	 * 
	 * @param dateString a date <code>String</code> in the <em>RFC 1123</em> format.
	 * @return the parsed <code>Date</code> in milliseconds.
	 */
	private static long parseDateStringToMilliseconds(final String dateString) {
		synchronized (DateUtil.RFC_1123_FORMAT) {
			try {
				return DateUtil.RFC_1123_FORMAT.parse(dateString).getTime();
			} catch (final ParseException e) {
				return 0;
			}
		}
	}

	/**
	 * Parse a given date <code>String</code> to a <code>long</code> representation
	 * of the time. Where the provided value is all digits the value is returned as
	 * a <code>long</code>, otherwise attempt is made to parse the
	 * <code>String</code> as a <em>RFC 1123</em> date.
	 * 
	 * @param dateValue the value to parse.
	 * @return the <code>long</code> value following parse, or zero where not
	 *         successful.
	 */
	public static long parseToMilliseconds(final String dateValue) {

		long ms = 0;

		if (DateUtil.DIGIT_PATTERN.matcher(dateValue).matches()) {
			ms = Long.parseLong(dateValue);
		} else {
			ms = parseDateStringToMilliseconds(dateValue);
		}

		return ms;
	}

	/**
	 * Converts a millisecond representation of a date to a <code>RFC 1123</code>
	 * formatted <code>String</code>.
	 * 
	 * @param dateValue the <code>Date</code> represented as milliseconds.
	 * @return a <code>String</code> representation of the date.
	 */
	public static String parseToRFC1123(final long dateValue) {

		final Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(dateValue);

		synchronized (DateUtil.RFC_1123_FORMAT) {
			return DateUtil.RFC_1123_FORMAT.format(calendar.getTime());
		}
	}

	/**
	 * Convert a given <code>Date</code> object to a <code>RFC 1123</code> formatted
	 * <code>String</code>.
	 * 
	 * @param date the <code>Date</code> object to convert
	 * @return a <code>String</code> representation of the date.
	 */
	public static String getDateAsString(Date date) {
		synchronized (DateUtil.RFC_1123_FORMAT) {
			return RFC_1123_FORMAT.format(date);
		}
	}

}
