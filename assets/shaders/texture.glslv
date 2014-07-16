/*
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
// Simple vertex shader with texture coords

attribute vec4 aPosition;   // in 2d worldspace
attribute vec2 aTexCoord;   // texture coordinates for vertex
uniform mat4 uMvpTransform; // transforms from worldspace to clip space
uniform mat4 uUvTransform;  // transforms texture coords; allows uv scrolling etc
varying vec2 vTexCoord;          // output original texture coords for fragment
                                 // shader. [0,1]
varying vec2 vScrollingTexCoord; // output scrolling texture coords for
                                 // fragment shader. [0,1]

void main() {
    gl_Position = uMvpTransform * aPosition;
    // multiply texture coordinates with uvTransform and extract only xy
    vTexCoord = aTexCoord;
    vScrollingTexCoord = (uUvTransform * vec4(aTexCoord, 0, 1)).xy;
}
