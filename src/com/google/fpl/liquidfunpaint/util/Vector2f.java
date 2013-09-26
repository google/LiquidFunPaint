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
package com.google.fpl.liquidfunpaint.util;

/**
 * A simple 2d vector class with extended functionality.
 * API heavily borrowed from math.geom2d.Vector2D which is not available on
 * Android.
 */
public class Vector2f extends android.graphics.PointF {
    public Vector2f(float x, float y) {
        super(x, y);
    }

    public Vector2f(Vector2f v) {
        super(v.x, v.y);
    }

    /**
     * Returns the sum of current vector with vector given as parameter.
     * Inner fields are not modified.
     */
    public Vector2f add(Vector2f inVec) {
      return new Vector2f(x + inVec.x, y + inVec.y);
  }

    /**
     * Returns the subtraction of current vector with vector given as parameter.
     * Inner fields are not modified.
     */
    public Vector2f sub(Vector2f inVec) {
        return new Vector2f(x - inVec.x, y - inVec.y);
    }

    /**
     * Multiplies the vector by a scalar amount. Inner fields are not modified.
     */
    public Vector2f mul(float s) {
        return new Vector2f(x * s, y * s);
    }

    /**
     * Returns distance between 2 vectors.
     */
    public static float length(Vector2f a, Vector2f b) {
        Vector2f dist = a.sub(b);
        return dist.length();
    }

    /**
     * Interpolate between start and end, in fixed increments defined by the
     * number of segments (segmentCount) and which segment we want (segmentLoc).
     */
    public static Vector2f lerpFixedInterval(
            Vector2f start, Vector2f end,
            float segmentLoc, float segmentCount) {
        // v0 = start * (segmentLoc + 1)
        Vector2f v0 = start.mul(segmentLoc + 1);
        // v1 = end * (segmentCount - segmentLoc);
        Vector2f v1 = end.mul(segmentCount - segmentLoc);
        // (v0 + v1) / (segmentCount + 1)
        return v0.add(v1).mul(1 / (segmentCount + 1));
    }
}
