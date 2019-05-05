package com.hzmt.IDCardFdvUsb.CameraUtil;

import android.app.Activity;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.hzmt.IDCardFdvUsb.R;

public class DebugLayout {
    private Activity mActivity;
    private LinearLayout mMainLayout;

    private TextView mDebugText;

    //构造函数
    DebugLayout(Activity activity) {
        this.mActivity = activity;
        mMainLayout = mActivity.findViewById(R.id.debug_layout);
        ViewGroup.LayoutParams mainLP = mMainLayout.getLayoutParams();
        mainLP.width = (int) (CameraActivityData.CameraActivity_width * 0.3);

        mDebugText = mActivity.findViewById(R.id.debug_text);
        mDebugText.setMovementMethod(ScrollingMovementMethod.getInstance());

        if(CameraActivityData.DEBUG_INFO)
            mMainLayout.setVisibility(View.VISIBLE);
        else
            mMainLayout.setVisibility(View.INVISIBLE);
    }

    public void setText(final CharSequence text){
        if(!CameraActivityData.DEBUG_INFO)
            return;
        mDebugText.post(new Runnable(){
            @Override
            public void run() {
                mDebugText.setText(text);
                int offset=mDebugText.getLineCount()*mDebugText.getLineHeight();
                if(offset>(mDebugText.getHeight()-mDebugText.getLineHeight())){
                    mDebugText.scrollTo(0,offset-mDebugText.getHeight()+mDebugText.getLineHeight());
                }
            }
        });
    }

    public void addText(final CharSequence text){
        if(!CameraActivityData.DEBUG_INFO)
            return;
        mDebugText.post(new Runnable(){
            @Override
            public void run() {
                mDebugText.append(text);
                int offset=mDebugText.getLineCount()*mDebugText.getLineHeight();
                if(offset>(mDebugText.getHeight()-mDebugText.getLineHeight())){
                    mDebugText.scrollTo(0,offset-mDebugText.getHeight()+mDebugText.getLineHeight());
                }
            }
        });
    }
}
