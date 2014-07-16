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

import android.opengl.GLES20;

/**
 * BlurRenderer.
 * This is the blur renderer. It takes an input texture, and uses a blur
 * shader to blur it into an output RenderSurface.
 * Blur shader is hard-coded to use gaussian blur, with a sigma or 2.3 and a
 * kernel size of 5.
 */
public class BlurRenderer {
    private static final String TAG = "BlurRenderer";
    private static final String BLUR_TEXTURE_NAME = "uBlurTexture";
    // Small intermediate framebuffer since we do want the image to lose a bit
    // of detail and this allows the fragment shader computation to go way
    // faster.
    private static final int FB_SIZE = 128;

    private RenderSurface mBlurSurface;

    private Material mXBlurMaterial;
    private Material mYBlurMaterial;

    public BlurRenderer() {
        mXBlurMaterial = new Material(
                new ShaderProgram("x_blur.glslv", "blur.glslf"));
        mYBlurMaterial = new Material(
                new ShaderProgram("y_blur.glslv", "blur.glslf"));

        mXBlurMaterial.addAttribute(
                "aPosition", 3, Material.AttrComponentType.FLOAT, 4, false,
                RenderHelper.SCREEN_QUAD_VERTEX_STRIDE);
        mXBlurMaterial.addAttribute(
                "aTexCoord", 2, Material.AttrComponentType.FLOAT, 4, false,
                RenderHelper.SCREEN_QUAD_VERTEX_STRIDE);
        mYBlurMaterial.addAttribute(
                "aPosition", 3, Material.AttrComponentType.FLOAT, 4, false,
                  RenderHelper.SCREEN_QUAD_VERTEX_STRIDE);
        mYBlurMaterial.addAttribute(
                "aTexCoord", 2, Material.AttrComponentType.FLOAT, 4, false,
                RenderHelper.SCREEN_QUAD_VERTEX_STRIDE);

        mBlurSurface = new RenderSurface(FB_SIZE, FB_SIZE);
    }

    /**
     * Draw function for the geometry that this class owns.
     */
    public void draw(Texture inputTexture, RenderSurface outputSurface) {
        // X-blur: Blur into a temporary surface
        mBlurSurface.beginRender(0);
        mXBlurMaterial.beginRender();

        RenderHelper.SCREEN_QUAD_VERTEX_BUFFER.rewind();

        // Set attribute arrays
        mXBlurMaterial.setVertexAttributeBuffer(
                "aPosition", RenderHelper.SCREEN_QUAD_VERTEX_BUFFER, 0);
        mXBlurMaterial.setVertexAttributeBuffer(
                "aTexCoord", RenderHelper.SCREEN_QUAD_VERTEX_BUFFER, 3);

        // Set the input texture
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(
                GLES20.GL_TEXTURE_2D, inputTexture.getTextureId());

        // Set the correct uniforms
        GLES20.glUniform1i(
                mXBlurMaterial.getUniformLocation(BLUR_TEXTURE_NAME), 0);
        GLES20.glUniform1f(
                mXBlurMaterial.getUniformLocation("uBlurBufferSize"),
                1.0f / FB_SIZE);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 4);

        mXBlurMaterial.endRender();
        mBlurSurface.endRender();

        // Issue a flush call to make sure previous frame buffer commands are
        // sent asap, as we are using it in the next render call.
        GLES20.glFlush();

        // Y-blur: blur into spcified output surface
        outputSurface.beginRender(0);
        mYBlurMaterial.beginRender();

        // Set attribute arrays
        mYBlurMaterial.setVertexAttributeBuffer(
                "aPosition", RenderHelper.SCREEN_QUAD_VERTEX_BUFFER, 0);
        mYBlurMaterial.setVertexAttributeBuffer(
                "aTexCoord", RenderHelper.SCREEN_QUAD_VERTEX_BUFFER, 3);

        // Set texture
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(
                GLES20.GL_TEXTURE_2D, mBlurSurface.getTexture().getTextureId());

        // Set the correct uniform
        GLES20.glUniform1i(
                mYBlurMaterial.getUniformLocation(BLUR_TEXTURE_NAME), 0);
        GLES20.glUniform1f(
                mYBlurMaterial.getUniformLocation("uBlurBufferSize"),
                1.0f / FB_SIZE);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 4);

        mYBlurMaterial.endRender();
        outputSurface.endRender();
    }
}
