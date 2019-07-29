package com.hzmt.IDCardFdvUsb.CameraUtil;

import android.Manifest;
import android.app.Activity;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.util.Log;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;
import android.hardware.Camera;
import android.view.Window;
import android.view.WindowManager;
import android.content.Intent;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.ArrayList;

import com.hzmt.IDCardFdvUsb.MyApplication;
import com.hzmt.IDCardFdvUsb.R;
import com.hzmt.IDCardFdvUsb.SubActivity;

//import com.invs.UsbBase;
import com.hzmt.IDCardFdvUsb.util.AppUtils;
import com.hzmt.IDCardFdvUsb.util.AssetExtractor;
import com.hzmt.IDCardFdvUsb.util.ConfigUtil;
import com.hzmt.IDCardFdvUsb.util.IdcardFdvRegister;
import com.hzmt.IDCardFdvUsb.util.NV21ToBitmap;
import com.hzmt.IDCardFdvUsb.util.ShowToastUtils;
import com.hzmt.IDCardFdvUsb.util.SystemUtil;
import com.hzmt.aifdrsclib.AiFdrScPkg;

public class CameraActivity extends AppCompatActivity{
    private static final int PERMISSION_FINE_LOCATION = 0;
    private static final int PERMISSION_COARSE_LOCATION = 1;
    private static final int PERMISSION_CAMERA = 2;
    private static final int PERMISSION_STORAGE = 3;
    private static final int TOAST_OFFSET_PERMISSION_RATIONALE = 100;

    List<Integer> mPermissionIdxList = new ArrayList<>();
    private CameraMgt mCameraMgt;
    public NV21ToBitmap mNV21ToBitmap;
    public NV21ToBitmap mNV21ToBitmapSub;

    private SurfaceView mPreviewSV;
    private SurfaceDraw mFaceRect;
    public  InfoLayout mInfoLayout;
    private ImageView mHelpImg;
    public  LinearLayout mAttLayout;
    public DebugLayout mDebugLayout;

    public IDCardReader mIDCardReader;
    public IDCardReadHandler mIDCardReadHandler;

    public FdvWorkHandler mFdvWorkHandler;

    private AssetExtractor assetExtractor;
    // sound
    public AudioTrack mATRight;
    public AudioTrack mATWrong;

    // service
    public FdvRestfulService mFdvSrv;
    private RestfulRequestReceiver mFdvSrvReceiver;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //去除title
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //去掉Activity上面的状态栏
        getWindow().setFlags(WindowManager.LayoutParams. FLAG_FULLSCREEN ,
                WindowManager.LayoutParams. FLAG_FULLSCREEN);
        setContentView(R.layout.activity_camera);

        assetExtractor = new AssetExtractor(this);

        // service
        if(!AppUtils.isServiceRunning(this,"com.hzmt.idcardfdvupload.UploadSrv")) {
            Intent uploadIntent = new Intent();
            ComponentName cn = new ComponentName("com.hzmt.idcardfdvupload", "com.hzmt.idcardfdvupload.UploadSrv");
            uploadIntent.setComponent(cn);
            uploadIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startService(uploadIntent);
        }

        if(!AppUtils.isServiceRunning(this,"com.hzmt.idcardfdvupgrade.UpgradeSrv")) {
            Intent upgradeIntent = new Intent();
            ComponentName cn2 = new ComponentName("com.hzmt.idcardfdvupgrade", "com.hzmt.idcardfdvupgrade.UpgradeSrv");
            upgradeIntent.setComponent(cn2);
            upgradeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startService(upgradeIntent);
        }

        // restful service
        bindService(new Intent(this,FdvRestfulService.class),mSrvConn, Service.BIND_AUTO_CREATE);
        // 动态注册用于通信的接收器
        mFdvSrvReceiver = new RestfulRequestReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.hzmt.IDCardFdvUsb.FDV_REQUEST");
        registerReceiver(mFdvSrvReceiver, filter);

        // intent data
        Intent intent=getIntent();
        if(intent!=null) {
            int reqtype = intent.getIntExtra("RequestType", -1);
            if(reqtype == -1) {
                // CameraActivityData.RequestType = CameraActivityData.REQ_TYPE_LOGIN; // default
                CameraActivityData.RequestType = CameraActivityData.REQ_TYPE_IDCARDFDV;
                // CameraActivityData.RequestType = CameraActivityData.REQ_TYPE_REGISTER;
            }
            else
                CameraActivityData.RequestType = reqtype;
        }
        else{
            // default
            // CameraActivityData.RequestType = CameraActivityData.REQ_TYPE_LOGIN;
            CameraActivityData.RequestType = CameraActivityData.REQ_TYPE_IDCARDFDV;
        }

        // screen size
        Point point = new Point();
        getWindowManager().getDefaultDisplay().getRealSize(point); // 全屏分辨率
        CameraActivityData.CameraActivity_width = point.x;
        CameraActivityData.CameraActivity_height = point.y;

        mPreviewSV = (SurfaceView) findViewById(R.id.camera_preview);
//        mPreviewSV.setTranslationX(CameraActivityData.CameraActivity_width * 0.4f);
        mFaceRect = (SurfaceDraw) findViewById(R.id.surface_draw);
//        mFaceRect.setTranslationX(CameraActivityData.CameraActivity_width * 0.4f);
        mFaceRect.setVisibility(View.VISIBLE);

        mCameraMgt = new CameraMgt(this, mPreviewSV, mFaceRect);
        mNV21ToBitmap = new NV21ToBitmap(this);
        mNV21ToBitmapSub = new NV21ToBitmap(this);
        if(MyApplication.certstream_baos == null){
            try {
                MyApplication.certstream_baos = new ByteArrayOutputStream();
                InputStream certstream = getAssets().open("cert.pem");
                int size = certstream.available();
                byte[] buffer = new byte[size];
                int len;
                while ((len = certstream.read(buffer)) > -1) {
                    MyApplication.certstream_baos.write(buffer, 0, len);
                }
                MyApplication.certstream_baos.flush();
            } catch (IOException e){
                e.printStackTrace();
            }
        }

        mInfoLayout = new InfoLayout(this);

        mHelpImg = findViewById(R.id.helpimg);

        // config信息
        ConfigUtil.readConfigFile(this);

        // 设备ID
        String deviceID = ConfigUtil.getValue(ConfigUtil.KEY_DEVICE_ID);
        if(deviceID == null || deviceID.equals("")){
            // 去掉生成，改为注册时获取
            //deviceID = WorkUtils.createDeviceID(this);
            //ConfigUtil.setValue(ConfigUtil.KEY_DEVICE_ID, deviceID);
            //ConfigUtil.writeConfigFile();
        }
        else{
            TextView didView = findViewById(R.id.version_str);
            String text = getText(R.string.ver_str).toString() + "-" + deviceID;
            didView.setText(text);
        }


        // 公安提醒画面
        setAttLayout();

        // sound data
        new Thread() {
            @Override
            public void run() {
                mATRight = getAudioPlayer(R.raw.right);
            }
        }.start();

        new Thread() {
            @Override
            public void run() {
                mATWrong = getAudioPlayer(R.raw.wrong);
            }
        }.start();

        // network, ip
        WorkUtils.setNetworkChangeWork(this);

        // brightness
        WorkUtils.startBrightnessWork(this);

        // debug output
        mDebugLayout = new DebugLayout(this);
        mDebugLayout.setText("Debug:\n");
        //mDebugLayout.addText(getExternalFilesDir(null).getAbsolutePath());


        // Android 6.0 运行时权限
        String[] permissions = new String[]
                {
                 //       Manifest.permission.ACCESS_FINE_LOCATION,
                 //       Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.CAMERA,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                };
        List<String> permissionList = new ArrayList<>();

        permissionList.clear();
        mPermissionIdxList.clear();
        for(int i = 0; i < permissions.length; i++){
            if (ContextCompat.checkSelfPermission(this, permissions[i]) != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(permissions[i]);
                mPermissionIdxList.add(i);
            }
        }

        if (permissionList.isEmpty()) {//未授予的权限为空，表示都授予了
            initWork(false); // 此处不开启预览，CameraMgt surfaceCreated()处开启
        } else {//请求权限方法
            String[] reqpermissions = permissionList.toArray(new String[permissionList.size()]);//将List转为数组
            ActivityCompat.requestPermissions(this, reqpermissions, 1);
        }

    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        // 界面加载完成后处理
        if (hasFocus) {
            if(!mCameraMgt.isCameraPreviewStarted()){
                ShowToastUtils.showToast(this, "摄像头初始化失败！", Toast.LENGTH_SHORT);
            }

            // 航信对接，如执行至此仍未上报则上报一次
            if(CameraActivityData.HX_runOnStart) {
                WorkUtils.HX_DeviceReg(this);
                CameraActivityData.HX_runOnStart = false;
            }
        }
    }

    @Override
    protected void onResume(){
        super.onResume();
        Log.e("onResume","onResume");
    }


    private ServiceConnection mSrvConn = new ServiceConnection(){
        @Override
        public void onServiceConnected(ComponentName name, IBinder service)
        {
            // 获取服务上的IBinder对象，调用IBinder对象中定义的自定义方法，获取Service对象
            FdvRestfulService.LocalBinder binder=(FdvRestfulService.LocalBinder)service;
            mFdvSrv = binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name)
        {
            mFdvSrv = null;
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1)
        {
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                            permissions[i])){
                        // 仅此次禁止
                        permissionToast(mPermissionIdxList.get(i)+TOAST_OFFSET_PERMISSION_RATIONALE);
                    }
                    else{
                        //禁止且不再提示
                        permissionToast(mPermissionIdxList.get(i));
                    }
                }
                else{
                    // 允许
                }
            }

        }

        initWork(true);
    }

    private void initWork(boolean bStartCamera){
        // 初始化AiFdrSc
        MyApplication.AiFdrScIns = new AiFdrScPkg();
        //String path = Environment.getExternalStorageDirectory().getPath();
        //String path = getExternalFilesDir(null).getAbsolutePath();
        String path = getFilesDir().getAbsolutePath();
        //String ver = getVersionStr(path+"version.txt");
        //mDebugLayout.addText(ver+"\n");
        //mDebugLayout.addText(MyApplication.AiFdrScIns.testJNI()+"\n");
        path = path + "/fdrmodel/";
        {
            // 检查模型文件
            File file1 = new File(path + "markscc.dat");
            if(!file1.exists())
                assetExtractor.copyAssetFile("markscc.dat", file1.getAbsolutePath());

            File file2 = new File(path + "modelcc.dat");
            if(!file2.exists())
                assetExtractor.copyAssetFile("modelcc.dat", file2.getAbsolutePath());

            File file3 = new File(path + "modeld.dat");
            if(!file3.exists())
                assetExtractor.copyAssetFile("modeld.dat", file3.getAbsolutePath());

            File file4 = new File(path + "modelsc.dat");
            if(!file4.exists())
                assetExtractor.copyAssetFile("modelsc.dat", file4.getAbsolutePath());
        }
        MyApplication.AiFdrScIns.initAiFdrSc(path);
        mDebugLayout.addText("models loaded!\n");

        // IDCardReader
        mIDCardReader = new IDCardReader();
        int oret = mIDCardReader.OpenIDCardReader(this);
        if(oret == -1){
            String errMsg = "未找到身份证阅读器!";
            ShowToastUtils.showToast(this, errMsg, Toast.LENGTH_SHORT);
        }
        mIDCardReadHandler = new IDCardReadHandler(this);
        CameraActivityData.FdvIDCardInfos = new IDCardInfos();
        CameraActivityData.CheckIDCardReaderCnt = 0;

        // handler for fdv work
        mFdvWorkHandler = new FdvWorkHandler(this);
        IdcardFdvRegister.checkRegister(this);
        WorkUtils.startIPReportThread(this);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED){
            mCameraMgt.setPreviewCallback(mPreviewCB, mSubPreviewCB);
            mCameraMgt.setTakePictureJpegCallback(mTakePictrueJpegCB);
            mCameraMgt.openCamera(bStartCamera);
        }
    }

    private void permissionToast(int code){
        String msg;
        switch(code){
            case PERMISSION_CAMERA:
                msg = "请在设置里打开摄像头使用权限！";
                break;
            case PERMISSION_STORAGE:
                msg = "请在设置里打开存储权限！";
                break;
            case TOAST_OFFSET_PERMISSION_RATIONALE+PERMISSION_FINE_LOCATION:
                msg = "无法使用GPS定位服务！";
                break;
            case TOAST_OFFSET_PERMISSION_RATIONALE+PERMISSION_CAMERA:
                msg = "无法使用摄像头，请退出重试！";
                break;
            case TOAST_OFFSET_PERMISSION_RATIONALE+PERMISSION_STORAGE:
                msg = "无法读取模型文件，请退出重试！";
                break;
            default:
                return;
        }

        ShowToastUtils.showToast(this, msg, Toast.LENGTH_SHORT);
    }

    private Camera.PreviewCallback mPreviewCB = new Camera.PreviewCallback(){
        @Override
        public void onPreviewFrame(byte[] data, Camera camera){
            if(camera.getParameters().getPreviewFormat() != ImageFormat.NV21)
                return;

            //Camera.Size s = camera.getParameters().getPreviewSize();
            //String pres = s.width+" x " +s.height;
            //ShowToastUtils.showToast(CameraActivity.this, pres, Toast.LENGTH_LONG);

            // 切入后台处理
            //boolean backflag = MyApplication.moveTaskToBack_enable &&       // 允许切换至后台
            //                    CameraActivityData.detect_face_enable &&    // 识别处理入口，需截断其他识别处理。
            //                    CameraActivityData.moveTaskToBack_doMove;   // 转入后台的启动标识

            //if(backflag){
            //    CameraActivity.this.moveTaskToBack(false);
            //    return;
            //}

            if(CameraActivityData.detect_face_enable){
                DetectFaceThread detectFaceTh = new DetectFaceThread(CameraActivity.this,
                        data,
                        mCameraMgt.getCurrentCameraId(),
                        camera);

                CameraActivityData.detect_face_enable = false;
                detectFaceTh.execute((Void) null);
            }

            if(CameraActivityData.capture_face_enable) {
                if(MyApplication.idcardfdv_subCameraEnable) {
                    //CameraActivityData.CameraImageDataSub = null;
                    CameraActivityData.capture_subface_done = false;
                    CameraActivityData.capture_subface_enable = true;
                }
                else
                    CameraActivityData.capture_subface_done = true;
                FdvCameraFaceThread fdvCameraFaceTh = new FdvCameraFaceThread(CameraActivity.this,
                        data,
                        mCameraMgt.getCurrentCameraId(),
                        camera);

                CameraActivityData.capture_face_enable = false;
                fdvCameraFaceTh.execute((Void) null);
            }

        }
    };

    private Camera.PreviewCallback mSubPreviewCB = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {

            if(camera.getParameters().getPreviewFormat() != ImageFormat.NV21)
                return;

            if(CameraActivityData.capture_subface_enable){
                CameraActivityData.capture_subface_enable = false;
                FdvSubCameraFaceThread fdvSubCameraFaceTh = new FdvSubCameraFaceThread(CameraActivity.this,
                        data,
                        mCameraMgt.getCurrentSubCameraId(),
                        camera);

                fdvSubCameraFaceTh.start();
            }
        }
    };

    private Camera.PictureCallback mTakePictrueJpegCB = new Camera.PictureCallback(){
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            //Log.e("onPictureTaken","Take Picture success!");
            //Bitmap bm = CameraMgt.getBitmapFromBytes(data, mCameraMgt.getCurrentCameraId(), 4);
            //mInfoLayout.setCameraImage(bm);


            Intent intent = new Intent();
            intent.setClass(CameraActivity.this, SubActivity.class);
            //intent.putExtra("facedata", data);
            MyApplication.TestImageData = data;
            intent.putExtra("cameraid", mCameraMgt.getCurrentCameraId());
            //setResult(CameraActivityData.REQ_TYPE_REGISTER,intent);
            startActivity(intent);
            //CameraActivity.this.finish();

            //camera.startPreview();//重新开始预览

        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 100){
        }
    }

    public void setHelpImgVisibility(final int visibility){
        mHelpImg.post(new Runnable(){
            @Override
            public void run() {
                if(visibility == View.VISIBLE){
                    // 检查deviceID
                    TextView didView = findViewById(R.id.version_str);
                    String ver = getText(R.string.ver_str).toString();
                    String deviceID = ConfigUtil.getValue(ConfigUtil.KEY_DEVICE_ID);
                    if(deviceID  !=null && !deviceID.equals("") &&
                        didView.getText().toString().equals(ver)){
                        String text = ver + "-" + deviceID;
                        didView.setText(text);
                    }
                }
                mHelpImg.setVisibility(visibility);
            }
        });
    }

    @Override
    public void onBackPressed() {
        //mCameraMgt.closeCamera();
        //mATRight.release();
        //mATWrong.release();
        //super.onBackPressed();
    }

    @Override
    public void onDestroy(){
        mIDCardReader.CloseIDCardReader();
        CameraActivityData.CheckIDCardReaderCnt = 0;
        mATRight.release();
        mATWrong.release();

        unregisterReceiver(mFdvSrvReceiver);
        mFdvSrvReceiver = null;
        unbindService(mSrvConn);
        mFdvSrv = null;
        super.onDestroy();
    }

    // 点击->无证入住
    public void onHelpImgClick(View v){
        CameraActivityData.idcardfdv_NoIDCardMode = true;
    }

    // 点击预览画面事件
    public void onPreviewClick(View v){
        mInfoLayout.clearIDCardNoInputFocus();
    }

    // 公安提醒画面设置
    public void setAttLayOutVisibility(final int visibility){
        mAttLayout.post(new Runnable(){
            @Override
            public void run() {
                mAttLayout.setVisibility(visibility);
            }
        });
    }
    private void setAttLayout(){
        String police = ConfigUtil.getValue(ConfigUtil.KEY_POLICE);
        mAttLayout = findViewById(R.id.att_layout);
        ViewGroup.LayoutParams attLP = mAttLayout.getLayoutParams();
        attLP.width = (int)(CameraActivityData.CameraActivity_width * 0.4);
        EditText policeText = findViewById(R.id.police);
        policeText.setText(police);
        EditText policeAttText = findViewById(R.id.police_att);
        String police_att = police + "提醒：";
        policeAttText.setText(police_att);
    }

    // 加载声音数据
    private AudioTrack getAudioPlayer(int resId){
        AudioTrack at = null;
        byte[] wavData;
        try {
            InputStream in = getResources().openRawResource(resId);
            wavData = new byte[in.available()];
            try {
                 ByteArrayOutputStream out = new ByteArrayOutputStream();
                 for (int b; (b = in.read()) != -1; ) {
                    out.write(b);
                 }
                wavData = out.toByteArray();
            } finally {
                in.close();
            }

            // pcm数据长度。注意wav文件如更改，读取位置可能需修改
            int pcmlen = 0;
            pcmlen+=wavData[0x4d];
            pcmlen=pcmlen*256+wavData[0x4c];
            pcmlen=pcmlen*256+wavData[0x4b];
            pcmlen=pcmlen*256+wavData[0x4a];

            at = new AudioTrack(AudioManager.STREAM_MUSIC, 44100,
                                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                                wavData.length, AudioTrack.MODE_STATIC);

            at.write(wavData, 0x4e, pcmlen);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return at;
    }


    public void backToHelp() {
        mDebugLayout.addText("back to Help!\n");
        //CameraActivityData.idcardfdv_cameraState = FdvCameraFaceThread.CAMERA_FACE_STATE_NONE;
        //CameraActivityData.idcardfdv_idcardState = IDCardReadThread.IDCARD_STATE_NONE;
        mInfoLayout.resetCameraImage();
        mInfoLayout.setResultSimilarity("--%");
        if (CameraActivityData.redo_info < 0){
            mInfoLayout.resetIdcardPhoto();
            setHelpImgVisibility(View.VISIBLE);
            setAttLayOutVisibility(View.INVISIBLE);
        }
        CameraActivityData.CameraImage = null;
        CameraActivityData.CameraImageFeat = "";
        CameraActivityData.idcardfdv_NoIDCardMode = false;
        if(CameraActivityData.idcardfdv_RequestMode && CameraActivityData.redo_info < 0 && mFdvSrv != null) {
            if(CameraActivityData.idcardfdv_result != CameraActivityData.RESULT_NOT_PASS &&
                    CameraActivityData.idcardfdv_result != CameraActivityData.RESULT_PASS )
            {
                mFdvSrv.setRequestResult("","",CameraActivityData.RESULT_FAILED, null,0.0);
            }
        }
        CameraActivityData.idcardfdv_RequestMode = false;
        CameraActivityData.idcardfdv_result = CameraActivityData.RESULT_NONE;
    }

    // restful接收外部识别请求
    public class RestfulRequestReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            IDCardInfos info = (IDCardInfos) intent.getSerializableExtra("person_data");
            action_fdv(info);
        }
    }

    public void action_fdv(IDCardInfos info){
        if(mHelpImg.getVisibility() == View.INVISIBLE){
            // 不在帮助画面时不执行
            if(mFdvSrv != null)
                mFdvSrv.setRequestResult("","",
                        CameraActivityData.RESULT_FAILED,"设备正忙，请稍候重试。", 0.0);
            return;
        }

        try {
            CameraActivityData.FdvIDCardInfos = (IDCardInfos) info.clone();
        }catch (CloneNotSupportedException e){
            e.printStackTrace();
        }
        CameraActivityData.idcardfdv_RequestMode = true;
    }
}


