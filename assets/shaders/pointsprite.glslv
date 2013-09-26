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
// Point sprite vertex shader with varying point sizes

attribute vec4 aPosition;   // in 2d worldspace
attribute vec4 aColor;      // vertex color
attribute float aPointSize; // point size
uniform mat4 uTransform;    // transforms from worldspace to clip space
varying vec4 vColor;      // output color to fragment shader

void main() {
  gl_Position = uTransform * aPosition;
  gl_PointSize = aPointSize;
  vColor = aColor;
}
