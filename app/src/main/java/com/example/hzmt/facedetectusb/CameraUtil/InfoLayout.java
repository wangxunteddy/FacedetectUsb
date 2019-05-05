package com.example.hzmt.facedetectusb.CameraUtil;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.text.InputFilter;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.Button;
import android.view.View;
import android.view.ViewGroup;
import android.graphics.Point;
import android.graphics.Bitmap;
import android.text.InputType;

import android.support.v7.app.AppCompatActivity;
import com.example.hzmt.facedetectusb.R;

/**
 * Created by xun on 2018/7/26.
 */

public class InfoLayout {
    private Activity mActivity;
    private LinearLayout mMainLayout;

    private ImageView mCameraImage;
    private ImageView mIdcardPhoto;

    private TextView mResultSimilarity;

    private TextView mThreshold;
    private Button mThresholdBtn;

    private ImageView mResultIcon;

    //构造函数
    InfoLayout(Activity activity){
        this.mActivity = activity;
        mMainLayout = (LinearLayout) mActivity.findViewById(R.id.info_layout);
        ViewGroup.LayoutParams mainLP = mMainLayout.getLayoutParams();
        mainLP.width = (int)(CameraActivityData.CameraActivity_width * 0.1);

        LinearLayout imagesLayout = (LinearLayout) mActivity.findViewById(R.id.info_images);
        imagesLayout.getLayoutParams().height = CameraActivityData.CameraActivity_height / 4;
        mCameraImage = (ImageView) mActivity.findViewById(R.id.camera_image);
        mIdcardPhoto = (ImageView) mActivity.findViewById(R.id.idcard_photo);

        mResultSimilarity = (TextView) mActivity.findViewById(R.id.result_similarity);
        mResultIcon = (ImageView) mActivity.findViewById(R.id.result_icon);

        mThreshold = (TextView) mActivity.findViewById(R.id.threshold);
        mThresholdBtn = (Button) mActivity.findViewById(R.id.thresholdSet);

        String str = String.format("%.1f%%",CameraActivityData.SimThreshold * 100);
        setThreshold(str);
        mThresholdBtn.setOnClickListener(new ThresholdBtnOnclick());
    }

    public int getInfoLayoutWidth(){
        ViewGroup.LayoutParams mainLP = mMainLayout.getLayoutParams();
        return mainLP.width;
    }

    public void setCameraImage(final Bitmap bm){
        mCameraImage.post(new Runnable(){
            @Override
            public void run() {
                mCameraImage.setImageBitmap(bm);
            }
        });
    }

    public void resetCameraImage(){
        mCameraImage.post(new Runnable(){
            @Override
            public void run() {
                mCameraImage.setImageDrawable(
                        mActivity.getResources().getDrawable(R.drawable.icon_camera)
                );
            }
        });
    }

    public void setIdcardPhoto(final Bitmap bm){
        mIdcardPhoto.post(new Runnable(){
            @Override
            public void run() {
                mIdcardPhoto.setImageBitmap(bm);
            }
        });
    }

    public void resetIdcardPhoto(){
        mIdcardPhoto.post(new Runnable(){
            @Override
            public void run() {
                mIdcardPhoto.setImageDrawable(
                        mActivity.getResources().getDrawable(R.drawable.icon_photo)
                );
            }
        });
    }

    public void setResultSimilarity(final CharSequence text){
        mResultSimilarity.post(new Runnable(){
            @Override
            public void run() {
                mResultSimilarity.setText(text);
            }
        });
    }

    public void setResultIconPass(){
        mResultIcon.post(new Runnable(){
            @Override
            public void run() {
                mResultIcon.setImageDrawable(mActivity.getResources().getDrawable(R.drawable.icon_right));
                mResultIcon.setVisibility(View.VISIBLE);
            }
        });
    }

    public void setResultIconNotPass(){
        mResultIcon.post(new Runnable(){
            @Override
            public void run() {
                mResultIcon.setImageDrawable(mActivity.getResources().getDrawable(R.drawable.icon_wrong));
                mResultIcon.setVisibility(View.VISIBLE);
            }
        });
    }

    public void resetResultIcon(){
        mResultIcon.post(new Runnable(){
            @Override
            public void run() {
                mResultIcon.setImageDrawable(mActivity.getResources().getDrawable(R.drawable.icon_wrong));
                mResultIcon.setVisibility(View.INVISIBLE);
            }
        });
    }

    public void setThreshold(final CharSequence text){
        mThreshold.post(new Runnable(){
            @Override
            public void run() {
                mThreshold.setText(text);
            }
        });
    }


    //定义内部类，实现View.OnClickListener接口
    class  ThresholdBtnOnclick implements View.OnClickListener{
        @Override
        public void onClick(View v) {
            AlertDialog.Builder dialog = new AlertDialog.Builder(mActivity);
            dialog.setTitle("设定相似度阈值");
            final EditText edit = new EditText(mActivity);
            edit.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            edit.setWidth(240);
            edit.setGravity(Gravity.CENTER);
            edit.setFilters( new InputFilter[]{ new InputFilter.LengthFilter( 4 )});
            edit.setTextSize(24);
            TextView textView = new TextView(mActivity);
            textView.setText("%");
            textView.setTextSize(24);

            LinearLayout layout = new LinearLayout(mActivity);
            layout.setOrientation(LinearLayout.HORIZONTAL);
            layout.setGravity(Gravity.CENTER);
            layout.addView(edit);
            layout.addView(textView);

            dialog.setView(layout);
            dialog.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    CameraActivity.startBrightnessWork(mActivity);

                    String thStr = edit.getText().toString();
                    if(thStr.isEmpty())
                        return;
                    Double threshold = Double.valueOf(thStr) / 100.0;
                    if(threshold > 1.0 || threshold < 0)
                        return;

                    CameraActivityData.SimThreshold = threshold;
                    String str = String.format("%.1f%%",CameraActivityData.SimThreshold * 100);
                    setThreshold(str);
                }
            });
            dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    CameraActivity.startBrightnessWork(mActivity);
                }
            });

            AlertDialog dlg = dialog.create();
            dlg.show();


            CameraActivity.keepBright(mActivity);
        }
    }

}
