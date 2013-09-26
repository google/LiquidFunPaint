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

import com.google.fpl.liquidfunpaint.util.FileHelper;

import android.content.res.AssetManager;
import android.opengl.GLES20;
import android.util.Log;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides shader utilities for OpenGLES 2.0.
 * Also provides a container for OpenGL shaders, and various methods to help
 * with loading attributes, uniforms, and setting them.
 */
public class ShaderProgram {
    private static final String TAG = "ShaderProgram";
    // Set to 3 because that's the max we need for
    // glGetActiveUniform or the like.
    private static final int MAX_NUM_PARAMS = 3;

    // Vertex and fragment programs loaded from the assets/shaders folder
    private static final String SHADER_DIRECTORY = "shaders";
    private static final String VERTEX_SHADER_EXTENSION = "glslv";
    private static final String FRAGMENT_SHADER_EXTENSION = "glslf";
    private static final Map<String, Integer> COMPILED_SHADERS =
            new HashMap<String, Integer>();

    private class ParamInfo {
        String mName;
        int mSize;
        int mType;
        int mLocation;

        private ParamInfo(String name, int size, int type, int location) {
            mName = name;
            mSize = size;
            mType = type;
            mLocation = location;
        }
    }

    /// Member variables
    protected int mProgram;
    private String mVSName = null;
    private String mFSName = null;
    private Map<String, ParamInfo> mVertexAttributes =
            new HashMap<String, ParamInfo>();
    private Map<String, ParamInfo> mUniforms =
            new HashMap<String, ParamInfo>();

    /// Temp variables for getting OpenGL params
    /// We have this because we might query params during runtime and we can
    /// reuse this object for all such calls.
    private static int[] sGlParams = new int[MAX_NUM_PARAMS];

    /// Static helper methods

    /**
     * Helper function for checking OpenGL errors
     * Warning: this is slow on runtime. It is provided as a convenience during
     * debugging.
     * @param glFunction
     */
    private static void checkGlError(String glFunction) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
                Log.e(TAG, glFunction + ": glError " + error);
        }
    }

    /**
     * Loads a GLSL shader.
     * @return Returns the GLES shader handle
     */
    private static int loadShader(
            int shaderType, String shaderName, String shaderSource) {
        int shaderProg = GLES20.glCreateShader(shaderType);
        GLES20.glShaderSource(shaderProg, shaderSource);
        GLES20.glCompileShader(shaderProg);

        // Check for errors
        int[] status = new int[1];
        GLES20.glGetShaderiv(shaderProg, GLES20.GL_COMPILE_STATUS, status, 0);
        if (status[0] == 0) {
            Log.e(TAG, "Could not compile shader " + shaderName + ":");
            Log.e(TAG, GLES20.glGetShaderInfoLog(shaderProg));
            GLES20.glDeleteShader(shaderProg);
            shaderProg = 0;
        }

        return shaderProg;
    }


    /**
     * Loads all shader files from the Assets folder
     */
    public static void loadAllShaders(AssetManager assetMgr) {
        // Clear the map; OpenGLES context could be destroyed while app is in
        // background. We have to reload all the shaders.
        COMPILED_SHADERS.clear();

        try {
            String[] shaderFiles = assetMgr.list(SHADER_DIRECTORY);
            for (String shaderFile : shaderFiles) {
                String fileContent = FileHelper.loadAsset(
                        assetMgr, SHADER_DIRECTORY + "/" + shaderFile);
                int shaderProg = 0;
                if (shaderFile.substring(shaderFile.lastIndexOf('.') + 1)
                        .equals(VERTEX_SHADER_EXTENSION)) {
                    shaderProg = loadShader(
                            GLES20.GL_VERTEX_SHADER, shaderFile, fileContent);
                } else {
                    shaderProg = loadShader(
                            GLES20.GL_FRAGMENT_SHADER, shaderFile, fileContent);
                }

                if (shaderProg != 0) {
                    COMPILED_SHADERS.put(shaderFile, shaderProg);
                }
            }
        } catch (IOException ex) {
            Log.e(TAG,"Cannot find shader files!");
        }
    }

    /**
     * Wrapper for GLES20.glGetProgramiv with a better return interface.
     */
    private static int getProgramiv(int program, int pname) {
        GLES20.glGetProgramiv(program, pname, sGlParams, 0);
        return sGlParams[0];
    }

    /**
     * Wrapper for GLES20.glGetIntegerv with a better return interface.
     */
    private static int getIntegerv(int pname) {
        GLES20.glGetIntegerv(pname, sGlParams, 0);
        return sGlParams[0];
    }

    /// Member methods

    public ShaderProgram(String vsName, String psName) {
        createProgram(vsName, psName);

        if (isShaderCompiled()) {
            initAttributes();
            initUniforms();
        }
    }

    /**
     * Creates a shader program.
     */
    private void createProgram(String vsName, String psName) {
        int program = GLES20.glCreateProgram();

        // Technically, we could cache the compiled shader,
        // since a lot of shaders might share the same VS or FS. Unless the
        // number of reused shaders get large, there's not much of a gain.
        int vertexShaderProg = COMPILED_SHADERS.get(vsName);
        GLES20.glAttachShader(program, vertexShaderProg);

        int fragmentShaderProg = COMPILED_SHADERS.get(psName);
        GLES20.glAttachShader(program, fragmentShaderProg);

        // Check for errors
        GLES20.glLinkProgram(program);
        int[] status = new int[1];
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, status, 0);
        if (status[0] == 0) {
            Log.e(TAG, "Could not link shaders " + vsName + " and "
                      + psName + ". OpenGL log:");
            Log.e(TAG, GLES20.glGetProgramInfoLog(program));
            program = 0;
        }

        mVSName = vsName;
        mFSName = psName;
        mProgram = program;
    }

    protected boolean isShaderCompiled() {
        return (mProgram > 0);
    }

    /**
     * Load the attributes from the compiled shader.
     */
    private void initAttributes() {
        int numAttributes = getProgramiv(mProgram, GLES20.GL_ACTIVE_ATTRIBUTES);
        int maxNameSize =
                getProgramiv(mProgram, GLES20.GL_ACTIVE_ATTRIBUTE_MAX_LENGTH);

        byte[] nameBuffer = new byte[maxNameSize];
        for (int i = 0; i < numAttributes; ++i) {
            GLES20.glGetActiveAttrib(
                    mProgram, i, maxNameSize, sGlParams, 0, sGlParams, 1,
                    sGlParams, 2, nameBuffer, 0);
            String name = new String(nameBuffer, 0, sGlParams[0]);
            int location = GLES20.glGetAttribLocation(mProgram, name);
            mVertexAttributes.put(name, new ParamInfo(
                    name, sGlParams[1], sGlParams[2], location));
        }
    }

    protected int getAttributeLocation(String name) {
        ParamInfo attrInfo = mVertexAttributes.get(name);
        if (attrInfo == null) {
            return -1;
        }
        return attrInfo.mLocation;
    }

    /**
     * Load uniform info from compiled shader.
     */
    private void initUniforms() {
        int numUniforms = getProgramiv(mProgram, GLES20.GL_ACTIVE_UNIFORMS);
        int maxNameSize =
                getProgramiv(mProgram, GLES20.GL_ACTIVE_UNIFORM_MAX_LENGTH);

        byte[] nameBuffer = new byte[maxNameSize];
        for (int i = 0; i < numUniforms; ++i) {
            GLES20.glGetActiveUniform(
                    mProgram, i, maxNameSize, sGlParams, 0, sGlParams, 1,
                    sGlParams, 2, nameBuffer, 0);
            String name = new String(nameBuffer, 0, sGlParams[0]);
            int location = GLES20.glGetUniformLocation(mProgram, name);
            mUniforms.put(name, new ParamInfo(
                    name, sGlParams[1], sGlParams[2], location));
        }
    }

    protected int getUniformLocation(String name) {
        ParamInfo uniformInfo = mUniforms.get(name);
        if (uniformInfo == null) {
            return -1;
        }
        return uniformInfo.mLocation;
    }

    protected void beginRender() {
        // Only reset the program and bindings if it's not the same one.
        if (getIntegerv(GLES20.GL_CURRENT_PROGRAM) != mProgram) {
            GLES20.glUseProgram(mProgram);
        }
    }

    protected void endRender() {
    }

    @Override
    public String toString() {
        return "VS(" + mVSName + ") FS(" + mFSName + ")";
    }
}
