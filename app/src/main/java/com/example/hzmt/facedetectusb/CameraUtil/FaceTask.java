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
import com.invs.invsUtil;
import com.invs.invswlt;

/**
 * Created by xun on 2017/8/29.
 */

public class FaceTask extends AsyncTask<Void, Void, FaceDetector.Face>{
    private Activity mActivity;
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
    FaceTask(Activity activity, byte[] data, int cameraId, Camera camera,
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
                CameraActivity.startBrightnessWork(mActivity, mInfoLayout);

                PointF pointF = new PointF();
                face.getMidPoint(pointF);//获取人脸中心点
                float eyesDistance = face.eyesDistance();//获取人脸两眼的间距

                if(eyesDistance > mScreenBm.getWidth() * 1.0f / 10) {

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
                    mSurface.setFaceRect(l, t, r, b);

                    // http work
                /*
                sendBitmapData(bmcopy, MyApplication.FaceDetectUrl, new SendDataCallback(){
                    @Override
                    public void onSuccess(int l, int t, int w, int h){
                        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
                        Camera.getCameraInfo(mCameraIdx, cameraInfo); // get camerainfo
                        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT){
                            l = mScreenBm.getWidth()-l-w;
                        }
                        mSurface.setFaceRect2(l*4,t*4,(l+w)*4,(t+h)*4);
                    }
                });*/
                    //sendBitmapData(mScreenBm, MyApplication.FaceDetectUrl,null);

                    //FaceHttpThread httpth = new FaceHttpThread(mActivity,mSendRawImage, mCameraIdx, MyApplication.FaceDetectUrl,null);
                    //httpth.start();

                    //Thread.sleep(500);

                    Date dt = new Date();
                    Long nowTime = dt.getTime();
                    Long spaceTime = new Long(0);
                    if (MyApplication.idcardfdvCnt != null)
                        spaceTime = (nowTime - MyApplication.idcardfdvCnt) / 1000;

                    if ((MyApplication.idcardfdv_working == false) &&
                            (MyApplication.idcardfdvCnt == null || spaceTime > 1)
                            ) {
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

                        MyApplication.idcardfdv_idcarderror = false;
                        IdcardReadThead readcard = new IdcardReadThead(mActivity);
                        readcard.start();

                        FaceHttpThread httpth = new FaceHttpThread(mActivity, mInfoLayout,
                                mScreenBm, croprect,
                                MyApplication.idcardfdvUrl, null);

                        MyApplication.idcardfdvCnt = nowTime;
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

    private static void sendRawImageData(byte[] rawImage, int cameraIdx,
                                         String urlstring, final SendDataCallback callback){
        Bitmap bm = CameraMgt.getBitmapFromBytes(rawImage, cameraIdx, 1);
        sendBitmapData(bm, urlstring, callback);
    }

    private static void sendBitmapData(Bitmap bm, String urlstring, final SendDataCallback callback){
        String bmbase64 = "data:image/jpeg;base64," + B64Util.bitmapToBase64(bm);

        Map<String, String> map = new HashMap<>();
        map.put("imguri", bmbase64);
        //map.put("imguri", "");
        String macaddr = SystemUtil.getMacAddress();
        map.put("mac", macaddr);
        JSONObject object = new JSONObject(map);
        JSONObject resultJSON = HttpUtil.JsonObjectRequest(object, urlstring);
        if(resultJSON != null) {
            //int t = resultJSON.optInt("t");
            //int l = resultJSON.optInt("l");
            //int w = resultJSON.optInt("w");
            //int h = resultJSON.optInt("h");
            //if(callback != null)
            //    callback.onSuccess(l, t, w, h);
        }

    }

    private static void idcardfdvRequest(Context context,InfoLayout infoL,
                                         Bitmap vbm,Rect croprect,
                                         String urlstring){

        Bitmap verify_photo = Bitmap.createBitmap(vbm,
                croprect.left, croprect.top,
                croprect.right - croprect.left,
                croprect.bottom-croprect.top,
                null, false);
        List<Bitmap> verify_photos = new ArrayList<>();
        verify_photos.add(verify_photo);
        final Context cbctx = context;
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

                MyApplication.idcardfdv_working = false;
            }

            @Override
            public void onFailure(int errno) {
                MyApplication.idcardfdv_working = false;
            }
        };

        //InputStream certstream = new ByteArrayInputStream(MyApplication.certstream_baos.toByteArray());

        while(MyApplication.PhotoImageData == null){
            if(MyApplication.idcardfdv_idcarderror){
                MyApplication.idcardfdv_working = false;
                return;
            }

            try {
                Thread.sleep(10);
            } catch (InterruptedException e){
                e.printStackTrace();
            }
        }

        byte[] idcard_photo_Data = MyApplication.PhotoImageData;
        Bitmap bm = BitmapFactory.decodeByteArray(idcard_photo_Data, 0, idcard_photo_Data.length);
        String idcard_photo = "data:image/jpeg;base64,"+B64Util.bitmapToBase64(bm);

        // set info layout
        if(null != infoL) {
            infoL.setCameraImage(verify_photo);
            infoL.setIdcardPhoto(bm);
            infoL.setResultSimilarity("--%");
            infoL.resetResultIcon();
        }

        IdcardFdv.request(context,
                    urlstring,
                    CameraActivityData.Idcard_id,
                    CameraActivityData.Idcard_issuedate,
                    idcard_photo,
                    verify_photos,
                    null, //certstream,
                    reqcb);
    }

    private static void idcardfdvRequest(Context context, InfoLayout infoL,
                                         byte[] verify_photo_data, int cameraIdx, Rect croprect,
                                         String urlstring){

        Bitmap vbm = CameraMgt.getBitmapFromBytes(verify_photo_data, cameraIdx, 1);
        idcardfdvRequest(context, infoL, vbm, croprect,urlstring);
    }

    public interface SendDataCallback{
        void onSuccess(int l, int t, int w, int h);
    }


    private static class FaceHttpThread extends Thread {
        private Context context;
        private InfoLayout infolayout;
        private byte[] rawImage;
        private int cameraIdx;
        private Rect croprect;
        private Bitmap bm;
        private String url;
        private SendDataCallback cb;

        public FaceHttpThread(Context context, InfoLayout infoL,
                              byte[] rawImage, int cameraIdx, Rect croprect,
                              String url, SendDataCallback cb) {
            this.context = context;
            this.infolayout = infoL;
            this.rawImage = rawImage;
            this.cameraIdx = cameraIdx;
            this.croprect = croprect;
            this.bm = null;
            this.url = url;
            this.cb = cb;
        }

        public FaceHttpThread(Context context, InfoLayout infoL,
                              Bitmap verify_photo, Rect croprect,
                              String url, SendDataCallback cb) {
            this.context = context;
            this.infolayout = infoL;
            this.croprect = croprect;
            this.rawImage = null;
            this.bm = verify_photo;
            this.url = url;
            this.cb = cb;
        }

        @Override
        public void run() {
            if(null!=bm) {
                // sendBitmapData(bm, url, cb);
                idcardfdvRequest(context, infolayout, bm, croprect, url);
            }
            else {
                // sendRawImageData(rawImage, cameraIdx, url, cb);
                idcardfdvRequest(context, infolayout, rawImage, cameraIdx, croprect, url);
            }
        }
    }

    private static class IdcardReadThead extends Thread {
        private Context context;

        public IdcardReadThead(Context context){
            this.context = context;
            MyApplication.PhotoImageData = null;
        }

        @Override
        public void run() {

            int iRet = 0;
            iRet = UsbBase.CheckDev(this.context);
            if (iRet == -1){
                Log.e("INVS300:", "连接失败");
                MyApplication.idcardfdv_idcarderror = true;
                return;
            }

            UsbSam mTermb = new UsbSam();
            iRet = mTermb.ReadCard(this.context,false);

            if(iRet == 0) {
                invsIdCard mCard;
                mCard = mTermb.mCard;
                CameraActivityData.Idcard_id = mCard.getIdNo();
                CameraActivityData.Idcard_issuedate = mCard.getStart();
                byte[] szBmp = invswlt.Wlt2Bmp(mCard.wlt);
                if ((szBmp != null) && (szBmp.length == 38862)) {
                    //Bitmap bmp = BitmapFactory.decodeByteArray(szBmp, 0, szBmp.length);
                    //Intent intent = new Intent();
                    //intent.setClass(this.context, SubActivity.class);
                    //intent.putExtra("facedata", szBmp);

                    MyApplication.PhotoImageData = szBmp;
                    //intent.putExtra("RequestType", CameraActivityData.REQ_TYPE_IDCARDFDV);
                    //this.context.startActivity(intent);
                } else {
                    MyApplication.idcardfdv_idcarderror = true;
                }
            }
            else{
                Log.e("INVS300:", "读卡失败");
                MyApplication.idcardfdv_idcarderror = true;
            }

        }
    }
}
