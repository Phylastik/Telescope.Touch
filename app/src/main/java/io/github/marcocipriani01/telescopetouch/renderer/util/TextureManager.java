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

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.opengl.GLUtils;
import android.util.Log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.microedition.khronos.opengles.GL10;

/**
 * Manages all textures used by the application.  Useful to make sure that we don't accidentally
 * use deleted textures and don't leak textures, and that we don't create multiple instances of the
 * same texture.
 * <p>
 * TODO(jpowell): We only ever need one instance of this class, but it would be cleaner if it was
 * a normal class instead of a global singleton, so I should change it when I get a chance.
 *
 * @author James Powell
 */
public final class TextureManager {

    private final Resources mRes;
    private final Map<Integer, TextureData> mResourceIdToTextureMap = new HashMap<>();
    private final ArrayList<TextureReferenceImpl> mAllTextures = new ArrayList<>();

    public TextureManager(Resources res) {
        mRes = res;
    }

    public TextureReference createTexture(GL10 gl) {
        return createTextureInternal(gl);
    }

    public TextureReference getTextureFromResource(GL10 gl, int resourceID) {
        // If the texture already exists, return it.
        TextureData texData = mResourceIdToTextureMap.get(resourceID);
        if (texData != null) {
            // Increment the reference count
            texData.refCount++;
            return texData.ref;
        }

        TextureReferenceImpl tex = createTextureFromResource(gl, resourceID);

        // Add it to the map.
        TextureData data = new TextureData();
        data.ref = tex;
        data.refCount = 1;
        mResourceIdToTextureMap.put(resourceID, data);

        return tex;
    }

    public void reset() {
        mResourceIdToTextureMap.clear();
        for (TextureReferenceImpl ref : mAllTextures) {
            ref.invalidate();
        }
        mAllTextures.clear();
    }

    private TextureReferenceImpl createTextureFromResource(GL10 gl, int resourceID) {
        // The texture hasn't been loaded yet, so load it.
        TextureReferenceImpl tex = createTextureInternal(gl);
        Options opts = new Options();
        opts.inScaled = false;
        Bitmap bmp = BitmapFactory.decodeResource(mRes, resourceID, opts);
        tex.bind(gl);
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);

        GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bmp, 0);

        bmp.recycle();
        return tex;
    }

    private TextureReferenceImpl createTextureInternal(GL10 gl) {
        // The texture hasn't been loaded yet, so load it.
        int[] texID = new int[1];
        gl.glGenTextures(1, texID, 0);
        TextureReferenceImpl tex = new TextureReferenceImpl(texID[0]);
        mAllTextures.add(tex);
        return tex;
    }

    private static class TextureReferenceImpl implements TextureReference {

        private final int mTextureID;
        private boolean mValid = true;

        public TextureReferenceImpl(int id) {
            mTextureID = id;
        }

        @Override
        public void bind(GL10 gl) {
            checkValid();
            gl.glBindTexture(GL10.GL_TEXTURE_2D, mTextureID);
        }

        @Override
        public void delete(GL10 gl) {
            checkValid();
            gl.glDeleteTextures(1, new int[]{mTextureID}, 0);
            invalidate();
        }

        public int getID() {
            return mTextureID;
        }

        public void invalidate() {
            mValid = false;
        }

        private void checkValid() {
            if (!mValid) {
                Log.e("TextureManager", "Setting invalidated texture ID: " + mTextureID);
                StringWriter writer = new StringWriter();
                new Throwable().printStackTrace(new PrintWriter(writer));
                Log.e("TextureManager", writer.toString());
            }
        }
    }

    private static class TextureData {
        public TextureReferenceImpl ref = null;
        public int refCount = 0;
    }
}