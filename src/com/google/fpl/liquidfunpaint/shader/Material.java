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

import android.opengl.GLES20;
import android.util.Log;

import java.nio.Buffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * A layer on top of ShaderProgram to store specific parameters to be reused.
 * It stores render states and textures that we will set in conjunction with
 * a specific ShaderProgram -- allowing the ShaderProgram to be reused with
 * different parameters.
 */
public class Material {
    private static final String TAG = "Material";

    /**
     * Defines which component types are accepted.
     * OpenGL ES simply has these as global constants but we want to type check.
     * According to some sources (not benchmarked), enums can take up space
     * and are slower. Investigate these if we see related performance issues.
     */
    public enum AttrComponentType {
        BYTE(GLES20.GL_BYTE),
        UNSIGNED_BYTE(GLES20.GL_UNSIGNED_BYTE),
        SHORT(GLES20.GL_SHORT),
        UNSIGNED_SHORT(GLES20.GL_UNSIGNED_SHORT),
        FIXED(GLES20.GL_FIXED),
        FLOAT(GLES20.GL_FLOAT);

        private final int mGlComponentType;
        private AttrComponentType(int glComponentType) {
            mGlComponentType = glComponentType;
        }

        protected int getGlType() {
            return mGlComponentType;
        }
    }

    /**
     * Defines which blend types are accepted.
     * OpenGL ES simply has these as global constants but we want to type check.
     */
    public enum BlendFactor {
        ZERO(GLES20.GL_ZERO),
        ONE(GLES20.GL_ONE),
        SRC_COLOR(GLES20.GL_SRC_COLOR),
        ONE_MINUS_SRC_COLOR(GLES20.GL_ONE_MINUS_SRC_COLOR),
        DST_COLOR(GLES20.GL_DST_COLOR),
        ONE_MINUS_DST_COLOR(GLES20.GL_ONE_MINUS_DST_COLOR),
        SRC_ALPHA(GLES20.GL_SRC_ALPHA),
        ONE_MINUS_SRC_ALPHA(GLES20.GL_ONE_MINUS_SRC_ALPHA),
        DST_ALPHA(GLES20.GL_DST_ALPHA),
        ONE_MINUS_DST_ALPHA(GLES20.GL_ONE_MINUS_DST_ALPHA),
        CONSTANT_COLOR(GLES20.GL_CONSTANT_COLOR),
        ONE_MINUS_CONSTANT_COLOR(GLES20.GL_ONE_MINUS_CONSTANT_COLOR),
        CONSTANT_ALPHA(GLES20.GL_CONSTANT_ALPHA),
        ONE_MINUS_CONSTANT_ALPHA(GLES20.GL_ONE_MINUS_CONSTANT_ALPHA);

        private final int mGlBlendFactor;
        private BlendFactor(int glBlendFactor) {
          mGlBlendFactor = glBlendFactor;
        }

        protected int getGlType() {
            return mGlBlendFactor;
        }
    }

    /**
     * A class for defining the specific attribute's extra info
     * like type, stride, etc
     */
    public class AttributeInfo {
        String mName;
        int mNumComponents;
        AttrComponentType mComponentType;
        int mComponentSize;
        boolean mNormalized;
        int mStride;
        int mLocation;

        public AttributeInfo(
                String name, int numComponents, AttrComponentType componentType,
                int componentSize, boolean normalized, int stride, int location) {
            mName = name;
            mNumComponents = numComponents;
            mComponentType = componentType;
            mComponentSize = componentSize;
            mNormalized = normalized;
            mStride = stride;
            mLocation = location;

            if (location < 0) {
                Log.e(TAG, "Invalid vertex attribute location" + name +
                           "! Is the name spelled correctly?");
            }
        }
    }

    /**
     * A class for storing render states to be set at the beginning of render.
     */
    private class RenderState {
        boolean mEnableBlend = false;
        // These defaults are the OpenGL defaults
        BlendFactor mBlendColorSFactor = BlendFactor.ONE;
        BlendFactor mBlendColorDFactor = BlendFactor.ZERO;
    }

    /// Member variables
    protected ShaderProgram mShader = null;
    private Map<String, AttributeInfo> mVertexAttributes =
        new HashMap<String, AttributeInfo>();
    private Map<String, Texture> mTextures = new HashMap<String, Texture>(1);
    private RenderState mRenderState = new RenderState();

    /// Member methods

    public Material(ShaderProgram shader) {
        // Don't take the shader if it's not generated correctly
        if (shader != null && shader.isShaderCompiled()) {
            mShader = shader;
        } else {
            throw new IllegalArgumentException(
                    "ShaderProgram " + shader + " is not valid! Failed to " +
                    "assign to new Material.");
        }
    }

    public AttributeInfo addAttribute(
            String name, int numComponents, AttrComponentType componentType,
            int componentSize, boolean normalized, int stride) {
        int location = mShader.getAttributeLocation(name);
        AttributeInfo attr = new AttributeInfo(
                name, numComponents, componentType,
                componentSize, normalized, stride, location);
        mVertexAttributes.put(name, attr);
        return attr;
    }

    public void addTexture(String textureUniformName, Texture texture) {
        mTextures.put(textureUniformName, texture);

        if (GLES20.GL_TEXTURE0 + mTextures.size() >
                GLES20.GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS) {
            Log.e(TAG, "Too many textures in material! Failed to add: " +
                    texture);
        }
    }

    public void beginRender() {
        mShader.beginRender();

        // Set render states
        if (mRenderState.mEnableBlend) {
            GLES20.glEnable(GLES20.GL_BLEND);
            GLES20.glBlendFunc(
                    mRenderState.mBlendColorSFactor.getGlType(),
                    mRenderState.mBlendColorDFactor.getGlType());
        }

        // enable all vertex attributes we have info on
        for (AttributeInfo attr : mVertexAttributes.values()) {
            GLES20.glEnableVertexAttribArray(attr.mLocation);
        }

        // enable all textures
        int textureIdx = 0;
        for (Entry<String, Texture> texture : mTextures.entrySet()) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + textureIdx);
            GLES20.glBindTexture(
                    GLES20.GL_TEXTURE_2D, texture.getValue().getTextureId());

            // Set the correct uniform
            GLES20.glUniform1i(
                    getUniformLocation(texture.getKey()), textureIdx);

            // Get next texture index
            ++textureIdx;
        }
    }

    public void endRender() {
        // disable all textures
        for (int i = 0; i < mTextures.size(); ++i) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + i);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        }

        // enable all vertex attributes
        for (AttributeInfo attr : mVertexAttributes.values()) {
            GLES20.glDisableVertexAttribArray(attr.mLocation);
        }

        // Reset render states
        if (mRenderState.mEnableBlend) {
            GLES20.glDisable(GLES20.GL_BLEND);
        }

        mShader.endRender();
    }

    public void setVertexAttributeBuffer(
            AttributeInfo attr, Buffer buffer, int offset) {
        buffer.position(offset);
        GLES20.glVertexAttribPointer(
                attr.mLocation, attr.mNumComponents,
                attr.mComponentType.getGlType(), attr.mNormalized,
                attr.mStride, buffer);
    }

    public void setVertexAttributeBuffer(
            String name, Buffer buffer, int offset) {
        AttributeInfo attr = mVertexAttributes.get(name);
        buffer.position(offset);
        GLES20.glVertexAttribPointer(
                attr.mLocation, attr.mNumComponents,
                attr.mComponentType.getGlType(), attr.mNormalized,
                attr.mStride, buffer);
    }

    /**
     * Provide access to the ShaderProgram function
     */
    public int getUniformLocation(String name) {
        int location = mShader.getUniformLocation(name);
        if (location < 0) {
            Log.e(TAG, "Invalid uniform location for " + name +
                    " Is the name spelled correctly?");
        }
        return location;
    }

    public void setBlendFunc(BlendFactor sFactor, BlendFactor dFactor) {
        // Optimize for (ONE, ZERO) -- that's the same as no blending.
        if (sFactor == BlendFactor.ONE && dFactor == BlendFactor.ZERO) {
            mRenderState.mEnableBlend = false;
        } else {
            mRenderState.mEnableBlend = true;
            mRenderState.mBlendColorSFactor = sFactor;
            mRenderState.mBlendColorDFactor = dFactor;
        }
    }
}
