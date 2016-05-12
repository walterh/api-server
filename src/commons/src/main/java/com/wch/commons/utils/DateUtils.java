package com.wch.commons.utils;

import java.util.Calendar;
import java.util.Date;
import java.util.regex.Pattern;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DateUtils {
    public static final Pattern LONG_BASE10_PATTERN = Pattern.compile("^[0-9]+$");
    public static final Pattern LONG_BASE16_PATTERN = Pattern.compile("^[a-f0-9]+$", Pattern.CASE_INSENSITIVE);

    public static final String DATE_FORMAT_ISO_8601 = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    public static final String DATE_FORMAT_FILENAME = "yyyy-MM-dd";
    public static final String DATE_FORMAT_READABLE = "MM/dd/yy hh:mm:ss a";

    public static final java.util.TimeZone TIMEZONE_UTC = java.util.TimeZone.getTimeZone("UTC");
    // This is 9999-12-31T23:59:59.999+0000
    public static final long MAX_TIME = 253402300799999L;
    public static final long MIN_TIME = 0L;

    @SuppressWarnings("unused")
    private static String[][] __daysOfWeek = { new String[] { "MON", "MONDAY" },
            new String[] { "TUE", "TUESDAY" },
            new String[] { "WED", "WEDNESDAY" },
            new String[] { "THU", "THURSDAY" },
            new String[] { "FRI", "FRIDAY" },
            new String[] { "SAT", "SATURDAY" },
            new String[] { "SUN", "SUNDAY" } };

    @SuppressWarnings("unused")
    private static String[] __months = { "JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC" };

    private static SimpleDateFormat CreateSimpleDateFormat(String fmt) {
        SimpleDateFormat sdf = null;
        if (!Utils.isNullOrEmptyString(fmt)) {
            sdf = new SimpleDateFormat(fmt);
        } else {
            sdf = new SimpleDateFormat();
        }

        sdf.setLenient(true);
        sdf.setTimeZone(TIMEZONE_UTC);

        return sdf;
    }

    public static String nowAsIso8601() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeZone(TIMEZONE_UTC);
        return CreateSimpleDateFormat(DATE_FORMAT_ISO_8601).format(cal.getTime());
    }

    public static String nowAsFileNameSafe() {
        return internalFormatNowAsString(DATE_FORMAT_FILENAME);
    }

    public static String dateAsFileNameSafe(Date d) {
        return internalFormatDateAsString(d, DATE_FORMAT_FILENAME);
    }

    private static String internalFormatNowAsString(String fmt) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeZone(TIMEZONE_UTC);

        return CreateSimpleDateFormat(fmt).format(cal.getTime());
    }

    private static String internalFormatDateAsString(Date d, String fmt) {
        return CreateSimpleDateFormat(fmt).format(d.getTime());
    }

    public static long nowAsTicks() {
        return System.currentTimeMillis();
    }

    public static Date now() {
        return new Date(nowAsTicks());
    }

    public static String dateAsIso8601(long l) {
        return dateAsIso8601(new Date((Long) l));
    }

    public static String dateAsIso8601(Date d) {
        if (d != null) {
            return CreateSimpleDateFormat(DATE_FORMAT_ISO_8601).format(d);
        } else {
            return "";
        }
    }

    public static String dateAsReadable(long l) {
        return dateAsReadable(new Date((Long) l));
    }

    public static String dateAsReadable(Date d) {
        if (d != null) {
            return CreateSimpleDateFormat(DATE_FORMAT_READABLE).format(d);
        } else {
            return "";
        }
    }

    public static Date parseIso8601(String input) throws ParseException {
        if (input.endsWith("Z")) {
            input = input.substring(0, input.length() - 1) + "GMT-00:00";
        } else {
            int inset = 6;

            String s0 = input.substring(0, input.length() - inset);
            String s1 = input.substring(input.length() - inset, input.length());

            input = s0 + "GMT" + s1;
        }

        return CreateSimpleDateFormat(DATE_FORMAT_ISO_8601).parse(input);
    }

    public static Long parseStringToTime(String date) {
        return parseStringToTime(date, DATE_FORMAT_FILENAME);
    }

    public static Long parseStringToTime(String date, String format) {
        SimpleDateFormat df = new SimpleDateFormat(format);
        Long ret = (long) -1;
        try {
            ret = df.parse(date).getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return ret;
    }

    public static Date parseDateAnyFormat(String input) {
        Date d = null;

        // try for long base10
        if (LONG_BASE10_PATTERN.matcher(input).matches()) {
            // sent in seconds, so we must multiply by 1000 to get ms
            d = new Date(Long.parseLong(input) * 1000);
        } else if (LONG_BASE16_PATTERN.matcher(input).matches()) {
            d = new Date(Long.parseLong(input, 16));
        } else {
            try {
                d = parseIso8601(input);
            } catch (ParseException e) {
                try {
                    d = CreateSimpleDateFormat(null).parse(input);
                } catch (ParseException ee) {
                }
            }
        }

        return d;
    }

    /*
     * Taken from http://www.java2s.com/Tutorial/Java/0040__Data-Type/CreateajavautilDateObjectfromaYearMonthDayFormat.htm
     * I can't believe we have to go through this kind of trouble!
     */
    public static Date createDate(int month, int day, int year) throws ParseException {
        String date = year + "/" + month + "/" + day;
        java.util.Date utilDate = null;

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd");
        utilDate = formatter.parse(date);
        System.out.println("utilDate:" + utilDate);

        return utilDate;
    }

    // Returns the number of milliseconds since January 1, 1970, 00:00:00 GMT
    // note that we should just ignore timezone issues, unless we want to do
    // something as crazy as this:
    // http://stackoverflow.com/questions/5584602/determine-timezone-from-latitude-longitude-without-using-web-services-like-geona
    // The reason we don't care is that the video should respect the timezone of the creator, not of the
    // viewer, so we can do this by effectively assuming everything in GMT
    public static Long create(int year, int monthOneBased, int day, int hour, int minute, int second, int millisecond) {
        Calendar c = Calendar.getInstance(TIMEZONE_UTC);

        // http://docs.oracle.com/javase/6/docs/api/java/util/Calendar.html#set%28int,%20int,%20int,%20int,%20int,%20int%29
        c.set(year, monthOneBased - 1, day, hour, minute, second);
        //c.add(millisecond, millisecond);

        // nf idea why ms does't end with 3 zero's...it should, eh???
        Long ms = c.getTimeInMillis();

        // make sure it ends with 3 zero's, ensuring zero ms.
        ms /= 1000;
        ms *= 1000;

        // tack on the ms component.
        ms += millisecond;

        //logger.warn(String.format("y=%d, m=%d, d=%d, h=%d, min=%d, sec=%d, ms=%d, ticks=%d", year, month, day, hour, minute, second, millisecond, ms));

        return ms;
    }

    private static boolean isNumber(char ch) {
        return (ch >= '0' && ch <= '9');
    }

    public static void main(String[] args) {
        Long now = nowAsTicks() / 1000;
        now -= 172800;

        System.out.println(now);
    }
}