package com.example.hzmt.facedetectusb.CameraUtil;

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

    public static String Idcard_id = null;
    public static String Idcard_issuedate = null;
}
