package com.hzmt.IDCardFdvUsb.CameraUtil;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import com.hzmt.IDCardFdvUsb.MyApplication;
import com.hzmt.IDCardFdvUsb.util.B64Util;
import com.hzmt.IDCardFdvUsb.util.IdcardFdv;
import com.hzmt.IDCardFdvUsb.util.IdcardFdvComplete;
import com.hzmt.IDCardFdvUsb.util.ShowToastUtils;

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

                if (CameraActivityData.idcardfdv_idcardState == IDCardReadThread.IDCARD_ERR_READERR &&
                        !CameraActivityData.idcardfdv_NoIDCardMode &&
                        !CameraActivityData.idcardfdv_RequestMode) {
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
                long fdvtime = System.currentTimeMillis() - MyApplication.idcardfdvTotalCnt;
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
        // 仅返回验证开始阶段(等待读卡),没有退出本线程。
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
        if(0 == MyApplication.idcardfdv_requestType || CameraActivityData.idcardfdv_NoIDCardMode) {
            if(CameraActivityData.idcardfdv_NoIDCardMode)
                idcard_photo = "None";
            else if(CameraActivityData.idcardfdv_RequestMode){
                idcard_photo = CameraActivityData.FdvIDCardInfos.idcard_photo;
            }
            else {
                // 使用在IDCardReadThread生成的base64
                idcard_photo = CameraActivityData.FdvIDCardInfos.idcard_photo;
            }
            //long b64time = System.currentTimeMillis();
            verify_photo = CameraActivityData.CameraFaceB64;//"data:image/jpeg;base64," + Base64.encodeToString(CameraActivityData.CameraImageData, Base64.NO_WRAP);
            //b64time = System.currentTimeMillis() - b64time;
            //activity.mDebugLayout.addText("b64time:"+b64time+"\n");
        }
        else if(1 == MyApplication.idcardfdv_requestType) {
            idcard_photo = CameraActivityData.PhotoImageFeat;
            verify_photo = CameraActivityData.CameraImageFeat;

            //CameraActivityData.FdvIDCardInfos.idcard_photo = "data:image/png;base64," +
            //        B64Util.bitmapToBase64(CameraActivityData.PhotoImage,Bitmap.CompressFormat.PNG);
        }

        final CameraActivity cbctx = activity;
        IdcardFdv.RequestCallBack reqcb = new IdcardFdv.RequestCallBack() {
            @Override
            public void onSuccess(JSONObject object) {
                Log.i("IdcardFdv cb", object.toString());
                boolean saveUpload = false;
                double sim = 0.0f;
                String serial_no = "";
                try {
                    if (object.getInt("Err_no") == 0){
                        serial_no = object.getString("Serial_No");

                        // 无证模式获取身份证信息
                        if(CameraActivityData.idcardfdv_NoIDCardMode){
                            if(object.has("name"))
                                CameraActivityData.FdvIDCardInfos.name
                                        = B64Util.base64ToString(object.getString("name"));
                            if(object.has("issuing_authority"))
                                CameraActivityData.FdvIDCardInfos.issuing_authority
                                        = B64Util.base64ToString(object.getString("issuing_authority"));
                            if(object.has("birthdate"))
                                CameraActivityData.FdvIDCardInfos.birthdate
                                        = object.getString("birthdate");
                            if(object.has("sex"))
                                CameraActivityData.FdvIDCardInfos.sex
                                        = B64Util.base64ToString(object.getString("sex"));
                            if(object.has("idcard_issuedate"))
                                CameraActivityData.FdvIDCardInfos.idcard_issuedate
                                        = object.getString("idcard_issuedate");
                            if(object.has("idcard_expiredate"))
                                CameraActivityData.FdvIDCardInfos.idcard_expiredate
                                        = B64Util.base64ToString(object.getString("idcard_expiredate"));
                            //if(object.has("idcard_id"))
                            //    CameraActivityData.FdvIDCardInfos.idcard_id = object.getString("idcard_id");
                            if(object.has("ethnicgroup"))
                                CameraActivityData.FdvIDCardInfos.ethnicgroup
                                        = B64Util.base64ToString(object.getString("ethnicgroup"));
                            if(object.has("address"))
                                CameraActivityData.FdvIDCardInfos.address
                                        = B64Util.base64ToString(object.getString("address"));
                            if(object.has("idcard_photo")){
                                // 身份证照片
                                CameraActivityData.FdvIDCardInfos.idcard_photo = object.getString("idcard_photo");
                                String b64str = CameraActivityData.FdvIDCardInfos.idcard_photo;
                                int sidx = b64str.indexOf("base64,") + 7;
                                b64str = b64str.substring(sidx);
                                CameraActivityData.PhotoImage = B64Util.base64ToBitmap(b64str);
                                if(CameraActivityData.PhotoImage != null){
                                    cbctx.mInfoLayout.setIdcardPhoto(CameraActivityData.PhotoImage);
                                    cbctx.mInfoLayout.setMode(false);
                                }
                            }
                        }

                        sim = object.getDouble("Similarity");
                        String retstr = String.format("%.1f%%",sim * 100);
                        cbctx.mInfoLayout.setResultSimilarity(retstr);

                        boolean simPass = sim > CameraActivityData.SimThreshold;
                        if(simPass) {
                            cbctx.mInfoLayout.setResultIconPass();
                            CameraActivityData.idcardfdv_result = CameraActivityData.RESULT_PASS;

                            // 播放提示音
                            new Thread(){
                                @Override
                                public void run() {
                                    cbctx.mATRight.stop();
                                    cbctx.mATRight.reloadStaticData();
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
                            CameraActivityData.idcardfdv_result = CameraActivityData.RESULT_NOT_PASS;
                            // 播放提示音
                            new Thread(){
                                @Override
                                public void run() {
                                    cbctx.mATWrong.stop();
                                    cbctx.mATWrong.reloadStaticData();
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

                        // feat fdv模式补发人脸图
                        if(1 == MyApplication.idcardfdv_requestType && !CameraActivityData.idcardfdv_NoIDCardMode)
                            sendFdvCompleteImages(serial_no);

                        if(cbctx.mFdvSrv != null){
                            if(CameraActivityData.idcardfdv_RequestMode) {
                                // 请求模式设置结果
                                int _result = simPass ? CameraActivityData.RESULT_PASS : CameraActivityData.RESULT_NOT_PASS;
                                cbctx.mFdvSrv.setRequestResult(CameraActivityData.CameraFaceB64,
                                        CameraActivityData.FdvIDCardInfos.idcard_photo,
                                        _result, null,sim * 100);
                            }
                            // 设置最后识别信息
                            cbctx.mFdvSrv.setIDCardInfos(CameraActivityData.FdvIDCardInfos);
                            cbctx.mFdvSrv.setLastResult(CameraActivityData.CameraImage,
                                                        CameraActivityData.PhotoImage,
                                                        sim * 100,
                                                        simPass);
                        }
                        saveUpload = true;
                    }
                    else{
                        CameraActivityData.idcardfdv_result = CameraActivityData.RESULT_FAILED;
                        if(CameraActivityData.idcardfdv_RequestMode && cbctx.mFdvSrv != null)
                            cbctx.mFdvSrv.setRequestResult("","",
                                    CameraActivityData.RESULT_FAILED,null,0.0);

                        String err_msg = object.getString("Err_msg");
                        ShowToastUtils.showToast(cbctx, err_msg, Toast.LENGTH_SHORT);
                    }
                } catch (JSONException e) {
                    CameraActivityData.idcardfdv_result = CameraActivityData.RESULT_FAILED;
                    if(CameraActivityData.idcardfdv_RequestMode && cbctx.mFdvSrv != null)
                        cbctx.mFdvSrv.setRequestResult("","",
                                CameraActivityData.RESULT_FAILED, null,0.0);
                    e.printStackTrace();
                }

                String savePrename = "";
                if(saveUpload) {
                    // 因身份证号码缓存清理，需提前确定保存文件名
                    int simInt = (int) (sim * 1000);
                    savePrename = String.format("%s_%s_%03d",
                            CameraActivityData.FdvIDCardInfos.idcard_id,
                            serial_no,
                            simInt);
                }

                // 清理身份证号码缓存以避免下次直接使用身份证产生问题
                boolean clean_flag = (CameraActivityData.idcardfdv_NoIDCardMode ||
                                      CameraActivityData.idcardfdv_RequestMode
                                     );
                if(clean_flag){
                    CameraActivityData.FdvIDCardInfos.idcard_id = "";
                }

                //===================
                // test code
                //cbctx.mDebugLayout.addText("FDV-package Time:"+MyApplication.idcardfdvStepCnt2+"\n");
                cbctx.mDebugLayout.addText("network Time:"+(System.currentTimeMillis() - MyApplication.idcardfdvStepCnt)+"\n");
                long fdvtime = System.currentTimeMillis() - MyApplication.idcardfdvTotalCnt;
                cbctx.mDebugLayout.addText("FDV-TotalTime:"+fdvtime+"\n");
                //===================

                WorkUtils.startBrightnessWork(cbctx);
                delayResumeFdvWork(cbctx,3*1000);

                if(saveUpload){
                    if(!MyApplication.DebugNoIDCardReader) {
                        // 身份证照片
                        if(!CameraActivityData.idcardfdv_NoIDCardMode)
                            WorkUtils.saveUploadBitmap(cbctx, CameraActivityData.PhotoImage,
                                    savePrename + "_0.png", Bitmap.CompressFormat.PNG);

                        // 主摄像头照片
                        WorkUtils.saveUploadBitmap(cbctx,CameraActivityData.UploadCameraImage,
                                            savePrename + "_1.jpeg", Bitmap.CompressFormat.JPEG);
                        // 红外照片
                        if(MyApplication.idcardfdv_subCameraEnable) {
                            WorkUtils.saveUploadBitmap(cbctx, CameraActivityData.UploadCameraImageSub,
                                    savePrename + "_2.jpeg",Bitmap.CompressFormat.JPEG);
                        }

                        CameraActivityData.UploadCameraImage = null;
                        CameraActivityData.UploadCameraImageSub = null;
                    }
                }
            }

            @Override
            public void onFailure(int errno) {
                cbctx.mDebugLayout.addText("network failed!\n");
                CameraActivityData.idcardfdv_result = CameraActivityData.RESULT_FAILED;

                if( 3 == errno){
                    // fdv failed
                }
                else{  } // others

                // 清理身份证号码缓存以避免下次直接使用身份证产生问题
                boolean clean_flag = (CameraActivityData.idcardfdv_NoIDCardMode ||
                        CameraActivityData.idcardfdv_RequestMode
                );
                if(clean_flag){
                    CameraActivityData.FdvIDCardInfos.idcard_id = "";
                }

                ShowToastUtils.showToast(cbctx, "网络请求错误！", Toast.LENGTH_SHORT);
                WorkUtils.startBrightnessWork(cbctx);
                delayResumeFdvWork(cbctx,3*1000);
            }
        };

        // 加载证书
        InputStream certstream = new ByteArrayInputStream(MyApplication.certstream_baos.toByteArray());
        int reqType = MyApplication.idcardfdv_requestType;
        String reqUrl = MyApplication.idcardfdvUrl;
        if(CameraActivityData.idcardfdv_NoIDCardMode) {
            reqType = 0;
            reqUrl = MyApplication.idcardfdvUrl_img;
        }
        IdcardFdv.request(activity,
                reqType,
                reqUrl,
                CameraActivityData.FdvIDCardInfos,
                idcard_photo,
                verify_photo,
                certstream,
                reqcb);
    }

    private void sendFdvCompleteImages(final String serial_no){

        new Thread(){
            @Override
            public void run(){
                // feat fdv时可能未生成base64
                boolean cflag = CameraActivityData.CameraImage != null &&
                        (CameraActivityData.CameraFaceB64 == null ||
                                CameraActivityData.CameraFaceB64.equals(""));
                if(cflag){
                    //long stime = System.currentTimeMillis();
                    synchronized(CameraActivityData.AiFdrScLock) {
                        CameraActivityData.CameraFaceB64 = MyApplication.AiFdrScIns.
                                ai_fdr_get_face(CameraActivityData.CameraFaceRect, 95, true);
                    }
                    //long dttime = System.currentTimeMillis() - stime;
                    //Log.i("complete b64",""+dttime);
                }

                String idcard_photo = null;
                String verify_photo = null;

                idcard_photo = CameraActivityData.FdvIDCardInfos.idcard_photo;
                verify_photo = CameraActivityData.CameraFaceB64;

                InputStream certstream = new ByteArrayInputStream(MyApplication.certstream_baos.toByteArray());
                String reqUrl = MyApplication.fdvCompleteUrl;
                IdcardFdvComplete.request(reqUrl,
                        serial_no,
                        CameraActivityData.FdvIDCardInfos,
                        idcard_photo,
                        verify_photo,
                        certstream,
                        null);
            }
        }.start();
    }
}
