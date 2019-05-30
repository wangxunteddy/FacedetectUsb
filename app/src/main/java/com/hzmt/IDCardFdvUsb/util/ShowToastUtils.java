package com.hzmt.IDCardFdvUsb.util;

import android.content.Context;
import android.widget.Toast;

public class ShowToastUtils {

    private static Toast mToast;

    public static void showToast(Context mContext, String text, int duration) {

        if (mToast == null) {
            mToast = Toast.makeText(mContext, text, duration);
        } else {
            mToast.setText(text);
            mToast.setDuration(duration);
        }
        mToast.show();
    }

    public static void showToast(Context mContext, int resId, int duration) {
        showToast(mContext, mContext.getResources().getString(resId), duration);
    }
}


