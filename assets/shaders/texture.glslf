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
// Simple fragment shader with one texture
// It only utilizes the scrolling texture coordinates right now.

precision lowp float;
uniform sampler2D uDiffuseTexture; // diffuse texture
uniform float uAlphaScale;         // scale to apply to alpha
varying vec2 vTexCoord;            // input original texture coords from vertex
                                   // shader. [0,1]
varying vec2 vScrollingTexCoord;   // input scrolling texture coords from
                                   // vertex shader. [0,1]

void main() {
    gl_FragColor = texture2D(uDiffuseTexture, vScrollingTexCoord);
    gl_FragColor.a *= uAlphaScale;
}
