package com.hzmt.IDCardFdvUsb.CameraUtil;

import android.os.Handler;
import android.os.Message;

import java.lang.ref.WeakReference;

public class FdvWorkHandler extends Handler {
    private final WeakReference<CameraActivity> mActivity;

    public FdvWorkHandler(CameraActivity activity) {
        mActivity = new WeakReference<>(activity);
    }

    @Override
    public void handleMessage(Message msg) {
        CameraActivity activity = mActivity.get();
        switch (msg.what) {
            case FdvWorkThread.FDVWORK_IDCARDERR:
                if (activity != null) {
                    activity.mDebugLayout.addText("IDCard read error!\n");
                    FdvWorkThread.delayResumeFdvWork(activity, 1000 * 3);
                }
                break;
            case FdvWorkThread.FDVWORK_ON_ALL_DATA_READY:
                WorkUtils.keepBright(activity);
                if(CameraActivityData.idcardfdv_NoIDCardMode)
                    activity.mInfoLayout.setIDCardNoBtnEnabled(false);
                break;
        }
    }
}
