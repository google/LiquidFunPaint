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
package com.google.fpl.liquidfunpaint.tool;

import com.google.fpl.liquidfunpaint.tool.Tool.ToolOperation;

/**
 * Eraser tool
 * We don't create anything -- just use the generated Shapes to destroy
 * particles.
 */
public class EraserTool extends Tool {
    public EraserTool() {
        super(ToolType.ERASER);
        // Set the eraser tool size a bit bigger to make it easier to erase
        mBrushSize = MINIMUM_BRUSHSIZE * 1.25f;
        mOperations.remove(ToolOperation.ADD_PARTICLES);
    }
}
