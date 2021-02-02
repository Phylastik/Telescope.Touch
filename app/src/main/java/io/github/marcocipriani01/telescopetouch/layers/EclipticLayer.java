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

package io.github.marcocipriani01.telescopetouch.layers;

import android.content.res.Resources;
import android.graphics.Color;

import java.util.ArrayList;
import java.util.List;

import io.github.marcocipriani01.telescopetouch.R;
import io.github.marcocipriani01.telescopetouch.source.AstronomicalSource;
import io.github.marcocipriani01.telescopetouch.source.LineSource;
import io.github.marcocipriani01.telescopetouch.source.TextSource;
import io.github.marcocipriani01.telescopetouch.source.impl.LineSourceImpl;
import io.github.marcocipriani01.telescopetouch.source.impl.TextSourceImpl;
import io.github.marcocipriani01.telescopetouch.units.GeocentricCoordinates;

/**
 * Creates a Layer for the Ecliptic.
 *
 * @author John Taylor
 * @author Brent Bryan
 */
public class EclipticLayer extends AbstractLayer {

    public EclipticLayer(Resources resources) {
        super(resources, false);
    }

    @Override
    protected void initializeAstroSources(ArrayList<AstronomicalSource> sources) {
        sources.add(new EclipticSource(getResources()));
    }

    @Override
    public int getLayerDepthOrder() {
        return 50;
    }

    @Override
    protected int getLayerNameId() {
        return R.string.show_grid_pref;
    }

    @Override
    public String getPreferenceId() {
        return "source_provider.4";
    }

    /**
     * Implementation of {@link AstronomicalSource} for the ecliptic source.
     */
    private static class EclipticSource extends AstronomicalSource {
        // Earth's Angular Tilt
        private static final float EPSILON = 23.439281f;
        private static final int LINE_COLOR = Color.argb(20, 248, 239, 188);

        private final ArrayList<LineSource> lineSources = new ArrayList<>();
        private final ArrayList<TextSource> textSources = new ArrayList<>();

        public EclipticSource(Resources res) {
            String title = res.getString(R.string.ecliptic);
            textSources.add(new TextSourceImpl(90.0f, EPSILON, title, LINE_COLOR));
            textSources.add(new TextSourceImpl(270f, -EPSILON, title, LINE_COLOR));

            // Create line source.
            float[] ra = {0, 90, 180, 270, 0};
            float[] dec = {0, EPSILON, 0, -EPSILON, 0};

            ArrayList<GeocentricCoordinates> vertices = new ArrayList<>();
            for (int i = 0; i < ra.length; ++i) {
                vertices.add(GeocentricCoordinates.getInstance(ra[i], dec[i]));
            }
            lineSources.add(new LineSourceImpl(LINE_COLOR, vertices, 1.5f));
        }

        @Override
        public List<? extends TextSource> getLabels() {
            return textSources;
        }

        @Override
        public List<? extends LineSource> getLines() {
            return lineSources;
        }
    }
}