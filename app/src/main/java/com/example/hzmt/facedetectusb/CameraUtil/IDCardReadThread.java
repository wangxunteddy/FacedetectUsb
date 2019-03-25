package com.example.hzmt.facedetectusb.CameraUtil;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.example.hzmt.facedetectusb.MyApplication;
import com.example.hzmt.facedetectusb.R;
import com.invs.UsbBase;
import com.invs.UsbSam;
import com.invs.invsIdCard;
import com.invs.invsUtil;
import com.invs.invswlt;

import java.lang.ref.WeakReference;

public class IDCardReadThread extends Thread {
    public static final int IDCARD_ERR_DEVERR = -1;
    public static final int IDCARD_ERR_READERR = -2;
    public static final int IDCARD_STATE_NONE = 0;
    public static final int IDCARD_READ_OK = 1;
    public static final int IDCARD_ALL_OK = 2;
    private final WeakReference<CameraActivity> mActivity;
    private IDCardReadHandler mHandler;

    public IDCardReadThread(CameraActivity activity, IDCardReadHandler handler){
        mActivity = new WeakReference<>(activity);
        this.mHandler = handler;
    }

    @Override
    public void run() {
        //CameraActivityData.Idcard_id = "";
        //CameraActivityData.Idcard_issuedate = "";
        //CameraActivityData.PhotoImageData = null;
        //CameraActivityData.PhotoImage = null;
        //CameraActivityData.PhotoImageFeat = "";

        CameraActivity activity = mActivity.get();

        int readerState = activity.mIDCardReader.GetInitState();
        while(IDCardReader.STATE_REQUUST_PERMISSION == readerState){
            // 读卡器权限确认中，等待确认结果
            try {
                Thread.sleep(100);
            } catch (InterruptedException e){
                e.printStackTrace();
            }

            readerState = activity.mIDCardReader.GetInitState();
        }

        if(readerState != IDCardReader.STATE_INIT_OK) {
            Message msg = new Message();
            msg.what = IDCARD_ERR_READERR;
            mHandler.sendMessage(msg);
            return;
        }

        boolean bIDCardNoChange = false;
        int timeout = 15 * 1000;
        while(true){
            int iRet = activity.mIDCardReader.Authenticate_IDCard();

            if(0 == iRet) {
                iRet = activity.mIDCardReader.Read_Content();
                if(0 == iRet){
                    String tmpId = activity.mIDCardReader.GetPeopleIDCode();
                    if(tmpId.equals(CameraActivityData.Idcard_id)){
                        // 相同身份证
                        bIDCardNoChange = true;
                    }
                    else {
                        CameraActivityData.Idcard_id = activity.mIDCardReader.GetPeopleIDCode();
                        CameraActivityData.Idcard_issuedate = activity.mIDCardReader.GetIssueDate();
                        CameraActivityData.PhotoImageData = activity.mIDCardReader.GetPhotoDate();
                    }

                    Message msg = new Message();
                    msg.what = IDCARD_READ_OK;
                    mHandler.sendMessage(msg);

                    String dbgstr;
                    activity.mDebugLayout.addText("read card: true\n");
                    dbgstr = "idcardno:"+ CameraActivityData.Idcard_id;
                    activity.mDebugLayout.addText(dbgstr+"\n");

                    break;
                }
                else
                    activity.mDebugLayout.addText("Read_Content: false,"+iRet+"\n");
            }
            else
                activity.mDebugLayout.addText("Authenticate: false,"+iRet+"\n");

            try {
                Thread.sleep(300);
                timeout -= 300;
                if(timeout <=0) {
                    Message msg = new Message();
                    msg.what = IDCARD_ERR_READERR;
                    mHandler.sendMessage(msg);
                    return;
                }
            } catch (InterruptedException e){
                e.printStackTrace();
            }
        }

        // 身份证照片处理
        if(!bIDCardNoChange) {
            byte[] idcard_photo_Data = CameraActivityData.PhotoImageData;
            //======================== test
            //CameraActivityData.Idcard_id = "332526198407210014";
            //CameraActivityData.Idcard_issuedate = "20131212-20241212";
            //BitmapFactory.Options opts = new BitmapFactory.Options();
            //opts.inScaled = false;
            //Bitmap bm = BitmapFactory.decodeResource(activity.getResources(), R.drawable.zp,opts);
            //CameraActivityData.PhotoImage = bm;
            //========================
            CameraActivityData.PhotoImage = BitmapFactory.decodeByteArray(idcard_photo_Data, 0, idcard_photo_Data.length);
            Rect faceRect = new Rect();
            synchronized (CameraActivityData.AiFdrSclock) {
                CameraActivityData.PhotoImageFeat = MyApplication.AiFdrScIns.get_photo_feat(CameraActivityData.PhotoImage, faceRect);
            }
        }

        if(CameraActivityData.PhotoImageFeat.equals("")){
            CameraActivityData.Idcard_id = "";
            CameraActivityData.Idcard_issuedate = "";
            CameraActivityData.PhotoImageData = null;
            CameraActivityData.PhotoImage = null;
            //MyApplication.PhotoImageFeat = "";

            Message msg = new Message();
            msg.what = IDCARD_ERR_READERR;
            mHandler.sendMessage(msg);
        }
        else {
            Message msg = new Message();
            msg.what = IDCARD_ALL_OK;
            mHandler.sendMessage(msg);
        }
    }
}
