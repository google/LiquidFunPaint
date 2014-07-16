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

import com.google.fpl.liquidfun.Body;
import com.google.fpl.liquidfun.BodyDef;
import com.google.fpl.liquidfun.Draw;
import com.google.fpl.liquidfun.ParticleSystem;
import com.google.fpl.liquidfun.ParticleSystemDef;
import com.google.fpl.liquidfun.PolygonShape;
import com.google.fpl.liquidfun.World;
import com.google.fpl.liquidfunpaint.shader.ShaderProgram;

import android.app.Activity;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;
import android.widget.TextView;

import java.util.Observable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Renderer class. Contains the game update and render loop.
 *
 * This also contains the pointer to the LiquidFun world. The convention for
 * thread-safety is to called acquireWorld to obtain a thread-safe world
 * pointer, and releaseWorld when you are done with the object.
 */
public class Renderer extends Observable implements GLSurfaceView.Renderer {
    // Private constants
    private static final Renderer _instance = new Renderer();
    private static final String TAG = "Renderer";
    private static final int ONE_SEC = 1000000000;
    private static final float WORLD_HEIGHT = 3f;
    public static final int MAX_PARTICLE_COUNT = 5000;
    public static final float PARTICLE_RADIUS = 0.06f;
    public static final float PARTICLE_REPULSIVE_STRENGTH = 0.5f;
    public static final boolean DEBUG_DRAW = false;

    // Parameters for world simulation
    private static final float TIME_STEP = 1 / 60f; // 60 fps
    private static final int VELOCITY_ITERATIONS = 6;
    private static final int POSITION_ITERATIONS = 2;
    private static final int PARTICLE_ITERATIONS = 5;
    private static final float BOUNDARY_THICKNESS = 20.0f;

    // Public static constants; variables for reuse
    public static final float MAT4X4_IDENTITY[];

    // Public constants; records render states
    public float sRenderWorldWidth = WORLD_HEIGHT;
    public float sRenderWorldHeight = WORLD_HEIGHT;
    public int sScreenWidth = 1;
    public int sScreenHeight = 1;

    /// Member variables
    private Activity mActivity = null;

    // Renderer class owns all Box2D objects, for thread-safety
    private World mWorld = null;
    private ParticleSystem mParticleSystem = null;
    private Body mBoundaryBody = null;
    // Variables for thread synchronization
    private volatile boolean mSimulation = false;
    private Lock mWorldLock = new ReentrantLock();

    private ParticleRenderer mParticleRenderer;
    protected DebugRenderer mDebugRenderer = null;

    // Measure the frame rate
    long totalFrames = -10000;
    private int mFrames;
    private long mStartTime;
    private long mTime;

    static {
        MAT4X4_IDENTITY = new float[16];
        Matrix.setIdentityM(MAT4X4_IDENTITY, 0);
    }


    @Override
    protected void finalize() {
        deleteWorld();

        if (mDebugRenderer != null) {
            mDebugRenderer.delete();
            mDebugRenderer = null;
        }
    }

    private Renderer() {
    }

    public static Renderer getInstance() {
      return _instance;
    }

    public void init(Activity activity) {
        mActivity = activity;

        // Initialize all the different renderers
        mParticleRenderer = new ParticleRenderer();
        if (DEBUG_DRAW) {
            mDebugRenderer = new DebugRenderer();
            mDebugRenderer.setFlags(Draw.SHAPE_BIT | Draw.PARTICLE_BIT);
        }

        reset();
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        // Show the frame rate
        if (BuildConfig.DEBUG) {
            long time = System.nanoTime();
            if (time - mTime > ONE_SEC) {
                if (totalFrames < 0) {
                    totalFrames = 0;
                    mStartTime = time - 1;
                }
                final float fps = mFrames / ((float) time - mTime) * ONE_SEC;
                float avefps = totalFrames / ((float) time - mStartTime) * ONE_SEC;
                final int count = mParticleSystem.getParticleCount();
                Log.d(TAG, fps + " fps (Now)");
                Log.d(TAG, avefps + " fps (Average)");
                Log.d(TAG, count + " particles");
                mTime = time;
                mFrames = 0;

                mActivity.runOnUiThread(new Runnable() {
                        @Override
                    public void run() {
                        String message = MainActivity.sVersionName + '\n'
                                + fps + " fps\n"
                                + count + " particles\n"
                                + mParticleSystem.getParticleGroupCount()
                                + " particle groups\n"
                                + mWorld.getBodyCount() + " bodies\n";
                        ((TextView) mActivity.findViewById(R.id.fps))
                                .setText(message);
                    }
                });
            }
            mFrames++;
            totalFrames++;
        }

        update(TIME_STEP);
        render();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);

        sRenderWorldHeight = WORLD_HEIGHT;
        sRenderWorldWidth = width * WORLD_HEIGHT / height;
        sScreenWidth = width;
        sScreenHeight = height;

        // Reset the boundary
        initBoundaries();

        mParticleRenderer.onSurfaceChanged(width, height);

        if (DEBUG_DRAW) {
            mDebugRenderer.onSurfaceChanged();
        }
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        if (mWorld == null) {
            throw new IllegalStateException("Init world before rendering");
        }

        // Load all shaders
        ShaderProgram.loadAllShaders(mActivity.getAssets());

        TextureRenderer.getInstance().onSurfaceCreated();

        mParticleRenderer.onSurfaceCreated(mActivity);

        if (DEBUG_DRAW) {
            mDebugRenderer.onSurfaceCreated(mActivity);
        }
    }

    /** Update function for render loop */
    private void update(float dt) {
        if (mSimulation) {
            setChanged();
            notifyObservers(dt);

            mParticleRenderer.update(dt);

            World world = acquireWorld();
            try {
                world.step(
                        dt, VELOCITY_ITERATIONS,
                        POSITION_ITERATIONS, PARTICLE_ITERATIONS);
            } finally {
                releaseWorld();
            }
        }
    }

    /** Render function for render loop */
    private void render() {
        GLES20.glClearColor(1, 1, 1, 1);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        // Draw particles
        mParticleRenderer.draw();

        if (DEBUG_DRAW) {
            mDebugRenderer.draw();
        }
    }

    public void pauseSimulation() {
        mSimulation = false;
    }

    public void startSimulation() {
        mSimulation = true;
    }

    private void deleteWorld() {
        World world = acquireWorld();

        try {
            if (mBoundaryBody != null) {
              mBoundaryBody.delete();
              mBoundaryBody = null;
            }
            if (world != null) {
              world.delete();
              mWorld = null;
              mParticleSystem = null;
            }
        } finally {
            releaseWorld();
        }
    }

    /**
     * Resets the world -- which means a delete and a new.
     * Initializes the boundaries and reset the ParticleRenderer as well.
     */
    public void reset() {
        World world = acquireWorld();
        try {
            deleteWorld();
            mWorld = new World(0, 0);

            initParticleSystem();
            initBoundaries();

            if (DEBUG_DRAW) {
                mWorld.setDebugDraw(mDebugRenderer);
            }

            mParticleRenderer.reset();
        } finally {
            releaseWorld();
        }
    }

    /** Create a new particle system */
    private void initParticleSystem() {
        World world = acquireWorld();
        try {
            // Create a new particle system; we only use one.
            ParticleSystemDef psDef = new ParticleSystemDef();
            psDef.setRadius(PARTICLE_RADIUS);
            psDef.setRepulsiveStrength(PARTICLE_REPULSIVE_STRENGTH);
            mParticleSystem = mWorld.createParticleSystem(psDef);
            mParticleSystem.setMaxParticleCount(MAX_PARTICLE_COUNT);
            psDef.delete();
        } finally {
            releaseWorld();
        }
    }

    /** Constructs boundaries for the canvas. **/
    private void initBoundaries() {
        World world = acquireWorld();

        try {
            // clean up previous Body if exists
            if (mBoundaryBody != null) {
                world.destroyBody(mBoundaryBody);
            }

            // Create native objects
            BodyDef bodyDef = new BodyDef();
            PolygonShape boundaryPolygon = new PolygonShape();

            mBoundaryBody = world.createBody(bodyDef);

            // boundary definitions
            // top
            boundaryPolygon.setAsBox(
                    sRenderWorldWidth,
                    BOUNDARY_THICKNESS,
                    sRenderWorldWidth / 2,
                    sRenderWorldHeight + BOUNDARY_THICKNESS,
                    0);
            mBoundaryBody.createFixture(boundaryPolygon, 0.0f);
            // bottom
            boundaryPolygon.setAsBox(
                    sRenderWorldWidth,
                    BOUNDARY_THICKNESS,
                    sRenderWorldWidth / 2,
                    -BOUNDARY_THICKNESS,
                    0);
            mBoundaryBody.createFixture(boundaryPolygon, 0.0f);
            // left
            boundaryPolygon.setAsBox(
                    BOUNDARY_THICKNESS,
                    sRenderWorldHeight,
                    -BOUNDARY_THICKNESS,
                    sRenderWorldHeight / 2,
                    0);
            mBoundaryBody.createFixture(boundaryPolygon, 0.0f);
            // right
            boundaryPolygon.setAsBox(
                    BOUNDARY_THICKNESS,
                    sRenderWorldHeight,
                    sRenderWorldWidth + BOUNDARY_THICKNESS,
                    sRenderWorldHeight / 2,
                    0);
            mBoundaryBody.createFixture(boundaryPolygon, 0.0f);

            // Clean up native objects
            bodyDef.delete();
            boundaryPolygon.delete();
        } finally {
          releaseWorld();
        }
    }

    /**
     * Acquire the world for thread-safe operations.
     */
    public World acquireWorld() {
        mWorldLock.lock();
        return mWorld;
    }

    /**
     * Release the world after thread-safe operations.
     */
    public void releaseWorld() {
        mWorldLock.unlock();
    }

    /**
     * Acquire the particle system for thread-safe operations.
     * Uses the same lock as World, as all LiquidFun operations should be
     * synchronized. For example, if we are in the middle of World.sync(), we
     * don't want to call ParticleSystem.createParticleGroup() at the same
     * time.
     */
    public ParticleSystem acquireParticleSystem() {
        mWorldLock.lock();
        return mParticleSystem;
    }

    /**
     * Release the world after thread-safe operations.
     */
    public void releaseParticleSystem() {
        mWorldLock.unlock();
    }

    /**
     * This provides access to the main Activity class that our Renderer is
     * associated with. Provided for debug access; use with care.
     */
    public Activity getCurrentActivity() {
        return mActivity;
    }
}
