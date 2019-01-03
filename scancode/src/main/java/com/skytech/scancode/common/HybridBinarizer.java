package com.skytech.scancode.common;

import com.google.zxing.Binarizer;
import com.google.zxing.LuminanceSource;
import com.google.zxing.NotFoundException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.GlobalHistogramBinarizer;

public class HybridBinarizer extends GlobalHistogramBinarizer {

    private static final int BLOCK_SIZE_POWER = 3;
    private static final int BLOCK_SIZE = 8;
    private static final int BLOCK_SIZE_MASK = 7;
    private static final int MINIMUM_DIMENSION = 40;
    private static final int MIN_DYNAMIC_RANGE = 24;

    private BitMatrix matrix;

    public HybridBinarizer(LuminanceSource source) {
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
               // medianFiltering(luminances, width, height);
               // imageStretchByHistogram(luminances, height, width);

                int subWidth = width >> BLOCK_SIZE_POWER;
                if ((width & BLOCK_SIZE_MASK) != 0) {
                    ++subWidth;
                }

                int subHeight = height >> BLOCK_SIZE_POWER;
                if ((height & BLOCK_SIZE_MASK) != 0) {
                    ++subHeight;
                }
                BitMatrix newMatrix = new BitMatrix(width, height);

                int[][] blackPoints = calculateBlackPoints(luminances, subWidth, subHeight, width, height);
                calculateThresholdForBlock(luminances, subWidth, subHeight, width, height, blackPoints, newMatrix);

                this.matrix = newMatrix;
            } else {
                this.matrix = super.getBlackMatrix();
            }

            return this.matrix;
        }
    }

    public Binarizer createBinarizer(LuminanceSource source) {
        return new HybridBinarizer(source);
    }

    /**
     * 通过直方图变换进行图像增强，将图像灰度的域值拉伸到0-255
     *
     * @param luminances 单通道灰度图像
     */
    protected void imageStretchByHistogram(byte[] luminances, int height, int width) {
        double[] p = new double[256];
        double[] p1 = new double[256];
        double[] num = new double[256];

        long wMulh = height * width;

        //statistics
        for (int y = 0; y < height; y++) {
            int offset = y * width;
            for (int x = 0; x < width; x++) {
                int v = luminances[offset + x] & 255;
                num[v]++;
            }
        }
        //calculate probability
        for (int i = 0; i < 256; i++) {
            p[i] = num[i] / wMulh;
        }

        //p1[i]=sum(p[j]);  j<=i;
        for (int i = 0; i < 256; i++) {
            for (int k = 0; k <= i; k++)
                p1[i] += p[k];
        }

        // histogram transformation
        for (int y = 0; y < height; y++) {
            int offset = y * width;
            for (int x = 0; x < width; x++) {
                int v = luminances[offset + x] & 255;
                v = (int) (p1[v] * 255 + 0.5);
                if (v > 255) v = 255;
                luminances[offset + x] = (byte) v;
            }
        }
    }

    private static void calculateThresholdForBlock(byte[] luminances, int subWidth, int subHeight, int width, int height, int[][] blackPoints, BitMatrix matrix) {
        for (int y = 0; y < subHeight; ++y) {
            int yoffset = y << BLOCK_SIZE_POWER;
            int maxYOffset = height - BLOCK_SIZE;
            if (yoffset > maxYOffset) {
                yoffset = maxYOffset;
            }

            for (int x = 0; x < subWidth; ++x) {
                int xoffset = x << BLOCK_SIZE_POWER;
                int maxXOffset = width - BLOCK_SIZE;
                if (xoffset > maxXOffset) {
                    xoffset = maxXOffset;
                }

                int left = cap(x, 2, subWidth - BLOCK_SIZE_POWER);
                int top = cap(y, 2, subHeight - BLOCK_SIZE_POWER);
                int sum = 0;

                int average;
                for (average = -2; average <= 2; ++average) {
                    int[] blackRow = blackPoints[top + average];
                    sum += blackRow[left - 2] + blackRow[left - 1] + blackRow[left] + blackRow[left + 1] + blackRow[left + 2];
                }

                average = sum / 25;
                thresholdBlock(luminances, xoffset, yoffset, average, width, matrix);
            }
        }

    }

    private static int cap(int value, int min, int max) {
        return value < min ? min : (value > max ? max : value);
    }

    private static void thresholdBlock(byte[] luminances, int xoffset, int yoffset, int threshold, int stride, BitMatrix matrix) {

        int offset = yoffset * stride + xoffset;
        for (int y = 0; y < BLOCK_SIZE; ++y) {
            for (int x = 0; x < BLOCK_SIZE; ++x) {
                if ((luminances[offset + x] & 255) <= threshold) {
                    matrix.set(xoffset + x, yoffset + y);
                }
            }
            offset += stride;
        }

    }

    private static int[][] calculateBlackPoints(byte[] luminances, int subWidth, int subHeight, int width, int height) {
        int[][] blackPoints = new int[subHeight][subWidth];

        for (int y = 0; y < subHeight; ++y) {
            int yoffset = y << BLOCK_SIZE_POWER;
            int maxYOffset = height - BLOCK_SIZE;
            if (yoffset > maxYOffset) {
                yoffset = maxYOffset;
            }

            for (int x = 0; x < subWidth; ++x) {
                int xoffset = x << BLOCK_SIZE_POWER;
                int maxXOffset = width - BLOCK_SIZE;
                if (xoffset > maxXOffset) {
                    xoffset = maxXOffset;
                }

                int sum = 0;
                int min = 255;
                int max = 0;
                int average = 0;

                int averageNeighborBlackPoint;
                for (averageNeighborBlackPoint = yoffset * width + xoffset; average < BLOCK_SIZE; averageNeighborBlackPoint += width) {
                    int xx;
                    for (xx = 0; xx < BLOCK_SIZE; ++xx) {
                        int pixel = luminances[averageNeighborBlackPoint + xx] & 255;
                        sum += pixel;
                        if (pixel < min) {
                            min = pixel;
                        }

                        if (pixel > max) {
                            max = pixel;
                        }
                    }

                    if (max - min > MIN_DYNAMIC_RANGE) {
                        ++average;

                        for (averageNeighborBlackPoint += width; average < BLOCK_SIZE; averageNeighborBlackPoint += width) {
                            for (xx = 0; xx < BLOCK_SIZE; ++xx) {
                                sum += luminances[averageNeighborBlackPoint + xx] & 255;
                            }

                            ++average;
                        }
                    }

                    ++average;
                }

                average = sum >> 6;
                if (max - min <= MIN_DYNAMIC_RANGE) {
                    average = min;
                    if (y > 0 && x > 0) {
                        averageNeighborBlackPoint = (blackPoints[y - 1][x] + 2 * blackPoints[y][x - 1] + blackPoints[y - 1][x - 1]) >> 2;
                        if (average + MIN_DYNAMIC_RANGE < averageNeighborBlackPoint) {
                            average = averageNeighborBlackPoint;
                        }
                    }
                }

                blackPoints[y][x] = average;
            }
        }

        return blackPoints;
    }
}