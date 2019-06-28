package com.hzmt.IDCardFdvUsb.CameraUtil;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.Message;
import android.util.Log;

import com.hzmt.IDCardFdvUsb.MyApplication;
import com.hzmt.IDCardFdvUsb.R;
import com.hzmt.IDCardFdvUsb.util.B64Util;

import java.lang.ref.WeakReference;

public class IDCardReadThread extends Thread {
    public static final int IDCARD_ERR_DEVERR = -1;
    public static final int IDCARD_ERR_READERR = -2;
    public static final int IDCARD_REOPEN_READER = -3;
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
            // 开始计时
            MyApplication.idcardfdvTotalCnt = System.currentTimeMillis();
            boolean no_read_flag = (MyApplication.DebugNoIDCardReader ||
                                    CameraActivityData.idcardfdv_NoIDCardMode ||
                                    CameraActivityData.idcardfdv_RequestMode
                                    );

            if(no_read_flag){
                Message msg = new Message();
                msg.what = IDCARD_CHECK_OK;
                mHandler.sendMessage(msg);
                break;
            }
            else {
                int iRet = activity.mIDCardReader.Authenticate_IDCard();

                if (0 == iRet) {
                    Message msg = new Message();
                    msg.what = IDCARD_CHECK_OK;
                    mHandler.sendMessage(msg);

                    iRet = activity.mIDCardReader.Read_Content();
                    if (0 == iRet) {
                        String tmpId = activity.mIDCardReader.GetPeopleIDCode();
                        if (tmpId.equals(CameraActivityData.FdvIDCardInfos.idcard_id) &&
                                CameraActivityData.PhotoImage != null) {
                            // 相同身份证
                            bIDCardNoChange = true;
                        } else {
                            CameraActivityData.FdvIDCardInfos.clean();
                            CameraActivityData.PhotoImageData = null;
                            CameraActivityData.PhotoImage = null;
                            // bmp数据
                            CameraActivityData.PhotoImageData = activity.mIDCardReader.GetPhotoDate();
                            // IDCard infos
                            CameraActivityData.FdvIDCardInfos.name
                                    = activity.mIDCardReader.GetPeopleName();
                            CameraActivityData.FdvIDCardInfos.issuing_authority
                                    = activity.mIDCardReader.GetIssuingAuthority();
                            CameraActivityData.FdvIDCardInfos.birthdate
                                    = activity.mIDCardReader.GetPeopleBirthdate();
                            CameraActivityData.FdvIDCardInfos.sex
                                    = activity.mIDCardReader.GetPeopleSex();
                            CameraActivityData.FdvIDCardInfos.idcard_issuedate
                                    = activity.mIDCardReader.GetIssueDate();
                            CameraActivityData.FdvIDCardInfos.idcard_expiredate
                                    = activity.mIDCardReader.GetExpireDate();
                            CameraActivityData.FdvIDCardInfos.idcard_id
                                    = activity.mIDCardReader.GetPeopleIDCode();
                            CameraActivityData.FdvIDCardInfos.ethnicgroup
                                    = activity.mIDCardReader.GetPeopleNation();
                            CameraActivityData.FdvIDCardInfos.address
                                    = activity.mIDCardReader.GetAddress();
                        }


                        String dbgstr;
                        activity.mDebugLayout.addText("read card: true\n");
                        dbgstr = "idcardno:" + CameraActivityData.FdvIDCardInfos.idcard_id;
                        activity.mDebugLayout.addText(dbgstr + "\n");
                        //Log.i("IDCardReader","" +(System.currentTimeMillis()-MyApplication.idcardfdvTotalCnt));

                        break;
                    } else {
                        activity.mDebugLayout.addText("Read_Content: false," + iRet + "\n");
                        CameraActivityData.FdvIDCardInfos.clean();
                        CameraActivityData.PhotoImageData = null;
                        break;
                    }
                } else if(-1 == iRet){
                    // 错误，包括无阅读器和其他读取错误
                }
                else{
                    // invs300: 无卡或卡已读
                    //activity.mDebugLayout.addText("Authenticate: false," + iRet + "\n");
                }
            }

            try {
                int interval = activity.mIDCardReader.GetAuthInterval();
                Thread.sleep(interval);
                if(CameraActivityData.resume_work)
                    return;
            } catch (InterruptedException e){
                e.printStackTrace();
            }
        }

        if(CameraActivityData.idcardfdv_NoIDCardMode && !CameraActivityData.resume_work){
            // 无证模式直接返回
            CameraActivityData.FdvIDCardInfos.clean();
            CameraActivityData.PhotoImageData = null;
            CameraActivityData.PhotoImage = null;
            CameraActivityData.PhotoImageFeat = null;
            Message msg = new Message();
            msg.what = IDCARD_ALL_OK;
            mHandler.sendMessage(msg);
            return;
        }

        // 身份证照片处理
        boolean data_error = false;
        if(!bIDCardNoChange) {
            if(MyApplication.DebugNoIDCardReader){
                //======================== test
                CameraActivityData.FdvIDCardInfos.idcard_id = "332526198407210014";
                CameraActivityData.FdvIDCardInfos.idcard_issuedate = "20131212";
                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inScaled = false;
                Bitmap bm = BitmapFactory.decodeResource(activity.getResources(), R.drawable.zp,opts);
                CameraActivityData.PhotoImage = bm;
                //========================
            }
            else {
                if(CameraActivityData.idcardfdv_RequestMode){
                    CameraActivityData.PhotoImage = null;
                    String b64str = CameraActivityData.FdvIDCardInfos.idcard_photo;
                    b64str = b64str.substring(b64str.indexOf("base64,") + 7);
                    CameraActivityData.PhotoImage = B64Util.base64ToBitmap(b64str);
                }
                else if(activity.mIDCardReader.GetReaderType() == IDCardReader.READER_MKR){
                    CameraActivityData.PhotoImage = activity.mIDCardReader.GetPhotoBitmap();
                }
                else if(activity.mIDCardReader.GetReaderType() == IDCardReader.READER_INVS) {
                    CameraActivityData.PhotoImage = null;
                    byte[] idcard_photo_Data = CameraActivityData.PhotoImageData;
                    if (idcard_photo_Data != null)
                        CameraActivityData.PhotoImage = BitmapFactory
                                .decodeByteArray(idcard_photo_Data, 0, idcard_photo_Data.length);
                }
            }

            if(!CameraActivityData.resume_work) {   // 如已超时，不发送更新
                Message msg = new Message();
                msg.what = IDCARD_IMG_OK;
                mHandler.sendMessage(msg);
            }

            // base64
            if(CameraActivityData.PhotoImage == null)
                data_error = true;
            else if(!CameraActivityData.idcardfdv_RequestMode){
                // 非请求模式时转身份证照片base64
                String photob64 = "data:image/png;base64," + B64Util.bitmapToBase64(CameraActivityData.PhotoImage, Bitmap.CompressFormat.PNG);
                if (CameraActivityData.FdvIDCardInfos.idcard_photo.equals(photob64))
                    data_error = true;
                CameraActivityData.FdvIDCardInfos.idcard_photo = photob64;
            }


            // feat
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
            if(!CameraActivityData.resume_work) {   // 如已超时，不发送更新
                Message msg = new Message();
                msg.what = IDCARD_IMG_OK;
                mHandler.sendMessage(msg);
            }
        }

        // 错误检查
        //boolean chkflag = (data_error ||
        //        (0 == MyApplication.idcardfdv_requestType && CameraActivityData.PhotoImage == null) ||
        //        (1 == MyApplication.idcardfdv_requestType && CameraActivityData.PhotoImageFeat.equals(""))
        //        );
        boolean chkflag = (data_error ||
                CameraActivityData.PhotoImage == null ||
               (1 == MyApplication.idcardfdv_requestType && CameraActivityData.PhotoImageFeat.equals(""))
                );

        if(chkflag){
            CameraActivityData.PhotoImageData = null;
            CameraActivityData.PhotoImage = null;
            CameraActivityData.PhotoImageFeat = "";
            CameraActivityData.FdvIDCardInfos.clean();

            if(!CameraActivityData.resume_work) {   // 如已超时，不发送更新
                Message msg = new Message();
                msg.what = IDCARD_ERR_READERR;
                mHandler.sendMessage(msg);
            }
        }
        else{
            if(!CameraActivityData.resume_work) {   // 如已超时，不发送更新
                Message msg = new Message();
                msg.what = IDCARD_ALL_OK;
                mHandler.sendMessage(msg);
            }
        }
    }
}
