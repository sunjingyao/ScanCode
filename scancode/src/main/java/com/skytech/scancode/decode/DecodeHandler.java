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

import android.graphics.Bitmap;
import android.os.*;
import android.util.Log;

import com.google.zxing.*;
import com.google.zxing.common.HybridBinarizer;
import com.skytech.scancode.CaptureActivity;
import com.skytech.scancode.R;

import java.io.ByteArrayOutputStream;
import java.util.Map;

class DecodeHandler extends Handler {

    private static final String TAG = DecodeHandler.class.getSimpleName();

    private final CaptureActivity activity;

    private final MultiFormatReader multiFormatReader;

    private boolean running = true;
    protected long start;

    DecodeHandler(CaptureActivity activity, Map<DecodeHintType, Object> hints) {
        multiFormatReader = new MultiFormatReader();
        multiFormatReader.setHints(hints);
        this.activity = activity;
    }

    @Override
    public void handleMessage(Message message) {
        if (!running) {
            return;
        }
        if (message.what == R.id.decode) {
            decode((byte[]) message.obj, message.arg1, message.arg2);
        } else if (message.what == R.id.quit) {
            running = false;
            Looper.myLooper().quit();
        }
    }

    protected void decode(LuminanceSource source) {
        Result result = null;
        if (source != null) {
            BinaryBitmap bBitmap = new BinaryBitmap(new HybridBinarizer(source));
            try {
                // 预览界面最终取到的是个bitmap，然后对其进行解码
                result = multiFormatReader.decodeWithState(bBitmap);
            } catch (ReaderException e) {
                Log.e(TAG, "decode matrix fail", e);
            } finally {
                multiFormatReader.reset();
            }
        }

        Handler handler = activity.getHandler();
        if (result != null) {
            // Don't log the barcode contents for security.
            long end = System.currentTimeMillis();
            Log.d(TAG, "Found barcode in " + (end - start) + " ms");
            if (handler != null) {
                Message message = Message.obtain(handler,
                        R.id.decode_succeeded, result);
                Bundle bundle = new Bundle();
                bundleThumbnail(source, bundle);
                message.setData(bundle);
                message.sendToTarget();
            }
        } else {
            if (handler != null) {
                Message message = Message.obtain(handler, R.id.decode_failed);
                message.sendToTarget();
            }
        }
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
        Log.d(TAG, "flag = " + CaptureActivity.getOrientationFlag());
        if (CaptureActivity.getOrientationFlag()) {
            byte[] rotatedData = new byte[data.length];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++)
                    rotatedData[x * height + height - y - 1] = data[x + y * width];
            }
            int tmp = width; // Here we are swapping, that's the difference to #11
            width = height;
            height = tmp;
            data = rotatedData;
        }

        decode(activity.getCameraManager().buildLuminanceSource(data, width, height));
    }

    private Bitmap luminance2Bitmap(LuminanceSource source) {
        byte[] matrix = source.getMatrix();
        int width = source.getWidth();
        int height = source.getHeight();
        int[] pixels = new int[width * height];
        for (int y = 0; y < height; ++y) {
            int outputOffset = y * width;
            for (int x = 0; x < width; ++x) {
                int grey = matrix[outputOffset + x] & 255;
                pixels[outputOffset + x] = -16777216 | grey * 65793;
            }
        }
        return Bitmap.createBitmap(pixels, 0, width, width, height, Bitmap.Config.ARGB_4444);
    }

    private void bundleThumbnail(LuminanceSource source, Bundle bundle) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        luminance2Bitmap(source).compress(Bitmap.CompressFormat.JPEG, 50, out);
        bundle.putByteArray(DecodeThread.BARCODE_BITMAP, out.toByteArray());
    }

}
