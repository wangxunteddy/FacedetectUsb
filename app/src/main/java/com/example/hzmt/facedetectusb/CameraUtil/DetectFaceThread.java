package com.example.hzmt.facedetectusb.CameraUtil;

import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.media.FaceDetector;
import android.os.AsyncTask;
import android.widget.Toast;

import com.example.hzmt.facedetectusb.MyApplication;

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
        boolean detect = false;
        Rect face = new Rect();
        if(0 == MyApplication.idcardfdv_AiDetect){
            // FaceDetector
            Bitmap bmcopy = fullPreviewBm.copy(Bitmap.Config.RGB_565, true); // 必须为RGB_565
            FaceDetector faceDetector = new FaceDetector(bmcopy.getWidth(),
                    bmcopy.getHeight(), CameraActivityData.FaceDetectNum);
            FaceDetector.Face[] faces = new FaceDetector.Face[CameraActivityData.FaceDetectNum];
            int faceNum = faceDetector.findFaces(bmcopy, faces);
            if(faceNum > 0){
                PointF pointF = new PointF();
                faces[0].getMidPoint(pointF);//获取人脸中心点
                float eyesDistance = faces[0].eyesDistance();//获取人脸两眼的间距
                Rect rect = new Rect();
                rect.left = (int)(pointF.x - eyesDistance);
                rect.top = (int)(pointF.y - eyesDistance);
                rect.right = (int)(pointF.x + eyesDistance);
                rect.bottom = (int)(pointF.y + eyesDistance);
                detect = true;
            }
        }
        else {
            // if(1 == MyApplication.idcardfdv_AiDetect)
            detect = MyApplication.AiFdrScIns.dectect_camera_face(fullPreviewBm, face);
        }

        if (detect) {
            return face;
        } else {
            return null;
        }
    }

    @Override
    protected void onPostExecute(Rect face) {
        if (face != null) {
            CameraActivity activity = mActivity.get();
            if(activity != null){
                CameraActivity.keepBright(activity);
                // 检查读卡器状态
                // 权限确认中或已经成功打开时再进行人脸认证
                if(!MyApplication.DebugNoIDCardReader) {
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
                mFdvWorkTh = new FdvWorkThread(activity);
                mFdvWorkTh.start();
            }
            else
                CameraActivityData.detect_face_enable = true;
        }
        else
            CameraActivityData.detect_face_enable = true;
    }
}
