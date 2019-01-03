/*
 * Copyright (C) 2010 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.skytech.scancode.decode;

import android.graphics.BitmapFactory;

import com.google.zxing.*;
import com.skytech.scancode.CaptureActivity;

import java.util.Map;

class RGBDecodeHandler extends DecodeHandler {

    private static final String TAG = RGBDecodeHandler.class.getSimpleName();

    RGBDecodeHandler(CaptureActivity activity, Map<DecodeHintType, Object> hints) {
        super(activity, hints);
    }

    /**
     * Decode the data within the viewfinder rectangle, and time how long it
     * took. For efficiency, reuse the same reader objects from one decode to
     * the next.
     *
     * @param data   a byte array of the picture data
     * @param width  The width of the preview frame.
     * @param height The height of the preview frame.
     */
    protected void decode(byte[] data, int width, int height) {
        start = System.currentTimeMillis();

        decode(new BitmapLuminanceSource(BitmapFactory.decodeByteArray(data, 0, data.length)));
    }
}
