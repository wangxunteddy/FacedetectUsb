package com.hzmt.IDCardFdvUsb.CameraUtil;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.widget.Toast;

import com.invs.UsbSam;
import com.invs.invswlt;

public class IDCardReader extends UsbSam{
    private static String TAG = "IDCardReader";
    public static final int STATE_NO_DEV = -1;
    public static final int STATE_REQUUST_PERMISSION = -2;
    public static final int STATE_REFUSE_PERMISSION = -3;
    public static final int STATE_INIT_ERR = -4;
    public static final int STATE_INIT_OK = 1;
    private int mInitState = STATE_NO_DEV;

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
                        //Toast.makeText(context, String.valueOf("Permission denied for device" + usbDevice), Toast.LENGTH_SHORT).show();
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
            String errMsg = "未找到身份证读卡器!";
            Toast.makeText(context, errMsg, Toast.LENGTH_SHORT).show();
            //Log.e(TAG,"未找到身份证读卡器USB");
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
        if(iRet != 0){
            mInitState = STATE_INIT_ERR;
            String errMsg = "未找到读卡器!";
            Toast.makeText(context, errMsg, Toast.LENGTH_SHORT).show();
        }
        else
            mInitState = STATE_INIT_OK;
    }

    private void afterRefuseUsbPermission(Context context) {
        mInitState = STATE_REFUSE_PERMISSION;
    }

    private int InitIDCardReader(Context context){
        if (1 != this.InitComm(context))
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

    public int Authenticate_IDCard() {
        int iRet = this.FindCardCmd();
        if(128 == iRet)
            return -6;  // 找卡失败，无卡或已读
        else if(159 != iRet)
            return -1;  // 找卡失败，错误
        else{
            // 159
            iRet = this.SelCardCmd();
            if(129 == iRet)
                return -7;  // 选卡失败，无卡或已读
            else if(144 != iRet)
                return -1;  // 选卡失败，错误
            else
                return 0;
        }
    }

    public int Read_Content(){
        if (144 != this.ReadCardCmd(false))
            return -1;
        else
            return 0;
    }

    public String GetPeopleIDCode() {
        return this.mCard.getIdNo();
    }

    public String GetIssueDate() {
        String str;
        str = this.mCard.getStart();// + "-" + this.mCard.getEnd();
        return str;
    }

    public byte[] GetPhotoDate() {
        byte[] szBmp = invswlt.Wlt2Bmp(this.mCard.wlt);
        if ((szBmp != null) && (szBmp.length == 38862))
            return szBmp;
        else
            return null;
    }

    public void CloseIDCardReader(){
        this.CloseComm();
    }
}
