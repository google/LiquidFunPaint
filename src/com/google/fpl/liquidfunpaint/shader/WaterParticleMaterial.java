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
package com.google.fpl.liquidfunpaint.shader;

import com.google.fpl.liquidfunpaint.ParticleRenderer;
import com.google.fpl.liquidfunpaint.Renderer;

import android.content.Context;
import android.opengl.GLES20;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * ParticleMaterial.
 * This is the particle point sprite material.
 */
public class WaterParticleMaterial extends Material {
    private static final String TAG = "WaterParticleMaterial";
    private static final String DIFFUSE_TEXTURE_NAME = "uDiffuseTexture";

    private float mParticleSizeScale;
    // Parameters for adding in particle weight.
    // 0: Scale - decreases the range of values
    // 1: Range shift - shift the range from [0.0, inf) to [value, inf) so we
    //    get a less abrupt dropoff.
    // 2: Cutoff - values above this will affect color
    private final float[] mWeightParams = new float[3];

    public WaterParticleMaterial(Context context, JSONObject json) {
        super(new ShaderProgram("water_particle.glslv", "particle.glslf"));

        // Read in values from the JSON file
        mParticleSizeScale =
                (float) json.optDouble("particleSizeScale", 1.0);

        // Scale of weight. This changes values from [0.0, max) to
        // [0.0, max*scale).
        mWeightParams[0] = (float) json.optDouble("weightScale", 1.0);

        // Range shift. This shifts values from [0.0, max) to
        // [range shift, max + range shift), so we take into account particles
        // with a small weight for a smoother curve.
        mWeightParams[1] = (float) json.optDouble("weightRangeShift", 0.0);

        // Cutoff. This means particles with a weight less than the cutoff
        // will not have any weight applied.
        mWeightParams[2] = (float) json.optDouble("weightCutoff", 1.0);

        // Add the water texture that is scrolling
        try {
            String textureName = json.getString(DIFFUSE_TEXTURE_NAME);
            addTexture(DIFFUSE_TEXTURE_NAME, new Texture(context, textureName));
        } catch (JSONException ex) {
            Log.e(TAG, "Missing point sprite texture!\n" + ex.getMessage());
        }
    }

    @Override
    public void beginRender() {
        super.beginRender();

        // Specific uniforms to this material
        GLES20.glUniform1f(
                getUniformLocation("uPointSize"),
                Math.max(1.0f, mParticleSizeScale * ParticleRenderer.FB_SIZE *
                    (Renderer.PARTICLE_RADIUS /
                    Renderer.getInstance().sRenderWorldHeight)));
        GLES20.glUniform3fv(
                getUniformLocation("uWeightParams"), 1, mWeightParams, 0);
    }
}
