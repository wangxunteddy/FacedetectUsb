package com.hzmt.IDCardFdvUsb.CameraUtil;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.Message;

import com.hzmt.IDCardFdvUsb.MyApplication;
import com.hzmt.IDCardFdvUsb.R;

import java.lang.ref.WeakReference;

public class IDCardReadThread extends Thread {
    public static final int IDCARD_ERR_DEVERR = -1;
    public static final int IDCARD_ERR_READERR = -2;
    public static final int IDCARD_STATE_NONE = 0;
    public static final int IDCARD_READY = 1;
    public static final int IDCARD_CHECK_OK = 2;
    public static final int IDCARD_IMG_OK = 3;
    public static final int IDCARD_ALL_OK = 10;

    private final WeakReference<CameraActivity> mActivity;
    private IDCardReadHandler mHandler;

    public IDCardReadThread(CameraActivity activity, IDCardReadHandler handler){
        mActivity = new WeakReference<>(activity);
        this.mHandler = handler;
    }

    @Override
     public void run(){
        //CameraActivityData.Idcard_id = "";
        //CameraActivityData.Idcard_issuedate = "";
        //CameraActivityData.PhotoImageData = null;
        //CameraActivityData.PhotoImage = null;
        //CameraActivityData.PhotoImageFeat = "";

        CameraActivity activity = mActivity.get();
        if(activity == null) {
            Message msg = new Message();
            msg.what = IDCARD_ERR_READERR;
            mHandler.sendMessage(msg);
            return;
        }

        if(!MyApplication.DebugNoIDCardReader) {
            int readerState = activity.mIDCardReader.GetInitState();
            while (IDCardReader.STATE_REQUUST_PERMISSION == readerState) {
                // 读卡器权限确认中，等待确认结果
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                readerState = activity.mIDCardReader.GetInitState();
            }

            if (readerState != IDCardReader.STATE_INIT_OK) {
                Message msg = new Message();
                msg.what = IDCARD_ERR_READERR;
                mHandler.sendMessage(msg);
                return;
            }
        }


        {
            // 循环读卡准备就绪后的处理
            Message msg = new Message();
            msg.what = IDCARD_READY;
            mHandler.sendMessage(msg);
        }

        boolean bIDCardNoChange = false;
        while(true){
            if(MyApplication.DebugNoIDCardReader) {
                Message msg = new Message();
                msg.what = IDCARD_CHECK_OK;
                mHandler.sendMessage(msg);
                break;
            }

            if(CameraActivityData.idcardfdv_NoIDCardMode){
                // 开始计时
                MyApplication.idcardfdvTotalCnt = System.currentTimeMillis();
                Message msg = new Message();
                msg.what = IDCARD_CHECK_OK;
                mHandler.sendMessage(msg);
                break;
            }
            else {
                int iRet = activity.mIDCardReader.Authenticate_IDCard();

                if (0 == iRet) {
                    // 开始计时
                    MyApplication.idcardfdvTotalCnt = System.currentTimeMillis();

                    Message msg = new Message();
                    msg.what = IDCARD_CHECK_OK;
                    mHandler.sendMessage(msg);

                    iRet = activity.mIDCardReader.Read_Content();
                    if (0 == iRet) {
                        String tmpId = activity.mIDCardReader.GetPeopleIDCode();
                        if (tmpId.equals(CameraActivityData.Idcard_id) &&
                                CameraActivityData.PhotoImageData != null) {
                            // 相同身份证
                            bIDCardNoChange = true;
                        } else {
                            CameraActivityData.Idcard_id = activity.mIDCardReader.GetPeopleIDCode();
                            CameraActivityData.Idcard_issuedate = activity.mIDCardReader.GetIssueDate();
                            CameraActivityData.PhotoImageData = activity.mIDCardReader.GetPhotoDate();
                        }


                        String dbgstr;
                        activity.mDebugLayout.addText("read card: true\n");
                        dbgstr = "idcardno:" + CameraActivityData.Idcard_id;
                        activity.mDebugLayout.addText(dbgstr + "\n");

                        break;
                    } else {
                        activity.mDebugLayout.addText("Read_Content: false," + iRet + "\n");
                        CameraActivityData.Idcard_id = "";
                        CameraActivityData.Idcard_issuedate = "";
                        CameraActivityData.PhotoImageData = null;
                        break;
                    }
                } else {
                    //activity.mDebugLayout.addText("Authenticate: false," + iRet + "\n");
                }
            }

            try {
                Thread.sleep(300);
                if(CameraActivityData.resume_work)
                    return;
            } catch (InterruptedException e){
                e.printStackTrace();
            }
        }

        if(CameraActivityData.resume_work)
            return;

        if(CameraActivityData.idcardfdv_NoIDCardMode){
            // 无证模式直接返回
            CameraActivityData.Idcard_issuedate = "";
            CameraActivityData.PhotoImageData = null;
            CameraActivityData.PhotoImage = null;
            CameraActivityData.PhotoImageFeat = null;
            Message msg = new Message();
            msg.what = IDCARD_ALL_OK;
            mHandler.sendMessage(msg);
            return;
        }

        // 身份证照片处理
        if(!bIDCardNoChange) {
            if(MyApplication.DebugNoIDCardReader){
                //======================== test
                CameraActivityData.Idcard_id = "332526198407210014";
                CameraActivityData.Idcard_issuedate = "20131212";
                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inScaled = false;
                Bitmap bm = BitmapFactory.decodeResource(activity.getResources(), R.drawable.zp,opts);
                CameraActivityData.PhotoImage = bm;
                //========================
            }
            else {
                byte[] idcard_photo_Data = CameraActivityData.PhotoImageData;
                CameraActivityData.PhotoImage = null;
                if(idcard_photo_Data!=null)
                    CameraActivityData.PhotoImage = BitmapFactory.decodeByteArray(idcard_photo_Data, 0, idcard_photo_Data.length);
            }
            Message msg = new Message();
            msg.what = IDCARD_IMG_OK;
            mHandler.sendMessage(msg);

            CameraActivityData.PhotoImageFeat = "";
            if(1 == MyApplication.idcardfdv_requestType) {
                long stime = System.currentTimeMillis();
                Rect faceRect = new Rect();
                synchronized (CameraActivityData.AiFdrSclock) {
                    CameraActivityData.PhotoImageFeat = MyApplication.AiFdrScIns.get_photo_feat(CameraActivityData.PhotoImage, faceRect);

                    //MyApplication.AiFdrScIns.dectect_photo_face(CameraActivityData.PhotoImage, faceRect);
                    //CameraActivityData.PhotoImageFeat = MyApplication.AiFdrScIns.get_photo_feat2(faceRect);
                }
                long feattime = System.currentTimeMillis() - stime;
                activity.mDebugLayout.addText("PhotoFeatTime:" + feattime + "\n");
            }
        }
        else{
            Message msg = new Message();
            msg.what = IDCARD_IMG_OK;
            mHandler.sendMessage(msg);
        }

        if((0 == MyApplication.idcardfdv_requestType &&
                CameraActivityData.PhotoImage == null)
        ||(1 == MyApplication.idcardfdv_requestType &&
                CameraActivityData.PhotoImageFeat.equals(""))
            ){
            CameraActivityData.Idcard_id = "";
            CameraActivityData.Idcard_issuedate = "";
            CameraActivityData.PhotoImageData = null;
            CameraActivityData.PhotoImage = null;
            //MyApplication.PhotoImageFeat = "";

            Message msg = new Message();
            msg.what = IDCARD_ERR_READERR;
            mHandler.sendMessage(msg);
        }
        else{
            Message msg = new Message();
            msg.what = IDCARD_ALL_OK;
            mHandler.sendMessage(msg);
        }
    }
}
