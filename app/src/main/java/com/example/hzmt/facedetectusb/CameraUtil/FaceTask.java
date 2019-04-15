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

public class FaceTask extends AsyncTask<Void, Void, Rect>{
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
    }

    @Override
    protected Rect doInBackground(Void... params) {
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
        mFullPreviewBm = CameraMgt.getBitmapFromBytes(rawImage, mCameraIdx, 2);
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

        // face detect
        Rect face = new Rect();
        boolean detect = MyApplication.AiFdrScIns.dectect_camera_face(mScreenBm,face);
        //==================
        //String crops=String.format("l:%d,t:%d,r:%d,b:%d",
        //        face.left,face.top,
        //        face.right,face.bottom);
        //Log.e("cropImg",crops);
        //===================
        if(face.left < 0) face.left = 0;
        if(face.top < 0) face.top = 0;
        if(face.right > mScreenBm.getWidth()) face.right = mScreenBm.getWidth();
        if(face.bottom > mScreenBm.getHeight()) face.bottom = mScreenBm.getHeight();


        if(detect){
            //mSendBm = CameraMgt.getBitmapFromBytes(rawImage, mCameraIdx, 1);
            mSendBm = mScreenBm;
            mSendRawImage = rawImage;
            return face;
        }
        else{
            return null;
        }
    }

    @Override
    protected void onPostExecute(Rect face) {
        try {
            //mInfoLayout.setCameraImage(mScreenBm); //     test

            if (face != null) {
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
                    //synchronized (CameraActivityData.fdvlock) {
                        CameraActivityData.idcardfdv_working = false;
                    //}
                    return;
                }
                else if(IDCardReader.STATE_REFUSE_PERMISSION == readerState){
                    String errMsg = "无权限访问身份证读卡器!";
                    Toast.makeText(mActivity, errMsg, Toast.LENGTH_SHORT).show();
                    //synchronized (CameraActivityData.fdvlock) {
                        CameraActivityData.idcardfdv_working = false;
                    //}
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

                // 读卡器读卡线程
                //synchronized(CameraActivityData.fdvlock) {
                    CameraActivityData.idcardfdv_idcardstate = IDCardReadThread.IDCARD_STATE_NONE;
                //}
                IDCardReadThread readcard = new IDCardReadThread(mActivity, mActivity.mIDCardReadHandler);
                readcard.start();

                // http work
                FaceHttpThread httpth = new FaceHttpThread(mActivity, mInfoLayout,
                            mScreenBm, face,
                            MyApplication.idcardfdvUrl);

                httpth.start();

            }
            else {
                // 无人脸
                //synchronized (CameraActivityData.fdvlock) {
                    CameraActivityData.idcardfdv_working = false;
                //}
            }
        }
        catch(Exception e){
            //synchronized(CameraActivityData.fdvlock){
                CameraActivityData.idcardfdv_working = false;
            //}
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

        String verify_photo_feat = "";

        List<Bitmap> verify_photos = new ArrayList<>();
        InputStream certstream = null;
        if(0 == MyApplication.idcardfdv_requestType) {
            verify_photos.add(verify_photo);
        }
        else if(1 == MyApplication.idcardfdv_requestType) {
            // image feat
            synchronized(CameraActivityData.AiFdrSclock) {
                verify_photo_feat = MyApplication.AiFdrScIns.get_camera_feat2(croprect);
            }
            activity.mDebugLayout.addText("camera face:"+
                                            "L("+croprect.left+")"+
                                            "T("+croprect.top+")"+
                                            "R("+croprect.right+")"+
                                            "B("+croprect.bottom+")\n");
            certstream = new ByteArrayInputStream(MyApplication.certstream_baos.toByteArray());
        }

        // 等待身份证读卡，bitmap生成完成
        int st = 0;
        //synchronized(CameraActivityData.fdvlock){
            st = CameraActivityData.idcardfdv_idcardstate;
        //}
        while(st != IDCardReadThread.IDCARD_ALL_OK){
            if (st == IDCardReadThread.IDCARD_ERR_READERR) {
                activity.mDebugLayout.addText("IDCard read error!\n");
                //synchronized(CameraActivityData.fdvlock){
                    CameraActivityData.idcardfdv_working = false;
                //}
                return;
            }

            try {
                Thread.sleep(100);
                //synchronized(CameraActivityData.fdvlock){
                    st = CameraActivityData.idcardfdv_idcardstate;
                //}
            } catch (InterruptedException e){
                e.printStackTrace();
            }
        }

        if ( verify_photo_feat.equals("")) {
            activity.mDebugLayout.addText("verify photo feat: null\n");
            //synchronized(CameraActivityData.fdvlock){
                CameraActivityData.idcardfdv_working = false;
            //}
            return;
        }

        // test
        //CameraActivityData.Idcard_id = "332526198407210014";
        //CameraActivityData.Idcard_issuedate = "201301212";
        //Bitmap bm = BitmapFactory.decodeResource(context.getResources(), R.drawable.zp);
        String idcard_photo = null;
        if(0 == MyApplication.idcardfdv_requestType) {
            idcard_photo = "data:image/jpeg;base64," + B64Util.bitmapToBase64(CameraActivityData.PhotoImage);
        }
        else if(1 == MyApplication.idcardfdv_requestType) {
            idcard_photo = CameraActivityData.PhotoImageFeat;
        }
        // set info layout
        if(null != infoL) {
            infoL.setCameraImage(verify_photo);
            infoL.setIdcardPhoto(CameraActivityData.PhotoImage);
            infoL.setResultSimilarity("--%");
            infoL.resetResultIcon();
        }

        final CameraActivity cbctx = activity;
        final InfoLayout infolayout = infoL;
        IdcardFdv.RequestCallBack reqcb = new IdcardFdv.RequestCallBack() {
            @Override
            public void onSuccess(JSONObject object) {
                Log.e("IdcardFdv cb", object.toString());
                boolean saveUpload = false;
                double sim = 0.0f;
                String serial_no = "";
                try {
                    if (object.getInt("Err_no") == 0){
                        serial_no = object.getString("Serial_No");

                        sim = object.getDouble("Similarity");
                        //String retstr = "相似度: " + sim;
                        //Toast.makeText(cbctx, retstr, Toast.LENGTH_LONG).show();
                        String retstr = String.format("%.1f%%",sim * 100);
                        infolayout.setResultSimilarity(retstr);

                        // access control
                        if(sim > CameraActivityData.SimThreshold) {
                            infolayout.setResultIconPass();

                            boolean action = true;
                            Date dt =new Date();
                            Long nowTime= dt.getTime();
                            if(MyApplication.accessControlCnt != null){
                                long spaceTime = (nowTime - MyApplication.accessControlCnt)/1000;
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
                        saveUpload = true;
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                Date dt = new Date();
                long fdvtime = dt.getTime() - MyApplication.idcardfdvCnt;
                cbctx.mDebugLayout.addText("FDV Time:"+fdvtime+"\n");

                CameraActivity.startBrightnessWork(cbctx, infolayout);
                CameraActivity.delayResumeFdvWork(2*1000);

                if(saveUpload){
                    int simInt = (int)(sim * 1000);
                    String prename = String.format("%s_%s_%03d",
                                                CameraActivityData.Idcard_id,
                                                serial_no,
                                                simInt);
                    CameraActivity.saveUploadBitmapBMP(CameraActivityData.PhotoImageData,prename+"_0");
                    CameraActivity.saveUploadBitmapJPEG(CameraActivityData.CameraImage,prename+"_1");
                }
            }

            @Override
            public void onFailure(int errno) {
                cbctx.mDebugLayout.addText("network failed!\n");


                if( 3 == errno){
                    // fdv failed
                }
                else{  } // others

                Toast.makeText(cbctx, "网络请求错误！", Toast.LENGTH_SHORT).show();
                CameraActivity.startBrightnessWork(cbctx, infolayout);
                CameraActivity.delayResumeFdvWork(2*1000);
            }
        };

        CameraActivityData.CameraImage = vbm; // mScreenBm
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
