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

import com.google.fpl.liquidfun.Fixture;
import com.google.fpl.liquidfun.ParticleSystem;
import com.google.fpl.liquidfun.QueryCallback;
import com.google.fpl.liquidfunpaint.Renderer;
import com.google.fpl.liquidfunpaint.util.Vector2f;

import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;

import java.util.Observable;
import java.util.Observer;
import java.util.Vector;

/**
 * Pencil tool
 * We create particle groups per draw, but we don't need to join them.
 * Particle groups are merely used to mimic the shape of a stroke.
 */
public class MoveTool extends Tool implements Observer {
    private MoveQueryCallback mCb = new MoveQueryCallback();
    private SparseArray<Vector<ParticleQueryResult>> mPointerResultList =
            new SparseArray<Vector<ParticleQueryResult>>();

    /**
     * Stores the Query result that LiquidFun returns, along with the delta
     * of the particle's position w.r.t. to the center of the shape we tested.
     */
    private class ParticleQueryResult {
        /**
         * Index of the particle in the LiquidFun particle system
         */
        int mIndex;
        /**
         * PointerInfo that contains the pointerID (from MotionEvent), and
         * the current location of the pointer.
         */
        PointerInfo mPInfo;
        /**
         * Delta of the particle's original position from the pointer's original
         * location.
         */
        Vector2f mDelta;

        private ParticleQueryResult(
                int index, PointerInfo pInfo, Vector2f delta) {
            mIndex = index;
            mPInfo = pInfo;
            mDelta = delta;
        }
    }

    /**
     * Callback function for LiquidFun queries, specifically for MoveTool.
     */
    private class MoveQueryCallback extends QueryCallback {
        /**
         * Pointer to MoveTool for access to functions.
         */
        private MoveTool mMoveTool;
        /**
         * PointerInfo that contains the pointerID (from MotionEvent), and
         * the current location of the pointer.
         */
        private PointerInfo mPInfo;

        private MoveQueryCallback() {}

        public void set(MoveTool tool, PointerInfo pInfo) {
            mMoveTool = tool;
            mPInfo = pInfo;
        }

        @Override
        public boolean reportFixture(Fixture fixture) {
            return false;
        }

        @Override
        public boolean reportParticle(ParticleSystem ps, int index) {
            // Store the distance vector from the center point
            // to the actual particle point as the query has a radius.
            Vector2f p = new Vector2f(
                    ps.getParticlePositionX(index),
                    ps.getParticlePositionY(index));
            Vector2f delta = mPInfo.getWorldPoint().sub(p);

            mMoveTool.addParticle(
                    mPInfo.getPointerId(),
                    new ParticleQueryResult(index, mPInfo, delta));
            return true;
        }
    }

    public MoveTool() {
        super(ToolType.MOVE);
        // Set the move tool size a bit bigger to make it easier to move
        mBrushSize = MINIMUM_BRUSHSIZE * 1.5f;
        mOperations.remove(ToolOperation.ADD_PARTICLES);
        mOperations.remove(ToolOperation.REMOVE_PARTICLES);
    }

    @Override
    public void finalize() {
        // Clean up native objects
        mCb.delete();
    }

    /**
      * @param pInfo The pointer info containing the previous group info
      * @param worldPoint The point we are initializing the touch with. We will
      *                   grab onto this point for the MoveTool.
      */
    @Override
    protected void initPointerInfo(PointerInfo pInfo, Vector2f worldPoint) {
        pInfo.init(worldPoint, false);
    }

    @Override
    protected void updatePointerInfo(PointerInfo pInfo, Vector2f worldPoint) {
        if (pInfo.isNewPointer()) {
            ParticleSystem ps = Renderer.getInstance().acquireParticleSystem();
            try {
                mCb.set(this, pInfo);
                mShape.setPosition(worldPoint.x, worldPoint.y);
                mShape.setRadius(mBrushSize / 2);
                ps.queryShapeAABB(mCb, mShape, MAT_IDENTITY);
            } finally {
                Renderer.getInstance().releaseParticleSystem();
            }
        }
    }


    /**
     * The move tool only needs to know where the touch point is at the moment
     * of capture -- it doesn't need to look at the history or have fine-grained
     * control via additional generated points.
     */
    @Override
    protected void processTouchInput(
            View v, MotionEvent e, PointerInfo pInfo, int pointerIndex,
            boolean useMotionHistory, boolean interpolatePoints) {
        super.processTouchInput(v, e, pInfo, pointerIndex, false, false);
    }

    @Override
    protected void endAction(int pointerId) {
        super.endAction(pointerId);

        Vector<ParticleQueryResult> particleList =
            mPointerResultList.get(pointerId);
        if (particleList != null) {
            particleList.clear();
        }
    }

    @Override
    public void deactivate() {
        Renderer.getInstance().deleteObserver(this);
    }

    @Override
    public void activate() {
        Renderer.getInstance().addObserver(this);
    }

    /**
     * Adds a particle with its position delta to the center of the touch event.
     */
    protected void addParticle(int pointerId, ParticleQueryResult pResult) {
        Vector<ParticleQueryResult> particleList =
                mPointerResultList.get(pointerId);
        if (particleList == null) {
            particleList = new Vector<ParticleQueryResult>();
            mPointerResultList.put(pointerId, particleList);
        }
        particleList.add(pResult);
    }

    // This is called from the Update thread. We only update particle velocity
    // once per frame.
    @Override
    public void update(Observable obj, Object arg) {
        // Scale the velocity by the framerate. However the max is still
        // limited by LiquidFun so the particles won't snap to finger.
        assert arg instanceof Float;
        float velocityScale = 1 / (Float) arg;

        ParticleSystem ps = Renderer.getInstance().acquireParticleSystem();
        try {
            for (int i = 0; i < mPointerResultList.size(); ++i) {
                Vector<ParticleQueryResult> particleList =
                        mPointerResultList.valueAt(i);
                for (ParticleQueryResult particle : particleList) {
                    Vector2f p = new Vector2f(
                            ps.getParticlePositionX(particle.mIndex),
                            ps.getParticlePositionY(particle.mIndex));

                    // We don't want all our particles to move to the same
                    // point as it will cause instability.
                    // Account for the delta that we stored earlier.
                    Vector2f projectedP =
                            particle.mPInfo.getWorldPoint().sub(particle.mDelta);

                    // The net velocity is:
                    // [(Particle's new location relative to current pointer loc)
                    // - (particle's old location)]
                    // The reason for not using the velocity vector [(current
                    // pointer location) - (previous pointer location)] is
                    // because we are adding impulses to the particles directly,
                    // and they might not have arrived at the previous pointer
                    // location due to velocity limits and other calculations in
                    // LiquidFun.
                    Vector2f velocity = projectedP.sub(p);
                    velocity = velocity.mul(velocityScale);

                    ps.setParticleVelocity(
                            particle.mIndex, velocity.x, velocity.y);
                }
            }
        } finally {
            Renderer.getInstance().releaseParticleSystem();
        }
    }
}
