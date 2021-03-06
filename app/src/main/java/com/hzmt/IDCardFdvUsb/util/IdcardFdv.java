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

//import com.fasterxml.uuid.Generators;
//import com.fasterxml.uuid.EthernetAddress;
import com.hzmt.IDCardFdvUsb.CameraUtil.IDCardInfos;
import com.hzmt.IDCardFdvUsb.CameraUtil.WorkUtils;
import com.hzmt.IDCardFdvUsb.MyApplication;
//import android.util.Log;



/**
 * Created by xun on 2017/10/15.
 */

public class IdcardFdv {
    public static void request( Context context,
                                int requestType,
                                String urlstring,
                         //       String idcard_id,
                         //       String idcard_issuedate,
                                IDCardInfos idcard_infos,
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
            String secretkey = IdcardFdvRegister.getSecretKey();
            HttpsThread th = new HttpsThread(urlstring,
                    requestType,
             //       idcard_id, idcard_issuedate,
                    idcard_infos,
                    sn, regno,secretkey,
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

            final Context context_p = context;
            final String urlstring_p = urlstring;
            final int requestType_p = requestType;
         //   final String idcard_id_p = idcard_id;
         //   final String idcard_issuedate_p = idcard_issuedate;
            final IDCardInfos idcard_infos_p = idcard_infos;
            final String idcard_photo_p = idcard_photo;
            final String verify_photo_p = verify_photo;
            final InputStream certstream_p = new ByteArrayInputStream(baos.toByteArray());
            final RequestCallBack cb_p = cb;
            IdcardFdvRegister.RegisterCallBack regcallback = new IdcardFdvRegister.RegisterCallBack(){
                @Override
                public void onSuccess(String sn, String regno, String secretkey){
                    HttpsThread th = new HttpsThread(urlstring_p,
                            requestType_p,
                     //       idcard_id_p, idcard_issuedate_p,
                            idcard_infos_p,
                            sn,regno,secretkey,
                            idcard_photo_p,verify_photo_p,
                            certstream_p, cb_p);

                    th.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,(Void)null);

                    // 广播IP
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            WorkUtils.reportIPChange(context_p);
                        }
                    }).start();
                }

                @Override
                public void onFailure(int errno){
                    if(null != cb_p)
                        cb_p.onFailure(errno);
                }
            };
            manager.setRegisterCallBack(regcallback);

            manager.register(context, null);

            return;
        }
    }

    public interface RequestCallBack{
        void onSuccess(JSONObject object);
        void onFailure(int errno);
    }


    private static class HttpsThread extends AsyncTask<Void, Void, JSONObject> {
        //Context context;
        private String urlstring;
        private int requestType;
        //private String idcard_id;
        //private String idcard_issuedate;
        private IDCardInfos idcard_infos;
        private String productsn;
        private String registerno;
        private String secretkey;
        private String idcard_photo;
        private String verify_photo;
        private InputStream certstream;
        private RequestCallBack callback;

        public HttpsThread(//Context context,
                           String urlstring,
                           int  requestType,
                           //String idcard_id,
                           //String idcard_issuedate,
                           IDCardInfos idcard_infos,
                           String productsn,
                           String registerno,
                           String secretkey,
                           String idcard_photo,
                           String verify_photo,
                           InputStream certstream,
                           RequestCallBack callback){
            //this.context=context;
            this.urlstring = urlstring;
            this.requestType = requestType;
            //this.idcard_id = idcard_id;
            //this.idcard_issuedate = idcard_issuedate;
            this.idcard_infos = idcard_infos;
            this.productsn = productsn;
            this.registerno = registerno;
            this.secretkey = secretkey;
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

                tempdata = "10022546";
                map.put("appId", tempdata);
                shaSrc += tempdata;

                tempdata = "NGRkZGFhZDAwMDAwOThlZTky";
                map.put("apiKey", tempdata);
                shaSrc += tempdata;

                tempdata = this.secretkey;//"ZTlmMjU2ODk1MTE4NGM3NGEyYWQ3ZDM4";
                //map.put("secretKey", "ZTlmMjU2ODk1MTE4NGM3NGEyYWQ3ZDM4");
                shaSrc += tempdata;

                tempdata = SystemUtil.getMacAddress();
                map.put("MacId", tempdata);
                shaSrc += tempdata;

                String uuidStr = UUID.randomUUID().toString();
                //String uuidStr = Generators.timeBasedGenerator(EthernetAddress.fromInterface()).generate();
                tempdata = uuidStr;
                map.put("uuid", tempdata);
                shaSrc += tempdata;

                //tempdata = productsn;
                //map.put("productsn", tempdata);
                //shaSrc += tempdata;

                tempdata = registerno;
                map.put("RegisteredNo", tempdata);
                shaSrc += tempdata;

                tempdata = idcard_infos.idcard_id; //idcard_id;
                map.put("idcard_id", tempdata);
                shaSrc += tempdata;

                tempdata = idcard_infos.idcard_issuedate; //idcard_issuedate;
                map.put("idcard_issuedate", tempdata);
                shaSrc += tempdata;

                // 其他身份证信息
                map.put("name", B64Util.stringToBase64(idcard_infos.name));
                map.put("issuing_authority", B64Util.stringToBase64(idcard_infos.issuing_authority));
                map.put("birthdate", idcard_infos.birthdate);
                map.put("sex", B64Util.stringToBase64(idcard_infos.sex));
                map.put("idcard_expiredate", B64Util.stringToBase64(idcard_infos.idcard_expiredate));
                map.put("ethnicgroup", B64Util.stringToBase64(idcard_infos.ethnicgroup));
                map.put("address", B64Util.stringToBase64(idcard_infos.address));
                // 护照阅读器10E的信息
                if(!idcard_infos.ReadedCardType.equals("")){
                    map.put("ReadedCardType", B64Util.stringToBase64(idcard_infos.ReadedCardType));
                    map.put("oriimage", idcard_infos.oriimage);
                    map.put("RFIDMRZ", idcard_infos.RFIDMRZ);
                    map.put("LocalName", B64Util.stringToBase64(idcard_infos.LocalName));
                    map.put("OCRMRZ", idcard_infos.RFIDMRZ);
                    map.put("EngName", idcard_infos.EngName);
                    map.put("POBPinyin", idcard_infos.POBPinyin);
                    map.put("IssuePlacePinyin", idcard_infos.IssuePlacePinyin);
                    map.put("DOB", idcard_infos.DOB);
                    map.put("IDCardNo", idcard_infos.IDCardNo);
                    map.put("PassportMRZ", idcard_infos.PassportMRZ);
                    map.put("ValidDate", idcard_infos.ValidDate);
                    map.put("IssueState", idcard_infos.IssueState);
                    map.put("SelfDefineInfo", B64Util.stringToBase64(idcard_infos.SelfDefineInfo));
                    map.put("EngSurname", idcard_infos.EngSurname);
                    map.put("POB", B64Util.stringToBase64(idcard_infos.POB));
                    map.put("EngFirstname", idcard_infos.EngFirstname);
                    map.put("CardNoMRZ", idcard_infos.CardNoMRZ);
                    map.put("MRZ1", idcard_infos.MRZ1);
                    map.put("CardNo", idcard_infos.CardNo);
                    map.put("MRZ2", idcard_infos.MRZ2);
                    map.put("CardType", B64Util.stringToBase64(idcard_infos.CardType));
                    map.put("Nationality", idcard_infos.Nationality);
                    map.put("ChnName", B64Util.stringToBase64(idcard_infos.ChnName));
                    map.put("PassportNo", idcard_infos.PassportNo);
                    map.put("ValidDateTo", B64Util.stringToBase64(idcard_infos.ValidDateTo));
                    map.put("BirthPlace", B64Util.stringToBase64(idcard_infos.BirthPlace));
                    map.put("MRZ3", idcard_infos.MRZ3);
                    map.put("ExchangeCardTimes", idcard_infos.ExchangeCardTimes);
                    map.put("FprInfo", B64Util.stringToBase64(idcard_infos.FprInfo));
                }

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
                    object.put("verify_faces", jsonArray);
                else //  if( 1 == requestType)
                    object.put("verify_face_infos", jsonArray);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            //idcard_infos.dump(context);
            //String dumpstr = object.toString();
            //SystemUtil.outputString2File(context,dumpstr);
            //===================
            // test code
            MyApplication.idcardfdvStepCnt2 = System.currentTimeMillis() - MyApplication.idcardfdvStepCnt;
            MyApplication.idcardfdvStepCnt = System.currentTimeMillis();
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
            if (r > maxX - 1) r = maxX -1;
            int b = (int) (pointF.y + eyesDistance);
            if (b >= maxY -1) b = maxY - 1;
            Bitmap newBM = Bitmap.createBitmap(origin, l, t, r-l+1, b-t+1, null, false);
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
