package com.example.hzmt.facedetectusb.CameraUtil;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.PointF;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Handler;
import android.os.AsyncTask;
import android.os.Message;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.media.FaceDetector;
import android.util.Log;
import android.content.Context;
import android.content.Intent;

import android.view.SurfaceView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.Date;


import com.example.hzmt.facedetectusb.MyApplication;
import com.example.hzmt.facedetectusb.R;
import com.example.hzmt.facedetectusb.util.B64Util;
import com.example.hzmt.facedetectusb.util.HttpUtil;
import com.example.hzmt.facedetectusb.util.SystemUtil;
import com.example.hzmt.facedetectusb.util.AccessControlUtil;
import com.example.hzmt.facedetectusb.SubActivity;

//import com.example.hzmt.idcardfdv.IdcardFdv;
import com.example.hzmt.facedetectusb.util.IdcardFdv;
//import com.invs.BtReaderClient;
//import com.invs.IClientCallBack;
//import com.invs.InvsIdCard;
import com.invs.UsbBase;
import com.invs.UsbSam;
import com.invs.invsIdCard;
import com.invs.invswlt;

/**
 * Created by xun on 2017/8/29.
 */

public class FaceTask extends AsyncTask<Void, Void, FaceDetector.Face>{
    private CameraActivity mActivity;
    private byte[] mData;
    private Camera mCamera;
    private int mCameraIdx;
    private InfoLayout mInfoLayout;
    private ImageView mImgView;
    private SurfaceDraw mSurface;
    private SurfaceView mCameraView;
    private Bitmap mFullPreviewBm;
    private Bitmap mScreenBm;
    private Bitmap mSendBm;
    private byte[] mSendRawImage;
    private IDCardReadHandler mHandler;

    //public invsIdCard mCard;

    //构造函数
    FaceTask(CameraActivity activity, byte[] data, int cameraId, Camera camera,
             InfoLayout infoL, SurfaceDraw surface, SurfaceView cameraview){
        super();

        this.mActivity = activity;
        this.mData = data;
        this.mCamera = camera;
        this.mInfoLayout = infoL;
        this.mSurface = surface;
        this.mCameraView = cameraview;
        this.mCameraIdx = cameraId;

        this.mHandler = null;
    }

    @Override
    protected FaceDetector.Face doInBackground(Void... params) {
        // TODO Auto-generated method stub
        Camera.Size previewSize;
        try {
            previewSize = mCamera.getParameters().getPreviewSize();
        }
        catch(Exception e){
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
        mFullPreviewBm = CameraMgt.getBitmapFromBytes(rawImage, mCameraIdx, 1);
        int cwidth = CameraActivityData.CameraActivity_width - mInfoLayout.getInfoLayoutWidth();
        // 保持宽高比时:
        //cwidth = cwidth * fullPreviewBm.getHeight() / CameraActivityData.CameraActivity_height;
        // 宽拉伸全屏时:
        cwidth = cwidth * mFullPreviewBm.getWidth() / CameraActivityData.CameraActivity_width;

        cwidth = (cwidth+1)/2*2; // 需确保用于人脸检测的图尺寸为偶数
        if(mFullPreviewBm.getWidth() - cwidth < 0) {
            // 避免负值错误
            // 保持宽高比且信息面板宽度小没有覆盖到预览区域时会出现
            cwidth = mFullPreviewBm.getWidth();
        }
        int sx = 0;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(mCameraIdx, cameraInfo); // get camerainfo
        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            sx = mFullPreviewBm.getWidth() - cwidth;
        }
        mScreenBm = Bitmap.createBitmap(mFullPreviewBm,
                sx,
                0,
                cwidth,
                mFullPreviewBm.getHeight(),
                null, false);

        // FaceDetector
        Bitmap bmcopy = mScreenBm.copy(Bitmap.Config.RGB_565, true); // 必须为RGB_565
        FaceDetector faceDetector = new FaceDetector(bmcopy.getWidth(),
                bmcopy.getHeight(), CameraActivityData.FaceDetectNum);
        FaceDetector.Face[] faces = new FaceDetector.Face[CameraActivityData.FaceDetectNum];
        int faceNum = faceDetector.findFaces(bmcopy, faces);
        if(faceNum > 0){
            //mSendBm = CameraMgt.getBitmapFromBytes(rawImage, mCameraIdx, 1);
            mSendBm = mScreenBm;
            mSendRawImage = rawImage;
            return faces[0];
        }
        else{
            return null;
        }
    }

    @Override
    protected void onPostExecute(FaceDetector.Face face) {
        try {
            //mInfoLayout.setCameraImage(mScreenBm); //     test

            if (face != null) {
                PointF pointF = new PointF();
                face.getMidPoint(pointF);//获取人脸中心点
                float eyesDistance = face.eyesDistance();//获取人脸两眼的间距

                if(eyesDistance > mScreenBm.getWidth() * 1.0f / 10) {
                    // 眼间距需6足够大以避免一些误识别

                    CameraActivity.keepBright(mActivity);

                    Date dt = new Date();
                    MyApplication.idcardfdvCnt = dt.getTime();

                    // 检查读卡器状态
                    // 权限确认中或已经成功打开时再进行人脸认证
                    int readerState = mActivity.mIDCardReader.GetInitState();
                    if(IDCardReader.STATE_NO_DEV == readerState ||
                            IDCardReader.STATE_INIT_ERR == readerState) {
                        String errMsg = "未找到身份证读卡器!";
                        Toast.makeText(mActivity, errMsg, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    else if(IDCardReader.STATE_REFUSE_PERMISSION == readerState){
                        String errMsg = "无权限访问身份证读卡器!";
                        Toast.makeText(mActivity, errMsg, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    mActivity.setHelpImgVisibility(View.VISIBLE);
                    // set info layout
                    if(null != mInfoLayout) {
                        mInfoLayout.resetCameraImage();
                        mInfoLayout.resetIdcardPhoto();
                        mInfoLayout.setResultSimilarity("--%");
                        mInfoLayout.resetResultIcon();
                    }
                    mHandler = new IDCardReadHandler(mActivity);

                    // 读卡器读卡线程
                    MyApplication.idcardfdv_idcardstate = 0;
                    IDCardReadThread readcard = new IDCardReadThread(mActivity, mHandler);
                    readcard.start();

                    // 计算人脸框
                    Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
                    Camera.getCameraInfo(mCameraIdx, cameraInfo); // get camerainfo
                    if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                        pointF.x = mScreenBm.getWidth() - pointF.x;
                    }

                    int maxX = CameraActivityData.CameraActivity_width - mInfoLayout.getInfoLayoutWidth();
                    int maxY = mSurface.getHeight();
                    float rateY = (mSurface.getHeight() - 0.0f) / mFullPreviewBm.getHeight();
                    //float rateX = rateY; // 保持宽高比时
                    float rateX = (mSurface.getWidth() - 0.0f) / mFullPreviewBm.getWidth(); // 全屏时
                    pointF.x = pointF.x * rateX;
                    pointF.y = pointF.y * rateY;
                    eyesDistance = eyesDistance * rateX;

                    int l = (int) (pointF.x - eyesDistance * 1.1f);
                    if (l < 0) l = 2;
                    int t = (int) (pointF.y - eyesDistance * 1.6f);
                    if (t < 0) t = 2;
                    int r = (int) (pointF.x + eyesDistance * 1.1f);
                    if (r > maxX - 2) r = maxX - 2;
                    int b = (int) (pointF.y + eyesDistance * 1.8f);
                    if (b > maxY - 2) b = maxY - 2;
                    //String pres=l+","+t+","+r+","+b;
                    //Toast.makeText(mActivity, pres, Toast.LENGTH_LONG).show();
                    // 人脸框
                    //mSurface.setFaceRect(l, t, r, b);

                    // http work
                    if (!MyApplication.idcardfdv_working) {
                        MyApplication.idcardfdv_working = true;
                        //int rate = 4;
                        float crate = 1;
                        PointF cpoint = new PointF();
                        face.getMidPoint(cpoint);
                        cpoint.x = cpoint.x * crate;
                        cpoint.y = cpoint.y * crate;
                        float ed = face.eyesDistance() * crate * 2;

                        int cmaxX = (int) (mScreenBm.getWidth() * crate);
                        int cmaxY = (int) (mScreenBm.getHeight() * crate);

                        //if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                        //    cpoint.x = cmaxX - cpoint.x;
                        //}

                        int cl = (int) (cpoint.x - ed);
                        if (cl < 0) cl = 0;
                        int ct = (int) (cpoint.y - ed);
                        if (ct < 0) ct = 0;
                        int cr = (int) (cpoint.x + ed);
                        if (cr > cmaxX) cr = cmaxX;
                        int cb = (int) (cpoint.y + ed);
                        if (cb > cmaxY) cb = cmaxY;
                        Rect croprect = new Rect(cl, ct, cr, cb);



                        FaceHttpThread httpth = new FaceHttpThread(mActivity, mInfoLayout,
                                mScreenBm, croprect,
                                MyApplication.idcardfdvUrl);

                        httpth.start();
                    }
                }
                else
                    mSurface.setFaceRect(0, 0, 0, 0);
            } else
                mSurface.setFaceRect(0, 0, 0, 0);
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    private static void idcardfdvRequest(CameraActivity activity,InfoLayout infoL,
                                         Bitmap vbm,Rect croprect,
                                         String urlstring){
        Bitmap verify_photo = Bitmap.createBitmap(vbm,
                croprect.left, croprect.top,
                croprect.right - croprect.left,
                croprect.bottom-croprect.top,
                null, false);

        Rect faceRect = new Rect();
        String verify_photo_feat = "";

        List<Bitmap> verify_photos = new ArrayList<>();
        InputStream certstream = null;
        if(0 == MyApplication.idcardfdv_requestType) {
            verify_photos.add(verify_photo);
        }
        else if(1 == MyApplication.idcardfdv_requestType) {
            // image feat
            verify_photo_feat = MyApplication.AiFdrScIns.get_camera_feat(vbm,faceRect);
            activity.mDebugLayout.addText("camera face:"+
                                            "L("+faceRect.left+")"+
                                            "T("+faceRect.top+")"+
                                            "R("+faceRect.right+")"+
                                            "B("+faceRect.bottom+")\n");
            certstream = new ByteArrayInputStream(MyApplication.certstream_baos.toByteArray());
        }


        // 等待身份证读卡，bitmap生成完成
        while(MyApplication.idcardfdv_idcardstate != IDCardReadThread.IDCARD_ALL_OK){
            if (MyApplication.idcardfdv_idcardstate == IDCardReadThread.IDCARD_ERR_READERR) {
                activity.mDebugLayout.addText("IDCard read error!\n");
                MyApplication.idcardfdv_working = false;
                CameraActivity.startBrightnessWork(activity, infoL);
                return;
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e){
                e.printStackTrace();
            }
        }

        if ( verify_photo_feat.equals("")) {
            activity.mDebugLayout.addText("verify photo feat: null\n");
            MyApplication.idcardfdv_working = false;
            return;
        }

        // photo feat
        MyApplication.PhotoImageFeat = MyApplication.AiFdrScIns.get_photo_feat(MyApplication.PhotoImage, faceRect);
        if(MyApplication.PhotoImageFeat.equals("")) {
            CameraActivityData.Idcard_id = "";
            CameraActivityData.Idcard_issuedate = "";
            MyApplication.PhotoImageData = null;
            MyApplication.PhotoImage = null;

            activity.mDebugLayout.addText("idcard photo feat: null\n");
            MyApplication.idcardfdv_working = false;
            return;
        }

        // test
        //CameraActivityData.Idcard_id = "332526198407210014";
        //CameraActivityData.Idcard_issuedate = "201301212";
        //Bitmap bm = BitmapFactory.decodeResource(context.getResources(), R.drawable.zp);
        String idcard_photo = null;
        if(0 == MyApplication.idcardfdv_requestType) {
            idcard_photo = "data:image/jpeg;base64," + B64Util.bitmapToBase64(MyApplication.PhotoImage);
        }
        else if(1 == MyApplication.idcardfdv_requestType) {
            idcard_photo = MyApplication.PhotoImageFeat;
        }
        // set info layout
        if(null != infoL) {
            infoL.setCameraImage(verify_photo);
            infoL.setIdcardPhoto(MyApplication.PhotoImage);
            infoL.setResultSimilarity("--%");
            infoL.resetResultIcon();
        }

        final CameraActivity cbctx = activity;
        final InfoLayout infolayout = infoL;
        IdcardFdv.RequestCallBack reqcb = new IdcardFdv.RequestCallBack() {
            @Override
            public void onSuccess(JSONObject object) {
                Log.e("IdcardFdv cb", object.toString());
                try {
                    if (object.getInt("Err_no") == 0){
                        Double sim = object.getDouble("Similarity");
                        //String retstr = "相似度: " + sim;
                        //Toast.makeText(cbctx, retstr, Toast.LENGTH_LONG).show();
                        String retstr = String.format("%.1f%%",sim * 100);
                        infolayout.setResultSimilarity(retstr);

                        // access control
                        if(sim > CameraActivityData.SimThreshold) {
                            infolayout.setResultIconPass();

                            Boolean action = true;
                            Date dt =new Date();
                            Long nowTime= dt.getTime();
                            if(MyApplication.accessControlCnt != null){
                                Long spaceTime = (nowTime - MyApplication.accessControlCnt)/1000;
                                if(spaceTime < 3)
                                    action = false;
                                // Log.e("SpaceTime:", space.toString());
                            }
                            if(action) {
                                // AccessControlUtil.OpenDoor(
                                //          MyApplication.accessControlUrl,
                                //          MyApplication.accessControlSn
                                // );
                                MyApplication.accessControlCnt = nowTime;
                            }
                        }
                        else{
                            infolayout.setResultIconNotPass();
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                Date dt = new Date();
                Long fdvtime = dt.getTime() - MyApplication.idcardfdvCnt;
                cbctx.mDebugLayout.addText("FDV Time:"+fdvtime+"\n");

                MyApplication.idcardfdv_working = false;
                CameraActivity.startBrightnessWork(cbctx, infolayout);
            }

            @Override
            public void onFailure(int errno) {
                cbctx.mDebugLayout.addText("network failed!\n");
                MyApplication.idcardfdv_working = false;
                CameraActivity.startBrightnessWork(cbctx, infolayout);
                if( 3 == errno){
                    // fdv failed
                }
                else{ /* others*/ }
            }
        };

        IdcardFdv.request(activity,
                    MyApplication.idcardfdv_requestType,
                    urlstring,
                    CameraActivityData.Idcard_id,
                    CameraActivityData.Idcard_issuedate,
                    idcard_photo,
                    verify_photo_feat,
                    verify_photos,
                    null,//certstream,
                    reqcb);
    }

    private static class FaceHttpThread extends Thread {
        private CameraActivity mActivity;
        private InfoLayout infolayout;
        private Rect croprect;
        private Bitmap bm;
        private String url;

        public FaceHttpThread(CameraActivity activity, InfoLayout infoL,
                              Bitmap verify_photo, Rect croprect,
                              String url) {
            this.mActivity = activity;
            this.infolayout = infoL;
            this.croprect = croprect;
            this.bm = verify_photo;
            this.url = url;
        }

        @Override
        public void run() {
            idcardfdvRequest(mActivity, infolayout, bm, croprect, url);
        }
    }

}
