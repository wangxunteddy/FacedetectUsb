package com.example.hzmt.facedetectusb.CameraUtil;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.util.Log;
import android.view.SurfaceView;
import android.widget.ImageView;
import android.widget.Toast;
import android.view.View;
import android.hardware.Camera;
import android.view.Window;
import android.view.WindowManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.hardware.usb.UsbManager;

import android.location.Location;
import android.location.LocationManager;
import android.location.LocationListener;
import android.provider.Settings;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.ArrayList;

import com.example.hzmt.facedetectusb.MyApplication;
import com.example.hzmt.facedetectusb.R;
import com.example.hzmt.facedetectusb.SubActivity;

//import com.invs.UsbBase;
import com.hzmt.aifdrsclib.AiFdrScPkg;
import com.invs.UsbBase;

public class CameraActivity extends AppCompatActivity {
    private static final int PERMISSION_FINE_LOCATION = 0;
    private static final int PERMISSION_COARSE_LOCATION = 1;
    private static final int PERMISSION_CAMERA = 2;
    private static final int PERMISSION_STORAGE = 3;
    private static final int TOAST_OFFSET_PERMISSION_RATIONALE = 100;
    List<Integer> mPermissionIdxList = new ArrayList<>();
    private CameraMgt mCameraMgt;
    private SurfaceView mPreviewSV;
    private SurfaceDraw mFaceRect;
    public  InfoLayout mInfoLayout;
    private ImageView mHelpImg;
    private FaceTask mFaceTask;

    public DebugLayout mDebugLayout;
    public IDCardReader mIDCardReader;
    public IDCardReadHandler mIDCardReadHandler;

    // sound
    public AudioTrack mATRight;
    public AudioTrack mATWrong;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //去除title
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //去掉Activity上面的状态栏
        getWindow().setFlags(WindowManager.LayoutParams. FLAG_FULLSCREEN ,
                WindowManager.LayoutParams. FLAG_FULLSCREEN);
        setContentView(R.layout.activity_camera);

        // service
        Intent uploadIntent = new Intent();
        ComponentName cn = new ComponentName("com.hzmt.idcardfdvupload", "com.hzmt.idcardfdvupload.UploadSrv");
        uploadIntent.setComponent(cn);
        uploadIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startService(uploadIntent);

        Intent upgradeIntent = new Intent();
        ComponentName cn2 = new ComponentName("com.hzmt.idcardfdvupgrade", "com.hzmt.idcardfdvupgrade.UpgradeSrv");
        upgradeIntent.setComponent(cn2);
        upgradeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startService(upgradeIntent);


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

        //mImgView = (ImageView) findViewById(R.id.imageView);
        //Bitmap infobg = Bitmap.createBitmap(2, 2,
        //        Bitmap.Config.ARGB_8888);
        //infobg.eraseColor(Color.parseColor("#FFFFFF"));//填充颜色
        //mImgView.setImageBitmap(infobg);
        //mImgView.setVisibility(View.INVISIBLE);

        mPreviewSV = (SurfaceView) findViewById(R.id.camera_preview);
        mFaceRect = (SurfaceDraw) findViewById(R.id.surface_draw);
        mFaceRect.setVisibility(View.VISIBLE);

        mCameraMgt = new CameraMgt(this, mPreviewSV, mFaceRect);
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

        // brightness
        CameraActivity.startBrightnessWork(this, mInfoLayout);

        // debug output
        mDebugLayout = new DebugLayout(this);
        mDebugLayout.setText("Debug:\n");

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
        String path = Environment.getExternalStorageDirectory().getPath();
        //path = path + "/fdrmodel/";
        path = "/sdcard/fdrmodel/";
        MyApplication.AiFdrScIns.initAiFdrSc(path);

        // IDCardReader
        mIDCardReader = new IDCardReader();
        mIDCardReader.OpenIDCardReader(this);
        mIDCardReadHandler = new IDCardReadHandler(this);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED){
            mCameraMgt.setPreviewCallback(mPreviewCB);
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

        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private Camera.PreviewCallback mPreviewCB = new Camera.PreviewCallback(){
        @Override
        public void onPreviewFrame(byte[] data, Camera camera){

            if(camera.getParameters().getPreviewFormat() != ImageFormat.NV21)
                return;

            //Camera.Size s = camera.getParameters().getPreviewSize();
            //String pres = s.width+" x " +s.height;
            //Toast.makeText(CameraActivity.this, pres, Toast.LENGTH_LONG).show();

            if(null != mFaceTask){
                switch(mFaceTask.getStatus()){
                    case RUNNING:
                        return;
                    case PENDING:
                        mFaceTask.cancel(false);
                        break;
                }
            }

            boolean working = false;
            //synchronized(CameraActivityData.fdvlock){
                working = CameraActivityData.idcardfdv_working;
            //}
            if (!working) {
                mFaceTask = new FaceTask(CameraActivity.this,
                        data,
                        mCameraMgt.getCurrentCameraId(),
                        camera,
                        mInfoLayout,
                        mFaceRect,
                        mCameraMgt.getCameraView());

                //synchronized(CameraActivityData.fdvlock){
                    CameraActivityData.idcardfdv_working = true;
                //}
                mFaceTask.execute((Void) null);
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
                mHelpImg.setVisibility(visibility);
            }
        });
    }

    @Override
    public void onBackPressed() {
        mCameraMgt.closeCamera();
        mATRight.release();
        mATWrong.release();
        super.onBackPressed();
    }

    @Override
    public void onDestroy(){
        mIDCardReader.CloseIDCardReader();
        mATRight.release();
        mATWrong.release();
        super.onDestroy();
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

    // ===========================================================
    // screen brightness
    public static void startBrightnessWork(final Activity activity, final InfoLayout infoL){
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

                    infoL.resetCameraImage();
                    infoL.resetIdcardPhoto();
                    infoL.setResultSimilarity("--%");
                    infoL.resetResultIcon();
                }
            };
        }
        MyApplication.BrightnessHandler.removeCallbacks(MyApplication.BrightnessRunnable);
        WindowManager.LayoutParams params = activity.getWindow().getAttributes();
        params.screenBrightness = 0.5f;
 //       activity.getWindow().setAttributes(params);
        Window w = activity.getWindow();
        w.setAttributes(params);
        MyApplication.BrightnessHandler.postDelayed(MyApplication.BrightnessRunnable,10*1000);
    }

    public static void keepBright(Activity activity){
        MyApplication.BrightnessHandler.removeCallbacks(MyApplication.BrightnessRunnable);
        WindowManager.LayoutParams params = activity.getWindow().getAttributes();
        params.screenBrightness = 0.5f;
        activity.getWindow().setAttributes(params);
    }
    // ===========================================================

    public static void delayResumeFdvWork(long delayMillis){
        Handler handler = new Handler();
        Runnable work = new Runnable() {
            @Override
            public void run() {
                //synchronized(CameraActivityData.fdvlock){
                    CameraActivityData.idcardfdv_working = false;
                //}
            }
        };

        handler.postDelayed(work,delayMillis);
    }

    /**
     * 保存图片
     */
    public static void saveUploadBitmapJPEG(Bitmap bitmap, String prename) {
        // 图片存放路径
        String uploadDir = "/sdcard/fdrmodel/UPLOAD";

        try {
            File dirFile = new File(uploadDir);
            if (!dirFile.exists()) {
                dirFile.mkdirs();
            }
            File file = new File(uploadDir, prename + ".jpeg");
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void saveUploadBitmapBMP(byte[] data, String prename) {
        // 图片存放路径
        String uploadDir = "/sdcard/fdrmodel/UPLOAD";

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


