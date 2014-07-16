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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * RenderHelper
 * Data and functions to help with rendering.
 */
public class RenderHelper {
    // Vertex data
    public static final float[] SCREEN_QUAD_VERTEX_DATA = {
        -1.0f, -1.0f, 0.0f, // Position 0
        0.0f, 0.0f, // TexCoord 0
        -1.0f, 1.0f, 0.0f, // Position 1
        0.0f, 1.0f, // TexCoord 1
        1.0f, 1.0f, 0.0f, // Position 2
        1.0f, 1.0f, // TexCoord 2
        1.0f, -1.0f, 0.0f, // Position 3
        1.0f, 0.0f // TexCoord 3
    };
    public static final FloatBuffer SCREEN_QUAD_VERTEX_BUFFER;
    public static final int SCREEN_QUAD_NUM_VERTICES = 4;
    // We get the size of the vertex data in floats, and multiply with
    // sizeof(float) which is 4 bytes.
    public static final int SCREEN_QUAD_VERTEX_STRIDE =
            SCREEN_QUAD_VERTEX_DATA.length / SCREEN_QUAD_NUM_VERTICES * 4;

    static {
        SCREEN_QUAD_VERTEX_BUFFER =
                ByteBuffer.allocateDirect(SCREEN_QUAD_VERTEX_DATA.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        SCREEN_QUAD_VERTEX_BUFFER.put(SCREEN_QUAD_VERTEX_DATA).position(0);
    }
}
