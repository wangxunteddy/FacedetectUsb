package com.hzmt.IDCardFdvUsb;

import android.app.Application;
import android.os.Handler;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
//import com.invs.BtReaderClient;

import com.hzmt.aifdrsclib.AiFdrScPkg;

import java.io.ByteArrayOutputStream;

/**
 * Created by xun on 2017/8/30.
 */

public class MyApplication extends Application {
    public static RequestQueue requestQueue;
//    public static String FaceDetectUrl = "http://192.168.1.12:8070/AppFaceDetect";

    public static AiFdrScPkg AiFdrScIns = null;
    public static String idcardfdvUrl = "https://118.31.14.72:8004/calcsimilarity";
    //public static String idcardfdvUrl = "http://192.168.1.201:8004/calcsimilarity";
    //public static String idcardfdvUrl = "http://192.168.1.201:8004/idcardfdv";
    //public static String idcardfdvUrl = "https://118.31.14.72:8004/idcardfdv";
    public static String idcardfdvUrl_img = "https://118.31.14.72:8004/idcardfdv";
    public static String fdvCompleteUrl = "https://118.31.14.72:8004/idfdv_complete";
    public static ByteArrayOutputStream certstream_baos = null;
    public static final String config_password = "d178f4caf81f4120ba096df47cc25fed";
    public static Long idcardfdvTotalCnt = null;
    public static Long idcardfdvCameraCnt = null;
    public static Long idcardfdvStepCnt = null;
    public static Long idcardfdvStepCnt2 = null;

    public static Handler BrightnessHandler = null;
    public static Runnable BrightnessRunnable = null;

    // access control
    public static String accessControlUrl = "192.168.1.124:60000";
    public static String accessControlSn = "153134193";
    public static Long accessControlCnt = null;
    
    // debug and test
    public static byte[] TestImageData = null;
    public static int idcardfdv_requestType = 1;    // feat fdv
    public static boolean idcardfdv_subCameraEnable = false;
    public static boolean DebugNoIDCardReader = false;

    @Override
    public void onCreate() {
        super.onCreate();
        requestQueue = Volley.newRequestQueue(this);
    }
}
