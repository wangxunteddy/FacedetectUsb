package com.example.hzmt.facedetectusb;

import android.app.Application;
import android.os.Handler;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
//import com.invs.BtReaderClient;

import java.io.ByteArrayOutputStream;

/**
 * Created by xun on 2017/8/30.
 */

public class MyApplication extends Application {
    public static RequestQueue requestQueue;
    public static String FaceDetectUrl = "http://192.168.1.12:8070/AppFaceDetect";

    public static byte[] PhotoImageData = null;
    public static boolean idcardfdv_working = false;
    public static boolean idcardfdv_idcarderror = false;
    public static String idcardfdvUrl = "http://192.168.1.201:8004/idcardfdv";
    //public static String idcardfdvUrl = "https://202.107.219.50:8004/idcardfdv";
    public static ByteArrayOutputStream certstream_baos = null;
    //public static BtReaderClient MyBtReaderClient = null;
    public static boolean isBtReaderClientConnected = false;
    public static Long idcardfdvCnt = null;

    public static Handler BrightnessHandler = null;
    public static Runnable BrightnessRunnable = null;

    // access control
    public static String accessControlUrl = "192.168.1.124:60000";
    public static String accessControlSn = "153134193";
    public static Long accessControlCnt = null;

    @Override
    public void onCreate() {
        super.onCreate();
        requestQueue = Volley.newRequestQueue(this);
    }
}
