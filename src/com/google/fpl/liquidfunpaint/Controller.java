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

import com.google.fpl.liquidfun.World;
import com.google.fpl.liquidfunpaint.tool.Tool;
import com.google.fpl.liquidfunpaint.tool.Tool.ToolType;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.View.OnTouchListener;

/**
 * Basic controller that listens to touch and sensor inputs
 */
public class Controller implements OnTouchListener, SensorEventListener {
    private SensorManager mManager;
    private Sensor mAccelerometer;
    private final float[] mGravityVec = new float[2];
    private Tool mTool = null;

    private static final String TAG = "Controller";
    private static final float GRAVITY = 10f;

    public Controller(Activity activity) {
        // Get rotation and set the vector
        switch (activity.getWindowManager().getDefaultDisplay().getRotation()) {
            case Surface.ROTATION_0:
                mGravityVec[0] = -GRAVITY;
                break;
            case Surface.ROTATION_90:
                mGravityVec[1] = -GRAVITY;
                break;
            case Surface.ROTATION_180:
                mGravityVec[0] = GRAVITY;
                break;
            case Surface.ROTATION_270:
                mGravityVec[1] = GRAVITY;
                break;
        }

        mManager = (SensorManager) activity.getSystemService(Activity.SENSOR_SERVICE);
        mAccelerometer = mManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    protected void onResume() {
        mManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
    }

    protected void onPause() {
        mManager.unregisterListener(this);
    }

    @Override
    public boolean onTouch(View v, MotionEvent e) {
        if (mTool != null) {
            mTool.onTouch(v, e);
        }
        return true;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];

            World world = Renderer.getInstance().acquireWorld();
            try {
                world.setGravity(
                        mGravityVec[0] * x - mGravityVec[1] * y,
                        mGravityVec[1] * x + mGravityVec[0] * y);
            } finally {
                Renderer.getInstance().releaseWorld();
            }
        }
    }

    public void setColor(int color) {
        if (mTool != null) {
            mTool.setColor(color);
        }
    }

    public void setTool(ToolType type) {
        Tool oldTool = mTool;
        mTool = Tool.getTool(type);

        if (oldTool != mTool) {
            if (oldTool != null) {
                oldTool.deactivate();
            }
            if (mTool != null) {
                mTool.activate();
            }
        }
    }

    public void reset() {
        Tool.resetAllTools();
    }
}

