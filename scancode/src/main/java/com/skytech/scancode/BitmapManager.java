package com.skytech.scancode;

public class BitmapManager {

    static {
        initTable();
    }

    static int r_v_table[], g_v_table[], g_u_table[], b_u_table[], y_table[];
    static int r_yv_table[][], b_yu_table[][];
    static int inited = 0;

    static void initTable() {
        if (inited == 0) {
            r_v_table = new int[256];
            g_v_table = new int[256];
            g_u_table = new int[256];
            b_u_table = new int[256];
            y_table = new int[256];
            r_yv_table = new int[256][256];
            b_yu_table = new int[256][256];
            inited = 1;
            int m = 0, n = 0;
            for (; m < 256; m++) {
                r_v_table[m] = 1634 * (m - 128);
                g_v_table[m] = 833 * (m - 128);
                g_u_table[m] = 400 * (m - 128);
                b_u_table[m] = 2066 * (m - 128);
                y_table[m] = 1192 * (m - 16);
            }
            int temp = 0;
            for (m = 0; m < 256; m++)
                for (n = 0; n < 256; n++) {
                    temp = 1192 * (m - 16) + 1634 * (n - 128);
                    if (temp < 0) temp = 0;
                    else if (temp > 262143) temp = 262143;
                    r_yv_table[m][n] = temp;

                    temp = 1192 * (m - 16) + 2066 * (n - 128);
                    if (temp < 0) temp = 0;
                    else if (temp > 262143) temp = 262143;
                    b_yu_table[m][n] = temp;
                }
        }
    }

    public static void decodeYUV420SP(int[] rgb, byte[] yuv420sp, int width, int height) {
        int frameSize = width * height;

        int i = 0, j = 0, yp = 0;
        int uvp = 0, u = 0, v = 0;
        for (j = 0, yp = 0; j < height; j++) {
            uvp = frameSize + (j >> 1) * width;
            u = 0;
            v = 0;
            for (i = 0; i < width; i++, yp++) {
                int y = (0xff & ((int) yuv420sp[yp]));
                if (y < 0)
                    y = 0;
                if ((i & 1) == 0) {
                    v = (0xff & yuv420sp[uvp++]);
                    u = (0xff & yuv420sp[uvp++]);
                }

                int y1192 = y_table[y];
                int r = r_yv_table[y][v];
                int g = (y1192 - g_v_table[v] - g_u_table[u]);
                int b = b_yu_table[y][u];

                if (g < 0) g = 0;
                else if (g > 262143) g = 262143;
                if (b < 0) b = 0;
                else if (b > 262143) b = 262143;

                r = ((r << 6) & 0xff0000);
                g = ((g >> 2) & 0xff00);
                b = (b >> 10) & 0xff;

                rgb[yp] = (0xff000000 | r | g | b);

                r = r >> 16;
                g = g >> 8;
                int highlight = 220;
                if (r > highlight || g > highlight || b > highlight) {
                    yuv420sp[yp] = (byte) 255;
                    rgb[yp] = 0xffffffff;
                }
            }
        }
    }

    public static void rotate(byte[] rotatedData, byte[] data, int width, int height) {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++)
                rotatedData[x * height + height - y - 1] = data[x + y * width];
        }
    }

    public static void brighten(byte[] data, int width, int height) {
        int yp = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                data[yp] = (byte) (data[yp] >> 1);
                if ((0xff & data[yp]) < 191) {
                    data[yp] = (byte) (data[yp] + (data[yp] >> 2));
                } else {
                    data[yp] = (byte) 255;
                }
                yp++;
            }
        }
    }
}
