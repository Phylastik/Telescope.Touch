/*
 * Copyright 2021 Marco Cipriani (@marcocipriani01)
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

package io.github.marcocipriani01.telescopetouch.catalog;

import android.content.Context;
import android.content.res.Resources;
import android.location.Location;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import io.github.marcocipriani01.telescopetouch.R;
import io.github.marcocipriani01.telescopetouch.astronomy.EquatorialCoordinates;

/**
 * Represents a deep sky object. This class also contains a loader to fetch DSO from the app's catalog.
 */
public class DSOEntry extends CatalogEntry {

    /**
     * Resource file.
     */
    private final static int RESOURCE = R.raw.ngc_ic_b;
    /**
     * The length of the name in each line.
     */
    private final static int NAME_LENGTH = 25;
    /**
     * The length of the magnitude in each line.
     */
    private final static int MAGNITUDE_LENGTH = 2;
    /**
     * The length of the type string in each line.
     */
    private final static int TYPE_LENGTH = 3;
    /**
     * The length of the object size in each line.
     */
    private final static int SIZE_LENGTH = 5;
    /**
     * The length of the RA coordinate in each line.
     */
    private final static int RA_LENGTH = 10;
    /**
     * The length of the DEC coordinate in each line.
     */
    //private final static int DEC_LENGTH = 9;

    private final String type;
    private final String size;

    /**
     * Create the entry from a formatted line
     * (ie. "Dumbbell nebula          8 Pl 15.2 19 59 36.1+22 43 00")
     *
     * @param data a line
     */
    private DSOEntry(String data) {
        int i = 0;
        name = data.substring(i, i + NAME_LENGTH).trim();
        i += NAME_LENGTH;
        magnitude = data.substring(i, i + MAGNITUDE_LENGTH).trim();
        if (!magnitude.equals(""))
            magnitudeDouble = Double.parseDouble(magnitude);
        i += MAGNITUDE_LENGTH;
        type = data.substring(i, i + TYPE_LENGTH).trim();
        i += TYPE_LENGTH;
        size = data.substring(i, i + SIZE_LENGTH).trim();
        i += SIZE_LENGTH;
        String raString = data.substring(i, i + RA_LENGTH).trim();
        i += RA_LENGTH;
        String decString = data.substring(i).trim();
        coord = new EquatorialCoordinates(raString, decString);
    }

    public static void loadToList(List<CatalogEntry> list, Resources resources) throws IOException {
        // Open and read the catalog file
        InputStream resourceStream = resources.openRawResource(RESOURCE);
        BufferedReader br = new BufferedReader(new InputStreamReader(resourceStream));
        String line;
        while ((line = br.readLine()) != null) {
            list.add(new DSOEntry(line));
        }
        resourceStream.close();
    }

    /**
     * Create the description rich-text string
     *
     * @param context Context (to access resource strings)
     * @return description Spannable
     */
    @Override
    public Spannable createDescription(Context context, Location location) {
        Resources r = context.getResources();
        String str = "<b>" + r.getString(R.string.entry_type) + ": </b>" + r.getString(getType()) + "<br>";
        if (!magnitude.equals(""))
            str += "<b>" + r.getString(R.string.entry_magnitude) + ": </b>" + magnitude + "<br>";
        if (!size.equals(""))
            str += "<b>" + r.getString(R.string.entry_size) + ": </b>" + size + " " + r.getString(R.string.arcmin) + "<br>";
        return new SpannableString(Html.fromHtml(str + getCoordinatesString(r, location)));
    }

    /**
     * Create the summary rich-text string (1 line)
     *
     * @param context Context (to access resource strings)
     * @return summary Spannable
     */
    @Override
    public Spannable createSummary(Context context) {
        Resources r = context.getResources();
        String str = "<b>" + r.getString(getTypeShort()) + "</b> ";
        if (!magnitude.equals(""))
            str += r.getString(R.string.entry_mag) + ": " + magnitude + " ";
        if (!size.equals(""))
            str += r.getString(R.string.entry_size).toLowerCase() + ": " + size + r.getString(R.string.arcmin);
        return new SpannableString(Html.fromHtml(str));
    }

    @Override
    public int getIconResource() {
        return R.drawable.galaxy_on;
    }

    /**
     * Return the string resource which correspond to the type acronym
     *
     * @return text
     */
    private int getType() {
        if (name.matches("B\\d+")) return R.string.dark_nebula;
        switch (type) {
            case "Gx":
                return R.string.entry_Gx;
            case "OC":
                return R.string.entry_OC;
            case "Gb":
                return R.string.entry_Gb;
            case "Nb":
                return R.string.entry_Nb;
            case "Pl":
                return R.string.entry_Pl;
            case "C+N":
                return R.string.entry_cluster_nebulosity;
            case "Ast":
                return R.string.entry_Ast;
            case "Kt":
                return R.string.entry_Kt;
            case "***":
                return R.string.entry_triStar;
            case "D*":
                return R.string.entry_doubleStar;
            case "*":
                return R.string.entry_star;
            case "?":
                return R.string.entry_uncertain;
            case "-":
                return R.string.entry_minus;
            case "PD":
                return R.string.entry_PD;
            case "":
            default:
                return R.string.entry_blank;
        }
    }

    /**
     * Return the string resource which correspond to the type acronym
     *
     * @return short text
     */
    private int getTypeShort() {
        if (name.matches("B\\d+")) return R.string.dark_nebula;
        switch (type) {
            case "Gx":
                return R.string.entry_short_Gx;
            case "OC":
                return R.string.entry_short_OC;
            case "Gb":
                return R.string.entry_short_Gb;
            case "Nb":
                return R.string.entry_short_Nb;
            case "Pl":
                return R.string.entry_short_Pl;
            case "C+N":
                return R.string.entry_short_cluster_nebula;
            case "Ast":
                return R.string.entry_short_Ast;
            case "Kt":
                return R.string.entry_short_Kt;
            case "***":
                return R.string.entry_short_triStar;
            case "D*":
                return R.string.entry_short_doubleStar;
            case "*":
                return R.string.entry_short_star;
            case "?":
                return R.string.entry_short_uncertain;
            case "-":
                return R.string.entry_short_minus;
            case "PD":
                return R.string.entry_short_PD;
            case "":
            default:
                return R.string.entry_short_blank;
        }
    }
}