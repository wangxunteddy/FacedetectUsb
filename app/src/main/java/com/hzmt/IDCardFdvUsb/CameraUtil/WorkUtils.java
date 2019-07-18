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

import com.hzmt.IDCardFdvUsb.MyApplication;
import com.hzmt.IDCardFdvUsb.util.IdcardFdvRegister;
import com.hzmt.IDCardFdvUsb.util.SystemUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class WorkUtils {
    public static boolean reportIPChange(Context cotntext){
        String newIp = SystemUtil.getIpAddress(cotntext);
        if(newIp == null){
            // 无网络连接
            return false;
        }

        //if(!newIp.equals(MyApplication.myIPAddress)){
        {
            String sn = IdcardFdvRegister.getProductSn();
            String sendstr = "ProductSn:";
            if(sn == null) {
                //sendstr += "null;";
                return false;
            }
            else {
                sn = sn.replace("-", ""); // 去'-'
                sendstr += (sn + ";");
            }
            sendstr+=("IPAddress:"+newIp);

            if(SystemUtil.sendUDPBrocast(sendstr.getBytes(), 55530)){
                MyApplication.myIPAddress = newIp;
                return true;
            }
        }

        return false;
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


    public static void setNetworkChangeWork(final Context context){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityManager != null) {
                connectivityManager.requestNetwork(new NetworkRequest.Builder().build(), new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onAvailable(Network network) {
                        super.onAvailable(network);
                        WorkUtils.reportIPChange(context);
                    }

                    @Override
                    public void onLost(Network network) {
                        super.onLost(network);
                        WorkUtils.reportIPChange(context);
                    }

                    @Override
                    public void onLinkPropertiesChanged(Network network, LinkProperties linkProperties) {
                        super.onLinkPropertiesChanged(network, linkProperties);
                        WorkUtils.reportIPChange(context);
                    }

                    @Override
                    public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
                        super.onCapabilitiesChanged(network, networkCapabilities);
                        WorkUtils.reportIPChange(context);
                    }
                });
            }
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
