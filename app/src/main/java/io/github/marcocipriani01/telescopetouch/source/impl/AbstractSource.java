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

package io.github.marcocipriani01.telescopetouch.source.impl;

import android.graphics.Color;

import java.util.List;

import io.github.marcocipriani01.telescopetouch.source.Colorable;
import io.github.marcocipriani01.telescopetouch.source.PositionSource;
import io.github.marcocipriani01.telescopetouch.astronomy.GeocentricCoordinates;

/**
 * This class represents the base of an astronomical object to be
 * displayed by the UI.  These object need not be only stars and
 * galaxies but also include labels (such as the name of major stars)
 * and constellation depictions.
 *
 * @author Brent Bryan
 */
public abstract class AbstractSource implements Colorable, PositionSource {

    private final int color;
    private final GeocentricCoordinates xyz;
    private List<String> names;

    @Deprecated
    AbstractSource() {
        this(GeocentricCoordinates.getInstance(0.0f, 0.0f), Color.BLACK);
    }

    protected AbstractSource(int color) {
        this(GeocentricCoordinates.getInstance(0.0f, 0.0f), color);
    }

    protected AbstractSource(GeocentricCoordinates coords, int color) {
        this.xyz = coords;
        this.color = color;
    }

    public List<String> getNames() {
        return names;
    }

    public void setNames(List<String> names) {
        this.names = names;
    }

    @Override
    public int getColor() {
        return color;
    }

    @Override
    public GeocentricCoordinates getLocation() {
        return xyz;
    }
}