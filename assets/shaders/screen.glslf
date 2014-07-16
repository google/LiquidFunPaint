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
// Particle framebuffer to screen shader.

precision lowp float;
uniform sampler2D uDiffuseTexture; // frame buffer (with water particles)
uniform float uAlphaThreshold;     // Alpha threshold for the output
varying vec2 vTexCoord;            // input original texture coords from vertex
                                   // shader. [0,1]
varying vec2 vScrollingTexCoord;   // input scrolling texture coords from
                                   // vertex shader. [0,1]

void main()
{
    gl_FragColor = texture2D(uDiffuseTexture, vTexCoord);

    // Alpha Threshold
    gl_FragColor.a = (gl_FragColor.a > uAlphaThreshold) ? gl_FragColor.a : 0.0;
}
