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
package com.google.fpl.liquidfunpaint;

import com.google.fpl.liquidfunpaint.shader.Material;
import com.google.fpl.liquidfunpaint.shader.ShaderProgram;
import com.google.fpl.liquidfunpaint.shader.Texture;
import com.google.fpl.liquidfunpaint.util.RenderHelper;

import android.content.Context;
import android.opengl.GLES20;

import org.json.JSONObject;

/**
 * ScreenRenderer.
 * Blends a frame buffer as an input onto the final screen.
 */
public class ScreenRenderer {
    private static final String TAG = "ScreenRenderer";
    private Material mMaterial;
    private float mAlphaThreshold;

    public ScreenRenderer(
            Context context, JSONObject json, Texture fboTexture) {
        mMaterial = new Material(
                new ShaderProgram("texture.glslv", "screen.glslf"));

        mMaterial.addAttribute(
                "aPosition", 3, Material.AttrComponentType.FLOAT, 4, false,
                RenderHelper.SCREEN_QUAD_VERTEX_STRIDE);
        mMaterial.addAttribute(
                "aTexCoord", 2, Material.AttrComponentType.FLOAT, 4, false,
                RenderHelper.SCREEN_QUAD_VERTEX_STRIDE);

        // Add the diffuse texture: particle FBO
        mMaterial.addTexture("uDiffuseTexture", fboTexture);

        mMaterial.setBlendFunc(
                Material.BlendFactor.SRC_ALPHA,
                Material.BlendFactor.ONE_MINUS_SRC_ALPHA);

        // Read in values from the JSON file

        // Alpha threshold
        mAlphaThreshold = (float) (json.optDouble("alphaThreshold", 0.0));
    }

    /**
     * Draw function for the geometry that this class owns.
     */
    public void draw(float[] transformFromTexture) {
      RenderHelper.SCREEN_QUAD_VERTEX_BUFFER.rewind();

        mMaterial.beginRender();

        // Set attribute arrays
        mMaterial.setVertexAttributeBuffer(
                "aPosition", RenderHelper.SCREEN_QUAD_VERTEX_BUFFER, 0);
        mMaterial.setVertexAttributeBuffer(
                "aTexCoord", RenderHelper.SCREEN_QUAD_VERTEX_BUFFER, 3);

        // Set per draw uniforms
        GLES20.glUniformMatrix4fv(
                mMaterial.getUniformLocation("uMvpTransform"), 1, false,
                transformFromTexture, 0);
        GLES20.glUniform1f(
                mMaterial.getUniformLocation("uAlphaThreshold"),
                mAlphaThreshold);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 4);

        mMaterial.endRender();
    }
}
