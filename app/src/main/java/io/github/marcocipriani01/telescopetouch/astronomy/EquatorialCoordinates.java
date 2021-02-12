/*
 * Copyright 2020 Marco Cipriani (@marcocipriani01) and the Sky Map Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.marcocipriani01.telescopetouch.astronomy;

import android.annotation.SuppressLint;
import android.location.Location;

import androidx.annotation.NonNull;

import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.marcocipriani01.telescopetouch.maths.Formatters;
import io.github.marcocipriani01.telescopetouch.maths.MathsUtils;

import static io.github.marcocipriani01.telescopetouch.maths.MathsUtils.RADIANS_TO_DEGREES;

public class EquatorialCoordinates {

    /**
     * Right ascension in degrees.
     */
    public double ra;
    /**
     * Declination in degrees.
     */
    public double dec;

    public EquatorialCoordinates() {
        this.ra = 0;
        this.dec = 0;
    }

    public EquatorialCoordinates(double ra, double dec) {
        this.ra = ra;
        this.dec = dec;
    }

    public EquatorialCoordinates(String ra, String dec) {
        this.ra = parseRAString(ra.trim());
        this.dec = parseDecString(dec.trim());
    }

    public static EquatorialCoordinates getInstance(HeliocentricCoordinates coords) {
        return new EquatorialCoordinates(
                MathsUtils.mod2pi(Math.atan2(coords.y, coords.x)) * RADIANS_TO_DEGREES,
                Math.atan(coords.z / Math.sqrt(coords.x * coords.x + coords.y * coords.y)) * RADIANS_TO_DEGREES);
    }

    public static EquatorialCoordinates getInstance(GeocentricCoordinates coords) {
        double raRad = Math.atan2(coords.y, coords.x);
        if (raRad < 0) raRad += 2f * Math.PI;
        double decRad = Math.atan2(coords.z, Math.sqrt(coords.x * coords.x + coords.y * coords.y));
        return new EquatorialCoordinates(raRad * RADIANS_TO_DEGREES, decRad * RADIANS_TO_DEGREES);
    }

    /**
     * Converts the sexagesimal degrees contained in the input string into degrees (ie. "01 02 03.4" → (1+2/60+3.4/3600)*15°)
     *
     * @param string an input string (right ascension)
     * @return the right ascension converted in decimal degrees.
     */
    private static double parseRAString(String string) throws NumberFormatException, NullPointerException {
        Pattern p = Pattern.compile("([0-9]{1,2})[h:\\s]([0-9]{1,2})([m:'\\s]([0-9]{1,2})([,.]([0-9]*))?[s\"]?)?[m:'\\s]?");
        Matcher m = p.matcher(string);
        double value = 0;
        if (m.matches()) {
            if (m.group(6) != null) {
                for (int i = 0; i < m.group(6).length(); ++i) {
                    value += 15. / 3600. * (m.group(6).charAt(i) - '0') * Math.pow(0.1, i + 1);
                }
            }
            if (m.group(4) != null) {
                for (int i = 0; i < m.group(4).length(); ++i) {
                    value += 15. / 3600. * (m.group(4).charAt(i) - '0') * Math.pow(10, m.group(4).length() - i - 1);
                }
            }
            if (m.group(2) != null) {
                for (int i = 0; i < m.group(2).length(); ++i) {
                    value += 15. / 60. * (m.group(2).charAt(i) - '0') * Math.pow(10, m.group(2).length() - i - 1);
                }
            }
            if (m.group(1) != null) {
                for (int i = 0; i < m.group(1).length(); ++i) {
                    value += 15. * (m.group(1).charAt(i) - '0') * Math.pow(10, m.group(1).length() - i - 1);
                }
            }
            return value;
        } else {
            throw new NumberFormatException(string + " is not a valid sexagesimal string");
        }
    }

    /**
     * Converts the sexagesimal degrees contained in the input string into degrees (ie. "01 02 03.4" → (1+2/60+3.4/3600)*15°)
     *
     * @param string an input string (declination)
     * @return the declination converted in decimal degrees.
     */
    private static double parseDecString(String string) throws NumberFormatException, NullPointerException {
        Pattern p = Pattern.compile("([+\\-]?)([0-9]{1,2})[°:\\s]([0-9]{1,2})([m:'\\s]([0-9]{1,2})([,.]([0-9]*))?[s\"]?)?[m:'\\s]?");
        Matcher m = p.matcher(string);
        double value = 0;
        if (m.matches()) {
            if (m.group(7) != null) {
                for (int i = 0; i < m.group(7).length(); ++i) {
                    value += 1. / 3600. * (m.group(7).charAt(i) - '0') * Math.pow(0.1, i + 1);
                }
            }
            if (m.group(5) != null) {
                for (int i = 0; i < m.group(5).length(); ++i) {
                    value += 1. / 3600. * (m.group(5).charAt(i) - '0') * Math.pow(10, m.group(5).length() - i - 1);
                }
            }
            if (m.group(3) != null) {
                for (int i = 0; i < m.group(3).length(); ++i) {
                    value += 1. / 60. * (m.group(3).charAt(i) - '0') * Math.pow(10, m.group(3).length() - i - 1);
                }
            }
            if (m.group(2) != null) {
                for (int i = 0; i < m.group(2).length(); ++i) {
                    value += (m.group(2).charAt(i) - '0') * Math.pow(10, m.group(2).length() - i - 1);
                }
            }
            if (m.group(1) != null) {
                if (m.group(1).equals("-")) {
                    value = -value;
                }
            }
            return value;
        } else {
            throw new NumberFormatException(string + " is not a valid sexagesimal string");
        }
    }

    /**
     * Compute celestial coordinates of zenith from utc, lat long.
     */
    public static EquatorialCoordinates ofZenith(Calendar utc, Location location) {
        return new EquatorialCoordinates(TimeUtils.meanSiderealTime(utc, location.getLongitude()), location.getLatitude());
    }

    /**
     * @return a string containing the right ascension (hh:mm:ss)
     */
    public String getRAString() {
        return Formatters.formatDegreesAsHours(ra);
    }

    @SuppressLint("DefaultLocale")
    public String getRATelescopeFormat() {
        double hours = Math.abs(ra / 15.0);
        int deg = (int) Math.floor(hours);
        double tmp = (hours - deg) * 60.0;
        int min = (int) Math.floor(tmp);
        int sec = (int) Math.round((tmp - min) * 60.0);
        if (Math.signum(ra) >= 0) {
            return String.format("%02d:%02d:%02d", deg, min, sec);
        } else {
            return String.format("-%02d:%02d:%02d", deg, min, sec);
        }
    }

    @SuppressLint("DefaultLocale")
    public String getDecTelescopeFormat() {
        double hours = Math.abs(dec);
        int deg = (int) Math.floor(hours);
        double tmp = (hours - deg) * 60.0;
        int min = (int) Math.floor(tmp);
        int sec = (int) Math.round((tmp - min) * 60.0);
        if (Math.signum(dec) >= 0) {
            return String.format("%02d:%02d:%02d", deg, min, sec);
        } else {
            return String.format("-%02d:%02d:%02d", deg, min, sec);
        }
    }

    /**
     * @return a string containing the declination (hh:mm:ss)
     */
    public String getDecString() {
        return Formatters.formatDegrees(dec);
    }

    @NonNull
    @Override
    public String toString() {
        return "RA: " + getRAString() + ", Dec: " + getDecString();
    }

    @SuppressLint("DefaultLocale")
    public String toStringArcmin() {
        return "RA: " + Formatters.formatHoursArcmin(ra / 15.0) + ", Dec: " + Formatters.formatDegreesArcmin(dec);
    }
}