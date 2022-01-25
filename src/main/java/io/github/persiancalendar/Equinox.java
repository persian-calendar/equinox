package io.github.persiancalendar;

// Adopted from https://github.com/xyproto/calendar/blob/master/equinox.go
//// Equinox and solstice algorithms from Astronomical Algorithms by Jean Meeus
//// Adapted from http://jgiesen.de/astro/astroJS/seasons/seasons.js
////// Equinoxes and Solstices
////// algorithm from Meeus
////// adapted by Juergen Giesen
////// 6.5.2012
// Its API is improved by ideas from https://github.com/MenoData/Time4J/blob/78cd60d6/base/src/main/java/net/time4j/calendar/astro/AstronomicalSeason.java

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public enum Equinox {
    /**
     * Spring equinox for the northern hemisphere in March (or Fall on the southern hemisphere).
     */
    NORTHWARD_EQUINOX,
    /**
     * Summer solstice for the northern hemisphere in June (or Winter on the southern hemisphere).
     */
    NORTHERN_SOLSTICE,
    /**
     * Fall equinox for the northern hemisphere in September (or Spring on the southern hemisphere).
     */
    SOUTHWARD_EQUINOX,
    /**
     * Winter solstice for the northern hemisphere in December (or Summer on the southern hemisphere).
     */
    SOUTHERN_SOLSTICE;

    // Meeus - Astronomical Algorithms - p179, Table 27.C
    private final static double[][] equinoxTerms = {
            {485, 324.96, 1934.136},
            {203, 337.23, 32964.467},
            {199, 342.08, 20.186},
            {182, 27.85, 445267.112},
            {156, 73.14, 45036.886},
            {136, 171.52, 22518.443},
            {77, 222.54, 65928.934},
            {74, 296.72, 3034.906},
            {70, 243.58, 9037.513},
            {58, 119.81, 33718.147},
            {52, 297.17, 150.678},
            {50, 21.02, 2281.226},
            {45, 247.54, 29929.562},
            {44, 325.15, 31555.956},
            {29, 60.93, 4443.417},
            {18, 155.12, 67555.328},
            {17, 288.79, 4562.452},
            {16, 198.04, 62894.029},
            {14, 199.76, 31436.921},
            {12, 95.39, 14577.848},
            {12, 287.11, 31931.756},
            {12, 320.81, 34777.259},
            {9, 227.73, 1222.114},
            {8, 15.45, 16859.074}
    };

    private static double periodic24(double x) {
        double result = 0;
        for (double[] term : equinoxTerms)
            result += term[0] * Math.cos(Math.PI / 180d * (term[1] + x * term[2]));
        return result;
    }

    private double jdMean(int year) {
        // Meeus - Astronomical Algorithms - p178, Table 27.B
        final double y = (year - 2000) / 1000.0;
        switch (this) {
            case NORTHWARD_EQUINOX:
                return 2451623.80984 + (365242.37404 + (.05169 + (-.00411 - .00057 * y) * y) * y) * y;
            case NORTHERN_SOLSTICE:
                return 2451716.56767 + (365241.62603 + (.00325 + (.00888 - .00030 * y) * y) * y) * y;
            case SOUTHWARD_EQUINOX:
                return 2451810.21715 + (365242.01767 + (-.11575 + (.00337 + .00078 * y) * y) * y) * y;
            default:
            case SOUTHERN_SOLSTICE:
                return 2451900.05952 + (365242.74049 + (-.06223 + (-.00823 + .00032 * y) * y) * y) * y;
        }
    }

    public Date inYear(int year) {
        final double a = jdMean(year);
        final double b = (a - 2451545) / 36525d;
        final double c = (35999.373 * b - 2.47) * Math.PI / 180d;
        final double d = a + (.00001 * periodic24(b)) / (1 + .0334 * Math.cos(c) + .0007 * Math.cos(2 * c))
                - (66 + year - 2000) / 86400d;
        final double e = Math.round(d);
        final double f = Math.floor((e - 1867216.25) / 36524.25);
        final double g = e + f - Math.floor(f / 4) + 1525d;
        final double h = Math.floor((g - 122.1) / 365.25);
        final double i = 365 * h + Math.floor(h / 4);
        final double k = Math.floor((g - i) / 30.6001);
        final double l = 24 * (d + .5 - e);
        final int day = (int) (Math.round(g - i) - Math.floor(30.6001 * k));
        final double month = k - 1 - 12 * Math.floor(k / 14);
        final int millisInDay = (int) Math.round(l * 60 * 60 * 1000);

        final GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        calendar.set(GregorianCalendar.YEAR, year);
        calendar.set(GregorianCalendar.MONTH, (int) month - 1);
        calendar.set(GregorianCalendar.DAY_OF_MONTH, day);
        calendar.set(GregorianCalendar.HOUR_OF_DAY, 0);
        calendar.set(GregorianCalendar.MINUTE, 0);
        calendar.set(GregorianCalendar.SECOND, 0);
        calendar.set(GregorianCalendar.MILLISECOND, 0);
        calendar.add(GregorianCalendar.MILLISECOND, millisInDay);
        return calendar.getTime();
    }
}
