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
// Vertex shader for particles

attribute vec4 aPosition; // in 2d worldspace
attribute vec4 aColor;    // vertex color
attribute float aWeight;  // particle weight
uniform mat4 uTransform;  // transforms from worldspace to clip space
uniform float uPointSize; // constant point size
uniform vec3 uWeightParams; // Parameters for adding in particle weight.
                            // 0: Scale - decreases the range of values
                            // 1: Range shift - shift the range from [0.0, inf)
                            //    to [value, inf) so we get a less abrupt
                            //    dropoff.
                            // 2: Cutoff - values above this will affect color
varying vec4 vColor;      // output color for fragment shader

void main() {
  gl_Position = uTransform * aPosition;
  gl_PointSize = uPointSize;
  vColor = aColor;

  // Square the weight to make a nicer curve, then apply scale and range shift.
  float weight = aWeight * aWeight * uWeightParams[0] + uWeightParams[1];

  // Modulate the color by weight, if the weight is above the cutoff.
  vColor.rgb *= (weight > uWeightParams[2]) ? 1.0 / weight : 1.0;
}
