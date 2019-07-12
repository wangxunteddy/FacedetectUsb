package com.hzmt.IDCardFdvUsb.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.Build;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by xun on 2017/9/11.
 */

public class SystemUtil {
    public static String getMacAddress(){
        String macAddress = null;
        StringBuffer buf = new StringBuffer();
        NetworkInterface networkInterface = null;
        try {
            networkInterface = NetworkInterface.getByName("eth1");
            if (networkInterface == null) {
                networkInterface = NetworkInterface.getByName("wlan0");
            }
            if (networkInterface == null) {
                return "02:00:00:00:00:02";
            }
            byte[] addr = networkInterface.getHardwareAddress();
            for (byte b : addr) {
                buf.append(String.format("%02X:", b));
            }
            if (buf.length() > 0) {
                buf.deleteCharAt(buf.length() - 1);
            }
            macAddress = buf.toString();
        } catch (SocketException e) {
            e.printStackTrace();
            return "02:00:00:00:00:02";
        }
        return macAddress;
    }


    /**
     * 优先获取以太网IP，其次wifi，否则null
     *
     */
    public static String getIpAddress(Context context){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ConnectivityManager connectivityManager = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            Network[] networks = connectivityManager.getAllNetworks();
            String ipRet = null;
            for(Network nw:networks){
                NetworkInfo networkInfo = connectivityManager.getNetworkInfo(nw);
                if (networkInfo != null && networkInfo.isAvailable()) {
                    LinkProperties properties = connectivityManager
                            .getLinkProperties(nw);

                    if (properties != null) {
                        String ipString = properties.getLinkAddresses().toString();
                        Pattern pattern = Pattern.compile("\\d+.\\d+.\\d+.\\d+");
                        Matcher matcher = pattern.matcher(ipString);
                        if (matcher.find()) {
                            if(networkInfo.getType() == ConnectivityManager.TYPE_ETHERNET)
                                return matcher.group();
                            else if(networkInfo.getType() == ConnectivityManager.TYPE_WIFI)
                                ipRet = matcher.group();
                        }
                    }
                }
            }
            return ipRet;
        }
        else
            return null;
    }

    /**
     * 按比例缩放图片
     *
     * @param origin 原图
     * @param ratio  比例
     * @return 新的bitmap
     */
    public static Bitmap scaleBitmap(Bitmap origin, float ratio, boolean recycle) {
        if (origin == null) {
            return null;
        }
        int width = origin.getWidth();
        int height = origin.getHeight();
        Matrix matrix = new Matrix();
        matrix.preScale(ratio, ratio);
        Bitmap newBM = Bitmap.createBitmap(origin, 0, 0, width, height, matrix, false);
        if (newBM.equals(origin)) {
            return newBM;
        }
        if(recycle)
            origin.recycle();
        return newBM;
    }

    /**
     * SHA加密
     *
     * @param strSrc
     *            明文
     * @return 加密之后的密文
     */
    public static String shaEncrypt(String strSrc) {
        MessageDigest md = null;
        String strDes = null;
        byte[] bt = strSrc.getBytes();
        try {
            md = MessageDigest.getInstance("SHA-256");// 将此换成SHA-1、SHA-512、SHA-384等参数
            md.update(bt);
            strDes = bytes2Hex(md.digest()); // to HexString
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
        return strDes;
    }

    /**
     * byte数组转换为16进制字符串
     *
     * @param bts
     *            数据源
     * @return 16进制字符串
     */
    public static String bytes2Hex(byte[] bts) {
        String des = "";
        String tmp = null;
        for (int i = 0; i < bts.length; i++) {
            tmp = (Integer.toHexString(bts[i] & 0xFF));
            if (tmp.length() == 1) {
                des += "0";
            }
            des += tmp;
        }
        return des;
    }

    /**
     * UDP广播
     *
     */
    public static void sendUDPBrocast(byte[] data, int port){
        DatagramSocket ms = null;
        DatagramPacket dataPacket = null;
        try {
            InetAddress address = InetAddress.getByName("255.255.255.255");
            ms = new DatagramSocket();
            //System.out.println(address.isMulticastAddress());
            dataPacket = new DatagramPacket(data, data.length, address,
                    port);
            ms.send(dataPacket);
            //ms.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (ms != null)
                ms.close();
        }
    }

}
