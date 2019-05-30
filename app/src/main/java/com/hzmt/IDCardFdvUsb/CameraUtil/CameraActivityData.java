package com.hzmt.IDCardFdvUsb.CameraUtil;

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
    public static boolean DEBUG_INFO = false;

    // screen size
    public static int CameraActivity_width = 1280;
    public static int CameraActivity_height = 720;

    public static int FaceDetectNum = 1;
    public static int DeviceOrientation = 90; // 0: portrait   90: landscape
    //  = activity.getWindowManager().getDefaultDisplay().getRotation();

    public static double SimThreshold = 0.77;

    //public static final byte[] fdvlock = new byte[0];  // 特别的instance变量，充当同步锁
    public static final byte[] AiFdrSclock = new byte[0];  // 特别的instance变量，充当同步锁

    public static final int RESULT_NONE = -2;
    public static final int RESULT_FAILED = -1;
    public static final int RESULT_NOT_PASS = 0;
    public static final int RESULT_PASS = 1;
    public static int idcardfdv_result = RESULT_NONE;   // 识别结果标记

    public static long testTime = 0;

    public static int idcardfdv_idcardState = 0;
    public static int idcardfdv_cameraState = 0;
    public static int CheckIDCardReaderCnt = 0;
    public static byte[] PhotoImageData = null;
    public static Bitmap PhotoImage;
    public static String PhotoImageFeat;
//    public static byte[] CameraImageData = null;
//    public static byte[] CameraImageDataSub = null;
    public static Bitmap CameraImage = null;
    public static Bitmap CameraImageSub = null;
    public static String CameraImageFeat;
    public static String CameraImageB64 = null;
    public static Bitmap UploadCameraImage = null;
    public static Bitmap UploadCameraImageSub = null;
    public static IDCardInfos FdvIDCardInfos;

    public static boolean idcardfdv_RequestMode = false;
    public static boolean idcardfdv_NoIDCardMode = false;
    public static boolean idcardfdv_IDCardNoReady = false;
    public static boolean detect_face_enable = true;
    public static boolean capture_face_enable = false;
    public static boolean capture_subface_enable = false;
    public static boolean capture_subface_done = false;
    public static boolean idcardfdv_working = false;
    public static boolean resume_work = false;
}
