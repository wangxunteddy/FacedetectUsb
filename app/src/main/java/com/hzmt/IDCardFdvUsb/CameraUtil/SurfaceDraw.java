package com.hzmt.IDCardFdvUsb.CameraUtil;

import android.content.Context;
import android.util.AttributeSet;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.view.SurfaceView;
import android.view.SurfaceHolder;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.PorterDuff.Mode;

/**
 * Created by xun on 2017/8/24.
 */

public class SurfaceDraw extends SurfaceView implements SurfaceHolder.Callback{
    protected SurfaceHolder mSurfaceHolder;
    private CustomThread mCustomThread;

    private Rect mFaceRect = new Rect(0,0,0,0);
    private Bitmap mFacePic = null;
    private Boolean mbDrawRequest = false;
    private Boolean mbDrawFaceRect = false;
    private Boolean mbDrawFacePic = false;

    public SurfaceDraw(Context context, AttributeSet attrs){
        super(context, attrs);

        setZOrderOnTop(true);
        setZOrderMediaOverlay(true);
        mSurfaceHolder = getHolder();
        mSurfaceHolder.setFormat(PixelFormat.TRANSPARENT);
        mSurfaceHolder.addCallback(this);

        mCustomThread = null;
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
       //String s=String.format("w:%d,h:%d",width,height);
       // Log.e("surfaceChanged", s);
       // ShowToastUtils.showToast(getContext(), s, Toast.LENGTH_LONG);
    }

    /**
    *  在 Surface 首次创建时被立即调用：获得焦点时。一般在这里开启画图的线程
    * @param surfaceHolder 持有当前 Surface 的 SurfaceHolder 对象
    */
    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        mCustomThread = new CustomThread(surfaceHolder);
        mCustomThread.start();
    }

    /**
    *  在 Surface 被销毁时立即调用：失去焦点时。一般在这里将画图的线程停止销毁
    * @param surfaceHolder 持有当前 Surface 的 SurfaceHolder 对象
    */
    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        mCustomThread.close();
        mCustomThread = null;
    }


    class CustomThread extends Thread {
        private SurfaceHolder surfaceHolder;
        public boolean canRun;

        public CustomThread(SurfaceHolder surfaceHolder) {
            this.surfaceHolder = surfaceHolder;
            canRun = true;
        }

        @Override
        public void run() {

            while (canRun) {
                Canvas canvas = null;
                if(mbDrawRequest) {
                    //线程同步
                    synchronized (surfaceHolder) {
                        canvas = surfaceHolder.lockCanvas();
                        if (canvas != null) {
                            canvas.drawColor(Color.TRANSPARENT, Mode.CLEAR);
                            Paint p = new Paint();

                            if(mbDrawFaceRect) {
                                //p.setAntiAlias(true);
                                p.setColor(Color.BLUE);
                                p.setStyle(Style.STROKE);
                                p.setStrokeWidth(5);
                                //canvas.drawPoint(100.0f, 100.0f, p);
                                //canvas.drawRect(200, 200, 500, 500, p);
                                if (mFaceRect.width() > 0 && mFaceRect.height() > 0)
                                    canvas.drawRect(mFaceRect, p);

                                //canvas.drawText(".895648", 100,100,p);
                                mbDrawRequest = false;
                                mbDrawFaceRect = false;
                            }

                            if(mbDrawFacePic){
                                Rect srcRect = new Rect(0,0,
                                        mFacePic.getWidth(),mFacePic.getHeight());
                                Rect destRect = new Rect(0,0,
                                        mFacePic.getWidth(),mFacePic.getHeight());
                                canvas.drawBitmap(mFacePic,srcRect,destRect,null);
                                mbDrawRequest = false;
                            }
                            surfaceHolder.unlockCanvasAndPost(canvas);
                        }
                    }

                    try {
                        //  Thread.sleep(1000);
                    } catch (Exception e) {
                    }
                }
            }

        }

        private void clearCanvas(){
            Canvas canvas = null;
            //线程同步
            synchronized (surfaceHolder) {
                canvas = surfaceHolder.lockCanvas();
                if (canvas != null) {
                    canvas.drawColor(Color.TRANSPARENT, Mode.CLEAR);
                    surfaceHolder.unlockCanvasAndPost(canvas);
                }
            }
        }

        private void close(){
            canRun = false;
        }
    }

    public void setFaceRect(int left, int top, int right, int bottom){
        mFaceRect.set(left,top,right,bottom);
        mbDrawRequest = true;
        mbDrawFaceRect = true;
    }

    public void setFacePic(Bitmap bm){
        mFacePic = bm;
        mbDrawRequest = true;
        mbDrawFacePic = true;
    }

 //   public void setLayout(int width, int height){
 //       ViewGroup.LayoutParams lp = getLayoutParams();
 //       lp.width = width;
 //       lp.height = height;
 //       setLayoutParams(lp);
 //   }
}
