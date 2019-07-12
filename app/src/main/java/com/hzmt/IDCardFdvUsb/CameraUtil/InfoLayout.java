package com.hzmt.IDCardFdvUsb.CameraUtil;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.text.InputFilter;
import android.view.Gravity;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.Button;
import android.view.View;
import android.view.ViewGroup;
import android.graphics.Bitmap;
import android.text.InputType;
import android.widget.Toast;

import com.hzmt.IDCardFdvUsb.R;
import com.hzmt.IDCardFdvUsb.util.ShowToastUtils;

import java.lang.ref.WeakReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by xun on 2018/7/26.
 */

public class InfoLayout {
    private final WeakReference<CameraActivity> mActivity;
    private LinearLayout mMainLayout;

    private ImageView mCameraImage;
    private ImageView mIdcardPhoto;

    private EditText mIDCardNoInput;
    private Button mIDCardNoBtn;

    private TextView mResultSimilarity;

    private TextView mThreshold;
    private Button mThresholdBtn;

    private ImageView mResultIcon;

    //构造函数
    InfoLayout(final CameraActivity activity){
        mActivity = new WeakReference<>(activity);
        mMainLayout = activity.findViewById(R.id.info_layout);
        ViewGroup.LayoutParams mainLP = mMainLayout.getLayoutParams();
        mainLP.width = (int)(CameraActivityData.CameraActivity_width * 0.1);

        LinearLayout imagesLayout = activity.findViewById(R.id.info_images);
        imagesLayout.getLayoutParams().height = CameraActivityData.CameraActivity_height / 4;
        mCameraImage = activity.findViewById(R.id.camera_image);
        mIdcardPhoto = activity.findViewById(R.id.idcard_photo);

        mIDCardNoInput = activity.findViewById(R.id.idcardno_input);
        mIDCardNoBtn = activity.findViewById(R.id.idcardno_btn);

        mResultSimilarity = activity.findViewById(R.id.result_similarity);
        mResultIcon = activity.findViewById(R.id.result_icon);

        mThreshold = activity.findViewById(R.id.threshold);
        mThresholdBtn = activity.findViewById(R.id.thresholdSet);

        mIDCardNoInput.setOnFocusChangeListener(new android.view.View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                    CameraActivity activity = mActivity.get();
            //        if(activity == null && !CameraActivityData.idcardfdv_NoIDCardMode)
            //            return;

                    if(hasFocus) {
                        WorkUtils.keepBright(activity);
                    }
                    else {
                        // 关闭软键盘
                        InputMethodManager imm = (InputMethodManager) v.getContext()
                                .getSystemService(Context.INPUT_METHOD_SERVICE);
                        if (imm != null) {
                            imm.hideSoftInputFromWindow(v.getWindowToken(),0);
                        }

                        WorkUtils.startBrightnessWork(activity);
                    }
            }
        });
        mIDCardNoBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                //if(!CameraActivityData.idcardfdv_NoIDCardMode)
                //    return;

                if(CameraActivityData.idcardfdv_IDCardNoReady){
                    mIDCardNoInput.setFocusableInTouchMode(true);
                    mIDCardNoInput.setFocusable(true);
                    mIDCardNoInput.setTextColor(Color.BLACK);
                    mIDCardNoBtn.setText("确定");

                    mIDCardNoInput.requestFocus();
                    CameraActivityData.idcardfdv_IDCardNoReady = false;
                }
                else{
                    // 正则判断是否合规
                    String strIDCardNo = mIDCardNoInput.getText().toString();
                    Pattern pa = Pattern.compile("^[1-9]\\d{7}((0\\d)|(1[0-2]))(([0|1|2]\\d)|3[0-1])\\d{3}$|^[1-9]\\d{5}[1-9]\\d{3}((0\\d)|(1[0-2]))(([0|1|2]\\d)|3[0-1])\\d{3}([0-9]|X)$");
                    Matcher ma = pa.matcher(strIDCardNo);
                    if(ma.matches()) {
                        CameraActivityData.FdvIDCardInfos.idcard_id = strIDCardNo;
                        // 禁止编辑
                        mIDCardNoInput.setFocusable(false);
                        mIDCardNoInput.setFocusableInTouchMode(false);
                        mIDCardNoInput.setTextColor(Color.GRAY);
                        mIDCardNoBtn.setText("修改");
                        CameraActivityData.idcardfdv_IDCardNoReady = true;
                    }
                    else{
                        CameraActivityData.FdvIDCardInfos.idcard_id = "";
                        CameraActivityData.idcardfdv_IDCardNoReady = false;
                        String errMsg = "输入的身份证号码无效！";
                        ShowToastUtils.showToast(activity, errMsg, Toast.LENGTH_SHORT);
                    }
                }
            }
        });


        String str = String.format("%.1f%%",CameraActivityData.SimThreshold * 100);
        setThreshold(str);
        mThresholdBtn.setOnClickListener(new ThresholdBtnOnclick());
    }

    public void setMode(boolean noidcard){
        CameraActivity activity = mActivity.get();
        if(activity == null)
            return;

        if(noidcard){
            mIdcardPhoto.setVisibility(View.INVISIBLE);
            activity.findViewById(R.id.idcardno_input_layout).setVisibility(View.VISIBLE);
            mIDCardNoInput.setFocusableInTouchMode(true);
            mIDCardNoInput.setFocusable(true);
            mIDCardNoInput.setText("");
            mIDCardNoInput.setTextColor(Color.BLACK);
            mIDCardNoBtn.setEnabled(true);
            mIDCardNoBtn.setText("确定");
            CameraActivityData.idcardfdv_IDCardNoReady = false;
        }
        else{
            mIdcardPhoto.setVisibility(View.VISIBLE);
            activity.findViewById(R.id.idcardno_input_layout).setVisibility(View.INVISIBLE);
        }
    }

    public void setIDCardNoBtnEnabled(boolean enabled)
    {
        mIDCardNoBtn.setEnabled(enabled);
    }

    public void clearIDCardNoInputFocus(){
        mIDCardNoInput.clearFocus();
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
                CameraActivity activity = mActivity.get();
                if(activity == null)
                    return;

                mCameraImage.setImageDrawable(
                        activity.getResources().getDrawable(R.drawable.icon_camera)
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
                CameraActivity activity = mActivity.get();
                if(activity == null)
                    return;

                mIdcardPhoto.setImageDrawable(
                        activity.getResources().getDrawable(R.drawable.icon_photo)
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
                CameraActivity activity = mActivity.get();
                if(activity == null)
                    return;

                mResultIcon.setImageDrawable(activity.getResources().getDrawable(R.drawable.icon_right));
                mResultIcon.setVisibility(View.VISIBLE);
            }
        });
    }

    public void setResultIconNotPass(){
        mResultIcon.post(new Runnable(){
            @Override
            public void run() {
                CameraActivity activity = mActivity.get();
                if(activity == null)
                    return;

                mResultIcon.setImageDrawable(activity.getResources().getDrawable(R.drawable.icon_wrong));
                mResultIcon.setVisibility(View.VISIBLE);
            }
        });
    }

    public void resetResultIcon(){
        mResultIcon.post(new Runnable(){
            @Override
            public void run() {
                CameraActivity activity = mActivity.get();
                if(activity == null)
                    return;

                mResultIcon.setImageDrawable(activity.getResources().getDrawable(R.drawable.icon_wrong));
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
            CameraActivity activity = mActivity.get();
            if(activity == null)
                return;

            AlertDialog.Builder dialog = new AlertDialog.Builder(activity);
            dialog.setTitle("设定相似度阈值");
            final EditText edit = new EditText(activity);
            edit.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            edit.setWidth(240);
            edit.setGravity(Gravity.CENTER);
            edit.setFilters( new InputFilter[]{ new InputFilter.LengthFilter( 4 )});
            edit.setTextSize(24);
            TextView textView = new TextView(activity);
            textView.setText("%");
            textView.setTextSize(24);

            LinearLayout layout = new LinearLayout(activity);
            layout.setOrientation(LinearLayout.HORIZONTAL);
            layout.setGravity(Gravity.CENTER);
            layout.addView(edit);
            layout.addView(textView);

            dialog.setView(layout);
            dialog.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    CameraActivity activity = mActivity.get();
                    if(activity == null)
                        return;

                    WorkUtils.startBrightnessWork(activity);

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
                    CameraActivity activity = mActivity.get();
                    if(activity == null)
                        return;
                    WorkUtils.startBrightnessWork(activity);
                }
            });

            AlertDialog dlg = dialog.create();
            dlg.show();


            WorkUtils.keepBright(activity);
        }
    }

}
