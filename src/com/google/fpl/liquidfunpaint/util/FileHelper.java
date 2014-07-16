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
package com.google.fpl.liquidfunpaint.util;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.IOException;
import java.io.InputStream;

public final class FileHelper {
    public static String loadAsset(AssetManager assetMgr, String fileName) {
        String fileContent = null;
        try {
            InputStream is = assetMgr.open(fileName);

            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();

            fileContent = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return fileContent;
    }

    public static Bitmap loadBitmap(AssetManager assetMgr, String fileName) {
        Bitmap bitmap = null;
        try {
            InputStream is = assetMgr.open(fileName);
            bitmap = BitmapFactory.decodeStream(is);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }

        return bitmap;
    }
}
