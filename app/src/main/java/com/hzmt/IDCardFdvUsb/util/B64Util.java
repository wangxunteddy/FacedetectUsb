package com.hzmt.IDCardFdvUsb.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Created by xun on 2017/8/30.
 */

public class B64Util {
    /**
     * bitmap转为base64
     * @param bitmap
     * @return
     */
    public static String bitmapToBase64(Bitmap bitmap,Bitmap.CompressFormat format) {

        String result = null;
        ByteArrayOutputStream baos = null;
        try {
            if (bitmap != null) {
                baos = new ByteArrayOutputStream();
                bitmap.compress(format, 100, baos);

                baos.flush();
                baos.close();

                byte[] bitmapBytes = baos.toByteArray();
                // Base64.NO_WRAP 省略换行符
                result = Base64.encodeToString(bitmapBytes, Base64.NO_WRAP);// Base64.DEFAULT
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (baos != null) {
                    baos.flush();
                    baos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    /**
     * base64转为bitmap
     * @param base64Data
     * @return
     */
    public static Bitmap base64ToBitmap(String base64Data) {
        byte[] bytes = Base64.decode(base64Data, Base64.NO_WRAP); // Base64.DEFAULT
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    /**
     * String编码base64
     * @param src
     * @return String
     */
    public static String stringToBase64(String src){
        byte[] bytes = src.getBytes();
        return Base64.encodeToString(bytes, Base64.NO_WRAP);
    }

    /**
     * String解码base64
     * @param base64Data
     * @return String
     */
    public static String base64ToString(String base64Data) {
        byte[] bytes = Base64.decode(base64Data, Base64.NO_WRAP);
        return new String(bytes);
    }
}
