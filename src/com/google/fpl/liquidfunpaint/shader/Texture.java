/**
* Copyright (c) 2014 Google, Inc. All rights reserved.
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
*
*/
package com.google.fpl.liquidfunpaint.shader;

import com.google.fpl.liquidfunpaint.util.FileHelper;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.opengl.GLES20;
import android.opengl.GLUtils;

/**
 * A texture.
 * Could be created from a drawable or a bitmap image, or as a container for
 * a generated texture id.
 */
public class Texture {
    /**
     * Defines which component types are accepted.
     * OpenGL ES simply has these as global constants but we want to type check.
     * According to some sources (not benchmarked), enums can take up space
     * and are slower. Investigate these if we see related performance issues.
     */
    public enum WrapParam {
        CLAMP_TO_EDGE(GLES20.GL_CLAMP_TO_EDGE),
        MIRRORED_REPEAT(GLES20.GL_MIRRORED_REPEAT),
        REPEAT(GLES20.GL_REPEAT),
        DEFAULT(GLES20.GL_CLAMP_TO_EDGE);

        private final int mGlComponentType;
        private WrapParam(int glComponentType) {
            mGlComponentType = glComponentType;
        }

        protected int getGlType() {
            return mGlComponentType;
        }
    }

    private int[] mTextureId = new int[1];
    private int mWidth = 0;
    private int mHeight = 0;
    private String mName = "Runtime texture";

    // OpenGL standard has (0,0) as the lower left corner,
    // whereas Android Bitmaps use (0,0) as the upper left corner.
    // We flip images on load to get around this.
    private static final Matrix Y_FLIP_MATRIX;
    static {
        Y_FLIP_MATRIX = new Matrix();
        Y_FLIP_MATRIX.setScale(1, -1);
    }

    /**
     * Default constructor.
     * Default params:
     * BitmapFactory.Options.inScale = true
     * GL_TEXTURE_MAG_FILTER = GL_NEAREST
     * GL_TEXTURE_MIN_FILTER = GL_NEAREST
     * GL_TEXTURE_WRAP_* = GL_CLAMP_TO_EDGE
     *
     * @param resourceId Resource ID of a drawable.
     */
    public Texture(Context context, int resourceId) {
        this(context, resourceId, true, WrapParam.DEFAULT, WrapParam.DEFAULT);
    }

    /**
     * Constructor with more parameters for creating a texture.
     * @param resourceId Resource ID of a drawable.
     * @param scale If true, BitmapFactory will scale image. Else it won't.
     */
    public Texture(
            Context context, int resourceId, boolean scale,
            WrapParam wrapS, WrapParam wrapT) {
        mName = context.getResources().getResourceEntryName(resourceId);
        generateTexture();
        loadTexture(context, resourceId, scale, wrapS, wrapT);
    }

    /**
     * Load a texture in the assets directory
     * @param assetName
     */
    public Texture(Context context, String assetName) {
        this(context, assetName, true, WrapParam.DEFAULT, WrapParam.DEFAULT);
    }

    /**
     * Load a texture in the assets directory
     * @param assetName
     */
    public Texture(Context context, String assetName, boolean scale,
            WrapParam wrapS, WrapParam wrapT) {
        mName = assetName;
        generateTexture();
        Bitmap bitmap = FileHelper.loadBitmap(context.getAssets(), assetName);
        loadTexture(bitmap, scale, wrapS, wrapT);
        bitmap.recycle();
    }

    /**
     * Constructor for textures not loaded from resource.
     * Notably texture use for render surfaces.
     */
    public Texture() {
        generateTexture();
    }

    private void generateTexture() {
        GLES20.glGenTextures(1, mTextureId, 0);
    }

    /**
     * Load the drawable into a texture.
     *
     * @param context Context
     */
    private void loadTexture(
            Context context, int resourceId, boolean scale,
            WrapParam wrapS, WrapParam wrapT) {
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inScaled = scale;
        Bitmap bitmap = BitmapFactory.decodeResource(
                context.getResources(), resourceId, opt);
        bitmap = Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(),
                Y_FLIP_MATRIX, false);
        // Load texture
        loadTexture(bitmap, scale, wrapS, wrapT);
        bitmap.recycle();
    }

    /**
     * Load a bitmap image into a texture.
     *
     * @param bitmap A bitmap image to load.
     */
    public void loadTexture(
            Bitmap bitmap, boolean scale, WrapParam wrapS, WrapParam wrapT) {
        // Set texture properties
        mWidth = bitmap.getWidth();
        mHeight = bitmap.getHeight();

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId[0]);

        GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_NEAREST);
        GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_NEAREST);
        GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S,
                wrapS.getGlType());
        GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T,
                wrapT.getGlType());

        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
    }

    /**
     * @return the texture ID.
     */
    public int getTextureId() {
        return mTextureId[0];
    }

    /** Get the width in pixels */
    public int getWidth() {
        return mWidth;
    }

    /** Get the height in pixels */
    public int getHeight() {
        return mHeight;
    }

    @Override
    public String toString() {
        return mName;
    }
}
