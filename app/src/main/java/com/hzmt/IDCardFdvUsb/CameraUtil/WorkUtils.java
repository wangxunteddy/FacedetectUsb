package com.hzmt.IDCardFdvUsb.CameraUtil;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.hzmt.IDCardFdvUsb.MyApplication;
import com.hzmt.IDCardFdvUsb.util.AESUtils;
import com.hzmt.IDCardFdvUsb.util.ConfigUtil;
import com.hzmt.IDCardFdvUsb.util.HttpUtil;
import com.hzmt.IDCardFdvUsb.util.IdcardFdvRegister;
import com.hzmt.IDCardFdvUsb.util.ShowToastUtils;
import com.hzmt.IDCardFdvUsb.util.SystemUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class WorkUtils {
    public static boolean reportIPChange(Context context){
        synchronized (CameraActivityData.IPReportLock) {
            String newIP = SystemUtil.getIpAddress(context);
            if (newIP == null) {
                // 无网络连接
                return false;
            }

            //if(!newIp.equals(MyApplication.myIPAddress)){
            {
                String did = ConfigUtil.getValue(ConfigUtil.KEY_DEVICE_ID);
                String sendStr = "";
                if (did == null || did.equals("")) {
                    return false;
                }

                sendStr += (did + ";");

                // dummy data
                // 31: 使AES加密填充时只补1个字节，尽量减小数据长度。
                int dummyLen = 31 - (did.length() + newIP.length() + 2);
                if(dummyLen < 0)
                    dummyLen = 0;
                StringBuilder dummy = new StringBuilder();
                Random random = new Random();
                for(int i = 0; i < dummyLen; i++)
                    dummy.append(random.nextInt(10));
                dummy.append(";");
                sendStr += dummy;

                sendStr += newIP;
                //String crypt_sendStr = AESUtils.encrypt(sendStr, MyApplication.IPReport_password);
                byte[] crypt_sendByte = null;
                try {
                    crypt_sendByte = AESUtils.encrypt(sendStr.getBytes("UTF-8"), MyApplication.IPReport_password);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

                if (crypt_sendByte!=null && SystemUtil.sendUDPBrocast(crypt_sendByte, 55530)) {
                    return true;
                }
            }

            return false;
        }
    }

    public static void startIPReportThread(final Context context){
        new Thread(){
            @Override
            public void run(){
                while(true){
                    WorkUtils.reportIPChange(context);
                    try {
                        Thread.sleep(1000 * 10);
                    } catch (InterruptedException e){
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }


    public static void setNetworkChangeWork(final CameraActivity activity){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ConnectivityManager connectivityManager = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityManager != null) {
                connectivityManager.requestNetwork(new NetworkRequest.Builder().build(), new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onAvailable(Network network) {
                        super.onAvailable(network);
                        WorkUtils.reportIPChange(activity);
                        WorkUtils.HX_DeviceReg(activity);
                    }

                    @Override
                    public void onLost(Network network) {
                        super.onLost(network);
                        WorkUtils.reportIPChange(activity);
                        WorkUtils.HX_DeviceReg(activity);
                    }

                    @Override
                    public void onLinkPropertiesChanged(Network network, LinkProperties linkProperties) {
                        super.onLinkPropertiesChanged(network, linkProperties);
                        WorkUtils.reportIPChange(activity);
                        WorkUtils.HX_DeviceReg(activity);
                    }

                    @Override
                    public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
                        super.onCapabilitiesChanged(network, networkCapabilities);
                        WorkUtils.reportIPChange(activity);
                        WorkUtils.HX_DeviceReg(activity);
                    }
                });
            }
        }
    }

    public static String createDeviceID(Context context) {
        String newDID = "";
        String ipStr = SystemUtil.getIpAddress(context);
        if (ipStr == null) {
            // 无网络连接
            int rint = new Random().nextInt(744)+256;
            newDID += String.format(Locale.CHINA, "%03d", rint);
        } else{
            String[] nums = ipStr.split("\\.");
            newDID += String.format(Locale.CHINA, "%03d", Integer.valueOf(nums[nums.length - 1]));
        }

        long cTime = System.currentTimeMillis();
        newDID += String.format(Locale.CHINA, "%09d",cTime);

        newDID += String.format(Locale.CHINA, "%04d", new Random().nextInt(10000));

        return newDID;
    }

    public static void HX_DeviceReg(final CameraActivity activity){
        final String url = ConfigUtil.getValue(ConfigUtil.KEY_HX_REG_ADDRESS);
        if(url == null || url.equals(""))
            return;     // 未读取config或无需注册

        final String deviceID = ConfigUtil.getValue(ConfigUtil.KEY_HX_DEVICE_ID);
        final String stationID = ConfigUtil.getValue(ConfigUtil.KEY_HX_STATION_ID);
        if(deviceID == null || deviceID.equals("") || stationID == null || stationID.equals("")) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    String errMsg = "IP上报缺少信息！";
                    ShowToastUtils.showToast(activity, errMsg, Toast.LENGTH_SHORT);
                }
            });
            return;
        }

        final String newIP = SystemUtil.getIpAddress(activity);
        if (newIP == null) {
            return;         // 无网络连接
        }

        if(!newIP.equals(CameraActivityData.HX_IPAddress)) {
            //IP上报
            new Thread() {
                @Override
                public void run() {
                    try {
                        JSONObject object = null;
                        Map<String, String> map = new HashMap<>();
                        map.put("deviceid", deviceID);
                        map.put("deviceip", newIP);
                        map.put("stationid", stationID);
                        object = new JSONObject(map);
                        JSONObject resultJSON = HttpUtil.JsonObjectRequest(object,url);
                        if (resultJSON.has("msg")){
                            final String msg = resultJSON.getString("msg");
                            if(msg.equals("0")){
                                // 成功
                                CameraActivityData.HX_IPAddress = newIP;
                            }
                            else{
                                activity.runOnUiThread(new Runnable() {
                                    public void run() {
                                        ShowToastUtils.showToast(activity, msg, Toast.LENGTH_SHORT);
                                    }
                                });
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }.start();
            CameraActivityData.HX_runOnStart = false; // 标记至少已经执行了一次。
        }
    }

    // ===========================================================
    // screen brightness
    public static void startBrightnessWork(final Activity activity){
        startBrightnessWorkMain(activity,10);
    }
    public static void startBrightnessWorkTime(final Activity activity, int sec){
        startBrightnessWorkMain(activity,sec);
    }
    public static void startBrightnessWorkMain(final Activity activity, int sec){
        if(activity == null)
            return;

        // 取消重复功能的timer
        removeResumeWorkTimer();

        if(null == MyApplication.BrightnessHandler){
            MyApplication.BrightnessHandler = new Handler();
        }
        if(null == MyApplication.BrightnessRunnable) {
            MyApplication.BrightnessRunnable = new Runnable() {
                @Override
                public void run() {
                    WindowManager.LayoutParams params = activity.getWindow().getAttributes();
                    params.screenBrightness = 0.005f;
                    activity.getWindow().setAttributes(params);

                    // 仅startBrightnessWork与startResumeWorkTimer时设置true,退回至无人状态
                    CameraActivityData.resume_work = true;
                    //infoL.resetCameraImage();
                    //infoL.resetIdcardPhoto();
                    //infoL.setResultSimilarity("--%");
                    //infoL.resetResultIcon();
                }
            };
        }
        MyApplication.BrightnessHandler.removeCallbacks(MyApplication.BrightnessRunnable);
        WindowManager.LayoutParams params = activity.getWindow().getAttributes();
        params.screenBrightness = 0.5f;
        //       activity.getWindow().setAttributes(params);
        Window w = activity.getWindow();
        w.setAttributes(params);
        MyApplication.BrightnessHandler.postDelayed(MyApplication.BrightnessRunnable,sec*1000);
    }

    public static void keepBright(Activity activity){
        if(activity == null)
            return;

        MyApplication.BrightnessHandler.removeCallbacks(MyApplication.BrightnessRunnable);
        WindowManager.LayoutParams params = activity.getWindow().getAttributes();
        params.screenBrightness = 0.5f;
        activity.getWindow().setAttributes(params);
    }
    // ===========================================================

    public static void removeResumeWorkTimer(){
        if(CameraActivityData.ResumeWorkHandler != null)
            CameraActivityData.ResumeWorkHandler.removeCallbacks(CameraActivityData.ResumeWorkRunnable);
    }

    // 返回至无人状态(DetectFaceThread运行检查),不改变屏幕亮度
    public static void startResumeWorkTimer(int sec){
        if(null == CameraActivityData.ResumeWorkHandler){
            CameraActivityData.ResumeWorkHandler = new Handler();
        }
        if(null == CameraActivityData.ResumeWorkRunnable) {
            CameraActivityData.ResumeWorkRunnable = new Runnable() {
                @Override
                public void run() {
                    // 仅startBrightnessWork与startResumeWorkTimer时设置true,退回至无人状态
                    CameraActivityData.resume_work = true;
                }
            };
        }
        CameraActivityData.ResumeWorkHandler.removeCallbacks(CameraActivityData.ResumeWorkRunnable);
        CameraActivityData.ResumeWorkHandler.postDelayed(CameraActivityData.ResumeWorkRunnable,sec*1000);
    }


    /**
     * 保存图片
     */
    public static void saveUploadBitmap(Context ctx, Bitmap bitmap, String fullname, Bitmap.CompressFormat format) {
        if(bitmap == null)
            return;

        // 图片存放路径
        String path = ctx.getFilesDir().getAbsolutePath();
        String uploadDir = path + "/UPLOAD/";

        try {
            File dirFile = new File(uploadDir);
            if (!dirFile.exists()) {
                dirFile.mkdirs();
            }
            File file = new File(uploadDir, fullname);
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(format, 100, fos);
            fos.flush();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void saveUploadBitmapBMP(Context ctx, byte[] data, String prename) {
        // 图片存放路径
        String path = ctx.getFilesDir().getAbsolutePath();
        String uploadDir = path + "/UPLOAD/";

        try {
            File dirFile = new File(uploadDir);
            if (!dirFile.exists()) {
                dirFile.mkdirs();
            }
            File file = new File(uploadDir, prename + ".bmp");
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(data);
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
