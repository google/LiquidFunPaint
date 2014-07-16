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

import com.google.fpl.liquidfun.ParticleColor;
import com.google.fpl.liquidfun.ParticleFlag;
import com.google.fpl.liquidfun.ParticleGroup;
import com.google.fpl.liquidfun.ParticleGroupFlag;

/**
 * Pencil tool
 * These are wall + barrier particles. To prevent leaking, they are all
 * contained in one ParticleGroup. The ParticleGroup can be empty as a result.
 */
public class PencilTool extends Tool {
    private static final int ALPHA_DECREMENT = 40;
    private static final int ALPHA_THRESHOLD = 10;
    private ParticleGroup mParticleGroup = null;
    private ParticleColor mTempColor = new ParticleColor();

    public PencilTool() {
        super(ToolType.PENCIL);
        mParticleFlags =
                ParticleFlag.wallParticle |
                ParticleFlag.barrierParticle;
        mParticleGroupFlags =
                ParticleGroupFlag.solidParticleGroup |
                ParticleGroupFlag.particleGroupCanBeEmpty;
    }

    @Override
    protected void finalize() {
        // Clean up C++ objects that we initialized.
        mTempColor.delete();
    }

    /**
      * @param pInfo The pointer info containing the previous group info
      */
    @Override
    protected void applyTool(PointerInfo pInfo) {
        // If we have a ParticleGroup saved already, assign it to pInfo.
        // If not, we take the first ParticleGroup created for wall particles,
        // which will be contained in pInfo.
        if (mParticleGroup != null) {
            pInfo.setParticleGroup(mParticleGroup);
        } else if (pInfo.getParticleGroup() != null) {
            mParticleGroup = pInfo.getParticleGroup();
        }

        super.applyTool(pInfo);
    }

    @Override
    protected void reset() {
        mParticleGroup = null;
    }
}
