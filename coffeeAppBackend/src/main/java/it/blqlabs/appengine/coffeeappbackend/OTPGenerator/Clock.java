package it.blqlabs.appengine.coffeeappbackend.OTPGenerator;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * Created by davide on 14/01/15.
 */
public class Clock {
    private final int interval;
    private Calendar calendar;

    public Clock() {
        interval = 30;
    }

    public Clock(int interval) {
        this.interval = interval;
    }

    public long getCurrentSecond() {
        calendar = GregorianCalendar.getInstance(TimeZone.getTimeZone("UTC"));
        return calendar.getTimeInMillis() / 1000;
    }

    public long getCurrentInterval() {
        calendar = GregorianCalendar.getInstance(TimeZone.getTimeZone("UTC"));
        long currentTimeSeconds = calendar.getTimeInMillis() / 1000;
        //Log.d("tempo:", String.valueOf(currentTimeSeconds));
        return currentTimeSeconds / interval;
        //return 1410880526 / interval;
    }
}
