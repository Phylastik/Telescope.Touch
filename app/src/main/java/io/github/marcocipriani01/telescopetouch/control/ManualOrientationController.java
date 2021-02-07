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

package io.github.marcocipriani01.telescopetouch.control;

import io.github.marcocipriani01.telescopetouch.astronomy.GeocentricCoordinates;
import io.github.marcocipriani01.telescopetouch.maths.Matrix3x3;
import io.github.marcocipriani01.telescopetouch.maths.Vector3;

/**
 * Allows user-input elements such as touch screens and trackballs to move the
 * map.
 *
 * @author John Taylor
 */
public class ManualOrientationController extends AbstractController {

    @Override
    public void start() {
        // Nothing to do
    }

    @Override
    public void stop() {
        // Nothing to do
    }

    /**
     * Moves the astronomer's pointing right or left.
     *
     * @param radians the angular change in the pointing in radians (only
     *                accurate in the limit as radians tends to 0.)
     */
    public void changeRightLeft(float radians) {
        // TODO(johntaylor): Some of the Math in here perhaps belongs in
        // AstronomerModel.
        if (!enabled) {
            return;
        }
        Pointing pointing = model.getPointing();
        GeocentricCoordinates pointingXyz = pointing.getLineOfSight();
        GeocentricCoordinates topXyz = pointing.getPerpendicular();
        Vector3 horizontalXyz = Vector3.vectorProduct(pointingXyz, topXyz);
        Vector3 deltaXyz = Vector3.scale(horizontalXyz, radians);

        Vector3 newPointingXyz = Vector3.sum(pointingXyz, deltaXyz);
        newPointingXyz.normalize();

        model.setPointing(newPointingXyz, topXyz);
    }

    /**
     * Moves the astronomer's pointing up or down.
     *
     * @param radians the angular change in the pointing in radians (only
     *                accurate in the limit as radians tends to 0.)
     */
    public void changeUpDown(float radians) {
        if (!enabled) {
            return;
        }
        // Log.d(TAG, "Scrolling up down");
        Pointing pointing = model.getPointing();
        GeocentricCoordinates pointingXyz = pointing.getLineOfSight();
        // Log.d(TAG, "Current view direction " + viewDir);
        GeocentricCoordinates topXyz = pointing.getPerpendicular();

        Vector3 deltaXyz = Vector3.scale(topXyz, -radians);
        Vector3 newPointingXyz = Vector3.sum(pointingXyz, deltaXyz);
        newPointingXyz.normalize();

        Vector3 deltaUpXyz = Vector3.scale(pointingXyz, radians);
        Vector3 newUpXyz = Vector3.sum(topXyz, deltaUpXyz);
        newUpXyz.normalize();

        model.setPointing(newPointingXyz, newUpXyz);
    }

    /**
     * Rotates the astronomer's view.
     */
    public void rotate(float degrees) {
        if (!enabled) {
            return;
        }
        Pointing pointing = model.getPointing();
        GeocentricCoordinates pointingXyz = pointing.getLineOfSight();
        Matrix3x3 rotation = Matrix3x3.calculateRotationMatrix(degrees, pointingXyz);

        GeocentricCoordinates topXyz = pointing.getPerpendicular();
        Vector3 newUpXyz = Matrix3x3.matrixVectorMultiply(rotation, topXyz);
        newUpXyz.normalize();

        model.setPointing(pointingXyz, newUpXyz);
    }
}