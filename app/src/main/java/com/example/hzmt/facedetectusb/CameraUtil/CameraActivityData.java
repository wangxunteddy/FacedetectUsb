package com.example.hzmt.facedetectusb.CameraUtil;

import android.graphics.Bitmap;

/**
 * Created by xun on 2017/9/7.
 */

public class CameraActivityData {
    public static int RequestType = -1;
    public static final int REQ_TYPE_LOGIN = 100;
    public static final int REQ_TYPE_REGISTER = 200;
    public static final int REQ_TYPE_IDCARDFDV = 300;

    // debug info
    public static boolean DEBUG_INFO = true;

    // screen size
    public static int CameraActivity_width = 1280;
    public static int CameraActivity_height = 720;

    public static int FaceDetectNum = 1;
    public static int DeviceOrientation = 90; // 0: portrait   90: landscape
    //  = activity.getWindowManager().getDefaultDisplay().getRotation();

    public static double SimThreshold = 0.77;

    //public static final byte[] fdvlock = new byte[0];  // 特别的instance变量，充当同步锁
    public static final byte[] AiFdrSclock = new byte[0];  // 特别的instance变量，充当同步锁
    public static int idcardfdv_idcardState = 0;
    public static int idcardfdv_cameraState = 0;
    public static String Idcard_id = null;
    public static String Idcard_issuedate = null;
    public static byte[] PhotoImageData = null;
    public static Bitmap PhotoImage;
    public static String PhotoImageFeat;
    public static byte[] CameraImageData = null;
    public static Bitmap CameraImage;
    public static String CameraImageFeat;

    public static boolean detect_face_enable = true;
    public static boolean capture_face_enable = false;
    public static boolean idcardfdv_working = false;
    public static boolean resume_work = false;
}
