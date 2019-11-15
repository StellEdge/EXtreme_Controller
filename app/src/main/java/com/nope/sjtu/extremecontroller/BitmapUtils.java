package com.nope.sjtu.extremecontroller;

import android.graphics.Bitmap;
import android.graphics.Matrix;

/**
 * Created by 10301 on 2019/11/15.
 */

public final class BitmapUtils {
    public static Bitmap rotate(Bitmap bitmap, int angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                bitmap.getHeight(), matrix, true);
    }
}
