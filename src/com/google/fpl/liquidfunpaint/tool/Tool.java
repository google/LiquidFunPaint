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
package com.google.fpl.liquidfunpaint.tool;

import com.google.fpl.liquidfun.CircleShape;
import com.google.fpl.liquidfun.ParticleColor;
import com.google.fpl.liquidfun.ParticleGroup;
import com.google.fpl.liquidfun.ParticleGroupDef;
import com.google.fpl.liquidfun.ParticleSystem;
import com.google.fpl.liquidfun.Transform;
import com.google.fpl.liquidfun.Vec2;
import com.google.fpl.liquidfunpaint.Renderer;
import com.google.fpl.liquidfunpaint.util.Vector2f;

import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;

import java.nio.ByteBuffer;
import java.util.EnumMap;
import java.util.EnumSet;

/**
 * A class that defines each tool we have for drawing
 */
public abstract class Tool {
    private static final String TAG = "Tool";
    // static members of the class
    private static final EnumMap<ToolType, Tool> TOOL_MAP =
            new EnumMap<ToolType, Tool>(constructToolMap());
    protected static final Transform MAT_IDENTITY;
    // The size of the circle shape we use to create ParticleGroups with.
    // Independent from the particle radius, but should not be smaller than it.
    protected static final float MINIMUM_BRUSHSIZE = 0.18f;

    /**
     * Type of tools
     */
    public enum ToolType {
        MOVE, ERASER, WATER, PENCIL, RIGID,
    }

    /**
     * Tool operations
     */
    protected enum ToolOperation {
        ADD_PARTICLES, REMOVE_PARTICLES,
    }

    // Static variables
    // Only one tool is active at a time, and all PointerInfos should be global
    private static SparseArray<PointerInfo> mGroupMap =
            new SparseArray<PointerInfo>();

    // Member variables of the class
    private ToolType mType;
    protected int mParticleFlags = 0;
    protected int mParticleGroupFlags = 0;
    protected float mBrushSize = MINIMUM_BRUSHSIZE;
    // Default for a tool is to both add and remove particles
    protected EnumSet<ToolOperation> mOperations =
            EnumSet.allOf(ToolOperation.class);

    // member native (C++) variables
    protected ParticleColor mColor = new ParticleColor();
    protected Vec2 mVelocity = new Vec2(0, 0);
    // This variable is a temporary variable to allow us to destroy particles.
    protected CircleShape mShape = new CircleShape();

    static {
        MAT_IDENTITY = new Transform();
        MAT_IDENTITY.setIdentity();
    }

    /** Initializes all the different tools */
    private static EnumMap<ToolType, Tool> constructToolMap() {
        EnumMap<ToolType, Tool> toolMap =
                new EnumMap<ToolType, Tool>(ToolType.class);

        Tool moveTool = new MoveTool();
        toolMap.put(ToolType.MOVE, moveTool);

        Tool eraserTool = new EraserTool();
        toolMap.put(ToolType.ERASER, eraserTool);

        Tool waterTool = new WaterTool();
        toolMap.put(ToolType.WATER, waterTool);

        Tool pencilTool = new PencilTool();
        toolMap.put(ToolType.PENCIL, pencilTool);

        Tool rigidTool = new RigidTool();
        toolMap.put(ToolType.RIGID, rigidTool);

        return toolMap;
    }

    /** Returns the tool based on the type */
    public static Tool getTool(ToolType type) {
        return TOOL_MAP.get(type);
    }

    /** Goes through all tools and call reset() */
    public static void resetAllTools() {
        for (Tool tool : TOOL_MAP.values()) {
            tool.reset();
        }
    }

    public Tool(ToolType type) {
        mType = type;
    }

    @Override
    protected void finalize() {
        // clean up native variables
        mColor.delete();
        mVelocity.delete();
        mShape.delete();
    }

    public ToolType getType() {
        return mType;
    }

    public void setColor (int color) {
        // Convert ABGR back into ParticleColor
        // Box2D doesn't have this functionality,
        // check why color is stored as an int to begin with.
        short a = (short) (color >> 24 & 0xFF);
        short b = (short) (color >> 16 & 0xFF);
        short g = (short) (color >> 8 & 0xFF);
        short r = (short) (color & 0xFF);
        mColor.set(r, g, b, a);
    }

    public int getParticleGroupFlags() {
        return mParticleGroupFlags;
    }

    public void onTouch(View v, MotionEvent e) {
        switch (e.getActionMasked()) {
          case MotionEvent.ACTION_DOWN:
          case MotionEvent.ACTION_POINTER_DOWN: {
              int pointerIndex = e.getActionIndex();
              int pID = e.getPointerId(pointerIndex);
              // Create new PointerInfo as this is a new pointer
              PointerInfo pInfo = new PointerInfo(pID);
              assert (mGroupMap.get(pID) == null);
              processTouchInput(v, e, pInfo, pointerIndex, true, true);
              // Put updated PointerInfo back in map
              mGroupMap.put(pID, pInfo);
              break;
          }
          case MotionEvent.ACTION_MOVE: {
              for (int pointerIndex = 0;
                      pointerIndex < e.getPointerCount();
                      ++pointerIndex) {
                  int pID = e.getPointerId(pointerIndex);
                  // Get cached PointerInfo
                  PointerInfo pInfo = mGroupMap.get(pID);
                  assert (pInfo != null);
                  processTouchInput(v, e, pInfo, pointerIndex, true, true);
                  // Put updated PointerInfo back in map
                  mGroupMap.put(pID, pInfo);
              }
              break;
          }
          case MotionEvent.ACTION_UP:
          case MotionEvent.ACTION_POINTER_UP:{
              int pointerIndex = e.getActionIndex();
              int pID = e.getPointerId(pointerIndex);
              // Get cached PointerInfo
              PointerInfo pInfo = mGroupMap.get(pID);
              assert (pInfo != null);
              processTouchInput(v, e, pInfo, pointerIndex, true, true);
              // Pointer is up -- end the action.
              endAction(pID);
              break;
          }
          case MotionEvent.ACTION_CANCEL: {
              // All pointers are cancelled, call endAction() on them.
              for (int i = 0; i < mGroupMap.size(); ++i) {
                  int pID = mGroupMap.keyAt(i);
                  endAction(pID);
              }
              mGroupMap.clear();
              break;
          }
          default:
              break;
        }
    }

    protected void clampToWorld(Vector2f worldPoint, float border) {
        worldPoint.x = Math.max(border,
                Math.min(
                    worldPoint.x,
                    Renderer.getInstance().sRenderWorldWidth - border));
        worldPoint.y = Math.max(border,
                Math.min(
                    worldPoint.y,
                    Renderer.getInstance().sRenderWorldHeight - border));
    }

    /**
     * Initializes a touch event
     * @param pInfo The pointer info associated with this touch event
     * @param worldPoint First point of this touch
     */
    protected void initPointerInfo(PointerInfo pInfo, Vector2f worldPoint) {
        pInfo.init(worldPoint, true);
    }

    /**
     * Updates a touch event
     * @param pInfo The pointer info associated with this touch event
     * @param worldPoint First point of this touch
     */
    protected void updatePointerInfo(PointerInfo pInfo, Vector2f worldPoint) {
        pInfo.update(worldPoint);
    }

    /**
     * This function transforms screen pixel coordinates, from touch events,
     * into world coordinates, then apply the current tool to it.
     * It also interpolates between the last world coordinates we looked at,
     * which is stored in PointerInfo, and the current world coordinates,
     * and generates in-between points, so we can also apply the current tool
     * to it.
     * @param v The current view
     * @param pInfo The current PointerInfo so we can get the last point
     * @param screenX The pixel X on screen (as generated by MotionEvent)
     * @param screenY The pixel Y on screen (as generated by MotionEvent)
     * @param interpolatePoints If true, we generate more points in between
     *                           values for fine-grained input to the tool
     * @return The modified PointerInfo
     */
    private PointerInfo applyToolAcrossRange(
            View v, PointerInfo pInfo, float screenX, float screenY,
            boolean interpolatePoints) {
        float radius = mBrushSize / 2;

        Vector2f worldPoint = new Vector2f(
                Renderer.getInstance().sRenderWorldWidth
                    * screenX / v.getWidth(),
                Renderer.getInstance().sRenderWorldHeight *
                    (v.getHeight() - screenY)
                / v.getHeight());
        clampToWorld(worldPoint, radius);

        // Initialize this touch event, specifically the buffers
        initPointerInfo(pInfo, worldPoint);

        if (interpolatePoints) {
            // Now generate in-between points
            int pointCount = (int) (
                    Vector2f.length(worldPoint, pInfo.getWorldPoint()) / radius);
            for (int j = 0; j < pointCount; ++j) {
                Vector2f incr = Vector2f.lerpFixedInterval(
                        pInfo.getWorldPoint(), worldPoint, j, pointCount);
                pInfo.putPoint(incr);
            }
        }

        // Check if the buffer needs flushing
        if (pInfo.needsFlush()) {
            applyTool(pInfo);
            pInfo.resetBuffer();
        }

        // Update the pointerInfo with the first point of this touch event.
        // PointerInfo contains the previous touch event for interpolation
        // so we update when the previous event info is not needed anymore.
        updatePointerInfo(pInfo, worldPoint);

        return pInfo;
    }

    /**
     * Looks at the history of the touch input, interpolate them, and use
     * the points generate to make CircleShapes that will aid in particle
     * creation or destruction.
     * It also distinguishes between touch inputs from different fingers.
     * @param v The current view
     * @param e The motion event
     * @param useMotionHistory If true, we look at the historical values in
     *                         addition to the current (x, y) value
     * @param interpolatePoints If true, we generate more points in between
     *                           values for fine-grained input to the tool
     */
    protected void processTouchInput(
            View v, MotionEvent e, PointerInfo pInfo, int pointerIndex,
            boolean useMotionHistory, boolean interpolatePoints) {
        if (useMotionHistory) {
            // Look into the historical x's and y's if needed
            for (int h = 0; h < e.getHistorySize(); ++h) {
                applyToolAcrossRange(
                        v, pInfo,
                        e.getHistoricalX(pointerIndex, h),
                        e.getHistoricalY(pointerIndex, h),
                        interpolatePoints);
            }
        }

        // Then look into the current x and y
        applyToolAcrossRange(
                v, pInfo, e.getX(pointerIndex), e.getY(pointerIndex),
                interpolatePoints);
    }

    /** End this tool's current action */
    protected void endAction(int pointerId) {
        mGroupMap.remove(pointerId);
        if (mGroupMap.size() == 0) {
            PointerInfo.resetGlobalBuffer();
        }
    }

    /** Reset the tool */
    protected void reset() {
        PointerInfo.resetGlobalBuffer();
    }

    /**
      * @param pInfo The pointer info containing information for creating
      *              particle groups.
      */
    protected void applyTool(PointerInfo pInfo) {
        float radius = mBrushSize / 2;

        ByteBuffer buffer = pInfo.getRawPointsBuffer();

        ParticleGroupDef pgd = null;
        if (mOperations.contains(ToolOperation.ADD_PARTICLES)) {
            pgd = new ParticleGroupDef();
            pgd.setFlags(mParticleFlags);
            pgd.setGroupFlags(mParticleGroupFlags);
            pgd.setLinearVelocity(mVelocity);
            pgd.setColor(mColor);
            buffer.position(pInfo.getBufferStart());
            pgd.setCircleShapesFromVertexList(
                    buffer.slice(), pInfo.getNumPoints(),
                    radius);
        }

        ParticleSystem ps = Renderer.getInstance().acquireParticleSystem();
        try {
            if (mOperations.contains(ToolOperation.REMOVE_PARTICLES)) {
                buffer.position(pInfo.getBufferStart());
                mShape.setRadius(radius);
                // Goes through each (x,y) pair and queries for the particles in
                // the circle shape to be destroyed.
                for (int i = 0; i < pInfo.getNumPoints(); ++i) {
                    mShape.setPosition(
                            buffer.getFloat(), buffer.getFloat());
                    ps.destroyParticlesInShape(mShape, MAT_IDENTITY);
                }
            }

            // Create ParticleGroup
            if (pgd != null) {
                // Join to existing group if the group has the same flags
                ParticleGroup pGroup = ps.createParticleGroup(pgd);
                ParticleGroup existingGroup = pInfo.getParticleGroup();
                if ((existingGroup == null) ||
                    (existingGroup.getGroupFlags() != pgd.getGroupFlags())) {
                    pInfo.setParticleGroup(pGroup);
                } else {
                    ps.joinParticleGroups(existingGroup, pGroup);
                }

                // Clean up native objects
                pgd.delete();
            }
        } finally {
            Renderer.getInstance().releaseParticleSystem();
        }
    }

    /**
     * These methods are called by Controller, when new tools are selected.
     * It allows for the tools to register/de-register themselves from different
     * listener or observer classes.
     */
    public void deactivate() {}
    public void activate() {}
}
