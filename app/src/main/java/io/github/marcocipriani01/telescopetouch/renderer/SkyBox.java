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

package io.github.marcocipriani01.telescopetouch.renderer;

import android.util.Log;

import javax.microedition.khronos.opengles.GL10;

import io.github.marcocipriani01.telescopetouch.astronomy.GeocentricCoordinates;
import io.github.marcocipriani01.telescopetouch.maths.Vector3;
import io.github.marcocipriani01.telescopetouch.renderer.util.ColorBuffer;
import io.github.marcocipriani01.telescopetouch.renderer.util.IndexBuffer;
import io.github.marcocipriani01.telescopetouch.renderer.util.TextureManager;
import io.github.marcocipriani01.telescopetouch.renderer.util.VertexBuffer;

public class SkyBox extends RendererObjectManager {

    private static final short NUM_VERTEX_BANDS = 8;
    // This number MUST be even
    private static final short NUM_STEPS_IN_BAND = 10;
    // Used to make sure rounding error doesn't make us have off-by-one errors in our iterations.
    private static final float EPSILON = 1e-3f;
    private final VertexBuffer vertexBuffer = new VertexBuffer(true);
    private final ColorBuffer colorBuffer = new ColorBuffer(true);
    private final IndexBuffer indexBuffer = new IndexBuffer(true);
    private GeocentricCoordinates sunPos = new GeocentricCoordinates(0, 1, 0);

    public SkyBox(int layer, TextureManager textureManager) {
        super(layer, textureManager);

        int numVertices = NUM_VERTEX_BANDS * NUM_STEPS_IN_BAND;
        int numIndices = (NUM_VERTEX_BANDS - 1) * NUM_STEPS_IN_BAND * 6;
        vertexBuffer.reset(numVertices);
        colorBuffer.reset(numVertices);
        indexBuffer.reset(numIndices);

        float[] sinAngles = new float[NUM_STEPS_IN_BAND];
        float[] cosAngles = new float[NUM_STEPS_IN_BAND];

        float angleInBand = 0;
        float dAngle = 2f * (float) Math.PI / (NUM_STEPS_IN_BAND - 1);
        for (int i = 0; i < NUM_STEPS_IN_BAND; i++) {
            sinAngles[i] = (float) Math.sin(angleInBand);
            cosAngles[i] = (float) Math.cos(angleInBand);
            angleInBand += dAngle;
        }

        float bandStep = 2.0f / (NUM_VERTEX_BANDS - 1) + EPSILON;

        float bandPos = 1;
        for (int band = 0; band < NUM_VERTEX_BANDS; band++, bandPos -= bandStep) {
            int color;
            if (bandPos > 0) {
                // TODO(jpowell): This isn't really intensity, name it more appropriately.
                // I=70 at bandPos = 1, I=50 at bandPos = 0
                byte intensity = (byte) (bandPos * 20 + 50);
                color = (intensity << 16) | 0xff000000;
            } else {
                // I=40 at bandPos = -1, I=0 at bandPos = 0
                byte intensity = (byte) (bandPos * 40 + 40);
                color = (intensity << 16) | (intensity << 8) | intensity | 0xff000000;
            }

            float sinPhi = bandPos > -1 ? (float) Math.sqrt(1 - bandPos * bandPos) : 0;
            for (int i = 0; i < NUM_STEPS_IN_BAND; i++) {
                vertexBuffer.addPoint(cosAngles[i] * sinPhi, bandPos, sinAngles[i] * sinPhi);
                colorBuffer.addColor(color);
            }
        }
        Log.d("SkyBox", "Vertices: " + vertexBuffer.size());

        // Set the indices for the first band.
        short topBandStart = 0;
        short bottomBandStart = NUM_STEPS_IN_BAND;
        for (short triangleBand = 0; triangleBand < NUM_VERTEX_BANDS - 1; triangleBand++) {
            for (short offsetFromStart = 0; offsetFromStart < NUM_STEPS_IN_BAND - 1; offsetFromStart++) {
                // Draw one quad as two triangles.
                short topLeft = (short) (topBandStart + offsetFromStart);
                short topRight = (short) (topLeft + 1);

                short bottomLeft = (short) (bottomBandStart + offsetFromStart);
                short bottomRight = (short) (bottomLeft + 1);

                // First triangle
                indexBuffer.addIndex(topLeft);
                indexBuffer.addIndex(bottomRight);
                indexBuffer.addIndex(bottomLeft);

                // Second triangle
                indexBuffer.addIndex(topRight);
                indexBuffer.addIndex(bottomRight);
                indexBuffer.addIndex(topLeft);
            }

            // Last quad: connect the end with the beginning.

            // Top left, bottom right, bottom left
            indexBuffer.addIndex((short) (topBandStart + NUM_STEPS_IN_BAND - 1));
            indexBuffer.addIndex(bottomBandStart);
            indexBuffer.addIndex((short) (bottomBandStart + NUM_STEPS_IN_BAND - 1));

            // Top right, bottom right, top left
            indexBuffer.addIndex(topBandStart);
            indexBuffer.addIndex(bottomBandStart);
            indexBuffer.addIndex((short) (topBandStart + NUM_STEPS_IN_BAND - 1));

            topBandStart += NUM_STEPS_IN_BAND;
            bottomBandStart += NUM_STEPS_IN_BAND;
        }
        Log.d("SkyBox", "Indices: " + indexBuffer.size());
    }

    @Override
    public void reload(GL10 gl, boolean fullReload) {
        vertexBuffer.reload();
        colorBuffer.reload();
        indexBuffer.reload();
    }

    public void setSunPosition(GeocentricCoordinates pos) {
        sunPos = pos.copy();
        //Log.d("SkyBox", "SunPos: " + pos.toString());
    }

    @Override
    protected void drawInternal(GL10 gl) {
        if (getRenderState().getNightVisionMode()) {
            return;
        }
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
        gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
        gl.glEnable(GL10.GL_CULL_FACE);
        gl.glFrontFace(GL10.GL_CW);
        gl.glCullFace(GL10.GL_BACK);
        gl.glShadeModel(GL10.GL_SMOOTH);
        gl.glPushMatrix();
        // Rotate the sky box to the position of the sun.
        Vector3 cp = Vector3.vectorProduct(new Vector3(0, 1, 0), sunPos);
        cp = Vector3.normalized(cp);
        float angle = 180.0f / (float) Math.PI * (float) Math.acos(sunPos.y);
        gl.glRotatef(angle, (float) cp.x, (float) cp.y, (float) cp.z);
        vertexBuffer.set(gl);
        colorBuffer.set(gl);
        indexBuffer.draw(gl, GL10.GL_TRIANGLES);
        gl.glPopMatrix();
    }
}