package com.example.hzmt.facedetectusb.CameraUtil;

import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.media.FaceDetector;
import android.os.AsyncTask;

import com.example.hzmt.facedetectusb.MyApplication;

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

        YuvImage yuvimage = new YuvImage(
                mData,
                ImageFormat.NV21,
                previewSize.width,
                previewSize.height,
                null);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        yuvimage.compressToJpeg(
                new Rect(previewSize.width / 4, // 裁去两边
                        0,
                        previewSize.width * 3 / 4,
                        previewSize.height),
                100,
                baos);
        byte[] rawImage =baos.toByteArray();
        mBaseBm = CameraMgt.getBitmapFromBytes(rawImage, mCameraIdx, 1);

        // face detect
        boolean  detect = false;
        Rect face = new Rect();
        if(0 == MyApplication.idcardfdv_AiDetect){
            Bitmap bmCopy = mBaseBm.copy(Bitmap.Config.RGB_565, true); // 必须为RGB_565
            FaceDetector faceDetector = new FaceDetector(bmCopy.getWidth(),
                    bmCopy.getHeight(), CameraActivityData.FaceDetectNum);
            FaceDetector.Face[] faces = new FaceDetector.Face[CameraActivityData.FaceDetectNum];
            int faceNum = faceDetector.findFaces(bmCopy, faces);
            if(faceNum > 0) {
                PointF pointF = new PointF();
                faces[0].getMidPoint(pointF);//获取人脸中心点
                float eyesDistance = faces[0].eyesDistance();//获取人脸两眼的间距
                face.left = (int) (pointF.x - eyesDistance * 1.1f);
                face.top = (int) (pointF.y - eyesDistance * 1.4f);
                face.right = (int) (pointF.x + eyesDistance * 1.1f);
                face.bottom = (int) (pointF.y + eyesDistance * 1.8f);

                detect = true;
            }
        }
        else{
            detect = MyApplication.AiFdrScIns.dectect_camera_face(mBaseBm,face);
        }

        if(detect){
            //===================
            // test code
            long detectDone = System.currentTimeMillis();
            long detectTime = detectDone - MyApplication.idcardfdvCameraCnt;
            CameraActivity ac = mActivity.get();
            if(ac != null)
                ac.mDebugLayout.addText("FDV-detect Time:"+detectTime+"\n");
            //===================
            publishProgress(CAMERA_FACE_DETECTED);
            Rect cropRect = new Rect(face);
            if(cropRect.left < 0) cropRect.left = 0;
            if(cropRect.top < 0) cropRect.top = 0;
            if(cropRect.right > mBaseBm.getWidth()) cropRect.right = mBaseBm.getWidth();
            if(cropRect.bottom > mBaseBm.getHeight()) cropRect.bottom = mBaseBm.getHeight();
            mFaceBm = Bitmap.createBitmap(mBaseBm,
                    cropRect.left, cropRect.top,
                    cropRect.right - cropRect.left,
                    cropRect.bottom-cropRect.top,
                    null, false);
            publishProgress(CAMERA_FACE_IMG_OK);

            CameraActivityData.CameraImageData = rawImage;
            CameraActivityData.CameraImage = mBaseBm;
            CameraActivityData.CameraImageFeat = "";
            if(1 == MyApplication.idcardfdv_requestType) {
                CameraActivity activity = mActivity.get();
                // image feat
                synchronized(CameraActivityData.AiFdrSclock) {
                    long stime = new Date().getTime();
                    CameraActivityData.CameraImageFeat = MyApplication.AiFdrScIns.get_camera_feat2(face);
                    long feattime = new Date().getTime() - stime;
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
            if(ac != null)
                ac.mDebugLayout.addText("FDV-cameraImg Time:"+cameraImgTime+"\n");
            //===================

            if(1 == MyApplication.idcardfdv_requestType &&
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

            if(CameraActivityData.idcardfdv_idcardState == IDCardReadThread.IDCARD_ERR_READERR) {
                // 身份证读卡出错，重启定时复位
                if(activity!=null)
                    CameraActivity.startBrightnessWork(activity);
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
                CameraActivity.keepBright(activity);
                break;
            case CAMERA_FACE_IMG_OK:
                activity.mInfoLayout.setCameraImage(mFaceBm);
                break;
        }
    }
}
