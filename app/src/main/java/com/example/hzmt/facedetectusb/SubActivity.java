package com.example.hzmt.facedetectusb;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.content.Intent;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.example.hzmt.facedetectusb.CameraUtil.CameraActivityData;
import com.example.hzmt.facedetectusb.CameraUtil.CameraMgt;

public class SubActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //去除title
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //去掉Activity上面的状态栏
        getWindow().setFlags(WindowManager.LayoutParams. FLAG_FULLSCREEN ,
                WindowManager.LayoutParams. FLAG_FULLSCREEN);
        setContentView(R.layout.activity_sub);

        ImageView mFaceView = (ImageView) findViewById(R.id.sub_faceview);

        Intent intent=getIntent();
        if(intent!=null)
        {
            //byte[] data = intent.getByteArrayExtra("facedata");
            int reqtype = intent.getIntExtra("RequestType", -1);
            if(reqtype == CameraActivityData.REQ_TYPE_IDCARDFDV){
                byte[] data = MyApplication.TestImageData;
                Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
                mFaceView.setImageBitmap(bmp);
            } else {
                byte[] data = MyApplication.TestImageData;
                int cameraid = intent.getIntExtra("cameraid", -1);
                if (cameraid >= 0) {
                    Bitmap bm = CameraMgt.getBitmapFromBytes(data, cameraid, 1);
                    mFaceView.setImageBitmap(bm);
                }
            }

            MyApplication.TestImageData = null;
        }
    }
}
