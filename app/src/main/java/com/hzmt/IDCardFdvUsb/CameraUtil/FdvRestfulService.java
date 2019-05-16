package com.hzmt.IDCardFdvUsb.CameraUtil;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FdvRestfulService extends Service {

    private AsyncHttpServer server = new AsyncHttpServer();

    private IDCardInfos mInfos;
    private String mResult = "";

    private int mRequestResult = CameraActivityData.RESULT_NONE;

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
        mInfos = new IDCardInfos();
        server.get("/retrieveidfvinfo",new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                Map<String, String> info_map = new HashMap<>();

                info_map.put("name", mInfos.name);
                info_map.put("issuing_authority", mInfos.issuing_authority);
                info_map.put("idcard_photo", mInfos.idcard_photo);
                info_map.put("birthdate", mInfos.birthdate);
                info_map.put("sex", mInfos.sex);
                info_map.put("idcard_issuedate", mInfos.idcard_issuedate);
                info_map.put("idcard_expiredate", mInfos.idcard_expiredate);
                info_map.put("idcard_id", mInfos.idcard_id);
                info_map.put("ethnicgroup", mInfos.ethnicgroup);
                info_map.put("address", mInfos.address);

                info_map.put("verification_result", mResult);
                String uuidStr = UUID.randomUUID().toString();
                info_map.put("uuid", uuidStr);
                info_map.put("err_code", "0");
                info_map.put("err_msg", "success");

                JSONObject resultJSON = new JSONObject(info_map);
                response.send(resultJSON.toString());
            }
        });

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
                        info_map.put("err_code", "200");
                        info_map.put("err_msg", "success");
                    }
                    else if(mRequestResult == CameraActivityData.RESULT_NOT_PASS) {
                        info_map.put("verification_result", "failure");
                        info_map.put("err_code", "200");
                        info_map.put("err_msg", "success");
                    }
                    else {
                        info_map.put("verification_result", "");
                        info_map.put("err_code", "500");
                        info_map.put("err_msg", "验证未成功执行！");
                    }
                }

                JSONObject resultJSON = new JSONObject(info_map);
                response.send(resultJSON.toString());
            }
        });

        server.listen(8010);

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

    public void setResult(boolean pass) {
        if(pass)
            mResult = "pass";
        else
            mResult = "failure";
    }

    public void setRequestResult(int ret) {
        mRequestResult = ret;
    }

    public class LocalBinder extends Binder {
        public FdvRestfulService getService() {
            return FdvRestfulService.this;
        }
    }
}
