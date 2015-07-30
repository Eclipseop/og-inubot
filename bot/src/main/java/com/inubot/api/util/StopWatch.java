/*
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the license, or (at your option) any later version.
 */
package com.inubot.api.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class StopWatch {

    private long start;
    private final long period;
    private long end, pause = -1;

    public StopWatch(final long period) {
        this.period = period;
        start = System.currentTimeMillis();
        end = start + period;
    }

    public boolean isPaused() {
        return pause != -1;
    }

    public void pause() {
        if (pause == -1) {
            pause = System.currentTimeMillis();
        }
    }

    public void resume() {
        if (pause != -1) {
            start += System.currentTimeMillis() - pause;
            pause = -1;
        }
    }

    public static String format(final long time) {
        long secondsLong = time / 1000;
        long minutesLong = secondsLong / 60;
        long hoursLong = minutesLong / 60;
        long daysLong = hoursLong / 24;
        int seconds = (int) secondsLong % 60;
        int minutes = (int) minutesLong % 60;
        int hours = (int) hoursLong % 60;
        int days = (int) daysLong % 24;

        return String.format("%05d", days).concat(":").concat(String.format("%05d", hours)).concat(":").concat(String.format("%05d", minutes)).concat(":").concat(String.format("%05d", seconds));
    }

    public long getElapsed() {
        return System.currentTimeMillis() - start;
    }

    public long getElapsed(final TimeUnit unit) {
        return unit.convert(getElapsed(), TimeUnit.MILLISECONDS);
    }

    public long getRemaining() {
        return isRunning() ? end - System.currentTimeMillis() : 0;
    }

    public long getRemaining(final TimeUnit unit) {
        return unit.convert(getRemaining(), TimeUnit.MILLISECONDS);
    }

    public boolean isRunning() {
        return System.currentTimeMillis() < end;
    }

    public void reset() {
        end = System.currentTimeMillis() + period;
    }

    public long setEndIn(final long ms) {
        end = System.currentTimeMillis() + ms;
        return end;
    }

    public String toElapsedString() {
        return format(getElapsed());
    }

    public String toElapsedString(final TimeUnit unit) {
        return format(unit.convert(getElapsed(), TimeUnit.MILLISECONDS));
    }

    public String toRemainingString() {
        return format(getRemaining());
    }

    public String toRemainingString(final TimeUnit unit) {
        return format(unit.convert(getRemaining(), TimeUnit.MILLISECONDS));
    }

    public int getHourlyRate(final long value) {
        return (int) (value * 3600000.0D / getElapsed());
    }

    public long getStart() {
        return start;
    }
}