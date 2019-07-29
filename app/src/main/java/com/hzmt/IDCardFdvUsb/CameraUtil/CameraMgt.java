package com.hzmt.IDCardFdvUsb.CameraUtil;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Matrix;
import android.graphics.Point;
import android.view.SurfaceView;
import android.view.SurfaceHolder;
import android.view.Surface;
import android.view.View;
import android.view.MotionEvent;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.Camera;

import android.util.Log;
import android.view.ViewGroup;
import android.widget.Toast;

import com.hzmt.IDCardFdvUsb.MainActivity;
import com.hzmt.IDCardFdvUsb.MyApplication;
import com.hzmt.IDCardFdvUsb.util.ShowToastUtils;

import java.io.IOException;
import java.util.List;


/**
 * Created by xun on 2017/8/23.
 */

public class CameraMgt {
    private Activity mActivity;
    private SurfaceView mSurfaceView;
    private SurfaceDraw mFaceRect;

    private Camera mCamera = null;
    private Camera mCameraSub = null;
    private int mCamIdx = 0;
    private int mCamIdxSub = 1;
    private Boolean isPreview = false;
    private Camera.PreviewCallback mCameraFrameWork = null;
    private Camera.PreviewCallback mCameraFrameWorkSub = null;
    private Camera.PictureCallback mCameraTakePictureJpegCB = null;

    public CameraMgt(Activity activity, SurfaceView preview, SurfaceDraw facerect) {
        mActivity = activity;

        mSurfaceView = preview;
        mFaceRect = facerect;

        //mSurfaceView.setVisibility(View.VISIBLE);
        mSurfaceView.setOnTouchListener(mSurfaceViewTouch);
        // 获得 SurfaceHolder 对象
        SurfaceHolder mSurfaceHolder = mSurfaceView.getHolder();
        // 设置 Surface 格式
        // 参数： PixelFormat中定义的 int 值 ,详细参见 PixelFormat.java
        //mSurfaceHolder.setFormat(PixelFormat.TRANSPARENT);
        // 添加 Surface 的 callback 接口
        mSurfaceHolder.addCallback(mSurfaceCallback);
    }

    public int getCurrentCameraId() {
        return mCamIdx;
    }
    public int getCurrentSubCameraId() {
        return mCamIdxSub;
    }
    public boolean isCameraPreviewStarted(){return isPreview;}

    public SurfaceView getCameraView() {
        return mSurfaceView;
    }

    public void setPreviewCallback(Camera.PreviewCallback cb, Camera.PreviewCallback cbSub) {
        mCameraFrameWork = cb;
        mCameraFrameWorkSub = cbSub;
    }

    public void setTakePictureJpegCallback(Camera.PictureCallback cb) {
        mCameraTakePictureJpegCB = cb;
    }

    public void openCamera(boolean bStartCamera) {
        if(bStartCamera)
            startCamera();
    }

    public void closeCamera(){
        stopCamera();
    }

    private void stopCamera() {
        if (isPreview) { //正在预览
            if (mCamera != null) {
                mCamera.setPreviewCallback(null);
                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
            }

            if (mCameraSub != null) {
                mCameraSub.setPreviewCallback(null);
                mCameraSub.stopPreview();
                mCameraSub.release();
                mCameraSub = null;
            }

            isPreview = false;
        }
    }

    private void startCamera() {
        if (isPreview)
            return;

        int cameraCnt = Camera.getNumberOfCameras();
        if(cameraCnt < 1){
            // 无可用摄像头
            return;
        }
        else if(MyApplication.idcardfdv_subCameraEnable && cameraCnt<2){
            // 要求双目，摄像头不足
            return;
        }

        try {
            // Camera.open() 默认返回的后置摄像头信息
            mCamera = Camera.open(mCamIdx);//Camera.open(mCamIdx);//打开硬件摄像头，这里导包得时候一定要注意是android.hardware.Camera
            //设置角度
            setCameraDisplayOrientation(mActivity, mCamIdx, mCamera);
            mCamera.setPreviewDisplay(mSurfaceView.getHolder());//通过SurfaceView显示取景画面

            // 设置参数
            Camera.Parameters parameters = mCamera.getParameters();
            List<Integer> previewFormats = parameters.getSupportedPreviewFormats();
            if (previewFormats.contains(ImageFormat.NV21)) {
                parameters.setPreviewFormat(ImageFormat.NV21);
            }
            List<Integer> picFormats = parameters.getSupportedPictureFormats();
            if (picFormats.contains(PixelFormat.JPEG)) {
                parameters.setPictureFormat(PixelFormat.JPEG);
            }

            // 设置最佳预览分辨率
            Camera.Size previewSize = getSuitablePreviewSize(parameters);
            if(null != previewSize){
                parameters.setPreviewSize(previewSize.width, previewSize.height);
                // parameters.setPreviewSize(1280, 960); //test
            }

            List<Camera.Size> picSizes = parameters.getSupportedPictureSizes();
            Camera.Size picSize = picSizes.get(0);
            for(int i=0;i<picSizes.size();i++){
                Camera.Size s = picSizes.get(i);
                if(s.width >= 640 && s.width <= 1280){
                    picSize = s;
                    break;
                }
            }
            parameters.setPictureSize(picSize.width, picSize.height);
            //========== for SHANGZHU=========
            parameters.setPreviewSize(1280, 720);
            parameters.setPictureSize(1280, 720);
            //===============================
            List<String> focusModes = parameters.getSupportedFocusModes();
            if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            }
            //List<String> sceneModes=parameters.getSupportedSceneModes();
            //if (sceneModes.contains(Camera.Parameters.SCENE_MODE_PORTRAIT)) {
            //    parameters.setSceneMode(Camera.Parameters.SCENE_MODE_PORTRAIT);
            //}

            mCamera.setParameters(parameters);
            setSuitableSurface(); // 设置绘图窗口尺寸
            mCamera.setPreviewCallback(mCameraFrameWork);

            // sub camera
            mCameraSub = null;
            if(MyApplication.idcardfdv_subCameraEnable){
                mCameraSub = Camera.open(mCamIdxSub);
                //设置角度
                setCameraDisplayOrientation(mActivity, mCamIdxSub, mCameraSub);
                // 设置参数
                Camera.Parameters parametersSub = mCameraSub.getParameters();
                List<Integer> previewFormatsSub = parametersSub.getSupportedPreviewFormats();
                if (previewFormatsSub.contains(ImageFormat.NV21)) {
                    parametersSub.setPreviewFormat(ImageFormat.NV21);
                }
                List<Integer> picFormatsSub = parametersSub.getSupportedPictureFormats();
                if (picFormatsSub.contains(PixelFormat.JPEG)) {
                    parametersSub.setPictureFormat(PixelFormat.JPEG);
                }
                //========== for SHANGZHU=========
                parametersSub.setPreviewSize(1280, 720);
                parametersSub.setPictureSize(1280, 720);
                //================================
                List<String> focusModesSub = parametersSub.getSupportedFocusModes();
                if (focusModesSub.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                    parametersSub.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                }
                mCameraSub.setParameters(parametersSub);
                mCameraSub.setPreviewCallback(mCameraFrameWorkSub);
            }

            mCamera.startPreview();//开始预览
            if(MyApplication.idcardfdv_subCameraEnable) {
                mCameraSub.startPreview();
            }
            isPreview = true;//设置是否预览参数为真
        } catch (IOException e) {
            Log.e("surfaceCreated", e.toString());
        }
    }


    private Camera.Size getSuitablePreviewSize(Camera.Parameters parameters){
        Point point = new Point();
        mActivity.getWindowManager().getDefaultDisplay().getRealSize(point);
        float rate = (point.x < point.y)?(point.x * 1.0f / point.y):(point.y * 1.0f / point.x);

        List<Camera.Size> previewSizes = parameters.getSupportedPreviewSizes();
        Camera.Size retSize = null;
        int maxLen = 0;
        //String pres=new String();
        for(int i=0; i<previewSizes.size();i++) {
            Camera.Size size = previewSizes.get(i);

            float r = (size.width < size.height) ? (size.width * 1.0f / size.height) :
                    (size.height * 1.0f / size.width);
            int l = (size.width > size.height) ? size.width : size.height;
            if((Math.abs(rate - r) < 0.01) && (l > maxLen) ){
                maxLen = l;
                retSize = size;
            }
            //pres += (previewSizes.get(i).width + " x " + previewSizes.get(i).height);
            //pres += "   ";
        }

        //ShowToastUtils.showToast(mActivity, pres, Toast.LENGTH_LONG);
        return retSize;
    }

    private void setSuitableSurface(){
        Camera.Size previewSize;
        try {
            previewSize = mCamera.getParameters().getPreviewSize();
        }
        catch(Exception e){
            return;
        }

        if(0 != previewSize.height){
            //Point point = new Point();
            // mActivity.getWindowManager().getDefaultDisplay().getRealSize(point); // 全屏分辨率

            // 设置预览窗口大小
            ViewGroup.LayoutParams cameraLP = mSurfaceView.getLayoutParams();
            cameraLP.height = CameraActivityData.CameraActivity_height;
            //cameraLP.width = cameraLP.height * previewSize.width / previewSize.height; // 保持宽高比
            cameraLP.width = CameraActivityData.CameraActivity_width; // 拉伸全屏
            mSurfaceView.setTranslationX(CameraActivityData.CameraActivity_width * 0.15f); // 公安提醒占0.4, 截图从0.25起

            // 设置人脸框绘图窗口大小，与预览窗口等大
            ViewGroup.LayoutParams facerectL = mFaceRect.getLayoutParams();
            facerectL.height = cameraLP.height;
            facerectL.width = cameraLP.width;
            mFaceRect.setTranslationX(CameraActivityData.CameraActivity_width * 0.15f);
        }
    }

    /**
     * 计算摄像头的显示角度
     *
     */
    public static int getCameraDisplayOrientation(Activity activity,int cameraId) {

        Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
        //获取摄像头信息
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        //获取摄像头当前的角度
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0;break;
            case Surface.ROTATION_90: degrees = 90;break;
            case Surface.ROTATION_180: degrees = 180;break;
            case Surface.ROTATION_270: degrees = 270;break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            //前置摄像头
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360; // compensate the mirror
        } else {
            // back-facing  后置摄像头
            result = (info.orientation - degrees + 360) % 360;
        }

       return result;
    }

    /**
     * 设置 摄像头的角度
     *
     * @param activity 上下文
     * @param cameraId 摄像头ID（假如手机有N个摄像头，cameraId 的值 就是 0 ~ N-1）
     * @param camera   摄像头对象
     */
    public static void setCameraDisplayOrientation(Activity activity,
                                                   int cameraId, android.hardware.Camera camera) {
        int result = getCameraDisplayOrientation(activity,cameraId);
        camera.setDisplayOrientation(result);
    }

    private SurfaceHolder.Callback mSurfaceCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
            //String s=String.format("w:%d,h:%d",width,height);
            //Log.e("surfaceChanged", s);
            //ShowToastUtils.showToast(mActivity, s, Toast.LENGTH_LONG);
        }

        /**
         *  在 Surface 首次创建时被立即调用：获得焦点时。一般在这里开启画图的线程
         * @param surfaceHolder 持有当前 Surface 的 SurfaceHolder 对象
         */
        @Override
        public void surfaceCreated(SurfaceHolder surfaceHolder) {
            startCamera();
        }

        /**
         *  在 Surface 被销毁时立即调用：失去焦点时。一般在这里将画图的线程停止销毁
         * @param surfaceHolder 持有当前 Surface 的 SurfaceHolder 对象
         */
        @Override
        public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
            stopCamera();
        }


    };

    private SurfaceView.OnTouchListener mSurfaceViewTouch = new SurfaceView.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if(mCamera != null) {
                    if(CameraActivityData.RequestType == CameraActivityData.REQ_TYPE_REGISTER)
                        mCamera.takePicture(null, null, mCameraTakePictureJpegCB);
                    else if(CameraActivityData.RequestType == CameraActivityData.REQ_TYPE_LOGIN){
                        // cancel
                        stopCamera();

                        Intent intent = new Intent();
                        //mActivity.setResult(CameraActivityData.REQ_TYPE_LOGIN,intent);
                        //mActivity.finish();
                        intent.setClass(mActivity, MainActivity.class);
                        mActivity.startActivity(intent);
                    }
                    else if(CameraActivityData.RequestType == CameraActivityData.REQ_TYPE_IDCARDFDV) {
                        //mCamera.takePicture(null, null, mCameraTakePictureJpegCB);
                    }
                }
            }
            return false;
        }
    };

    public static Bitmap getBitmapFromBytes(byte[] data, int cameraid, int samplesize){
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraid, cameraInfo); // get camerainfo

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = samplesize; //
        Bitmap bm = BitmapFactory.decodeByteArray(data, 0, data.length,options);
        Matrix matrix = new Matrix();
        //if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK)
        //    matrix.postRotate(90);
        //else
        //    matrix.postRotate(270);

        matrix.postRotate(0);
        Bitmap nbmp = Bitmap.createBitmap(bm,
                0, 0, bm.getWidth(),  bm.getHeight(), matrix, true);

        return nbmp;
    }
}
