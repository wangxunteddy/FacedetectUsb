package com.hzmt.IDCardFdvUsb.CameraUtil;

import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.hzmt.IDCardFdvUsb.MyApplication;
import com.hzmt.IDCardFdvUsb.util.ShowToastUtils;

import java.io.ByteArrayOutputStream;
import java.lang.ref.WeakReference;

public class DetectFaceThread extends AsyncTask<Void, Integer, Rect>{
    public static final int REOPEN_IDCARDREADER = 0;

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

        CameraActivity activity = mActivity.get();
        if(activity == null )
            return null;

        Bitmap fullPreviewBm = activity.mNV21ToBitmap.nv21ToBitmap(mData, previewSize.width, previewSize.height);

        // face detect
        Rect face = new Rect();
        boolean detect = MyApplication.AiFdrScIns.dectect_camera_face(fullPreviewBm, face);

        // 检查和重新初始化阅读器
        CameraActivityData.CheckIDCardReaderCnt++;
        if(CameraActivityData.CheckIDCardReaderCnt >= 1){
            if (!activity.mIDCardReader.IsReaderConnected()) {
                activity.mIDCardReader.CloseIDCardReader();
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e){
                    e.printStackTrace();
                }
                publishProgress(REOPEN_IDCARDREADER);
            }
            CameraActivityData.CheckIDCardReaderCnt = 0;
        }

        if (detect) {
            //long stime = System.currentTimeMillis();
            //MyApplication.AiFdrScIns.get_camera_feat2(face);
            //long feattime = System.currentTimeMillis() - stime;
            //Log.i("full FeatTime",""+feattime);
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

        pass_flag = true; //19.06.10修改为全部放行。

        if (face != null || pass_flag) {
        //if(false){
            CameraActivity activity = mActivity.get();
            if(activity != null){
                //CameraActivity.keepBright(activity);
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
                        CameraActivity.startBrightnessWork(activity);
                        String errMsg = "未找到身份证阅读器!";
                        ShowToastUtils.showToast(activity, errMsg, Toast.LENGTH_SHORT);
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e){
                            e.printStackTrace();
                        }
                        CameraActivityData.detect_face_enable = true;
                        return;
                    } else if (IDCardReader.STATE_REFUSE_PERMISSION == readerState) {
                        CameraActivity.startBrightnessWork(activity);
                        String errMsg = "无权限访问身份证阅读器!";
                        ShowToastUtils.showToast(activity, errMsg, Toast.LENGTH_SHORT);
                        try {
                            Thread.sleep(10);
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

    @Override
    protected void onProgressUpdate(Integer... values) {
        CameraActivity activity = mActivity.get();
        if (activity == null)
            return;

        int value = values[0];
        switch(value) {
            case REOPEN_IDCARDREADER:
                activity.mIDCardReader.OpenIDCardReader(activity);
                if(activity.mIDCardReader.GetInitState() == IDCardReader.STATE_INIT_OK){
                    String msg = "已连接身份证阅读器!";
                    ShowToastUtils.showToast(activity, msg, Toast.LENGTH_SHORT);
                }
                break;
        }
    }
}
