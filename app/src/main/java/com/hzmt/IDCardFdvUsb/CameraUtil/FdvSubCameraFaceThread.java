package com.hzmt.IDCardFdvUsb.CameraUtil;

import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;

import java.io.ByteArrayOutputStream;
import java.lang.ref.WeakReference;

public class FdvSubCameraFaceThread extends Thread{
    private final WeakReference<CameraActivity> mActivity;
    private byte[] mData;
    private Camera mCamera;
    private int mCameraIdx;

    //构造函数
    FdvSubCameraFaceThread(CameraActivity activity, byte[] data, int cameraId, Camera camera){
        super();

        this.mActivity = new WeakReference<>(activity);
        this.mData = data;
        this.mCameraIdx = cameraId;
        this.mCamera = camera;
    }

    @Override
    public void run(){
        Camera.Size previewSize;
        try {
            previewSize = mCamera.getParameters().getPreviewSize();
        } catch (Exception e) {
            return;
        }

        YuvImage yuvimage = new YuvImage(
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
        Bitmap fullBm = CameraMgt.getBitmapFromBytes(rawImage, mCameraIdx, 1);
        Bitmap bm = fullBm;
        //Bitmap bm = Bitmap.createBitmap(fullBm,
        //        previewSize.width / 4, 0,
        //        previewSize.width / 2, previewSize.height,
        //        null, false);

        CameraActivityData.CameraImageDataSub = rawImage;
        CameraActivityData.CameraImageSub = bm;
        CameraActivityData.capture_subface_done = true;

        CameraActivityData.UploadCameraImageSub = fullBm;
        return;
    }
}
