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

import android.opengl.GLES20;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Arrays;

/**
 * Renderer to draw textures.
 */
public class TextureRenderer {
    private ShaderProgram mTextureShader;
    private Material mTextureMaterial;

    // Temporary variables for drawing purposes
    private float[] uvTransform = new float[16];

    private final FloatBuffer mPositionBuffer;
    private final FloatBuffer mTexCoordBuffer;

    private static final TextureRenderer INSTANCE = new TextureRenderer();

    private TextureRenderer() {
        mPositionBuffer = ByteBuffer.allocateDirect(8 * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();

        float data[] = new float[] {
                0, 0, 1, 0, 0, 1, 1, 1
        };
        mTexCoordBuffer = ByteBuffer.allocateDirect(8 * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mTexCoordBuffer.put(data);
    }

    /**
     * @return The singleton.
     */
    public static TextureRenderer getInstance() {
        return INSTANCE;
    }

    /**
     * Inform the renderer that the surface is created or recreated.
     */
    public void onSurfaceCreated() {
        mTextureShader = new ShaderProgram("texture.glslv", "texture.glslf");

        mTextureMaterial = new Material(mTextureShader);
        mTextureMaterial.addAttribute(
                "aPosition", 2, Material.AttrComponentType.FLOAT, 4, false, 0);
        mTextureMaterial.addAttribute(
                "aTexCoord", 2, Material.AttrComponentType.FLOAT, 4, false, 0);
        mTextureMaterial.setBlendFunc(
                Material.BlendFactor.ONE,
                Material.BlendFactor.ONE_MINUS_SRC_ALPHA);
    }

    /**
     * Draw a texture within a rectangle, with default params.
     *
     * @param texture A texture to draw
     * @param transform Matrix to transform to screen coordinates
     * @param left Left coordinate of a rectangle to draw the texture within
     * @param bottom Bottom coordinate of a rectangle to draw the texture within
     * @param right Right coordinate of a rectangle to draw the texture within
     * @param top Top coordinate of a rectangle to draw the texture within
     */
    public void drawTexture(
            Texture texture, float transform[],
            float left, float bottom, float right, float top) {
        drawTexture(
                texture, transform, Renderer.MAT4X4_IDENTITY,
                left, bottom, right, top, 1.0f, false);
    }

    /**
     * Draw a texture within a rectangle.
     *
     * @param texture A texture to draw
     * @param inTransform Matrix to transform from the coordinate system to
     *                  the OpenGL screen one
     * @param inUvTransform Matrix for UV transformations.
     * @param left Left coordinate of a rectangle to draw the texture within
     * @param bottom Bottom coordinate of a rectangle to draw the texture within
     * @param right Right coordinate of a rectangle to draw the texture within
     * @param top Top coordinate of a rectangle to draw the texture within
     * @param alphaScale The alpha scale to apply to the alpha of the texture
     * @param noScale If true, we will scale UVs to keep the aspect ratio and
     *                size of the texture for tiling
     */
    public void drawTexture(
            Texture texture, float inTransform[], float inUvTransform[],
            float left, float bottom, float right, float top,
            float alphaScale, boolean noScale) {
        setRect(left, bottom, right, top);

        uvTransform = Arrays.copyOf(inUvTransform, uvTransform.length);

        if (noScale) {
            // We first calculate the actual screen dimensions to be drawn.
            // left/bottom/right/top spans [-1, 1], so (right - left) / 2 will
            // give us the % of Renderer.sScreenWidth we will be drawing on.
            // Then we calculate the ratio of the texture's width to the
            // previous value to get how much the texture's UVs should be
            // scaled. The UV will be scaled inverse proportionally.
            float widthUvScale =
                    (right - left) / 2 * Renderer.getInstance().sScreenWidth /
                    texture.getWidth();
            float heightUvScale =
                    (top - bottom) / 2 * Renderer.getInstance().sScreenHeight /
                    texture.getHeight();
            Matrix.scaleM(uvTransform, 0, widthUvScale, heightUvScale, 1);
        }

        mTexCoordBuffer.rewind();
        mPositionBuffer.rewind();

        mTextureMaterial.beginRender();

        // We set our own texture here to be bound
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture.getTextureId());

        // Set attribute arrays
        mTextureMaterial.setVertexAttributeBuffer(
                "aPosition", mPositionBuffer, 0);
        mTextureMaterial.setVertexAttributeBuffer(
                "aTexCoord", mTexCoordBuffer, 0);

        // Set uniforms
        // Set texture uniform explicitly here because it is passed in
        GLES20.glUniform1i(
                mTextureMaterial.getUniformLocation("uDiffuseTexture"), 0);
        GLES20.glUniformMatrix4fv(
                mTextureMaterial.getUniformLocation("uMvpTransform"),
                1, false, inTransform, 0);
        GLES20.glUniformMatrix4fv(
                mTextureMaterial.getUniformLocation("uUvTransform"),
                1, false, uvTransform, 0);
        GLES20.glUniform1f(
              mTextureMaterial.getUniformLocation("uAlphaScale"), alphaScale);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        mTextureMaterial.endRender();
    }

    private void setRect(float left, float bottom, float right, float top) {
        float[] data = new float[] {
                left, bottom, right, bottom, left, top, right, top
        };
        mPositionBuffer.position(0);
        mPositionBuffer.put(data);
    }
}
