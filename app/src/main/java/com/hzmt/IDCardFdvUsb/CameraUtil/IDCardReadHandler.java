package com.hzmt.IDCardFdvUsb.CameraUtil;

import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Toast;

import java.lang.ref.WeakReference;

public class IDCardReadHandler extends Handler {
    private final WeakReference<CameraActivity> mActivity;

    public IDCardReadHandler(CameraActivity activity) {
        mActivity = new WeakReference<>(activity);
    }

    @Override
    public void handleMessage(Message msg) {
        CameraActivity activity = mActivity.get();
        switch(msg.what){
            case IDCardReadThread.IDCARD_ERR_READERR:
                if (activity != null) {
                    String errMsg = "身份证读卡失败!";
                    Toast.makeText(activity, errMsg, Toast.LENGTH_SHORT).show();
                    // 屏幕亮度及信息清理Timer
                    CameraActivity.startBrightnessWork(activity);
                }

                CameraActivityData.idcardfdv_idcardState = IDCardReadThread.IDCARD_ERR_READERR;
                break;
            case IDCardReadThread.IDCARD_READY:
                if (activity != null)
                    CameraActivity.startBrightnessWork(activity);
                break;
            case IDCardReadThread.IDCARD_CHECK_OK:
                if(CameraActivityData.resume_work)
                    return;

                if (activity != null) {
                    activity.setHelpImgVisibility(View.INVISIBLE);
                    activity.setAttLayOutVisibility(View.VISIBLE);
                    activity.mInfoLayout.setMode(CameraActivityData.idcardfdv_NoIDCardMode);
                    CameraActivity.startBrightnessWork(activity);
                    // 开启捕捉人脸
                    CameraActivityData.capture_face_enable = true;
                }
                break;
            case IDCardReadThread.IDCARD_IMG_OK:
                if (activity != null && CameraActivityData.PhotoImage != null)
                    activity.mInfoLayout.setIdcardPhoto(CameraActivityData.PhotoImage);

                break;
            case IDCardReadThread.IDCARD_ALL_OK:
                if (activity != null) {
                    activity.setHelpImgVisibility(View.INVISIBLE);
                    activity.setAttLayOutVisibility(View.VISIBLE);
                }

                CameraActivityData.idcardfdv_idcardState = IDCardReadThread.IDCARD_ALL_OK;
                break;
            default:
                break;
        }

    }
}
