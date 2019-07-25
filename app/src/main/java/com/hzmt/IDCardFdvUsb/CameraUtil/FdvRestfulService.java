package com.hzmt.IDCardFdvUsb.CameraUtil;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.hzmt.IDCardFdvUsb.util.B64Util;
import com.hzmt.IDCardFdvUsb.util.SystemUtil;
import com.koushikdutta.async.http.Headers;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.KeyStore;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

public class FdvRestfulService extends Service {

    private AsyncHttpServer server = new AsyncHttpServer();
    private AsyncHttpServer server2 = new AsyncHttpServer();
    final private String SRV2_VER = "Ver10";

    private IDCardInfos mInfos;
    private String mResult = "";
    private String mRet_faceImg = "";   // 摄像头照片
    private String mRet_pic = "";       // 身份证照片
    private String mRet_compareValue = "";

    private int mRequestResult = CameraActivityData.RESULT_NONE;
    private String mRequestResultMsg = null;
    private String mReqRet_faceImg = "";
    private String mReqRet_pic = "";
    private String mReqRet_checkFlag = "";
    private String mReqRet_compareValue = "";

    public IBinder onBind(Intent intent)
    {
        return new LocalBinder();
    }

    @Override
    public boolean onUnbind(Intent intent)
    {
        return super.onUnbind(intent);
    }


    @Override
    public void onCreate() {
        //SSLContext sslContext = getSLLContext();
        mInfos = new IDCardInfos();
        server.get("/retrieveidfvinfo",new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                Map<String, String> info_map = new HashMap<>();

                info_map.put("name", mInfos.name);
                info_map.put("issuing_authority", mInfos.issuing_authority);
                //info_map.put("idcard_photo", mInfos.idcard_photo);
                info_map.put("birthdate", mInfos.birthdate);
                info_map.put("sex", mInfos.sex);
                info_map.put("idcard_issuedate", mInfos.idcard_issuedate);
                info_map.put("idcard_expiredate", mInfos.idcard_expiredate);
                info_map.put("idcard_id", mInfos.idcard_id);
                info_map.put("ethnicgroup", mInfos.ethnicgroup);
                info_map.put("address", mInfos.address);

                info_map.put("idcard_photo", mRet_pic);
                info_map.put("onsitepicture", mRet_faceImg);
                info_map.put("compareValue", mRet_compareValue);
                info_map.put("verification_result", mResult);
                String uuidStr = UUID.randomUUID().toString();
                info_map.put("uuid", uuidStr);
                info_map.put("err_code", "0");
                info_map.put("err_msg", "success");

                JSONObject resultJSON = new JSONObject(info_map);
                String retStr = resultJSON.toString().replace("\\/", "/"); // 去转义字符

                Headers headers = response.getHeaders();
                headers.set("Access-Control-Allow-Origin","*"); // 允许跨域

                response.send(retStr);
            }
        });

				
				/* // 不开放
        server.post("/faceverification",new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                Map<String, String> info_map = new HashMap<>();
                IDCardInfos info = new IDCardInfos();

                String noneinfo = "";
                try {
                    JSONObject requestJSON = (JSONObject)request.getBody().get();

                    if(requestJSON.has("uuid"))
                        info_map.put("uuid", requestJSON.getString("uuid"));
                    else{
                        info_map.put("uuid", "");
                        noneinfo = "uuid";
                    }

                    if(requestJSON.has("idcard_issuedate"))
                        info.idcard_issuedate = requestJSON.getString("idcard_issuedate");
                    else
                        noneinfo = "idcard_issuedate";
                    if(requestJSON.has("idcard_id")) {
                        info.idcard_id = requestJSON.getString("idcard_id");
                        info_map.put("idcard_id", info.idcard_id);
                    }
                    else {
                        info_map.put("idcard_id", "");
                        noneinfo = "idcard_id";
                    }
                    if(requestJSON.has("idcard_photo"))
                        info.idcard_photo = requestJSON.getString("idcard_photo");
                    else
                        noneinfo = "idcard_photo";

                    if(requestJSON.has("name"))
                        info.name = requestJSON.getString("name");
                    if(requestJSON.has("issuing_authority"))
                        info.issuing_authority = requestJSON.getString("issuing_authority");
                    if(requestJSON.has("birthdate"))
                        info.birthdate = requestJSON.getString("birthdate");
                    if(requestJSON.has("sex"))
                        info.sex = requestJSON.getString("sex");
                    if(requestJSON.has("idcard_expiredate"))
                        info.idcard_expiredate = requestJSON.getString("idcard_expiredate");
                    if(requestJSON.has("ethnicgroup"))
                        info.ethnicgroup = requestJSON.getString("ethnicgroup");
                    if(requestJSON.has("address"))
                        info.address = requestJSON.getString("address");
                } catch (JSONException e){
                    e.printStackTrace();
                }

                String fdv_result = "";
                if(!noneinfo.equals("")){
                    info_map.put("verification_result", fdv_result);
                    info_map.put("err_code", "400");
                    info_map.put("err_msg", "错误请求!缺少字段"+noneinfo);
                }
                else{
                    mRequestResult = CameraActivityData.RESULT_NONE;
                    Intent intent = new Intent("com.hzmt.IDCardFdvUsb.FDV_REQUEST");
                    //intent.setComponent(); //Android 8.0 need it
                    intent.putExtra("person_data",info);
                    sendBroadcast(intent);

                    // 等待结果
                    while(mRequestResult == CameraActivityData.RESULT_NONE){
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e){
                            e.printStackTrace();
                        }
                    }

                    if(mRequestResult == CameraActivityData.RESULT_PASS) {
                        info_map.put("verification_result", "pass");
                        info_map.put("err_code", "0"); // 200
                        info_map.put("err_msg", "success");
                    }
                    else if(mRequestResult == CameraActivityData.RESULT_NOT_PASS) {
                        info_map.put("verification_result", "failure");
                        info_map.put("err_code", "0"); // 200
                        info_map.put("err_msg", "success");
                    }
                    else {
                        info_map.put("verification_result", "");
                        info_map.put("err_code", "500");
                        info_map.put("err_msg", "验证未成功执行！");
                    }
                }

                JSONObject resultJSON = new JSONObject(info_map);
                String retStr = resultJSON.toString().replace("\\/", "/"); // 去转义字符
                response.send(retStr);
            }
        });
        //*/


        server.listen(8010);

        server2.post("/"+SRV2_VER+"/Face_Instruct",new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                Map<String, String> info_map = new HashMap<>();

                IDCardInfos info = new IDCardInfos();

                String noneinfo = "";
                boolean flag10E = false;
                try {
                    JSONObject requestJSON = (JSONObject)request.getBody().get();

                    if(requestJSON.has("instructType")) {
                        String instructType = requestJSON.getString("instructType");
                        if(instructType.equals("10E"))
                            flag10E = true;
                    }
                    else
                        noneinfo = "instructType";

                    if(requestJSON.has("pic")) {
                        String b64str = requestJSON.getString("pic");
                        if(!b64str.contains("data:image") && !b64str.contains("base64,")){
                            b64str = "data:image/png;base64,"+b64str;
                        }
                        info.idcard_photo = b64str;
                    }
                    else
                        noneinfo = "pic";

                    // 10E护照阅读器信息
                    if(requestJSON.has("oriimage"))
                        info.oriimage = requestJSON.getString("oriimage");

                    if(requestJSON.has("cardType"))
                        info.ReadedCardType = URLDecoder.decode(requestJSON.getString("cardType"),"utf-8");
                    if(requestJSON.has("cardName"))
                        info.name = URLDecoder.decode(requestJSON.getString("cardName"),"utf-8");//二代证通用信息
                    if(requestJSON.has("RFIDMRZ"))
                        info.RFIDMRZ = URLDecoder.decode(requestJSON.getString("RFIDMRZ"),"utf-8");
                    if(requestJSON.has("LocalName"))
                        info.LocalName = URLDecoder.decode(requestJSON.getString("LocalName"),"utf-8");
                    if(requestJSON.has("OCRMRZ"))
                        info.OCRMRZ = URLDecoder.decode(requestJSON.getString("OCRMRZ"),"utf-8");
                    if(requestJSON.has("EngName"))
                        info.EngName = URLDecoder.decode(requestJSON.getString("EngName"),"utf-8");
                    if(requestJSON.has("POBPinyin"))
                        info.POBPinyin = URLDecoder.decode(requestJSON.getString("POBPinyin"),"utf-8");
                    if(requestJSON.has("Gender"))
                        info.sex = URLDecoder.decode(requestJSON.getString("Gender"),"utf-8");//二代证通用信息
                    if(requestJSON.has("IssuePlacePinyin"))
                        info.IssuePlacePinyin = URLDecoder.decode(requestJSON.getString("IssuePlacePinyin"),"utf-8");
                    if(requestJSON.has("DOB"))
                        info.DOB = URLDecoder.decode(requestJSON.getString("DOB"),"utf-8");
                    if(requestJSON.has("IDCardNo"))
                        info.IDCardNo = URLDecoder.decode(requestJSON.getString("IDCardNo"),"utf-8");
                    if(requestJSON.has("Birthday"))
                        info.birthdate = URLDecoder.decode(requestJSON.getString("Birthday"),"utf-8");//二代证通用信息
                    if(requestJSON.has("Minzu"))
                        info.ethnicgroup = URLDecoder.decode(requestJSON.getString("Minzu"),"utf-8");//二代证通用信息
                    if(requestJSON.has("PassportMRZ"))
                        info.PassportMRZ = URLDecoder.decode(requestJSON.getString("PassportMRZ"),"utf-8");
                    if(requestJSON.has("Address"))
                        info.address = URLDecoder.decode(requestJSON.getString("Address"),"utf-8");//二代证通用信息
                    if(requestJSON.has("ValidDate"))
                        info.ValidDate = URLDecoder.decode(requestJSON.getString("ValidDate"),"utf-8");
                    if(requestJSON.has("IDNo")) {
                        info.idcard_id = URLDecoder.decode(requestJSON.getString("IDNo"),"utf-8");//二代证通用信息
                        if(info.idcard_id.equals(""))
                            info.idcard_id = "000000181801010002";
                    }
                    if(requestJSON.has("IssueState"))
                        info.IssueState = URLDecoder.decode(requestJSON.getString("IssueState"),"utf-8");
                    if(requestJSON.has("SelfDefineInfo"))
                        info.SelfDefineInfo = URLDecoder.decode(requestJSON.getString("SelfDefineInfo"),"utf-8");
                    if(requestJSON.has("EngSurname"))
                        info.EngSurname = URLDecoder.decode(requestJSON.getString("EngSurname"),"utf-8");
                    if(requestJSON.has("POB"))
                        info.POB = URLDecoder.decode(requestJSON.getString("POB"),"utf-8");
                    if(requestJSON.has("EngFirstname"))
                        info.EngFirstname = URLDecoder.decode(requestJSON.getString("EngFirstname"),"utf-8");
                    if(requestJSON.has("CardNoMRZ"))
                        info.CardNoMRZ = URLDecoder.decode(requestJSON.getString("CardNoMRZ"),"utf-8");
                    if(requestJSON.has("MRZ1"))
                        info.MRZ1 = URLDecoder.decode(requestJSON.getString("MRZ1"),"utf-8");
                    if(requestJSON.has("CardNo"))
                        info.CardNo = URLDecoder.decode(requestJSON.getString("CardNo"),"utf-8");
                    if(requestJSON.has("MRZ2"))
                        info.MRZ2 = URLDecoder.decode(requestJSON.getString("MRZ2"),"utf-8");
                    if(requestJSON.has("CardType"))
                        info.CardType = URLDecoder.decode(requestJSON.getString("CardType"),"utf-8");
                    if(requestJSON.has("Nationality"))
                        info.Nationality = URLDecoder.decode(requestJSON.getString("Nationality"),"utf-8");
                    if(requestJSON.has("ChnName"))
                        info.ChnName = URLDecoder.decode(requestJSON.getString("ChnName"),"utf-8");
                    if(requestJSON.has("PassportNo"))
                        info.PassportNo = URLDecoder.decode(requestJSON.getString("PassportNo"),"utf-8");
                    if(requestJSON.has("ValidDateTo"))
                        info.ValidDateTo = URLDecoder.decode(requestJSON.getString("ValidDateTo"),"utf-8");
                    if(requestJSON.has("BirthPlace"))
                        info.BirthPlace = URLDecoder.decode(requestJSON.getString("BirthPlace"),"utf-8");
                    if(requestJSON.has("MRZ3"))
                        info.MRZ3 = URLDecoder.decode(requestJSON.getString("MRZ3"),"utf-8");
                    if(requestJSON.has("IssuePlace"))
                        info.issuing_authority = URLDecoder.decode(requestJSON.getString("IssuePlace"),"utf-8");//二代证通用信息
                    if(requestJSON.has("ExchangeCardTimes"))
                        info.ExchangeCardTimes = URLDecoder.decode(requestJSON.getString("ExchangeCardTimes"),"utf-8");
                    if(requestJSON.has("IssueDate")) {
                        String IssueDate = URLDecoder.decode(requestJSON.getString("IssueDate"),"utf-8");
                        String[] dates = IssueDate.split("-");
                        if(dates.length == 2) {
                            info.idcard_issuedate = dates[0];//二代证通用信息
                            info.idcard_expiredate = dates[1];//二代证通用信息
                        }
                    }
                    if(requestJSON.has("FprInfo"))
                        info.FprInfo = URLDecoder.decode(requestJSON.getString("FprInfo"),"utf-8");//二代证信息，但普通模式未使用



                    if(!flag10E)
                        info.idcard_id = "000000181801010002";
                } catch (JSONException e){
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e){
                    e.printStackTrace();
                }

                String fdv_result = "";
                if(!noneinfo.equals("")){
                    info_map.put("err_code", "400");
                    info_map.put("err_msg", "错误请求!缺少字段"+noneinfo);
                }
                else{
                    mRequestResult = CameraActivityData.RESULT_NONE;
                    Intent intent = new Intent("com.hzmt.IDCardFdvUsb.FDV_REQUEST");
                    //intent.setComponent(); //Android 8.0 need it
                    intent.putExtra("person_data",info);
                    sendBroadcast(intent);

                    // 等待结果
                    while(mRequestResult == CameraActivityData.RESULT_NONE){
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e){
                            e.printStackTrace();
                        }
                    }

                    if(mRequestResult == CameraActivityData.RESULT_PASS ||
                            mRequestResult == CameraActivityData.RESULT_NOT_PASS) {
                        info_map.put("faceImg", mReqRet_faceImg);
                        if(!flag10E)
                            info_map.put("pic", mReqRet_pic);
                        info_map.put("checkFlag", mReqRet_checkFlag);
                        info_map.put("compareValue", mReqRet_compareValue);
                    }
                    else {
                        info_map.put("err_code", "500");
                        info_map.put("err_msg", "验证未成功执行！");
                    }
                }

                JSONObject resultJSON = new JSONObject(info_map);
                String retStr = resultJSON.toString().replace("\\/", "/"); // 去转义字符
                retStr = retStr.replace("\\n", ""); // 去换行
                Headers headers = response.getHeaders();
                headers.set("Content-Type","application/json;charset=utf-8");
                response.send(retStr);
            }
        });
        server2.listen(55532);

        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        if (server != null) {
            server.stop();
        }
        if (server2 != null) {
            server.stop();
        }

        mInfos = null;
    }

    public void setIDCardInfos(IDCardInfos srcInfos) {
        try{
            this.mInfos = (IDCardInfos)srcInfos.clone();
        } catch (CloneNotSupportedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void setLastResult(Bitmap camera_photo, Bitmap idcard_photo, double sim, boolean pass) {
        if(pass)
            mResult = "pass";
        else
            mResult = "failure";

        Bitmap s_camera_photo = SystemUtil.scaleBitmap(camera_photo,0.5f,false);
        mRet_faceImg = "data:image/jpeg;base64," + B64Util.bitmapToBase64(s_camera_photo, Bitmap.CompressFormat.JPEG, 50);
        mRet_pic = "data:image/jpeg;base64," + B64Util.bitmapToBase64(idcard_photo, Bitmap.CompressFormat.JPEG);
        DecimalFormat decimalFormat=new DecimalFormat(".00");
        mRet_compareValue = decimalFormat.format(sim);
    }

    public void setRequestResult(String camera_photo,String idcard_photo,
                                 int result, String resultMsg, double sim) {

        if(result == CameraActivityData.RESULT_PASS)
            mReqRet_checkFlag = "1";
        else if(result == CameraActivityData.RESULT_NOT_PASS)
            mReqRet_checkFlag = "0";

        mReqRet_faceImg = camera_photo;
        if(!mReqRet_faceImg.contains("data:image") && !mReqRet_faceImg.contains(";base64,"))
            mReqRet_faceImg = "data:image/jpeg;base64," + mReqRet_faceImg;
        mReqRet_pic = idcard_photo;

        DecimalFormat decimalFormat=new DecimalFormat(".00");
        mReqRet_compareValue = decimalFormat.format(sim);

        mRequestResultMsg = resultMsg;
        mRequestResult = result; // 必须在最后设置，否则前面设置的变量会有读写冲突
    }

    public class LocalBinder extends Binder {
        public FdvRestfulService getService() {
            return FdvRestfulService.this;
        }
    }

    public  SSLContext getSLLContext() {
        SSLContext sslContext = null;
        try {
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("X509");
            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
            ks.load(null, null);


            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            KeyStore ts = KeyStore.getInstance(KeyStore.getDefaultType());
            ts.load(null, null);

            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sslContext;
    }
}
