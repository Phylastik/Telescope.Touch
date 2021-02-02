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

import android.location.Location;
import android.util.Log;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import io.github.marcocipriani01.telescopetouch.R;
import io.github.marcocipriani01.telescopetouch.TelescopeTouchApp;
import io.github.marcocipriani01.telescopetouch.base.VisibleForTesting;
import io.github.marcocipriani01.telescopetouch.units.GeocentricCoordinates;
import io.github.marcocipriani01.telescopetouch.units.HeliocentricCoordinates;
import io.github.marcocipriani01.telescopetouch.units.RaDec;
import io.github.marcocipriani01.telescopetouch.util.MathsUtils;
import io.github.marcocipriani01.telescopetouch.util.TimeUtils;

public enum Planet {

    // The order here is the order in which they are drawn.  To ensure that during
    // conjunctions they display "naturally" order them in reverse distance from Earth.
    Pluto(R.drawable.pluto, R.string.pluto, TimeUtils.MILLISECONDS_PER_HOUR),
    Neptune(R.drawable.neptune, R.string.neptune, TimeUtils.MILLISECONDS_PER_HOUR),
    Uranus(R.drawable.uranus, R.string.uranus, TimeUtils.MILLISECONDS_PER_HOUR),
    Saturn(R.drawable.saturn, R.string.saturn, TimeUtils.MILLISECONDS_PER_HOUR),
    Jupiter(R.drawable.jupiter, R.string.jupiter, TimeUtils.MILLISECONDS_PER_HOUR),
    Mars(R.drawable.mars, R.string.mars, TimeUtils.MILLISECONDS_PER_HOUR),
    Sun(R.drawable.sun, R.string.sun, TimeUtils.MILLISECONDS_PER_HOUR),
    Mercury(R.drawable.mercury, R.string.mercury, TimeUtils.MILLISECONDS_PER_HOUR),
    Venus(R.drawable.venus, R.string.venus, TimeUtils.MILLISECONDS_PER_HOUR),
    Moon(R.drawable.moon4, R.string.moon, TimeUtils.MILLISECONDS_PER_MINUTE);

    private static final String TAG = TelescopeTouchApp.getTag(Planet.class);
    // Maximum number of times to calculate rise/set times. If we cannot
    // converge after this many iteretions, we will fail.
    private final static int MAX_ITERATIONS = 25;
    private final long updateFreqMs;
    // Resource ID to use for a planet's image.
    private final int imageResourceId;
    // String ID
    private final int nameResourceId;

    Planet(int imageResourceId, int nameResourceId, long updateFreqMs) {
        this.imageResourceId = imageResourceId;
        this.nameResourceId = nameResourceId;
        this.updateFreqMs = updateFreqMs;
        // Add Color, magnitude, etc.
    }

    /**
     * Calculate the geocentric right ascension and declination of the moon using
     * an approximation as described on page D22 of the 2008 Astronomical Almanac
     * All of the variables in this method use the same names as those described
     * in the text: lambda = Ecliptic longitude (degrees) beta = Ecliptic latitude
     * (degrees) pi = horizontal parallax (degrees) r = distance (Earth radii)
     * <p>
     * NOTE: The text does not give a specific time period where the approximation
     * is valid, but it should be valid through at least 2009.
     */
    public static RaDec calculateLunarGeocentricLocation(Calendar time) {
        // First, calculate the number of Julian centuries from J2000.0.
        float t = (float) TimeUtils.julianDayToCentury(TimeUtils.julianDayGreenwich(time));

        // Second, calculate the approximate geocentric orbital elements.
        float lambda =
                218.32f + 481267.881f * t + 6.29f
                        * (float) Math.sin((135.0f + 477198.87f * t) * MathsUtils.DEGREES_TO_RADIANS) - 1.27f
                        * (float) Math.sin((259.3f - 413335.36f * t) * MathsUtils.DEGREES_TO_RADIANS) + 0.66f
                        * (float) Math.sin((235.7f + 890534.22f * t) * MathsUtils.DEGREES_TO_RADIANS) + 0.21f
                        * (float) Math.sin((269.9f + 954397.74f * t) * MathsUtils.DEGREES_TO_RADIANS) - 0.19f
                        * (float) Math.sin((357.5f + 35999.05f * t) * MathsUtils.DEGREES_TO_RADIANS) - 0.11f
                        * (float) Math.sin((186.5f + 966404.03f * t) * MathsUtils.DEGREES_TO_RADIANS);
        float beta =
                5.13f * (float) Math.sin((93.3f + 483202.02f * t) * MathsUtils.DEGREES_TO_RADIANS) + 0.28f
                        * (float) Math.sin((228.2f + 960400.89f * t) * MathsUtils.DEGREES_TO_RADIANS) - 0.28f
                        * (float) Math.sin((318.3f + 6003.15f * t) * MathsUtils.DEGREES_TO_RADIANS) - 0.17f
                        * (float) Math.sin((217.6f - 407332.21f * t) * MathsUtils.DEGREES_TO_RADIANS);
        //float pi =
        //    0.9508f + 0.0518f * MathUtil.cos((135.0f + 477198.87f * t) * Geometry.DEGREES_TO_RADIANS)
        //        + 0.0095f * MathUtil.cos((259.3f - 413335.36f * t) * Geometry.DEGREES_TO_RADIANS)
        //        + 0.0078f * MathUtil.cos((235.7f + 890534.22f * t) * Geometry.DEGREES_TO_RADIANS)
        //        + 0.0028f * MathUtil.cos((269.9f + 954397.74f * t) * Geometry.DEGREES_TO_RADIANS);
        // float r = 1.0f / MathUtil.sin(pi * Geometry.DEGREES_TO_RADIANS);

        // Third, convert to RA and Dec.
        float l =
                (float) Math.cos(beta * MathsUtils.DEGREES_TO_RADIANS)
                        * (float) Math.cos(lambda * MathsUtils.DEGREES_TO_RADIANS);
        float m =
                0.9175f * (float) Math.cos(beta * MathsUtils.DEGREES_TO_RADIANS)
                        * (float) Math.sin(lambda * MathsUtils.DEGREES_TO_RADIANS) - 0.3978f
                        * (float) Math.sin(beta * MathsUtils.DEGREES_TO_RADIANS);
        float n =
                0.3978f * (float) Math.cos(beta * MathsUtils.DEGREES_TO_RADIANS)
                        * (float) Math.sin(lambda * MathsUtils.DEGREES_TO_RADIANS) + 0.9175f
                        * (float) Math.sin(beta * MathsUtils.DEGREES_TO_RADIANS);
        float ra = MathsUtils.mod2pi((float) Math.atan2(m, l)) * MathsUtils.RADIANS_TO_DEGREES;
        float dec = (float) Math.asin(n) * MathsUtils.RADIANS_TO_DEGREES;

        return new RaDec(ra, dec);
    }

    /**
     * Return the date of the next full moon after today.
     */
    // TODO(serafini): This could also be error prone right around the time
    // of the full and new moons...
    public static Calendar getNextFullMoon(Calendar now) {
        // First, get the moon's current phase.
        float phase = Moon.calculatePhaseAngle(now);

        // Next, figure out if the moon is waxing or waning.
        Calendar later = (Calendar) now.clone();
        later.add(Calendar.HOUR, 1);
        float phase2 = Moon.calculatePhaseAngle(later);
        boolean isWaxing = phase2 > phase;

        // If moon is waxing, next full moon is (180.0 - phase)/360.0 * 29.53.
        // If moon is waning, next full moon is (360.0 - phase)/360.0 * 29.53.
        final float LUNAR_CYCLE = 29.53f;  // In days.
        float baseAngle = (isWaxing ? 180.0f : 360.0f);
        float numDays = (baseAngle - phase) / 360.0f * LUNAR_CYCLE;

        Calendar res = Calendar.getInstance();
        res.setTimeInMillis(now.getTimeInMillis() + (long) (numDays * 24.0 * 3600.0 * 1000.0));
        return res;
    }

    // Calculates the hour angle of a given declination for the given location.
    // This is a helper application for the rise and set calculations. Its
    // probably not worth using as a general purpose method.
    // All values are in degrees.
    //
    // This method calculates the hour angle from the meridian using the
    // following equation from the Astronomical Almanac (p487):
    // cos ha = (sin alt - sin lat * sin dec) / (cos lat * cos dec)
    public static float calculateHourAngle(float altitude, float latitude,
                                           float declination) {
        float altRads = altitude * ((float) Math.PI / 180);
        float latRads = latitude * ((float) Math.PI / 180);
        float decRads = declination * ((float) Math.PI / 180);
        float cosHa = ((float) Math.sin(altRads) - (float) Math.sin(latRads) * (float) Math.sin(decRads)) /
                ((float) Math.cos(latRads) * (float) Math.cos(decRads));

        return 180 / (float) Math.PI * (float) Math.acos(cosHa);
    }


    // TODO(serafini): We need to correct the Ra/Dec for the user's location. The
    // current calculation is probably accurate to a degree or two, but we can,
    // and should, do better.

    public long getUpdateFrequencyMs() {
        return updateFreqMs;
    }

    /**
     * Returns the resource id for the string corresponding to the name of this
     * planet.
     */
    public int getNameResourceId() {
        return nameResourceId;
    }

    /**
     * Returns the resource id for the planet's image.
     */
    public int getImageResourceId(Calendar time) {
        if (this.equals(Planet.Moon)) {
            return getLunarPhaseImageId(time);
        }
        return this.imageResourceId;
    }

    /**
     * Determine the Moon's phase and return the resource ID of the correct
     * image.
     */
    private int getLunarPhaseImageId(Calendar time) {
        // First, calculate phase angle:
        float phase = calculatePhaseAngle(time);
        Log.d(TAG, "Lunar phase = " + phase);

        // Next, figure out what resource id to return.
        if (phase < 22.5f) {
            // New moon.
            return R.drawable.moon0;
        } else if (phase > 150.0f) {
            // Full moon.
            return R.drawable.moon4;
        }

        // Either crescent, quarter, or gibbous. Need to see whether we are
        // waxing or waning. Calculate the phase angle one day in the future.
        // If phase is increasing, we are waxing. If not, we are waning.
        Calendar tomorrow = (Calendar) time.clone();
        tomorrow.add(Calendar.DATE, 1);
        float phase2 = calculatePhaseAngle(tomorrow);
        Log.d(TAG, "Tomorrow's phase = " + phase2);

        if (phase < 67.5f) {
            // Crescent
            return (phase2 > phase) ? R.drawable.moon1 : R.drawable.moon7;
        } else if (phase < 112.5f) {
            // Quarter
            return (phase2 > phase) ? R.drawable.moon2 : R.drawable.moon6;
        }

        // Gibbous
        return (phase2 > phase) ? R.drawable.moon3 : R.drawable.moon5;
    }

    // Taken from JPL's Planetary Positions page: http://ssd.jpl.nasa.gov/?planet_pos
    // This gives us a good approximation for the years 1800 to 2050 AD.
    // TODO(serafini): Update the numbers so we can extend the approximation to cover
    // 3000 BC to 3000 AD.
    public OrbitalElements getOrbitalElements(Calendar date) {
        // Centuries since J2000
        float jc = (float) TimeUtils.julianDayToCentury(TimeUtils.julianDayGreenwich(date));

        switch (this) {
            case Mercury: {
                float a = 0.38709927f + 0.00000037f * jc;
                float e = 0.20563593f + 0.00001906f * jc;
                float i = (7.00497902f - 0.00594749f * jc) * MathsUtils.DEGREES_TO_RADIANS;
                float l =
                        MathsUtils.mod2pi((252.25032350f + 149472.67411175f * jc) * MathsUtils.DEGREES_TO_RADIANS);
                float w = (77.45779628f + 0.16047689f * jc) * MathsUtils.DEGREES_TO_RADIANS;
                float o = (48.33076593f - 0.12534081f * jc) * MathsUtils.DEGREES_TO_RADIANS;
                return new OrbitalElements(a, e, i, o, w, l);
            }

            case Venus: {
                float a = 0.72333566f + 0.00000390f * jc;
                float e = 0.00677672f - 0.00004107f * jc;
                float i = (3.39467605f - 0.00078890f * jc) * MathsUtils.DEGREES_TO_RADIANS;
                float l =
                        MathsUtils.mod2pi((181.97909950f + 58517.81538729f * jc) * MathsUtils.DEGREES_TO_RADIANS);
                float w = (131.60246718f + 0.00268329f * jc) * MathsUtils.DEGREES_TO_RADIANS;
                float o = (76.67984255f - 0.27769418f * jc) * MathsUtils.DEGREES_TO_RADIANS;
                return new OrbitalElements(a, e, i, o, w, l);
            }

            // Note that this is the orbital data for Earth.
            case Sun: {
                float a = 1.00000261f + 0.00000562f * jc;
                float e = 0.01671123f - 0.00004392f * jc;
                float i = (-0.00001531f - 0.01294668f * jc) * MathsUtils.DEGREES_TO_RADIANS;
                float l =
                        MathsUtils.mod2pi((100.46457166f + 35999.37244981f * jc) * MathsUtils.DEGREES_TO_RADIANS);
                float w = (102.93768193f + 0.32327364f * jc) * MathsUtils.DEGREES_TO_RADIANS;
                float o = 0.0f;
                return new OrbitalElements(a, e, i, o, w, l);
            }

            case Mars: {
                float a = 1.52371034f + 0.00001847f * jc;
                float e = 0.09339410f + 0.00007882f * jc;
                float i = (1.84969142f - 0.00813131f * jc) * MathsUtils.DEGREES_TO_RADIANS;
                float l =
                        MathsUtils.mod2pi((-4.55343205f + 19140.30268499f * jc) * MathsUtils.DEGREES_TO_RADIANS);
                float w = (-23.94362959f + 0.44441088f * jc) * MathsUtils.DEGREES_TO_RADIANS;
                float o = (49.55953891f - 0.29257343f * jc) * MathsUtils.DEGREES_TO_RADIANS;
                return new OrbitalElements(a, e, i, o, w, l);
            }

            case Jupiter: {
                float a = 5.20288700f - 0.00011607f * jc;
                float e = 0.04838624f - 0.00013253f * jc;
                float i = (1.30439695f - 0.00183714f * jc) * MathsUtils.DEGREES_TO_RADIANS;
                float l =
                        MathsUtils.mod2pi((34.39644051f + 3034.74612775f * jc) * MathsUtils.DEGREES_TO_RADIANS);
                float w = (14.72847983f + 0.21252668f * jc) * MathsUtils.DEGREES_TO_RADIANS;
                float o = (100.47390909f + 0.20469106f * jc) * MathsUtils.DEGREES_TO_RADIANS;
                return new OrbitalElements(a, e, i, o, w, l);
            }

            case Saturn: {
                float a = 9.53667594f - 0.00125060f * jc;
                float e = 0.05386179f - 0.00050991f * jc;
                float i = (2.48599187f + 0.00193609f * jc) * MathsUtils.DEGREES_TO_RADIANS;
                float l =
                        MathsUtils.mod2pi((49.95424423f + 1222.49362201f * jc) * MathsUtils.DEGREES_TO_RADIANS);
                float w = (92.59887831f - 0.41897216f * jc) * MathsUtils.DEGREES_TO_RADIANS;
                float o = (113.66242448f - 0.28867794f * jc) * MathsUtils.DEGREES_TO_RADIANS;
                return new OrbitalElements(a, e, i, o, w, l);
            }

            case Uranus: {
                float a = 19.18916464f - 0.00196176f * jc;
                float e = 0.04725744f - 0.00004397f * jc;
                float i = (0.77263783f - 0.00242939f * jc) * MathsUtils.DEGREES_TO_RADIANS;
                float l =
                        MathsUtils.mod2pi((313.23810451f + 428.48202785f * jc) * MathsUtils.DEGREES_TO_RADIANS);
                float w = (170.95427630f + 0.40805281f * jc) * MathsUtils.DEGREES_TO_RADIANS;
                float o = (74.01692503f + 0.04240589f * jc) * MathsUtils.DEGREES_TO_RADIANS;
                return new OrbitalElements(a, e, i, o, w, l);
            }

            case Neptune: {
                float a = 30.06992276f + 0.00026291f * jc;
                float e = 0.00859048f + 0.00005105f * jc;
                float i = (1.77004347f + 0.00035372f * jc) * MathsUtils.DEGREES_TO_RADIANS;
                float l =
                        MathsUtils.mod2pi((-55.12002969f + 218.45945325f * jc) * MathsUtils.DEGREES_TO_RADIANS);
                float w = (44.96476227f - 0.32241464f * jc) * MathsUtils.DEGREES_TO_RADIANS;
                float o = (131.78422574f - 0.00508664f * jc) * MathsUtils.DEGREES_TO_RADIANS;
                return new OrbitalElements(a, e, i, o, w, l);
            }

            case Pluto: {
                float a = 39.48211675f - 0.00031596f * jc;
                float e = 0.24882730f + 0.00005170f * jc;
                float i = (17.14001206f + 0.00004818f * jc) * MathsUtils.DEGREES_TO_RADIANS;
                float l =
                        MathsUtils.mod2pi((238.92903833f + 145.20780515f * jc) * MathsUtils.DEGREES_TO_RADIANS);
                float w = (224.06891629f - 0.04062942f * jc) * MathsUtils.DEGREES_TO_RADIANS;
                float o = (110.30393684f - 0.01183482f * jc) * MathsUtils.DEGREES_TO_RADIANS;
                return new OrbitalElements(a, e, i, o, w, l);
            }

            default:
                throw new RuntimeException("Unknown Planet: " + this);
        }
    }

    /**
     * Calculates the phase angle of the planet, in degrees.
     */
    @VisibleForTesting
    float calculatePhaseAngle(Calendar time) {
        // For the moon, we will approximate phase angle by calculating the
        // elongation of the moon relative to the sun. This is accurate to within
        // about 1%.
        if (this == Planet.Moon) {
            RaDec moonRaDec = calculateLunarGeocentricLocation(time);
            GeocentricCoordinates moon = GeocentricCoordinates.getInstance(moonRaDec);

            HeliocentricCoordinates sunCoords = HeliocentricCoordinates.getInstance(Planet.Sun, time);
            RaDec sunRaDec = RaDec.calculateRaDecDist(sunCoords);
            GeocentricCoordinates sun = GeocentricCoordinates.getInstance(sunRaDec);

            return 180.0f -
                    (float) Math.acos(sun.x * moon.x + sun.y * moon.y + sun.z * moon.z)
                            * MathsUtils.RADIANS_TO_DEGREES;
        }

        // First, determine position in the solar system.
        HeliocentricCoordinates planetCoords = HeliocentricCoordinates.getInstance(this, time);

        // Second, determine position relative to Earth
        HeliocentricCoordinates earthCoords = HeliocentricCoordinates.getInstance(Planet.Sun, time);
        float earthDistance = planetCoords.DistanceFrom(earthCoords);

        // Finally, calculate the phase of the body.
        return (float) Math.acos((earthDistance * earthDistance +
                planetCoords.radius * planetCoords.radius -
                earthCoords.radius * earthCoords.radius) /
                (2.0f * earthDistance * planetCoords.radius)) * MathsUtils.RADIANS_TO_DEGREES;
    }

    /**
     * Calculates the planet's magnitude for the given date.
     * <p>
     * TODO(serafini): I need to re-factor this method so it uses the phase
     * calculations above. For now, I'm going to duplicate some code to avoid
     * some redundant calculations at run time.
     */
    public float getMagnitude(Calendar time) {
        // TODO(serafini): For now, return semi-reasonable values for the Sun and
        // Moon. We shouldn't call this method for those bodies, but we want to do
        // something sane if we do.
        if (this == Planet.Sun) {
            return -27.0f;
        }
        if (this == Planet.Moon) {
            return -10.0f;
        }

        // First, determine position in the solar system.
        HeliocentricCoordinates planetCoords = HeliocentricCoordinates.getInstance(this, time);

        // Second, determine position relative to Earth
        HeliocentricCoordinates earthCoords = HeliocentricCoordinates.getInstance(Planet.Sun, time);
        float earthDistance = planetCoords.DistanceFrom(earthCoords);

        // Third, calculate the phase of the body.
        float phase = (float) Math.acos((earthDistance * earthDistance +
                planetCoords.radius * planetCoords.radius -
                earthCoords.radius * earthCoords.radius) /
                (2.0f * earthDistance * planetCoords.radius)) * MathsUtils.RADIANS_TO_DEGREES;
        float p = phase / 100.0f;     // Normalized phase angle

        // Finally, calculate the magnitude of the body.
        float mag;      // Apparent visual magnitude
        switch (this) {
            case Mercury:
                mag = -0.42f + (3.80f - (2.73f - 2.00f * p) * p) * p;
                break;
            case Venus:
                mag = -4.40f + (0.09f + (2.39f - 0.65f * p) * p) * p;
                break;
            case Mars:
                mag = -1.52f + 1.6f * p;
                break;
            case Jupiter:
                mag = -9.40f + 0.5f * p;
                break;
            case Saturn:
                // TODO(serafini): Add the real calculations that consider the position
                // of the rings. For now, lets assume the following, which gets us a reasonable
                // approximation of Saturn's magnitude for the near future.
                mag = -8.75f;
                break;
            case Uranus:
                mag = -7.19f;
                break;
            case Neptune:
                mag = -6.87f;
                break;
            case Pluto:
                mag = -1.0f;
                break;
            default:
                Log.e(TelescopeTouchApp.getTag(this), "Invalid planet: " + this);
                // At least make it faint!
                mag = 100f;
                break;
        }
        return (mag + 5.0f * (float) Math.log10(planetCoords.radius * earthDistance));
    }

    // TODO(serafini): This is experimental code used to scale planetary images.
    public float getPlanetaryImageSize() {
        switch (this) {
            case Mercury:
            case Venus:
            case Mars:
            case Pluto:
                return 0.01f;
            case Jupiter:
                return 0.025f;
            case Uranus:
            case Neptune:
                return 0.015f;
            case Saturn:
                return 0.035f;
            case Sun:
            case Moon:
            default:
                return 0.02f;
        }
    }

    /**
     * Calculates the next rise or set time of this planet from a given observer.
     * Returns null if the planet doesn't rise or set during the next day.
     *
     * @param now       Calendar time from which to calculate next rise / set time.
     * @param loc       Location of observer.
     * @param indicator Indicates whether to look for rise or set time.
     * @return New Calendar set to the next rise or set time if within
     * the next day, otherwise null.
     */
    public Calendar calcNextRiseSetTime(Calendar now, Location loc,
                                        RiseSetIndicator indicator) {
        // Make a copy of the calendar to return.
        Calendar riseSetTime = Calendar.getInstance();
        double riseSetUt = calcRiseSetTime(now.getTime(), loc, indicator);
        // Early out if no nearby rise set time.
        if (riseSetUt < 0) {
            return null;
        }

        // Find the start of this day in the local time zone. The (a / b) * b
        // formulation looks weird, it's using the properties of int arithmetic
        // so that (a / b) is really floor(a / b).
        long dayStart = (now.getTimeInMillis() / TimeUtils.MILLISECONDS_PER_DAY)
                * TimeUtils.MILLISECONDS_PER_DAY - riseSetTime.get(Calendar.ZONE_OFFSET);
        long riseSetUtMillis = (long) (calcRiseSetTime(now.getTime(), loc, indicator)
                * TimeUtils.MILLISECONDS_PER_HOUR);
        long newTime = dayStart + riseSetUtMillis + riseSetTime.get(Calendar.ZONE_OFFSET);
        // If the newTime is before the current time, go forward 1 day.
        if (newTime < now.getTimeInMillis()) {
            Log.d(TAG, "Nearest Rise/Set is in the past. Adding one day.");
            newTime += TimeUtils.MILLISECONDS_PER_DAY;
        }
        riseSetTime.setTimeInMillis(newTime);
        if (!riseSetTime.after(now)) {
            Log.e(TAG, "Next rise set time (" + riseSetTime.toString()
                    + ") should be after current time (" + now.toString() + ")");
        }
        return riseSetTime;
    }

    // Internally calculate the rise and set time of an object.
    // Returns a double, the number of hours through the day in UT.
    private double calcRiseSetTime(Date d, Location loc,
                                   RiseSetIndicator indicator) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UT"));
        cal.setTime(d);

        float sign = (indicator == RiseSetIndicator.RISE ? 1.0f : -1.0f);
        float delta = 5.0f;
        float ut = 12.0f;

        int counter = 0;
        while ((Math.abs(delta) > 0.008) && counter < MAX_ITERATIONS) {
            cal.set(Calendar.HOUR_OF_DAY, (int) (float) Math.floor(ut));
            float minutes = (ut - (float) Math.floor(ut)) * 60.0f;
            cal.set(Calendar.MINUTE, (int) minutes);
            cal.set(Calendar.SECOND, (int) ((minutes - (float) Math.floor(minutes)) * 60.f));

            // Calculate the hour angle and declination of the planet.
            // TODO(serafini): Need to fix this for arbitrary RA/Dec locations.
            HeliocentricCoordinates sunCoordinates = HeliocentricCoordinates.getInstance(Planet.Sun, cal);
            RaDec raDec = RaDec.getInstance(this, cal, sunCoordinates);

            // GHA = GST - RA. (In degrees.)
            float gst = TimeUtils.meanSiderealTime(cal, 0.0f);
            float gha = gst - raDec.ra;

            // The value of -0.83 works for the diameter of the Sun and Moon. We
            // assume that other objects are simply points.
            float bodySize = (this == Planet.Sun || this == Planet.Moon) ? -0.83f : 0.0f;
            float hourAngle = calculateHourAngle(bodySize, (float) loc.getLatitude(), raDec.dec);

            delta = (gha + ((float) loc.getLongitude()) + (sign * hourAngle)) / 15.0f;
            while (delta < -24.0f) {
                delta = delta + 24.0f;
            }
            while (delta > 24.0f) {
                delta = delta - 24.0f;
            }
            ut = ut - delta;

            // I think we need to normalize UT
            while (ut < 0.0f) {
                ut = ut + 24.0f;
            }
            while (ut > 24.0f) {
                ut = ut - 24.0f;
            }

            ++counter;
        }

        // Return failure if we didn't converge.
        if (counter == MAX_ITERATIONS) {
            Log.d(TAG, "Rise/Set calculation didn't converge.");
            return -1.0f;
        }

        // TODO(serafini): Need to handle latitudes above 60
        // At latitudes above 60, we need to calculate the following:
        // sin h = sin phi sin delta + cos phi cos delta cos (gha + lambda)
        return ut;
    }


    /**
     * Enum that identifies whether we are interested in rise or set time.
     */
    public enum RiseSetIndicator {RISE, SET}
}