package com.hzmt.IDCardFdvUsb.util;

import android.content.Context;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class ConfigUtil {
    public static String ConfigFileName = null;
    public static final String KEY_POLICE = "police";
    public static final String KEY_DEVICE_ID = "deviceID";
    public static final String KEY_HX_REG_ADDRESS = "HX_RegAddress";
    public static final String KEY_HX_DEVICE_ID = "HX_DeviceID";
    public static final String KEY_HX_STATION_ID = "HX_StationID";

    private static String mCfgVal_police = "武汉市公安局";
    private static String mCfgVal_deviceID = "";
    private static String mCfgVal_HXRegAddress = "";
    private static String mCfgVal_HXDeviceID = "";
    private static String mCfgVal_HXStationID = "";

    public static void initConfigFile(Context context){
        try {
            ConfigFileName = context.getExternalFilesDir(null).getAbsolutePath()
                    + File.separator + "config.txt";
        } catch(NullPointerException e){
            return;
        }

        mCfgVal_police = "武汉市公安局";
        mCfgVal_deviceID = "";
        mCfgVal_HXRegAddress = "";
        mCfgVal_HXDeviceID = "";
        mCfgVal_HXStationID = "";

        writeConfigFile();
    }

    public static void readConfigFile(Context context){
        if(context == null)
            return;

        if(ConfigFileName == null) {
            try {
                ConfigFileName = context.getExternalFilesDir(null).getAbsolutePath()
                        + File.separator + "config.txt";
            } catch(NullPointerException e){
                return;
            }
        }

        try {
            File cfgfile = new File(ConfigFileName);
            if(!cfgfile.exists()) {
                initConfigFile(context);
                return; // 初始化文件时各变量已赋值
            }
            FileInputStream inputStream = new FileInputStream(cfgfile);
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
            String lineTxt = null;
            while ((lineTxt = br.readLine()) != null) {
                String[] names = lineTxt.split(":",2);

                if (names.length >= 2) {
                    if (names[0].charAt(0) == '\uFEFF')  // unicode编码处理
                        names[0] = names[0].substring(1);

                    if (names[0].equals(KEY_POLICE))
                        mCfgVal_police = names[1];

                    if (names[0].equals(KEY_DEVICE_ID))
                        mCfgVal_deviceID = names[1];

                    if (names[0].equals(KEY_HX_REG_ADDRESS))
                        mCfgVal_HXRegAddress = names[1];

                    if (names[0].equals(KEY_HX_DEVICE_ID))
                        mCfgVal_HXDeviceID = names[1];

                    if (names[0].equals(KEY_HX_STATION_ID))
                        mCfgVal_HXStationID = names[1];
                }
            }
            br.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void writeConfigFile(){
        if(ConfigFileName == null)
            return;

        try {
            File cfgfile = new File(ConfigFileName);
            FileOutputStream outputStream = new FileOutputStream(cfgfile);
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(outputStream));
            bw.write(KEY_POLICE + ":" + mCfgVal_police + "\r\n");
            bw.write(KEY_DEVICE_ID + ":"+mCfgVal_deviceID + "\r\n");
            //bw.newLine();
            bw.write(KEY_HX_REG_ADDRESS + ":" + mCfgVal_HXRegAddress + "\r\n");
            bw.write(KEY_HX_DEVICE_ID + ":"+mCfgVal_HXDeviceID + "\r\n");
            bw.write(KEY_HX_STATION_ID + ":"+mCfgVal_HXStationID + "\r\n");
            bw.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getValue(String key){
        String target = null;
        if(key.equals(KEY_POLICE))
            target = mCfgVal_police;
        else if(key.equals(KEY_DEVICE_ID))
            target = mCfgVal_deviceID;
        else if(key.equals(KEY_HX_REG_ADDRESS))
            target = mCfgVal_HXRegAddress;
        else if(key.equals(KEY_HX_DEVICE_ID))
            target = mCfgVal_HXDeviceID;
        else if(key.equals(KEY_HX_STATION_ID))
            target = mCfgVal_HXStationID;

        return target;
    }

    public static void setValue(String key, String value){
        if(key.equals(KEY_POLICE))
            mCfgVal_police = value;
        else if(key.equals(KEY_DEVICE_ID))
            mCfgVal_deviceID = value;
        else if(key.equals(KEY_HX_REG_ADDRESS))
            mCfgVal_HXRegAddress = value;
        else if(key.equals(KEY_HX_DEVICE_ID))
            mCfgVal_HXDeviceID = value;
        else if(key.equals(KEY_HX_STATION_ID))
            mCfgVal_HXStationID = value;
    }
}
