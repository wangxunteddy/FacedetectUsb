package com.hzmt.IDCardFdvUsb.CameraUtil;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.widget.Toast;

import com.hzmt.IDCardFdvUsb.util.ShowToastUtils;
import com.invs.UsbSam;
import com.invs.invswlt;

import cn.mineki.CardReaders.IDCardInfo;
import cn.mineki.CardReaders.IUsbReaderCallback;
import cn.mineki.CardReaders.UsbReader;

public class IDCardReader extends UsbSam{
    private static String TAG = "IDCardReader";

    public static final int READER_INVS = 0;
    public static final int READER_MKR = 1;
    private int mReaderType = READER_INVS;

    //=====================
    // for READER_MKR
    private UsbReader mkrReader = null;
    private IDCardInfo mkr_idCardInfo = null;
    private boolean mkr_isOldModule = false;
    private boolean mkr_deAttach = false;
    //=====================

    public static final int STATE_NO_DEV = -1;
    public static final int STATE_REQUUST_PERMISSION = -2;
    public static final int STATE_REFUSE_PERMISSION = -3;
    public static final int STATE_INIT_ERR = -4;
    public static final int STATE_INIT_OK = 1;
    private int mInitState = STATE_NO_DEV;

    public int GetReaderType(){
        return mReaderType;
    }

    public IDCardReader(){
    }

    private final BroadcastReceiver mUsbPermissionActionReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice usbDevice = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        //user choose YES for your previously popup window asking for grant perssion for this usb device
                        if(null != usbDevice){
                            afterGetUsbPermission(context);
                        }
                    }
                    else {
                        //user choose NO for your previously popup window asking for grant perssion for this usb device
                        //ShowToastUtils.showToast(context, String.valueOf("Permission denied for device" + usbDevice), Toast.LENGTH_SHORT);
                        afterRefuseUsbPermission(context);
                    }
                }
            }
        }
    };


    public int OpenIDCardReader(Context context) {
        UsbDevice myUsbDevice = GetUsb(context);
        if(null == myUsbDevice) {
            mInitState = STATE_NO_DEV;
            //String errMsg = "未找到身份证阅读器!";
            //ShowToastUtils.showToast(context, errMsg, Toast.LENGTH_SHORT);
            //Log.e(TAG,"未找到身份证阅读器USB");
            return -1;
        }

        UsbManager mUsbManager = (UsbManager)context.getSystemService(Context.USB_SERVICE);

        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        context.registerReceiver(mUsbPermissionActionReceiver, filter);

        PendingIntent mPermissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0);

        if(mUsbManager.hasPermission(myUsbDevice)){
            //Log.e(TAG,myUsbDevice.getDeviceName()+"已获取过USB权限");
            afterGetUsbPermission(context);
        }else{
            mInitState = STATE_REQUUST_PERMISSION;
            //Log.e(TAG,myUsbDevice.getDeviceName()+"请求获取USB权限");
            mUsbManager.requestPermission(myUsbDevice, mPermissionIntent);
        }

        return 0;
    }

    private void afterGetUsbPermission(Context context){
        int iRet = InitIDCardReader(context);
        if (iRet != 0) {
            mInitState = STATE_INIT_ERR;
            String errMsg = "身份证阅读器初始化失败!";
            //ShowToastUtils.showToast(context, errMsg, Toast.LENGTH_SHORT);
        } else
            mInitState = STATE_INIT_OK;
    }

    private void afterRefuseUsbPermission(Context context) {
        mInitState = STATE_REFUSE_PERMISSION;
    }

    private int InitIDCardReader(Context context){
        if(mkrReader == null)
            mkrReader = UsbReader.getInstance(context);

        boolean mkrret = mkrReader.InitReader(null,new IUsbReaderCallback() {
            @Override
            public void ReaderInitSucc() {
            }
            @Override
            public void ReaderInitError() {
            }
            @Override
            public void UsbAttach() {
            }
            @Override
            public void UsbDeAttach(){
                mkr_deAttach = true;
            }
        });
        if(mkrret) {
            String[] sRet = new String[1];
            String sSamID = mkrReader.ReadSAMID(sRet);
            if(sSamID.indexOf("05.01") >= 0|| sSamID.indexOf("05.02") >= 0)
                mkr_isOldModule = true;

            mkrReader.GetAct();
            mReaderType = READER_MKR;
            mkr_deAttach = false;
            return 0;
        }

        //
        mReaderType = READER_INVS;
        int initRet = this.InitComm(context);
        if (1 != initRet)
            return -1;
        else if (!this.mSamId.substring(0, 2).equals("5-")) {
            this.CloseComm();
            return -1;
        }
        else
            return 0;
    }

    public int GetInitState(){
        return mInitState;
    }

    public boolean IsReaderConnected(){
        if (mInitState == IDCardReader.STATE_NO_DEV ||
                mInitState == IDCardReader.STATE_INIT_ERR )
            return false;

        if(mReaderType == READER_MKR){
            if(mkr_deAttach)
                return false;
        }
        else if(mReaderType == READER_INVS){
            int iRet = this.FindCardCmd();
            if (iRet != 128 && iRet != 159 )    // 找卡失败，且不是无卡和已读
                return false;
        }

        return true;
    }

    public int Authenticate_IDCard() {
        if(mReaderType == READER_MKR) {
            if(mkr_isOldModule) {
                mkr_idCardInfo = mkrReader.ReadBaseCardInfo(new String[1]);
            } else {
                mkr_idCardInfo = mkrReader.ReadAllCardInfo(new String[1]);
            }

            if(mkr_idCardInfo != null)
                return 0;
            else
                return -1;
        }
        else if(mReaderType == READER_INVS) {
            int iRet = this.FindCardCmd();
            if (128 == iRet)
                return -6;  // 找卡失败，无卡或已读
            else if (159 != iRet)
                return -1;  // 找卡失败，错误
            else {
                // 159
                iRet = this.SelCardCmd();
                if (129 == iRet)
                    return -7;  // 选卡失败，无卡或已读
                else if (144 != iRet)
                    return -1;  // 选卡失败，错误
                else
                    return 0;
            }
        }

        return -1;
    }

    // READER_INVS系列仅SelCardCmd()命令
    public int Authenticate_IDCard_SelCardCmd(){
        if(mReaderType != READER_INVS)
            return -1;

        int iRet = this.SelCardCmd();
        if (129 == iRet)
            return -7;  // 选卡失败，无卡或已读
        else if (144 != iRet)
            return -1;  // 选卡失败，错误
        else
            return 0;
    }

    public int GetAuthInterval(){
        int interval = 300; // ms
        if(mReaderType == READER_MKR){
            interval =  10;
        }
        else if(mReaderType == READER_INVS){
            interval = 300;
        }

        return interval;
    }

    public int Read_Content(){
        if(mReaderType == READER_MKR){
            return 0;
        }
        else if(mReaderType == READER_INVS) {
            try {
                if (144 != this.ReadCardCmd(false))
                    return -1;
                else
                    return 0;
            } catch (Exception e){
                e.printStackTrace();
            }
        }

        return -1;
    }

    public String GetPeopleName() {
        if(mReaderType == READER_MKR){
            if(mkr_idCardInfo != null)
                return mkr_idCardInfo.getName();
            else
                return "";
        }
        else if(mReaderType == READER_INVS)
            return this.mCard.getName();

        return "";
    }

    public String GetPeopleBirthdate() {
        if(mReaderType == READER_MKR){
            if(mkr_idCardInfo != null)
                return mkr_idCardInfo.getBirthday();
            else
                return "";
        }
        else if(mReaderType == READER_INVS)
            return this.mCard.getBirth();

        return "";
    }

    public String GetPeopleSex() {
        if(mReaderType == READER_MKR){
            if(mkr_idCardInfo != null)
                return mkr_idCardInfo.getSex();
            else
                return "";
        }
        else if(mReaderType == READER_INVS)
            return this.mCard.getSex1();

        return "";
    }

    public String GetPeopleNation() {
        if(mReaderType == READER_MKR){
            if(mkr_idCardInfo != null)
                return mkr_idCardInfo.getNation();
            else
                return "";
        }else if(mReaderType == READER_INVS)
            return this.mCard.getNation1();

        return "";
    }

    public String GetPeopleIDCode() {
        if(mReaderType == READER_MKR){
            if(mkr_idCardInfo != null)
                return mkr_idCardInfo.getCardNum();
            else
                return "";
        }
        else if(mReaderType == READER_INVS)
            return this.mCard.getIdNo();

        return "";
    }

    public String GetIssueDate() {
        if(mReaderType == READER_MKR){
            if(mkr_idCardInfo != null)
                return mkr_idCardInfo.getValidStartDate();
            else
                return "";
        }else if(mReaderType == READER_INVS)
            return this.mCard.getStart();// + "-" + this.mCard.getEnd();

        return "";
    }

    public String GetExpireDate() {
        if(mReaderType == READER_MKR){
            if(mkr_idCardInfo != null)
                return mkr_idCardInfo.getValidEndDate();
            else
                return "";
        }else if(mReaderType == READER_INVS)
            return this.mCard.getEnd();

        return "";
    }

    public String GetAddress() {
        if(mReaderType == READER_MKR){
            if(mkr_idCardInfo != null)
                return mkr_idCardInfo.getAddress();
            else
                return "";
        }else if(mReaderType == READER_INVS)
            return this.mCard.getAddress();

        return "";
    }


    public byte[] GetPhotoDate() {
        if(mReaderType == READER_MKR){
            return null;
        }
        else if(mReaderType == READER_INVS) {
            byte[] szBmp = invswlt.Wlt2Bmp(this.mCard.wlt);
            if ((szBmp != null) && (szBmp.length == 38862))
                return szBmp;
            else
                return null;
        }

        return null;
    }

    public Bitmap GetPhotoBitmap(){
        if(mReaderType == READER_MKR){
            if(mkr_idCardInfo != null)
                return mkr_idCardInfo.getPhoto();
            else
                return null;
        }
        else if(mReaderType == READER_INVS) {
            return null;
        }

        return null;
    }

    public String GetIssuingAuthority() {
        if(mReaderType == READER_MKR){
            if(mkr_idCardInfo != null)
                return mkr_idCardInfo.getRegistInstitution();
            else
                return "";
        }
        else if(mReaderType == READER_INVS)
            return this.mCard.getPolice();

        return "";
    }

    public void CloseIDCardReader(){
        if(mReaderType == READER_INVS)
            this.CloseComm();

        if(mkrReader != null) {
            mkrReader.ReleaseReader();
            mkrReader = null;
        }
    }

}
