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

package io.github.marcocipriani01.telescopetouch.source;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import io.github.marcocipriani01.telescopetouch.renderer.RendererObjectManager.UpdateType;
import io.github.marcocipriani01.telescopetouch.units.GeocentricCoordinates;

/**
 * Base implementation of the {@link AstronomicalSource} and {@link Sources}
 * interfaces.
 *
 * @author Brent Bryan
 */
public abstract class AstronomicalSource implements Sources {

    /**
     * Initializes and returns the elements for this {@link AstronomicalSource}.
     * Elements should have their positions, images, etc update to the current
     * time / location information.
     */
    public Sources initialize() {
        return this;
    }

    /**
     * Updates the {@link Sources} of this {@link AstronomicalSource} in response
     * to a change in the user's location or current time. Changes can be caused
     * by the user moving to a new location or time progressing, or by the user
     * manually selecting a different location. Returns the minimal Set of
     * UpdateType required to enact the changes required by this update.
     */
    public EnumSet<UpdateType> update() {
        return EnumSet.noneOf(UpdateType.class);
    }

    /**
     * Returns a list of names associated with this source. Names in this list
     * should be internationalized.
     * <p>
     * Implementors of this method must implement {@link #getSearchLocation}.
     */
    public List<String> getNames() {
        return Collections.emptyList();
    }

    /**
     * Returns the {@link GeocentricCoordinates} of the center of this object.
     * This is the point to which the user will be directed for a search.
     */
    public GeocentricCoordinates getSearchLocation() {
        throw new UnsupportedOperationException("Should not be called");
    }

    @Override
    public List<? extends ImageSource> getImages() {
        return Collections.emptyList();
    }

    @Override
    public List<? extends TextSource> getLabels() {
        return Collections.emptyList();
    }

    @Override
    public List<? extends LineSource> getLines() {
        return Collections.emptyList();
    }

    @Override
    public List<? extends PointSource> getPoints() {
        return Collections.emptyList();
    }
}
