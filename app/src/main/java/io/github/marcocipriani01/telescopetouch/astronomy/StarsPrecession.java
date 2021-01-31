package io.github.marcocipriani01.telescopetouch.astronomy;

import java.util.Calendar;

import io.github.marcocipriani01.telescopetouch.catalog.CatalogCoordinates;

public class StarsPrecession {

    private StarsPrecession() {
    }

    public static double[] precess(Calendar calendar, double ra, double dec) {
        double x = cos(dec) * cos(ra),
                y = cos(dec) * sin(ra),
                z = sin(dec),
                d1 = ((((double) calendar.get(Calendar.YEAR)) + (((double) calendar.get(Calendar.DAY_OF_YEAR)) / 365.0)) - 2000.0) / 100.0,
                d2 = (((((0.017998 * d1) + 0.30188) * d1) + 2306.2181) * d1) / 3600.0,
                d3 = (((((2.05E-4 * d1) + 0.7928) * d1) * d1) / 3600.0) + d2,
                d4 = ((2004.3109 - (((0.041833 * d1) + 0.42665) * d1)) * d1) / 3600.0,
                cosD2 = cos(d2), cosD3 = cos(d3), cosD4 = cos(d4),
                sinD2 = sin(d2), sinD3 = sin(d3), sinD4 = sin(d4),
                d6 = cosD3 * cosD4, d7 = sinD3 * cosD4;
        x = (((-sinD3 * sinD2) + (d6 * cosD2)) * x) + (((-sinD3 * cosD2) - (d6 * sinD2)) * y) + (((-cosD3) * sinD4) * z);
        y = (((cosD3 * sinD2) + (d7 * cosD2)) * x) + (((cosD3 * cosD2) - (d7 * sinD2)) * y) + ((-sinD3 * sinD4) * z);
        z = ((cosD2 * sinD4) * x) + (((-sinD4) * sinD2) * y) + (cosD4 * z);
        return new double[]{atan2(y, x), atan(z / Math.sqrt((x * x) + (y * y)))};
    }

    public static CatalogCoordinates precess(Calendar calendar, CatalogCoordinates in) {
        double[] precession = precess(calendar, in.getRa(), in.getDec());
        return new CatalogCoordinates(precession[0], precession[1]);
    }

    private static double sin(double d) {
        return Math.sin(d / 180.0 * Math.PI);
    }

    private static double cos(double d) {
        return Math.cos(d / 180.0 * Math.PI);
    }

    private static double atan(double d) {
        return Math.atan(d) * 180.0 / Math.PI;
    }

    private static double atan2(double y, double x) {
        double degrees = Math.atan2(y, x) * 180.0 / Math.PI;
        return (degrees < 0.0) ? (degrees + 360.0) : degrees;
    }
}