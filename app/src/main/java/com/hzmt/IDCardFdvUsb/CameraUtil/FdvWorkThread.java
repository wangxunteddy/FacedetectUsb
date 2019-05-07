package com.hzmt.IDCardFdvUsb.CameraUtil;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import com.hzmt.IDCardFdvUsb.MyApplication;
import com.hzmt.IDCardFdvUsb.util.B64Util;
import com.hzmt.IDCardFdvUsb.util.IdcardFdv;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.ref.WeakReference;

public class FdvWorkThread extends Thread {
    public static final int FDVWORK_IDCARDERR = 0;
    public static final int FDVWORK_ON_ALL_DATA_READY = 10;

    private final WeakReference<CameraActivity> mActivity;
    private IDCardReadThread mThReadCard;
    private FdvWorkHandler mHandler;

    //构造函数
    FdvWorkThread(CameraActivity activity, FdvWorkHandler handler){
        super();

        this.mActivity = new WeakReference<>(activity);
        this.mHandler = handler;
    }

    @Override
    public void run() {
        while(true) {
            CameraActivity activity = mActivity.get();
            if(activity == null)
                break;

            if (!CameraActivityData.idcardfdv_working) {
                CameraActivityData.idcardfdv_working = true;
                // set info layout
                activity.mInfoLayout.resetCameraImage();
                activity.mInfoLayout.resetIdcardPhoto();
                activity.mInfoLayout.setResultSimilarity("--%");
                activity.mInfoLayout.resetResultIcon();

                // 重置摄像头人脸捕捉状态
                CameraActivityData.idcardfdv_cameraState = FdvCameraFaceThread.CAMERA_FACE_STATE_NONE;

                // 读卡器读卡线程
                CameraActivityData.idcardfdv_idcardState = IDCardReadThread.IDCARD_STATE_NONE;
                mThReadCard = new IDCardReadThread(activity, activity.mIDCardReadHandler);
                mThReadCard.start();

                // 等待比对数据准备完成
                boolean fdvloop = true;
                while(fdvloop){
                    if(CameraActivityData.idcardfdv_NoIDCardMode){
                        fdvloop = !CameraActivityData.idcardfdv_IDCardNoReady ||
                                (CameraActivityData.idcardfdv_cameraState != FdvCameraFaceThread.CAMERA_FACE_ALL_OK);
                    }
                    else{
                        fdvloop = (CameraActivityData.idcardfdv_idcardState != IDCardReadThread.IDCARD_ALL_OK) ||
                        (CameraActivityData.idcardfdv_cameraState != FdvCameraFaceThread.CAMERA_FACE_ALL_OK);
                    }

                    if (CameraActivityData.idcardfdv_idcardState == IDCardReadThread.IDCARD_ERR_READERR)
                        break;

                    try {
                        Thread.sleep(10);
                        if(CameraActivityData.resume_work)
                            break;
                    } catch (InterruptedException e){
                        e.printStackTrace();
                    }
                }

                if(CameraActivityData.resume_work)
                    break;

                if (CameraActivityData.idcardfdv_idcardState == IDCardReadThread.IDCARD_ERR_READERR) {
                    Message msg = new Message();
                    msg.what = FDVWORK_IDCARDERR;
                    mHandler.sendMessage(msg);
                    continue;
                }

                Message msg = new Message();
                msg.what = FDVWORK_ON_ALL_DATA_READY;
                mHandler.sendMessage(msg);

                //===================
                // test code
                MyApplication.idcardfdvStepCnt = System.currentTimeMillis();
                long fdvtime = MyApplication.idcardfdvStepCnt - MyApplication.idcardfdvTotalCnt;
                activity.mDebugLayout.addText("FDV-img done:"+fdvtime+"\n");
                //===================

                // fdv
                idcardfdvRequest();
            }

            try {
                Thread.sleep(100);
                if(CameraActivityData.resume_work)
                    break;
            } catch (InterruptedException e){
                e.printStackTrace();
            }
        }

        // 退出前清理
        while(mThReadCard.getState() == State.RUNNABLE) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e){
                e.printStackTrace();
            }
        }

        CameraActivity activity = mActivity.get();
        if(activity != null)
            activity.backToHelp();
        CameraActivityData.detect_face_enable = true;
        CameraActivityData.idcardfdv_working = false;
    }



    // ===========================================================

    public static void delayResumeFdvWork(final CameraActivity activity, long delayMillis){
        Handler handler = new Handler();
        Runnable work = new Runnable() {
            @Override
            public void run() {
                if(activity!=null){
                    activity.backToHelp();
                }
                CameraActivityData.idcardfdv_working = false;
            }
        };

        handler.postDelayed(work,delayMillis);
    }

    private void idcardfdvRequest()
    {
        CameraActivity activity = mActivity.get();
        if(activity == null)
            return;

        String idcard_photo = null;
        String verify_photo = "";
        if(CameraActivityData.idcardfdv_NoIDCardMode){
            idcard_photo = CameraActivityData.PhotoImageFeat;
            verify_photo = CameraActivityData.CameraImageFeat;
        }
        else if(0 == MyApplication.idcardfdv_requestType) {
            idcard_photo = "data:image/png;base64," + B64Util.bitmapToBase64(CameraActivityData.PhotoImage,Bitmap.CompressFormat.PNG);
            //long b64time = System.currentTimeMillis();
            verify_photo = CameraActivityData.CameraImageB64;//"data:image/jpeg;base64," + Base64.encodeToString(CameraActivityData.CameraImageData, Base64.DEFAULT);
            //b64time = System.currentTimeMillis() - b64time;
            //activity.mDebugLayout.addText("b64time:"+b64time+"\n");
        }
        else if(1 == MyApplication.idcardfdv_requestType) {
            idcard_photo = CameraActivityData.PhotoImageFeat;
            verify_photo = CameraActivityData.CameraImageFeat;
        }

        final CameraActivity cbctx = activity;
        IdcardFdv.RequestCallBack reqcb = new IdcardFdv.RequestCallBack() {
            @Override
            public void onSuccess(JSONObject object) {
                Log.e("IdcardFdv cb", object.toString());
                boolean saveUpload = false;
                double sim = 0.0f;
                String serial_no = "";
                try {
                    if (object.getInt("Err_no") == 0){
                        if(1 == MyApplication.idcardfdv_requestType)
                            serial_no = object.getString("Serial_No");

                        sim = object.getDouble("Similarity");
                        String retstr = String.format("%.1f%%",sim * 100);
                        cbctx.mInfoLayout.setResultSimilarity(retstr);

                        if(sim > CameraActivityData.SimThreshold) {
                            cbctx.mInfoLayout.setResultIconPass();

                            // 播放提示音
                            new Thread(){
                                @Override
                                public void run() {
                                    cbctx.mATRight.stop();
                                    cbctx.mATRight.play();
                                    try {
                                        Thread.sleep(1100);
                                        cbctx.mATRight.stop();
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }.start();
                        }
                        else{
                            cbctx.mInfoLayout.setResultIconNotPass();
                            // 播放提示音
                            new Thread(){
                                @Override
                                public void run() {
                                    cbctx.mATWrong.stop();
                                    cbctx.mATWrong.play();
                                    try {
                                        Thread.sleep(1100);
                                        cbctx.mATWrong.stop();
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }.start();
                        }
                        saveUpload = true;
                    }
                    else{
                        String err_msg = object.getString("Err_msg");
                        Toast.makeText(cbctx, err_msg, Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                // 清理身份证号码缓存以避免下次直接使用身份证产生问题
                if(CameraActivityData.idcardfdv_NoIDCardMode){
                    CameraActivityData.Idcard_id = "";
                }

                //===================
                // test code
                cbctx.mDebugLayout.addText("FDV-package Time:"+MyApplication.idcardfdvStepCnt2+"\n");
                long fdvtime = System.currentTimeMillis() - MyApplication.idcardfdvTotalCnt;
                cbctx.mDebugLayout.addText("FDV-TotalTime:"+fdvtime+"\n");
                CameraActivity.startBrightnessWork(cbctx);
                //===================
                delayResumeFdvWork(cbctx,3*1000);

                if(saveUpload){
                    int simInt = (int)(sim * 1000);
                    String prename = String.format("%s_%s_%03d",
                            CameraActivityData.Idcard_id,
                            serial_no,
                            simInt);
                    if(!MyApplication.DebugNoIDCardReader) {
                        //CameraActivity.saveUploadBitmapBMP(CameraActivityData.PhotoImageData, prename + "_0");
                        //CameraActivity.saveUploadBitmapJPEG(CameraActivityData.CameraImage, prename + "_1");
                    }
                }
            }

            @Override
            public void onFailure(int errno) {
                cbctx.mDebugLayout.addText("network failed!\n");


                if( 3 == errno){
                    // fdv failed
                }
                else{  } // others

                // 清理身份证号码缓存以避免下次直接使用身份证产生问题
                if(CameraActivityData.idcardfdv_NoIDCardMode){
                    CameraActivityData.Idcard_id = "";
                }
                Toast.makeText(cbctx, "网络请求错误！", Toast.LENGTH_SHORT).show();
                CameraActivity.startBrightnessWork(cbctx);
                delayResumeFdvWork(cbctx,3*1000);
            }
        };

        // 加载证书
        InputStream certstream = new ByteArrayInputStream(MyApplication.certstream_baos.toByteArray());
        int reqType = 0;
        String reqUrl;
        if(CameraActivityData.idcardfdv_NoIDCardMode) {
            reqType = 1; // feat
            reqUrl = MyApplication.idcardfdvUrl_NoIDCard;
        }
        else {
            reqType = MyApplication.idcardfdv_requestType;
            reqUrl = MyApplication.idcardfdvUrl;
        }
        IdcardFdv.request(activity,
                reqType,
                reqUrl,
                CameraActivityData.Idcard_id,
                CameraActivityData.Idcard_issuedate,
                idcard_photo,
                verify_photo,
                certstream,
                reqcb);
    }
}
