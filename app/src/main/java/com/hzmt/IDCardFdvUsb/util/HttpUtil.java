package com.hzmt.IDCardFdvUsb.util;

import android.util.Log;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by xun on 2017/8/31.
 */

public class HttpUtil {

    public static JSONObject JsonObjectRequest(JSONObject data, String urlstring){
        InputStream inputStream = null;
        HttpURLConnection urlConnection = null;
        JSONObject resultJSON = null;
        try{
            URL url = new URL(urlstring);
            urlConnection = (HttpURLConnection) url.openConnection();
            // 设置连接超时时间
            urlConnection.setConnectTimeout(5 * 1000);
            //设置从主机读取数据超时
            urlConnection.setReadTimeout(5 * 1000);

            /* optional request header */
            urlConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            urlConnection.setRequestMethod("POST");
            // Post请求必须设置允许输出 默认false
            urlConnection.setDoOutput(true);
            //设置请求允许输入 默认是true
            urlConnection.setDoInput(true);
            //使用Post方式不能使用缓存
            urlConnection.setUseCaches(false);
            //urlConnection.connect();
            DataOutputStream dos = new DataOutputStream(urlConnection.getOutputStream());
            dos.writeBytes(data.toString());
            dos.flush();
            dos.close();
            // try to get response
            int statusCode = urlConnection.getResponseCode();
            if (statusCode == 200) {
                inputStream = urlConnection.getInputStream();
                String result = streamToString(inputStream);
                //Log.e("JsonObjectRequest", result);
                resultJSON = new JSONObject(result);
            }
        }catch(Exception e) {
            //Log.e("JsonObjectRequest", e.toString());
            e.printStackTrace();
        }
        finally
        {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (urlConnection != null) {
                urlConnection.disconnect();
            }

            return resultJSON;
        }
    }

    /**
     * 将输入流转换成字符串
     *
     * @param is 从网络获取的输入流
     * @return
     */
    private static String streamToString(InputStream is) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len = 0;
            while ((len = is.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
            baos.close();
            is.close();
            byte[] byteArray = baos.toByteArray();
            return new String(byteArray);
        } catch (Exception e) {
            Log.e("streamToString", e.toString());
            return null;
        }
    }
}
