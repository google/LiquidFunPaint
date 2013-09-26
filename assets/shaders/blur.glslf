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
// Blur fragment shader with a 5x5 gaussian blur.

precision mediump float;
uniform sampler2D uBlurTexture; // texture to blur
uniform float uBlurBufferSize;  // 1 / Size of the blur buffer
varying vec2 vTexCoord;         // input original texture coords for fragment
                                // shader. [0,1]
varying vec2 vBlurTexCoords[5]; // input texture coords for blur sampling, for
                                // fragment shader.

void main()
{
    vec4 sum = vec4(0.0);
    // Gaussian blur. Sigma: 2.3, kernel size: 5.
    sum += texture2D(uBlurTexture, vBlurTexCoords[0]) * 0.164074;
    sum += texture2D(uBlurTexture, vBlurTexCoords[1]) * 0.216901;
    sum += texture2D(uBlurTexture, vBlurTexCoords[2]) * 0.23805;
    sum += texture2D(uBlurTexture, vBlurTexCoords[3]) * 0.216901;
    sum += texture2D(uBlurTexture, vBlurTexCoords[4]) * 0.164074;
    gl_FragColor = sum;
}
