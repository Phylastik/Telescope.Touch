/*
 * Copyright 2021 Marco Cipriani (@marcocipriani01)
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.marcocipriani01.telescopetouch.renderer.util;

import javax.microedition.khronos.opengles.GL10;

/// Encapsulates a color vertex buffer where night vision can be enabled or diabled by a function call.
public class NightVisionColorBuffer {
    private ColorBuffer mNormalBuffer;
    private ColorBuffer mRedBuffer;

    public NightVisionColorBuffer(int numVertices) {
        reset(numVertices);
    }

    public NightVisionColorBuffer() {
        mNormalBuffer = new ColorBuffer(false);
        mRedBuffer = new ColorBuffer(false);
    }

    public NightVisionColorBuffer(boolean useVBO) {
        mNormalBuffer = new ColorBuffer(useVBO);
        mRedBuffer = new ColorBuffer(useVBO);
    }

    public int size() {
        return mNormalBuffer.size();
    }

    public void reset(int numVertices) {
        mNormalBuffer.reset(numVertices);
        mRedBuffer.reset(numVertices);
    }

    // Call this when we have to re-create the surface and reloading all OpenGL resources.
    public void reload() {
        mNormalBuffer.reload();
        mRedBuffer.reload();
    }

    public void addColor(int a, int r, int g, int b) {
        mNormalBuffer.addColor(a, r, g, b);
        // I tried luminance here first, but many objects we care a lot about weren't very noticable because they were
        // bluish.  An average gets a better result.
        int avg = (r + g + b) / 3;
        mRedBuffer.addColor(a, avg, 0, 0);
    }

    public void addColor(int abgr) {
        int a = (abgr >> 24) & 0xff;
        int b = (abgr >> 16) & 0xff;
        int g = (abgr >> 8) & 0xff;
        int r = abgr & 0xff;
        addColor(a, r, g, b);
    }

    public void set(GL10 gl, boolean nightVisionMode) {
        if (nightVisionMode) {
            mRedBuffer.set(gl);
        } else {
            mNormalBuffer.set(gl);
        }
    }
}
