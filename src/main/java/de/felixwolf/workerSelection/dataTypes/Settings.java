package de.felixwolf.workerSelection.dataTypes;

import java.util.TimeZone;

public class Settings {

    private static TimeZone timeZone = TimeZone.getTimeZone("Europe/Berlin");

    public static TimeZone getTimeZone() {
        return timeZone;
    }
}
