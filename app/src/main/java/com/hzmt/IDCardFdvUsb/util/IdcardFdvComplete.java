package com.hzmt.IDCardFdvUsb.util;

import android.util.Log;

import com.hzmt.IDCardFdvUsb.CameraUtil.IDCardInfos;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 上传已完成验证的图片数据
 */

public class IdcardFdvComplete {

    public static void request(final String urlstring,
                                final String serial_no,
                                final IDCardInfos idcard_infos,
                                final String idcard_photo,
                                final String verify_photo,
                                final InputStream certstream,
                                final RequestCallBack callback){

        new Thread(){
            @Override
            public void run(){
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

                    tempdata = IdcardFdvRegister.getSecretKey();//"ZTlmMjU2ODk1MTE4NGM3NGEyYWQ3ZDM4";
                    //map.put("secretKey", "ZTlmMjU2ODk1MTE4NGM3NGEyYWQ3ZDM4");
                    shaSrc += tempdata;

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

                    tempdata = IdcardFdvRegister.getRegisterdNo();
                    map.put("RegisteredNo", tempdata);
                    shaSrc += tempdata;

                    tempdata = idcard_infos.idcard_id; //idcard_id;
                    map.put("idcard_id", tempdata);
                    shaSrc += tempdata;

                    tempdata = idcard_infos.idcard_issuedate; //idcard_issuedate;
                    map.put("idcard_issuedate", tempdata);
                    shaSrc += tempdata;

                    tempdata = serial_no;
                    map.put("Serial_No", tempdata);
                    shaSrc += tempdata;

                    tempdata = idcard_photo;
                    map.put("idcard_photo", tempdata);
                    shaSrc += tempdata;

                    JSONArray jsonArray = new JSONArray();
                    jsonArray.put(verify_photo);
                    shaSrc += verify_photo;

                    tempdata = SystemUtil.shaEncrypt(shaSrc);
                    map.put("checksum", tempdata);
                    object = new JSONObject(map);
                    object.put("verify_photos", jsonArray);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                JSONObject resultJSON;
                if(null != certstream)
                    resultJSON = HttpsUtil.JsonObjectRequest(certstream,object,urlstring);
                else
                    resultJSON = HttpUtil.JsonObjectRequest(object,urlstring);

                Log.i("IdcardFdvComplete", resultJSON.toString());
                if(null != callback){
                    if(null != resultJSON)
                        callback.onSuccess(resultJSON);
                    else
                        callback.onFailure(0);
                }
            }
        }.start();
    }

    public interface RequestCallBack{
        void onSuccess(JSONObject object);
        void onFailure(int errno);
    }
}
