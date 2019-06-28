package com.hzmt.IDCardFdvUsb.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.view.View;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.text.Spannable;
import android.text.Selection;

import com.hzmt.IDCardFdvUsb.MyApplication;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by xun on 2017/10/18.
 */

public class IdcardFdvRegister {
    public static Boolean nowRegistering = false;

    private static String ProductSn = null;
    private static String RegisteredNo = null;
    private static String SecretKey = null;

    private static final String REG_FILE = "IDCardFdvReg.txt";

    public static String getProductSn(){
        return ProductSn;
    }

    public static String getRegisterdNo(){
        return RegisteredNo;
    }

    public static String getSecretKey(){
        return SecretKey;
    }

    private static void onFinishRegister(){
        nowRegistering = false;
    }

    public interface RegisterCallBack{
        void onSuccess(String sn, String regno, String secretkey);
        void onFailure(int errno);
    }

    public static class RegisterManager extends AsyncTask<Void, Void, Void> {
        private String urlstring = null;
        private InputStream certstream = null;
        private RegisterCallBack regcallback = null;
        private Context regcontext = null;
        private String defaultSn = null;

        private AlertDialog registerDialog = null;
        private ProgressDialog waitingDialog = null;

        public void setRegisterUrl(String url){
            urlstring = url;
        }
        public void setCertStream(InputStream stream){
            certstream = stream;
        }

        public void setRegisterCallBack(RegisterCallBack cb){
            regcallback = cb;
        }

        @Override
        protected Void doInBackground(Void... params) {
            return null;
        }

        @Override
        protected void onPostExecute(Void v) {
            AlertDialog.Builder dialog = new AlertDialog.Builder(regcontext);
            dialog.setTitle("注册人脸验证接口");
            TextView textView = new TextView(regcontext);
            textView.setText("请输入序列号:");
            textView.setTextSize(16);

            final EditText edit = new EditText(regcontext);
            //edit.setInputType();
            //DisplayMetrics dm2 = context.getResources().getDisplayMetrics();
            edit.setWidth(550);
            edit.setTextSize(14);
            if(null != defaultSn) {
                edit.setText(defaultSn);
                CharSequence text = edit.getText();
                if (text instanceof Spannable) {
                    Spannable spanText = (Spannable)text;
                    Selection.setSelection(spanText, text.length());
                }
            }

            LinearLayout layout = new LinearLayout(regcontext);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.addView(textView);
            layout.addView(edit);

            waitingDialog = new ProgressDialog(regcontext);

            layout.setPadding(100, 20, 100, 20);
            dialog.setView(layout);
            dialog.setPositiveButton("确认", null);
            dialog.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if(null != regcallback)
                        regcallback.onFailure(0);
                    nowRegistering = false;
                }
            });

            dialog.setCancelable(false);
            registerDialog = dialog.create();
            registerDialog.setCanceledOnTouchOutside(false);
            registerDialog.show();

            registerDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ProductSn = edit.getText().toString();
                            if(ProductSn.equals("")) {
                                Toast.makeText(regcontext, "空序列号！", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            postProductSn(ProductSn);
                            waitingDialog.setTitle("正在注册");
                            waitingDialog.setMessage("请稍候...");
                            waitingDialog.setIndeterminate(true);
                            waitingDialog.setCancelable(false);
                            waitingDialog.show();
                            registerDialog.dismiss();
                        }
                    });
        }

        public void register(Context context, String defaultSn){
            nowRegistering = true;
            regcontext = context;
            this.defaultSn = defaultSn;
            this.execute();
        }

        private void postProductSn(String productsn){
            RegisterHttpsThead work = new RegisterHttpsThead(regcontext, certstream,
                                                        waitingDialog, regcallback);
            work.execute(urlstring, productsn);
        }
    }


    public static boolean checkRegister(Context context){
        // read register info file
        try {
            FileInputStream inputStream = context.openFileInput(REG_FILE);
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
            ProductSn = null;
            RegisteredNo = null;
            SecretKey = null;
            String lineTxt = null;
            while ((lineTxt = br.readLine()) != null) {
                String[] names = lineTxt.split(":");
                if(names.length >=2 ){
                    if(names[0].equals("ProductSn"))
                        ProductSn = AESUtils.decrypt(names[1],MyApplication.config_password);
                    if(names[0].equals("RegisteredNo"))
                        RegisteredNo = AESUtils.decrypt(names[1],MyApplication.config_password);
                    if(names[0].equals("SecretKey"))
                        SecretKey = AESUtils.decrypt(names[1],MyApplication.config_password);
                }
            }
            br.close();

            if(null == ProductSn || ProductSn.equals("")){
                return false;
            }
            if(null == RegisteredNo || RegisteredNo.equals("")){
                return false;
            }
            if(null == SecretKey || SecretKey.equals("")){
                return false;
            }
        } catch (FileNotFoundException e) {
           // e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public static void clearRegisterInfo(Context context){
        saveRegisterInfo(context, "", "","");
    }

    private static void saveRegisterInfo(Context context, String prosn, String regno, String secretkey){
        String crypt_prosn = AESUtils.encrypt(prosn, MyApplication.config_password);
        String crypt_regno = AESUtils.encrypt(regno, MyApplication.config_password);
        String crypt_secretkey = AESUtils.encrypt(secretkey, MyApplication.config_password);

        try{
            FileOutputStream outputStream = context.openFileOutput(REG_FILE,
                    Activity.MODE_PRIVATE);
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(outputStream));
            bw.write("ProductSn:"+crypt_prosn);
            bw.newLine();
            bw.write("RegisteredNo:"+crypt_regno);
            bw.newLine();
            bw.write("SecretKey:"+crypt_secretkey);
            bw.newLine();
            bw.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class RegisterHttpsThead extends AsyncTask<String, Void, JSONObject> {
        private String urlstring = null;
        private Context regcontext = null;
        private InputStream certstream = null;
        private RegisterCallBack regcallback = null;

        private ProgressDialog waitingDialog = null;

        // for reuse certstream
        ByteArrayOutputStream baos = null;
        public RegisterHttpsThead(Context regcontext,
                                  InputStream certstream,
                                  ProgressDialog waitingDialog,
                                  RegisterCallBack regcallback){

            this.regcontext = regcontext;
            this.certstream = certstream;
            this.waitingDialog = waitingDialog;
            this.regcallback = regcallback;
        }


        @Override
        protected JSONObject doInBackground(String... params) {
            String url = params[0];
            this.urlstring = url;
            final String productsn = params[1];

            JSONObject object = null;

            Map<String, String> map = new HashMap<>();
            String shaSrc ="";
            String tempdata;

            tempdata = "10022546";
            map.put("appId", tempdata);
            shaSrc += tempdata;

            tempdata = "NGRkZGFhZDAwMDAwOThlZTky";
            map.put("apiKey", tempdata);
            shaSrc += tempdata;

            tempdata = "ZTlmMjU2ODk1MTE4NGM3NGEyYWQ3ZDM4";
            //map.put("secretKey", "ZTlmMjU2ODk1MTE4NGM3NGEyYWQ3ZDM4");
            shaSrc += tempdata;

            tempdata = SystemUtil.getMacAddress();
            map.put("MacId", tempdata);
            shaSrc += tempdata;

            tempdata = UUID.randomUUID().toString();
            map.put("uuid", tempdata);
            shaSrc += tempdata;

            tempdata = productsn;
            map.put("productsn", tempdata);
            shaSrc += tempdata;

            tempdata = SystemUtil.shaEncrypt(shaSrc);
            map.put("checksum", tempdata);

            object = new JSONObject(map);


            InputStream certstream_cpy = null;
            try {
                if(certstream != null) {
                    if(null == baos)
                        baos = new ByteArrayOutputStream();

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

            JSONObject resultJSON;
            if(null != certstream_cpy)
                resultJSON = HttpsUtil.JsonObjectRequest(certstream_cpy,object,url);
            else
                resultJSON = HttpUtil.JsonObjectRequest(object,url);

            return resultJSON;
        }

        @Override
        protected void onPostExecute(JSONObject resultJSON) {
            String msg = "error!";
            int err_no = 503;
            if( null != resultJSON){
                try{
                    err_no = resultJSON.getInt("Err_no");
                    if(err_no != 0){
                        if(err_no == 404)
                            msg = "该序列号已被注册！";
                        else if(err_no == 403)
                            msg = "无效的序列号！";
                        else// if(err_no == 503)
                            msg = "注册失败！";
                    }
                    else{
                        msg = "注册成功！";
                        RegisteredNo = resultJSON.getString("RegisteredNo");
                        if(resultJSON.has("SecretKey"))
                            SecretKey = resultJSON.getString("SecretKey");
                        else
                            SecretKey = "ZTlmMjU2ODk1MTE4NGM3NGEyYWQ3ZDM4";
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            else{
                msg = "注册服务无响应！";
            }

            waitingDialog.dismiss();
            final AlertDialog.Builder dialog =
                    new AlertDialog.Builder(regcontext);
            dialog.setTitle("Error");
            if(err_no == 0)
                dialog.setTitle("Success");
            dialog.setMessage(msg);
            final int err_no_cpy = err_no;
            dialog.setPositiveButton("确定",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //...To-do
                            if(err_no_cpy != 0){
                                // redo
                                RegisterManager manager = new RegisterManager();
                                manager.setRegisterUrl(urlstring);
                                InputStream newstream = null;
                                if(null != baos)
                                    newstream = new ByteArrayInputStream(baos.toByteArray());
                                manager.setCertStream(newstream);
                                manager.setRegisterCallBack(regcallback);
                                manager.register(regcontext,ProductSn);
                            }
                            else{
                                // save register info and continue
                                saveRegisterInfo(regcontext, ProductSn, RegisteredNo,SecretKey);
                                if(null != regcallback){
                                    regcallback.onSuccess(ProductSn, RegisteredNo,SecretKey);
                                }

                                onFinishRegister(); // clear
                            }
                        }
                    });
            dialog.setCancelable(false);
            AlertDialog resultDialog = dialog.create();
            resultDialog.setCanceledOnTouchOutside(false);
            resultDialog.show();
        }
    }
}
