package com.hzmt.IDCardFdvUsb;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
//import com.invs.BtReaderClient;

import com.hzmt.aifdrsclib.AiFdrScPkg;

import java.io.ByteArrayOutputStream;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;

/**
 * Created by xun on 2017/8/30.
 */

public class MyApplication extends Application {
    public static RequestQueue requestQueue;

    public static AiFdrScPkg AiFdrScIns = null;
    public static int idcardfdv_requestType = 0;    // img fdv
    //public static String idcardfdvUrl = "https://118.31.14.72:8004/calcsimilarity";
    public static String idcardfdvUrl = "https://118.31.14.72:8004/idcardfdv";
    //public static String idcardfdvUrl = "http://192.168.1.201:8004/calcsimilarity";
    //public static String idcardfdvUrl = "http://192.168.1.201:8004/idcardfdv";
    public static String idcardfdvUrl_img = "https://118.31.14.72:8004/idcardfdv";

    public static String fdvCompleteUrl = "https://118.31.14.72:8004/idfdv_complete";
    public static ByteArrayOutputStream certstream_baos = null;
    public static final String config_password = "d178f4caf81f4120ba096df47cc25fed";
    public static final String IPReport_password = "13c16d397ca54dd4af91a9fa4a0d0c22";

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

    // 功能切换
    public static boolean idcardfdv_subCameraEnable = false;    // 红外
    public static boolean moveTaskToBack_enable = false;        // 自动前后台切换

    // debug and test
    public static byte[] TestImageData = null;
    public static boolean DebugNoIDCardReader = false;
    public static int TestImageCnt = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        requestQueue = Volley.newRequestQueue(this);

        // 设置系统闹钟检查2个service运行
        AlarmManager am = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
        Intent RunCheckIntent = new Intent(this, ServicesRunCheckReceiver.class);
        PendingIntent pIntent = PendingIntent.getBroadcast(this, 0, RunCheckIntent, FLAG_UPDATE_CURRENT);
        am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, 60*1000, 20 * 60 * 1000, pIntent);
    }
}
