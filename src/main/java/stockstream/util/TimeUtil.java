package stockstream.util;

import lombok.extern.slf4j.Slf4j;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
public class TimeUtil {

    public static Optional<Date> createDateFromStr(final String format, final String strDate) {
        DateFormat dateFormat = new SimpleDateFormat(format, Locale.ENGLISH);

        try {
            return Optional.of(dateFormat.parse(strDate));
        } catch (ParseException e) {
            log.warn(e.getMessage(), e);
        }

        return Optional.empty();
    }

    public static String createStrFromDate(final Date date, final String format) {
        final SimpleDateFormat sdf = new SimpleDateFormat(format);
        final String dateStr = sdf.format(date);
        return dateStr;
    }

    public static Optional<Date> createDateFromStr(final String format, final String strDate, final String timezone) {
        DateFormat dateFormat = new SimpleDateFormat(format, Locale.ENGLISH);
        dateFormat.setTimeZone(TimeZone.getTimeZone(timezone));

        try {
            return Optional.of(dateFormat.parse(strDate));
        } catch (ParseException e) {
            log.warn(e.getMessage(), e);
        }

        return Optional.empty();
    }

    public static Optional<String> getCanonicalYMDString(final String format, final String strDate, final String timezone) {
        final Optional<Date> dateOptional = createDateFromStr(format, strDate, timezone);
        if (!dateOptional.isPresent()) {
            return Optional.empty();
        }

        final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        return Optional.of(dateFormat.format(dateOptional.get()));
    }

    public static String getCanonicalYMDString(final Date forDate) {
        final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        final String dateStr = dateFormat.format(forDate);
        return dateStr;
    }

    public static String getCanonicalMDYString(final Date forDate) {
        final DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
        final String dateStr = dateFormat.format(forDate);
        return dateStr;
    }

    public static Date getStartOfToday() {
        Calendar date = new GregorianCalendar();
        date.set(Calendar.HOUR_OF_DAY, 0);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);
        return date.getTime();
    }

}
