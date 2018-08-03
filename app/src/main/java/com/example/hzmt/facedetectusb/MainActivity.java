package com.example.hzmt.facedetectusb;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import com.example.hzmt.facedetectusb.CameraUtil.CameraActivity;
import com.example.hzmt.facedetectusb.CameraUtil.CameraActivityData;

public class MainActivity extends AppCompatActivity {
    private Button mBtnLogin;
    private Button mBtnRegister;
    private EditText mEditUrl;
    private Button mBtnSetUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //去除title
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //去掉Activity上面的状态栏
        getWindow().setFlags(WindowManager.LayoutParams. FLAG_FULLSCREEN ,
                WindowManager.LayoutParams. FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        mBtnLogin = (Button) findViewById(R.id.btn_login);
        mBtnRegister = (Button) findViewById(R.id.btn_register);
        mEditUrl = (EditText)findViewById(R.id.editUrl);
        mEditUrl.setText(MyApplication.FaceDetectUrl);

        mBtnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, CameraActivity.class);
                intent.putExtra("RequestType", CameraActivityData.REQ_TYPE_LOGIN);
                startActivity(intent);
                //startActivityForResult(intent, 100);
                //MainActivity.this.finish();
            }
        });

        mBtnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, CameraActivity.class);
                intent.putExtra("RequestType", CameraActivityData.REQ_TYPE_REGISTER);
                startActivity(intent);
            }
        });

        mBtnSetUrl = (Button) findViewById(R.id.btn_seturl);
        mBtnSetUrl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyApplication.FaceDetectUrl = mEditUrl.getText().toString();
            }
        });
    }

 //   @Override
 //   protected void onActivityResult(int requestCode, int resultCode, Intent data) {
 //       super.onActivityResult(requestCode, resultCode, data);
 //   }

}
