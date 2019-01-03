package com.skytech.scancode.common;

import com.google.zxing.Binarizer;
import com.google.zxing.LuminanceSource;
import com.google.zxing.NotFoundException;
import com.google.zxing.common.BitMatrix;

public class OtsuBinarizer extends HybridBinarizer {

    private static final int BLOCK_SIZE_POWER = 3;
    private static final int BLOCK_SIZE = 8;
    private static final int BLOCK_SIZE_MASK = 7;
    private static final int MINIMUM_DIMENSION = 40;
    private static final int MIN_DYNAMIC_RANGE = 24;

    private static final String TAG = "HybridBinarizer";

    private BitMatrix matrix;

    public OtsuBinarizer(LuminanceSource source) {
        super(source);
    }

    public BitMatrix getBlackMatrix() throws NotFoundException {
        if (this.matrix != null) {
            return this.matrix;
        } else {
            LuminanceSource source = this.getLuminanceSource();
            int width = source.getWidth();
            int height = source.getHeight();
            if (width >= MINIMUM_DIMENSION && height >= MINIMUM_DIMENSION) {
                byte[] luminances = source.getMatrix();
                imageStretchByHistogram(luminances, height, width);
                int subWidth = width >> BLOCK_SIZE_POWER;
                if ((width & BLOCK_SIZE_MASK) != 0) {
                    ++subWidth;
                }

                int subHeight = height >> BLOCK_SIZE_POWER;
                if ((height & BLOCK_SIZE_MASK) != 0) {
                    ++subHeight;
                }
                BitMatrix newMatrix = new BitMatrix(width, height);

                int[][] threshValue = calculateThreshValue(luminances, subWidth, subHeight, width, height);
                calculateBlackMatrix(luminances, subWidth, subHeight, width, height, threshValue, newMatrix);

                this.matrix = newMatrix;
            } else {
                this.matrix = super.getBlackMatrix();
            }

            return this.matrix;
        }
    }

       private void calculateBlackMatrix(byte[] luminances, int subWidth, int subHeight, int width, int height, int[][] threshValues, BitMatrix matrix) {
        for (int y = 0; y < BLOCK_SIZE; ++y) {
            int yoffset = y * subHeight;
            for (int x = 0; x < BLOCK_SIZE; ++x) {
                int xoffset = x * subWidth;
                thresholdBlock(luminances, xoffset, yoffset, threshValues[y][x], subWidth, subHeight, width, height, matrix);
            }
        }
    }

    private void thresholdBlock(byte[] luminances, int xoffset, int yoffset, int thresh, int subWidth, int subHeight, int width, int height, BitMatrix matrix) {
        int y = 0;

        for (int offset = yoffset * width + xoffset; y < subHeight && yoffset + y < height; offset += width) {
            for (int x = 0; x < subWidth && xoffset + x < width; ++x) {
                if ((luminances[offset + x] & 255) <= thresh) {
                    matrix.set(xoffset + x, yoffset + y);
                }
            }

            ++y;
        }
    }

    private int[][] calculateThreshValue(byte[] luminances, int subWidth, int subHeight, int width, int height) {
        int[][] threshValues = new int[BLOCK_SIZE][BLOCK_SIZE];
        for (int y = 0; y < BLOCK_SIZE; ++y) {
            int yoffset = y * subHeight;
            for (int x = 0; x < BLOCK_SIZE; ++x) {
                int[] histGram = new int[256];
                int xoffset = x * subWidth;

                int offset = yoffset * width + xoffset;
                for (int yy = 0; yy < subHeight && yoffset + yy < height; yy++) {
                    for (int xx = 0; xx < subWidth && xoffset + xx < width; ++xx) {
                        histGram[(luminances[offset + xx] & 255)]++;
                    }
                    offset += width;
                }
                threshValues[y][x] = getOSTUThreshold(histGram);
            }
        }
        return threshValues;
    }

    public Binarizer createBinarizer(LuminanceSource source) {
        return new OtsuBinarizer(source);
    }

    private static int getOSTUThreshold(int[] HistGram) {
        int X, Y, Amount = 0;
        int PixelBack = 0, PixelFore = 0, PixelIntegralBack = 0, PixelIntegralFore = 0, PixelIntegral = 0;
        double OmegaBack, OmegaFore, MicroBack, MicroFore, SigmaB, Sigma;              // 类间方差;
        int MinValue, MaxValue;
        int Threshold = 0;

        for (MinValue = 0; MinValue < 256 && HistGram[MinValue] == 0; MinValue++) ;
        for (MaxValue = 255; MaxValue > MinValue && HistGram[MinValue] == 0; MaxValue--) ;
        if (MaxValue == MinValue) return MaxValue;          // 图像中只有一个颜色
        if (MinValue + 1 == MaxValue) return MinValue;      // 图像中只有二个颜色

        for (Y = MinValue; Y <= MaxValue; Y++) Amount += HistGram[Y];        //  像素总数

        PixelIntegral = 0;
        for (Y = MinValue; Y <= MaxValue; Y++) PixelIntegral += HistGram[Y] * Y;
        SigmaB = -1;
        for (Y = MinValue; Y < MaxValue; Y++) {
            PixelBack = PixelBack + HistGram[Y];
            PixelFore = Amount - PixelBack;
            OmegaBack = (double) PixelBack / Amount;
            OmegaFore = (double) PixelFore / Amount;
            PixelIntegralBack += HistGram[Y] * Y;
            PixelIntegralFore = PixelIntegral - PixelIntegralBack;
            MicroBack = (double) PixelIntegralBack / PixelBack;
            MicroFore = (double) PixelIntegralFore / PixelFore;
            Sigma = OmegaBack * OmegaFore * (MicroBack - MicroFore) * (MicroBack - MicroFore);
            if (Sigma > SigmaB) {
                SigmaB = Sigma;
                Threshold = Y;
            }
        }
        return Threshold;
    }

}