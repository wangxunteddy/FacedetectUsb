package com.hzmt.IDCardFdvUsb.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.media.FaceDetector;
import android.os.AsyncTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import android.content.Context;

import com.hzmt.IDCardFdvUsb.MyApplication;
//import android.util.Log;


/**
 * Created by xun on 2017/10/15.
 */

public class IdcardFdv {
    public static void request( Context context,
                                int requestType,
                                String urlstring,
                                String idcard_id,
                                String idcard_issuedate,
                                String idcard_photo,
                                String verify_photo,
                                InputStream certstream,
                                RequestCallBack cb){

        Pattern pa = Pattern.compile("(\\d+\\.\\d+\\.\\d+\\.\\d+)\\:(\\d+)");
        Matcher ma = pa.matcher(urlstring);
        String ip,port;
        if(ma.find()){
            ip = ma.group(1);
            port = ma.group(2);
        }
        else {
            if(null != cb )
                cb.onFailure(1);
            return;
        }

      /*  JSONObject resultJSON = null;
        Map<String, String> ret_map = new HashMap<>();
        ret_map.put("Err_no", "0");
        ret_map.put("Err_msg", "test return msg");
        resultJSON = new JSONObject(ret_map);
        if(null != cb ){
            cb.onSuccess(resultJSON);
            return;
        }
        */


        //IdcardFdvRegister.clearRegisterInfo(context);
        if(IdcardFdvRegister.nowRegistering) {
            if(null != cb )
                cb.onFailure(2);
            return;
        }

        String regno = IdcardFdvRegister.getRegisterdNo();
        //if(IdcardFdvRegister.checkRegister(context)){
        if(regno != null && !regno.equals("")){
            String sn = IdcardFdvRegister.getProductSn();
            HttpsThread th = new HttpsThread(urlstring,
                    requestType,
                    idcard_id, idcard_issuedate,
                    sn, regno,
                    idcard_photo,verify_photo,
                    certstream, cb);

            th.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,(Void)null);
        }
        else{
            // register
            IdcardFdvRegister.RegisterManager manager = new IdcardFdvRegister.RegisterManager();
            String registerUrl;
            if(certstream != null)
                registerUrl = "https://" + ip + ":" + port + "/registerproduct";
            else
                registerUrl = "http://" + ip + ":" + port + "/registerproduct";
            manager.setRegisterUrl(registerUrl);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            InputStream certstream_cpy = null;
            try {
                if(certstream != null) {
                    int size = certstream.available();
                    byte[] buffer = new byte[size];
                    int len;
                    while ((len = certstream.read(buffer)) > -1) {
                        baos.write(buffer, 0, len);
                    }
                    baos.flush();

                    certstream_cpy = new ByteArrayInputStream(baos.toByteArray());
                }
            } catch (Exception e){
                e.printStackTrace();
            }
            manager.setCertStream(certstream_cpy);

            final String urlstring_p = urlstring;
            final int requestType_p = requestType;
            final String idcard_id_p = idcard_id;
            final String idcard_issuedate_p = idcard_issuedate;
            final String idcard_photo_p = idcard_photo;
            final String verify_photo_p = verify_photo;
            final InputStream certstream_p = new ByteArrayInputStream(baos.toByteArray());
            final RequestCallBack cb_p = cb;
            IdcardFdvRegister.RegisterCallBack regcallback = new IdcardFdvRegister.RegisterCallBack(){
                @Override
                public void onSuccess(String sn, String regno){
                    HttpsThread th = new HttpsThread(urlstring_p,
                            requestType_p,
                            idcard_id_p, idcard_issuedate_p,
                            sn,regno,
                            idcard_photo_p,verify_photo_p,
                            certstream_p, cb_p);

                    th.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,(Void)null);
                }

                @Override
                public void onFailure(int errno){
                    if(null != cb_p)
                        cb_p.onFailure(errno);
                }
            };
            manager.setRegisterCallBack(regcallback);

            manager.register(context, requestType,null);

            return;
        }
    }

    public interface RequestCallBack{
        void onSuccess(JSONObject object);
        void onFailure(int errno);
    }


    private static class HttpsThread extends AsyncTask<Void, Void, JSONObject> {
        private String urlstring;
        private int requestType;
        private String idcard_id;
        private String idcard_issuedate;
        private String productsn;
        private String registerno;
        private String idcard_photo;
        private String verify_photo;
        private InputStream certstream;
        private RequestCallBack callback;

        public HttpsThread(String urlstring,
                           int    requestType,
                           String idcard_id,
                           String idcard_issuedate,
                           String productsn,
                           String registerno,
                           String idcard_photo,
                           String verify_photo,
                           InputStream certstream,
                           RequestCallBack callback){
            this.urlstring = urlstring;
            this.requestType = requestType;
            this.idcard_id = idcard_id;
            this.idcard_issuedate = idcard_issuedate;
            this.productsn = productsn;
            this.registerno = registerno;
            this.idcard_photo = idcard_photo;
            this.verify_photo = verify_photo;
            this.certstream = certstream;
            this.callback = callback;
        }


        @Override
        protected JSONObject doInBackground(Void... params){
            JSONObject object = null;
            try {
                Map<String, String> map = new HashMap<>();
                String shaSrc ="";
                String tempdata;
                if(0 == requestType) {
                    // image fdv
                    tempdata = "10022245";
                    map.put("appId", tempdata);
                    shaSrc += tempdata;

                    tempdata = "MGRhNjEyYWExOTdhYzYxNTkx";
                    map.put("apiKey", tempdata);
                    shaSrc += tempdata;

                    tempdata = "NzQyNTg0YmZmNDg3OWFjMTU1MDQ2YzIw";
                    //map.put("secretKey", "NzQyNTg0YmZmNDg3OWFjMTU1MDQ2YzIw");
                    shaSrc += tempdata;
                }
                else{ // else if(1 == requestType) {
                    // feat fdv
                    tempdata = "10022546";
                    map.put("appId", tempdata);
                    shaSrc += tempdata;

                    tempdata = "NGRkZGFhZDAwMDAwOThlZTky";
                    map.put("apiKey", tempdata);
                    shaSrc += tempdata;

                    tempdata = "ZTlmMjU2ODk1MTE4NGM3NGEyYWQ3ZDM4";
                    //map.put("secretKey", "ZTlmMjU2ODk1MTE4NGM3NGEyYWQ3ZDM4");
                    shaSrc += tempdata;
                }

                tempdata = SystemUtil.getMacAddress();
                map.put("MacId", tempdata);
                shaSrc += tempdata;

                String uuidStr = UUID.randomUUID().toString();
                tempdata = uuidStr;
                map.put("uuid", tempdata);
                shaSrc += tempdata;

                //tempdata = productsn;
                //map.put("productsn", tempdata);
                //shaSrc += tempdata;

                tempdata = registerno;
                map.put("RegisteredNo", tempdata);
                shaSrc += tempdata;

                tempdata = idcard_id;
                map.put("idcard_id", tempdata);
                shaSrc += tempdata;

                tempdata = idcard_issuedate;
                map.put("idcard_issuedate", tempdata);
                shaSrc += tempdata;

                if(0 == requestType) {
                    // image fdv
                    tempdata = idcard_photo;
                    map.put("idcard_photo", tempdata);
                    shaSrc += tempdata;
                } else { // else if(1 == requestType) {
                    // feat fdv
                    tempdata = idcard_photo;
                    map.put("idcard_face_info", tempdata);
                    shaSrc += tempdata;
                }

                JSONArray jsonArray = new JSONArray();
                jsonArray.put(verify_photo);
                shaSrc += verify_photo;

                if(jsonArray.length() == 0){
                    JSONObject resultJSON = null;
                    Map<String, String> ret_map = new HashMap<>();
                    ret_map.put("uuid", uuidStr);
                    ret_map.put("Err_no", "103");
                    ret_map.put("Err_msg", "No qualified face in the verify photos.");
                    resultJSON = new JSONObject(ret_map);
                    if(null != callback ){
                        callback.onSuccess(resultJSON);
                    }
                    return null; // finish the thread
                }
                // List<String> list = new ArrayList<String>();
                // for(int i=0;i<verify_photos.length;i++)
                //     list.add(verify_photos[i]);
                // for (String s : list) {
                //     jsonArray.put(s);
                // }
                tempdata = SystemUtil.shaEncrypt(shaSrc);
                map.put("checksum", tempdata);
                object = new JSONObject(map);
                if( 0 == requestType)
                    object.put("verify_photos", jsonArray);
                else //  if( 1 == requestType)
                    object.put("verify_face_infos", jsonArray);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            //===================
            // test code
            MyApplication.idcardfdvStepCnt2 = System.currentTimeMillis() - MyApplication.idcardfdvStepCnt;
            //===================
            JSONObject resultJSON;
            if(null != certstream)
                resultJSON = HttpsUtil.JsonObjectRequest(certstream,object,urlstring);
            else
                resultJSON = HttpUtil.JsonObjectRequest(object,urlstring);

            return resultJSON;
            //Log.e("resultJSON:",resultJSON.toString());

        }

        @Override
        protected void onPostExecute(JSONObject resultJSON) {
            if(null != callback){
                if(null != resultJSON)
                    callback.onSuccess(resultJSON);
                else
                    callback.onFailure(3);
            }
        }
    }



    private static Bitmap cropBitmapByFaceDetector(Bitmap origin){
        if(null == origin)
            return null;

        // FaceDetector
        Bitmap bmcopy = origin.copy(Bitmap.Config.RGB_565, true); // 必须为RGB_565
        FaceDetector faceDetector = new FaceDetector(bmcopy.getWidth(),
                bmcopy.getHeight(), 1);
        FaceDetector.Face[] faces = new FaceDetector.Face[1];
        int faceNum = faceDetector.findFaces(bmcopy, faces);
        if(faceNum > 0) {
            PointF pointF = new PointF();
            faces[0].getMidPoint(pointF);//获取人脸中心点
            float eyesDistance = faces[0].eyesDistance();//获取人脸两眼的间距
            eyesDistance *= 2.0f;
            int maxX = origin.getWidth();
            int maxY = origin.getHeight();
            int l = (int) (pointF.x - eyesDistance);
            if (l < 0) l = 0;
            int t = (int) (pointF.y - eyesDistance);
            if (t < 0) t = 0;
            int r = (int) (pointF.x + eyesDistance);
            if (r > maxX) r = maxX;
            int b = (int) (pointF.y + eyesDistance);
            if (b > maxY) b = maxY;
            Bitmap newBM = Bitmap.createBitmap(origin, l, t, r-l, b-t, null, false);
            if(!newBM.equals(origin) )
                origin.recycle();

            return newBM;
        }
        else{
            return null;
        }
    }

    private static Bitmap getBitmapFromBytes(byte[] data, int samplesize){

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = samplesize; //
        Bitmap bm = BitmapFactory.decodeByteArray(data, 0, data.length,options);
        //Matrix matrix = new Matrix();

        Bitmap nbmp = Bitmap.createBitmap(bm,
                0, 0, bm.getWidth(),  bm.getHeight(), null, true);

        return nbmp;
    }
}
