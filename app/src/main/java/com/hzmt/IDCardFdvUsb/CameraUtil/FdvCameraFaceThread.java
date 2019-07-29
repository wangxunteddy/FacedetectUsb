package com.hzmt.IDCardFdvUsb.CameraUtil;

import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.util.Log;

import com.hzmt.IDCardFdvUsb.MyApplication;

import java.io.ByteArrayOutputStream;
import java.lang.ref.WeakReference;
import java.util.Date;

public class FdvCameraFaceThread extends AsyncTask<Void, Integer, Rect>{
    public static final int CAMERA_FACE_STATE_NONE = 0;
    public static final int CAMERA_FACE_DETECTED = 1;
    public static final int CAMERA_FACE_IMG_OK = 2;
    public static final int CAMERA_FACE_ALL_OK = 10;

    private final WeakReference<CameraActivity> mActivity;
    private byte[] mData;
    private Camera mCamera;
    private int mCameraIdx;

    private Bitmap mFullBm;
    private Bitmap mBaseBm;
    private Bitmap mFaceBm;

    //构造函数
    FdvCameraFaceThread(CameraActivity activity, byte[] data, int cameraId, Camera camera){
        super();

        this.mActivity = new WeakReference<>(activity);
        this.mData = data;
        this.mCameraIdx = cameraId;
        this.mCamera = camera;
    }

    @Override
    protected Rect doInBackground(Void... params) {
        // TODO Auto-generated method stub
        MyApplication.idcardfdvCameraCnt = System.currentTimeMillis();
        Camera.Size previewSize;
        try {
            previewSize = mCamera.getParameters().getPreviewSize();
        } catch (Exception e) {
            return null;
        }

        CameraActivity activity = mActivity.get();
        if(activity == null )
            return null;

        //long cmptime = System.currentTimeMillis();
        mFullBm = activity.mNV21ToBitmap.nv21ToBitmap(mData, previewSize.width, previewSize.height);
        if(MyApplication.idcardfdv_subCameraEnable) {
            mBaseBm = Bitmap.createBitmap(mFullBm,
                    previewSize.width / 4, 0,
                    previewSize.width / 2, previewSize.height,
                    null, false);
        }
        else
            mBaseBm = mFullBm;
        /*YuvImage yuvimage = new YuvImage(
                mData,
                ImageFormat.NV21,
                previewSize.width,
                previewSize.height,
                null);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        yuvimage.compressToJpeg(
                new Rect(previewSize.width / 4,
                        0,
                        previewSize.width * 3 / 4,
                        previewSize.height),
                100,
                baos);
        byte[] rawImage =baos.toByteArray();
        mFullBm = CameraMgt.getBitmapFromBytes(rawImage, mCameraIdx, 1);
        mBaseBm = mFullBm;
        */
        //long dttime = System.currentTimeMillis() - cmptime;
        //Log.i("nv21toBtimap",""+dttime);


        // 等待sub获取数据。
        while(!CameraActivityData.capture_subface_done){
            try {
                Thread.sleep(10);
                if(CameraActivityData.resume_work)
                    return null;
            } catch (InterruptedException e){
                e.printStackTrace();
            }
        }

        // face detect
        Rect face = new Rect();
        String faceb64 = null;
        boolean detect = false;
        if(MyApplication.idcardfdv_subCameraEnable) {
            faceb64 = MyApplication.AiFdrScIns.livecheck(
                    mBaseBm,
                    CameraActivityData.CameraImageSub,
                    face,
                    95,
                    0);
            if(faceb64 == null || faceb64.equals(""))
                detect = false;
            else
                detect = true;

            // 越界忽略
            if( face.left < 0 || face.top < 0 ||
                    face.right >= mBaseBm.getWidth() || face.bottom >= mBaseBm.getHeight())
            {
                detect = false;
            }
        }
        else{
            detect = MyApplication.AiFdrScIns.dectect_camera_face(mBaseBm,face);
            // 越界忽略
            if( face.left < 0 || face.top < 0 ||
                    face.right >= mBaseBm.getWidth() || face.bottom >= mBaseBm.getHeight())
            {
                detect = false;
            }
            // 需提前计算base64的情况
            if((CameraActivityData.idcardfdv_RequestMode ||
                    CameraActivityData.idcardfdv_NoIDCardMode ||
                    0 == MyApplication.idcardfdv_requestType) && detect){
                boolean cleanImg = !CameraActivityData.idcardfdv_RequestMode;
                faceb64 = MyApplication.AiFdrScIns.ai_fdr_get_face(face, 95, cleanImg);
                if(faceb64 == null || faceb64.equals(""))
                    detect = false;
            }
        }

        // 请求模式要求base64小于30k
        if(CameraActivityData.idcardfdv_RequestMode &&
                faceb64!=null && faceb64.length() > 30 * 1024){
            detect = false;
        }

        if(detect){
            //===================
            // test code
            long detectDone = System.currentTimeMillis();
            long detectTime = detectDone - MyApplication.idcardfdvCameraCnt;
            activity.mDebugLayout.addText("FDV-detect Time:" + detectTime + "\n");
            //===================
            publishProgress(CAMERA_FACE_DETECTED);
            Rect cropRect = new Rect(face);
            if(cropRect.left < 0) cropRect.left = 0;
            if(cropRect.top < 0) cropRect.top = 0;
            if(cropRect.right > mBaseBm.getWidth() - 1) cropRect.right = mBaseBm.getWidth() - 1;
            if(cropRect.bottom > mBaseBm.getHeight() - 1) cropRect.bottom = mBaseBm.getHeight() - 1;
            mFaceBm = Bitmap.createBitmap(mBaseBm,
                    cropRect.left, cropRect.top,
                    cropRect.width(),
                    cropRect.height(),
                    null, false);
            publishProgress(CAMERA_FACE_IMG_OK);

            //CameraActivityData.CameraImageData = rawImage;
            CameraActivityData.CameraImage = mBaseBm;
            CameraActivityData.CameraFaceRect = new Rect(face);
            CameraActivityData.CameraFaceB64 = faceb64;
            CameraActivityData.UploadCameraImage = mBaseBm;//mFullBm;

            CameraActivityData.CameraImageFeat = "";
            if(1 == MyApplication.idcardfdv_requestType && !CameraActivityData.idcardfdv_NoIDCardMode) {
                // image feat
                synchronized(CameraActivityData.AiFdrScLock) {
                    long stime = System.currentTimeMillis();
                    boolean cleanImg = CameraActivityData.idcardfdv_RequestMode;
                    CameraActivityData.CameraImageFeat = MyApplication.AiFdrScIns.get_camera_feat2(face,cleanImg);
                    long feattime = System.currentTimeMillis() - stime;
                    if(activity != null)
                        activity.mDebugLayout.addText("CameraFeatTime:"+feattime+"\n");
                }

                if(activity != null)
                    activity.mDebugLayout.addText("camera face:"+
                            "L("+cropRect.left+")"+
                            "T("+cropRect.top+")"+
                            "R("+cropRect.right+")"+
                            "B("+cropRect.bottom+")\n");
            }

            //===================
            // test code
            long cameraImgTime = System.currentTimeMillis() - detectDone;
            activity.mDebugLayout.addText("FDV-cameraImg Time:"+cameraImgTime+"\n");
            //===================

            if(1 == MyApplication.idcardfdv_requestType &&
                    !CameraActivityData.idcardfdv_NoIDCardMode &&
                    CameraActivityData.CameraImageFeat.equals(""))
                return null;
            else
                return face;
        }
        else{
            return null;
        }
    }

    @Override
    protected void onPostExecute(Rect face) {
        CameraActivity activity = mActivity.get();
        if (face != null) {
            CameraActivityData.capture_face_enable = false;
            CameraActivityData.idcardfdv_cameraState = CAMERA_FACE_ALL_OK;

            if(CameraActivityData.idcardfdv_NoIDCardMode && !InfoLayout.mIDCardNoInputting) {
                // 无证模式，重启定时复位
                if(activity!=null)
                    WorkUtils.startBrightnessWork(activity);
            }
        }
        else {
            if(CameraActivityData.idcardfdv_idcardState == IDCardReadThread.IDCARD_ERR_READERR)
                // 身份证读卡出错，摄像头人脸捕捉也终止
                CameraActivityData.capture_face_enable = false;
            else if(!CameraActivityData.resume_work)
                CameraActivityData.capture_face_enable = true;  // 进行下一次捕捉
        }
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        CameraActivity activity = mActivity.get();
        if(activity == null)
            return;

        int value = values[0];
        switch(value){
            case CAMERA_FACE_DETECTED:
                WorkUtils.keepBright(activity);
                break;
            case CAMERA_FACE_IMG_OK:
                activity.mInfoLayout.setCameraImage(mFaceBm);
                break;
        }
    }
}
