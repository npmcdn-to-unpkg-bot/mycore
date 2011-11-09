package org.mycore.datamodel.common;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

/**
 * is a helper class for MCRMetaISO8601Date.
 * 
 * Please be aware that this class is not supported. It may disappear some day or methods get removed.
 * 
 * @author Thomas Scheffler (yagee)
 *
 * @version $Revision: 18729 $ $Date: 2010-09-21 12:33:45 +0200 (Di, 21. Sep 2010) $
 * @since 1.3
 */
public final class MCRISO8601FormatChooser {

    public final static DateTimeFormatter YEAR_FORMAT = ISODateTimeFormat.year();

    public final static DateTimeFormatter YEAR_MONTH_FORMAT = ISODateTimeFormat.yearMonth();

    public final static DateTimeFormatter COMPLETE_FORMAT = ISODateTimeFormat.date();

    public final static DateTimeFormatter COMPLETE_HH_MM_FORMAT = ISODateTimeFormat.dateHourMinute();

    public final static DateTimeFormatter COMPLETE_HH_MM_SS_FORMAT = ISODateTimeFormat.dateTimeNoMillis();

    public final static DateTimeFormatter COMPLETE_HH_MM_SS_SSS_FORMAT = ISODateTimeFormat.dateTime();

    public final static DateTimeFormatter UTC_YEAR_FORMAT = ISODateTimeFormat.year().withZone(DateTimeZone.UTC);

    public final static DateTimeFormatter UTC_YEAR_MONTH_FORMAT = ISODateTimeFormat.yearMonth().withZone(DateTimeZone.UTC);

    public final static DateTimeFormatter UTC_COMPLETE_FORMAT = ISODateTimeFormat.date().withZone(DateTimeZone.UTC);

    public final static DateTimeFormatter UTC_COMPLETE_HH_MM_FORMAT = ISODateTimeFormat.dateHourMinute().withZone(DateTimeZone.UTC);

    public final static DateTimeFormatter UTC_COMPLETE_HH_MM_SS_FORMAT = ISODateTimeFormat.dateTimeNoMillis().withZone(DateTimeZone.UTC);

    public final static DateTimeFormatter UTC_COMPLETE_HH_MM_SS_SSS_FORMAT = ISODateTimeFormat.dateTime().withZone(DateTimeZone.UTC);

    private static final Pattern MILLI_CHECK_PATTERN = Pattern.compile("\\.\\d{4,}\\+");

    private static final boolean USE_UTC = true;

    /**
     * returns a DateTimeFormatter for the given isoString or format.
     * 
     * This method prefers the format parameter. So if it's not null or not
     * zero length this method will interpret the format string. You can
     * also get a formatter for e specific iso String. In either case if the
     * underlying algorithm can not determine an exact matching formatter it
     * will allway fall back to a default. So this method will never return
     * null.
     * 
     * @param isoString
     *            an ISO 8601 formatted time String, or null
     * @param isoFormat
     *            a valid format String, or null
     * @return returns a specific DateTimeFormatter
     */
    public static DateTimeFormatter getFormatter(String isoString, MCRISO8601Format isoFormat) {
        DateTimeFormatter df;
        if (isoFormat != null) {
            df = getFormatterForFormat(isoFormat);
        } else if (isoString != null && isoString.length() != 0) {
            String normalized = isoString.charAt(0) == '-' ? isoString.substring(1) : isoString;
            df = getFormatterForDuration(normalized);
        } else {
            df = COMPLETE_HH_MM_SS_SSS_FORMAT;
        }
        if (USE_UTC) {
            df = df.withZone(DateTimeZone.UTC);
        }
        return df;
    }

    private static DateTimeFormatter getFormatterForFormat(MCRISO8601Format isoFormat) {
        switch (isoFormat) {
        case YEAR:
            return USE_UTC ? UTC_YEAR_FORMAT : YEAR_FORMAT;
        case YEAR_MONTH:
            return USE_UTC ? UTC_YEAR_MONTH_FORMAT : YEAR_MONTH_FORMAT;
        case COMPLETE:
            return USE_UTC ? UTC_COMPLETE_FORMAT : COMPLETE_FORMAT;
        case COMPLETE_HH_MM:
            return USE_UTC ? UTC_COMPLETE_HH_MM_FORMAT : COMPLETE_HH_MM_FORMAT;
        case COMPLETE_HH_MM_SS:
            return USE_UTC ? UTC_COMPLETE_HH_MM_SS_FORMAT : COMPLETE_HH_MM_SS_FORMAT;
        case COMPLETE_HH_MM_SS_SSS:
            return USE_UTC ? UTC_COMPLETE_HH_MM_SS_SSS_FORMAT : COMPLETE_HH_MM_SS_SSS_FORMAT;
        default:
            return USE_UTC ? UTC_COMPLETE_HH_MM_SS_SSS_FORMAT : COMPLETE_HH_MM_SS_SSS_FORMAT;
        }
    }

    private static DateTimeFormatter getFormatterForDuration(String isoString) {
        boolean test = false;
        switch (isoString.length()) {
        case 1:
        case 2:
        case 3:
            return USE_UTC ? UTC_YEAR_FORMAT : YEAR_FORMAT;
        case 4:
            if (isoString.indexOf('-') == -1) {
                return USE_UTC ? UTC_YEAR_FORMAT : YEAR_FORMAT;
            }
        case 5:
        case 6:
        case 7:
            return USE_UTC ? UTC_YEAR_MONTH_FORMAT : YEAR_MONTH_FORMAT;
        case 10:
            return USE_UTC ? UTC_COMPLETE_FORMAT : COMPLETE_FORMAT;
        case 17: // YYYY-MM-DDThh:mm'Z'
            test = true;
        case 22:
            if (test || !isoString.endsWith("Z")) {
                // YYYY-MM-DDThh:mm[+-]hh:mm
                return USE_UTC ? UTC_COMPLETE_HH_MM_FORMAT : COMPLETE_HH_MM_FORMAT;
            }
            // YYYY-MM-DDThh:mm:ss.s'Z'
            return USE_UTC ? UTC_COMPLETE_HH_MM_SS_SSS_FORMAT : COMPLETE_HH_MM_SS_SSS_FORMAT;
        case 20: // YYYY-MM-DDThh:mm:ss'Z'
        case 25: // YYYY-MM-DDThh:mm:ss[+-]hh:mm
            return USE_UTC ? UTC_COMPLETE_HH_MM_SS_FORMAT : COMPLETE_HH_MM_SS_FORMAT;
        case 23: // YYYY-MM-DDThh:mm:ss.ss'Z'
        case 24: // YYYY-MM-DDThh:mm:ss.sss'Z'
        case 27: // YYYY-MM-DDThh:mm:ss.s[+-]hh:mm
        case 28: // YYYY-MM-DDThh:mm:ss.ss[+-]hh:mm
        case 29: // YYYY-MM-DDThh:mm:ss.ss[+-]hh:mm
            return USE_UTC ? UTC_COMPLETE_HH_MM_SS_SSS_FORMAT : COMPLETE_HH_MM_SS_SSS_FORMAT;
        default:
            return USE_UTC ? UTC_COMPLETE_HH_MM_SS_SSS_FORMAT : COMPLETE_HH_MM_SS_SSS_FORMAT;
        }
    }

    /**
     * returns a String that has not more than 3 digits representing
     * "fractions of a second".
     * 
     * If isoString has no or not more than 3 digits this method simply
     * returns isoString.
     * 
     * @param isoString
     *            an ISO 8601 formatted time String
     * @return an ISO 8601 formatted time String with at max 3 digits for
     *         fractions of a second
     */
    public final static String cropSecondFractions(String isoString) {
        Matcher matcher = MILLI_CHECK_PATTERN.matcher(isoString);
        boolean result = matcher.find();
        if (result) {
            return matcher.replaceFirst(isoString.substring(matcher.start(), matcher.start() + 4) + "+");
        }
        return isoString;
    }

}