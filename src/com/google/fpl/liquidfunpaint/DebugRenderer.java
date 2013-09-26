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

import com.google.fpl.liquidfun.Color;
import com.google.fpl.liquidfun.Draw;
import com.google.fpl.liquidfun.Transform;
import com.google.fpl.liquidfun.Vec2;
import com.google.fpl.liquidfun.World;
import com.google.fpl.liquidfunpaint.shader.Material;
import com.google.fpl.liquidfunpaint.shader.Material.AttributeInfo;
import com.google.fpl.liquidfunpaint.shader.ShaderProgram;
import com.google.fpl.liquidfunpaint.shader.Texture;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * DebugRenderer for LiquidFun, extending the b2Draw class.
 */
public class DebugRenderer extends Draw {
    private static final int DEBUG_CAPACITY = 20000;
    private static final float DEBUG_OPACITY = 0.8f;
    private static final float DEBUG_AXIS_SCALE = 0.3f;

    private final float[] mTransformFromWorld = new float[16];

    // Shaders, materials, and per-frame buffers for debug drawing
    // We cache all draw*() calls initiated from LiquidFun into buffers, then
    // draw all of them at once in draw() to be more efficient.
    private ShaderProgram mPolygonShader;
    private Material mPolygonMaterial;
    private ByteBuffer mPolygonPositionBuffer;
    private ByteBuffer mPolygonColorBuffer;
    private AttributeInfo mPolygonPositionAttr;
    private AttributeInfo mPolygonColorAttr;

    private ShaderProgram mCircleShader;
    private Material mCircleMaterial;
    private ByteBuffer mCirclePositionBuffer;
    private ByteBuffer mCircleColorBuffer;
    private ByteBuffer mCirclePointSizeBuffer;
    private AttributeInfo mCirclePositionAttr;
    private AttributeInfo mCircleColorAttr;
    private AttributeInfo mCirclePointSizeAttr;

    private ShaderProgram mLineShader;
    private Material mLineMaterial;
    private ByteBuffer mLinePositionBuffer;
    private ByteBuffer mLineColorBuffer;
    private AttributeInfo mLinePositionAttr;
    private AttributeInfo mLineColorAttr;

    public DebugRenderer() {
        mPolygonPositionBuffer = ByteBuffer.allocateDirect(DEBUG_CAPACITY)
                .order(ByteOrder.nativeOrder());
        mPolygonColorBuffer = ByteBuffer.allocateDirect(DEBUG_CAPACITY)
                .order(ByteOrder.nativeOrder());

        mCirclePositionBuffer = ByteBuffer.allocateDirect(DEBUG_CAPACITY)
                .order(ByteOrder.nativeOrder());
        mCircleColorBuffer = ByteBuffer.allocateDirect(DEBUG_CAPACITY)
                .order(ByteOrder.nativeOrder());
        mCirclePointSizeBuffer = ByteBuffer.allocateDirect(DEBUG_CAPACITY)
                .order(ByteOrder.nativeOrder());

        mLinePositionBuffer = ByteBuffer.allocateDirect(DEBUG_CAPACITY)
                .order(ByteOrder.nativeOrder());
        mLineColorBuffer = ByteBuffer.allocateDirect(DEBUG_CAPACITY)
                .order(ByteOrder.nativeOrder());
    }

    /// Helper functions for adding color to a ByteBuffer
    private void addColorToBuffer(
            ByteBuffer buffer, float r, float g, float b) {
        buffer.put((byte) (r * 255));
        buffer.put((byte) (g * 255));
        buffer.put((byte) (b * 255));
        buffer.put((byte) (DEBUG_OPACITY * 255));
    }

    private void addColorToBuffer(ByteBuffer buffer, Color color) {
        addColorToBuffer(buffer, color.getR(), color.getG(), color.getB());
    }

    @Override
    public void drawPolygon(byte[] vertices, int vertexCount, Color color) {
        // This is equivalent to drawing lines with the same color at each
        // vertex
        int elementSize = 8; // We are dealing with 2 floats per vertex
        mLinePositionBuffer.put(vertices, 0 * elementSize, elementSize);
        mLinePositionBuffer.put(vertices, 1 * elementSize, elementSize);

        mLinePositionBuffer.put(vertices, 1 * elementSize, elementSize);
        mLinePositionBuffer.put(vertices, 2 * elementSize, elementSize);

        mLinePositionBuffer.put(vertices, 2 * elementSize, elementSize);
        mLinePositionBuffer.put(vertices, 3 * elementSize, elementSize);

        mLinePositionBuffer.put(vertices, 3 * elementSize, elementSize);
        mLinePositionBuffer.put(vertices, 0 * elementSize, elementSize);

        for (int i = 0; i < 8; ++i) {
            addColorToBuffer(mLineColorBuffer, color);
        }
    }

    @Override
    public void drawSolidPolygon(
            byte[] vertices, int vertexCount, Color color) {
        // Create 2 triangles from the vertices. Not using TRIANGLE_FAN due to
        // batching. Could optimize using TRIANGLE_STRIP.
        // 0, 1, 2, 3 -> (0, 1, 2), (0, 2, 3)
        int elementSize = 8; // We are dealing with 2 floats per vertex
        mPolygonPositionBuffer.put(vertices, 0 * elementSize, elementSize);
        mPolygonPositionBuffer.put(vertices, 1 * elementSize, elementSize);
        mPolygonPositionBuffer.put(vertices, 2 * elementSize, elementSize);

        mPolygonPositionBuffer.put(vertices, 0 * elementSize, elementSize);
        mPolygonPositionBuffer.put(vertices, 2 * elementSize, elementSize);
        mPolygonPositionBuffer.put(vertices, 3 * elementSize, elementSize);

        for (int i = 0; i < 6; ++i) {
            addColorToBuffer(mPolygonColorBuffer, color);
        }
    }

    @Override
    public void drawCircle(Vec2 center, float radius, Color color) {
        mCirclePositionBuffer.putFloat(center.getX());
        mCirclePositionBuffer.putFloat(center.getY());
        addColorToBuffer(mCircleColorBuffer, color);

        float pointSize =
                Math.max(1.0f, Renderer.getInstance().sScreenWidth *
                (2.0f * radius / Renderer.getInstance().sRenderWorldWidth));
        mCirclePointSizeBuffer.putFloat(pointSize);
    }

    @Override
    public void drawSolidCircle(
            Vec2 center, float radius, Vec2 axis, Color color) {
        drawCircle(center, radius, color);

        // Draw the axis line
        float centerX = center.getX();
        float centerY = center.getY();
        addSegmentPoint(
                centerX, centerY, color.getR(), color.getG(), color.getB());
        addSegmentPoint(
                centerX + radius * axis.getX(),
                centerY + radius * axis.getY(),
                color.getR(),
                color.getG(),
                color.getB());
    }

    @Override
    public void drawParticles(
            byte[] centers, float radius, byte[] colors, int count) {
        // Draw them as circles
        mCirclePositionBuffer.put(centers);
        mCircleColorBuffer.put(colors);

        float pointSize =
                Math.max(1.0f, Renderer.getInstance().sScreenWidth *
                (2.0f * radius / Renderer.getInstance().sRenderWorldWidth));
        for (int i = 0; i < count; ++i) {
            mCirclePointSizeBuffer.putFloat(pointSize);
        }
    }

    /// Helper function for drawSegment to avoid making too many native objects
    private void addSegmentPoint(float x, float y, float r, float g, float b) {
        mLinePositionBuffer.putFloat(x);
        mLinePositionBuffer.putFloat(y);
        addColorToBuffer(mLineColorBuffer, r, g, b);
    }

    @Override
    public void drawSegment(Vec2 p1, Vec2 p2, Color color) {
        float r = color.getR();
        float g = color.getG();
        float b = color.getB();
        addSegmentPoint(p1.getX(), p1.getY(), r, g, b);
        addSegmentPoint(p2.getX(), p2.getY(), r, g, b);
    }

    @Override
    public void drawTransform(Transform xf) {
        float posX = xf.getPositionX();
        float posY = xf.getPositionY();

        float sine = xf.getRotationSin();
        float cosine = xf.getRotationCos();

        // X axis -- see b2Vec2::GetXAxis()
        addSegmentPoint(posX, posY, 1.0f, 0.0f, 0.0f);
        addSegmentPoint(
                posX + DEBUG_AXIS_SCALE * cosine,
                posY + DEBUG_AXIS_SCALE * sine,
                1.0f, 0.0f, 0.0f);

        // Y axis -- see b2Vec2::GetYAxis()
        addSegmentPoint(posX, posY, 0.0f, 1.0f, 0.0f);
        addSegmentPoint(
                posX + DEBUG_AXIS_SCALE * -sine,
                posY + DEBUG_AXIS_SCALE * cosine,
                0.0f, 1.0f, 0.0f);
    }

    public void draw() {
        World world = Renderer.getInstance().acquireWorld();
        try {
            resetAllBuffers();

            // This captures everything we need to draw into buffers
            world.drawDebugData();

            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
            GLES20.glViewport(
                    0, 0, Renderer.getInstance().sScreenWidth,
                    Renderer.getInstance().sScreenHeight);
            drawPolygons(mTransformFromWorld);
            drawCircles(mTransformFromWorld);
            drawSegments(mTransformFromWorld);
        } finally {
            Renderer.getInstance().releaseWorld();
        }
    }

    private void drawPolygons(float[] transformFromWorld) {
        mPolygonMaterial.beginRender();

        int numElements = mPolygonPositionBuffer.position() / (4 * 2);

        mPolygonMaterial.setVertexAttributeBuffer(
                mPolygonPositionAttr, mPolygonPositionBuffer, 0);
        mPolygonMaterial.setVertexAttributeBuffer(
                mPolygonColorAttr, mPolygonColorBuffer, 0);

        // Set uniforms
        GLES20.glUniformMatrix4fv(
            mPolygonMaterial.getUniformLocation("uTransform"),
            1,
            false,
            transformFromWorld,
            0);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, numElements);

        mPolygonMaterial.endRender();
    }

    private void drawCircles(float[] transformFromWorld) {
        mCircleMaterial.beginRender();

        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        int numElements = mCirclePointSizeBuffer.position() / 4;

        mCircleMaterial.setVertexAttributeBuffer(
                mCirclePositionAttr, mCirclePositionBuffer, 0);
        mCircleMaterial.setVertexAttributeBuffer(
                mCircleColorAttr, mCircleColorBuffer, 0);
        mCircleMaterial.setVertexAttributeBuffer(
                mCirclePointSizeAttr, mCirclePointSizeBuffer, 0);

        // Set uniforms
        GLES20.glUniformMatrix4fv(
                mCircleMaterial.getUniformLocation("uTransform"),
                1, false, transformFromWorld, 0);

        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, numElements);

        mCircleMaterial.endRender();
    }

    private void drawSegments(float[] transformFromWorld) {
        mLineMaterial.beginRender();

        int numElements = mLinePositionBuffer.position() / (4 * 2);

        mLineMaterial.setVertexAttributeBuffer(
                mLinePositionAttr, mLinePositionBuffer, 0);
        mLineMaterial.setVertexAttributeBuffer(
                mLineColorAttr, mLineColorBuffer, 0);

        // Set uniforms
        GLES20.glUniformMatrix4fv(
                mLineMaterial.getUniformLocation("uTransform"),
                1, false, transformFromWorld, 0);

        GLES20.glDrawArrays(GLES20.GL_LINES, 0, numElements);

        mLineMaterial.endRender();
    }

    public void onSurfaceChanged() {
          Matrix.setIdentityM(mTransformFromWorld, 0);
          Matrix.translateM(mTransformFromWorld, 0, -1, -1, 0);
          Matrix.scaleM(
                  mTransformFromWorld,
                  0,
                  2f / Renderer.getInstance().sRenderWorldWidth,
                  2f / Renderer.getInstance().sRenderWorldHeight,
                  1);
    }

    public void onSurfaceCreated(Context context) {
        // Create all the debug materials we need
        mPolygonShader = new ShaderProgram(
                "no_texture.glslv", "no_texture.glslf");

        mPolygonMaterial = new Material(mPolygonShader);
        mPolygonPositionAttr = mPolygonMaterial.addAttribute(
                "aPosition", 2, Material.AttrComponentType.FLOAT, 4, false, 0);
        mPolygonColorAttr = mPolygonMaterial.addAttribute(
                "aColor", 4, Material.AttrComponentType.UNSIGNED_BYTE,
                1, true, 0);

        mPolygonMaterial.setBlendFunc(
                Material.BlendFactor.SRC_ALPHA,
                Material.BlendFactor.ONE_MINUS_SRC_ALPHA);


        // Instead of making line segments for circles, we use a texture to allow
        // for higher performance
        mCircleShader = new ShaderProgram(
                "pointsprite.glslv", "particle.glslf");

        mCircleMaterial = new Material(mCircleShader);
        mCirclePositionAttr = mCircleMaterial.addAttribute(
                "aPosition", 2, Material.AttrComponentType.FLOAT, 4, false, 0);
        mCircleColorAttr = mCircleMaterial.addAttribute(
                "aColor", 4, Material.AttrComponentType.UNSIGNED_BYTE,
                1, true, 0);
        mCirclePointSizeAttr = mCircleMaterial.addAttribute(
                "aPointSize", 1, Material.AttrComponentType.FLOAT,
                4, false, 0);

        mCircleMaterial.setBlendFunc(
                Material.BlendFactor.SRC_ALPHA,
                Material.BlendFactor.ONE_MINUS_SRC_ALPHA);
        mCircleMaterial.addTexture("uDiffuseTexture",
                new Texture(context, R.drawable.debug_circle));

        mLineShader = new ShaderProgram(
                "no_texture.glslv", "no_texture.glslf");

        mLineMaterial = new Material(mLineShader);
        mLinePositionAttr = mLineMaterial.addAttribute(
                "aPosition", 2, Material.AttrComponentType.FLOAT, 4, false, 0);
        mLineColorAttr = mLineMaterial.addAttribute(
                "aColor", 4, Material.AttrComponentType.UNSIGNED_BYTE,
                1, true, 0);

        mLineMaterial.setBlendFunc(
                Material.BlendFactor.SRC_ALPHA,
                Material.BlendFactor.ONE_MINUS_SRC_ALPHA);

    }

    private void resetAllBuffers() {
        mPolygonPositionBuffer.clear();
        mPolygonColorBuffer.clear();

        mCirclePositionBuffer.clear();
        mCircleColorBuffer.clear();
        mCirclePointSizeBuffer.clear();

        mLinePositionBuffer.clear();
        mLineColorBuffer.clear();
    }
}
