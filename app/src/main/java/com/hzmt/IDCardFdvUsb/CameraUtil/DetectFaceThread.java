package com.hzmt.IDCardFdvUsb.CameraUtil;

import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.widget.Toast;

import com.hzmt.IDCardFdvUsb.MyApplication;

import java.io.ByteArrayOutputStream;
import java.lang.ref.WeakReference;

public class DetectFaceThread extends AsyncTask<Void, Void, Rect>{
    private final WeakReference<CameraActivity> mActivity;
    private byte[] mData;
    private Camera mCamera;
    private int mCameraIdx;
    private FdvWorkThread mFdvWorkTh;

    //构造函数
    DetectFaceThread(CameraActivity activity, byte[] data, int cameraId, Camera camera){
        super();

        this.mActivity = new WeakReference<>(activity);
        this.mData = data;
        this.mCameraIdx = cameraId;
        this.mCamera = camera;
    }

    @Override
    protected Rect doInBackground(Void... params) {
        // TODO Auto-generated method stub
        Camera.Size previewSize;
        try {
            previewSize = mCamera.getParameters().getPreviewSize();
        } catch (Exception e) {
            return null;
        }

        YuvImage yuvimage = new YuvImage(
                mData,
                ImageFormat.NV21,
                previewSize.width,
                previewSize.height,
                null);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        yuvimage.compressToJpeg(
                new Rect(0,
                        0,
                        previewSize.width,
                        previewSize.height),
                80,
                baos);
        byte[] rawImage =baos.toByteArray();
        Bitmap fullPreviewBm = CameraMgt.getBitmapFromBytes(rawImage, mCameraIdx, 1);

        // face detect
        Rect face = new Rect();
        boolean detect = MyApplication.AiFdrScIns.dectect_camera_face(fullPreviewBm, face);

        if (detect) {
            return face;
        } else {
            return null;
        }
    }

    @Override
    protected void onPostExecute(Rect face) {
        // 直接放行判断flag
        boolean pass_flag = (CameraActivityData.idcardfdv_NoIDCardMode ||
                             CameraActivityData.idcardfdv_RequestMode
                            );

        if (face != null || pass_flag) {
            CameraActivity activity = mActivity.get();
            if(activity != null){
                CameraActivity.keepBright(activity);
                // 检查读卡器状态
                // 权限确认中或已经成功打开时再进行人脸认证
                boolean no_check_flag = (MyApplication.DebugNoIDCardReader ||
                                         CameraActivityData.idcardfdv_NoIDCardMode ||
                                         CameraActivityData.idcardfdv_RequestMode
                                        );
                if(!no_check_flag) {
                    int readerState = activity.mIDCardReader.GetInitState();
                    if (IDCardReader.STATE_NO_DEV == readerState ||
                            IDCardReader.STATE_INIT_ERR == readerState) {
                        String errMsg = "未找到身份证读卡器!";
                        Toast.makeText(activity, errMsg, Toast.LENGTH_SHORT).show();
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e){
                            e.printStackTrace();
                        }
                        CameraActivityData.detect_face_enable = true;
                        return;
                    } else if (IDCardReader.STATE_REFUSE_PERMISSION == readerState) {
                        String errMsg = "无权限访问身份证读卡器!";
                        Toast.makeText(activity, errMsg, Toast.LENGTH_SHORT).show();
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e){
                            e.printStackTrace();
                        }
                        CameraActivityData.detect_face_enable = true;
                        return;
                    }
                }

                // 开始验证处理线程
                CameraActivityData.resume_work = false;
                mFdvWorkTh = new FdvWorkThread(activity, activity.mFdvWorkHandler);
                mFdvWorkTh.start();
            }
            else
                CameraActivityData.detect_face_enable = true;
        }
        else
            CameraActivityData.detect_face_enable = true;
    }
}
