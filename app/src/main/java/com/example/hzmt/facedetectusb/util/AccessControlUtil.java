package com.example.hzmt.facedetectusb.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Arrays;

import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Created by xun on 2018/3/22.
 */

public class AccessControlUtil {
    public static void OpenDoor(String urlstring, String sn){
        Pattern pa = Pattern.compile("(\\d+\\.\\d+\\.\\d+\\.\\d+)\\:(\\d+)");
        Matcher ma = pa.matcher(urlstring);
        String ip, port;
        if(ma.find()){
            ip = ma.group(1);
            port = ma.group(2);
        }
        else {
            return;
        }

        AccessControlThread acThread = new AccessControlThread(
                                        ip,
                                        Integer.parseInt(port),
                                        Integer.parseInt(sn));
        acThread.start();
    }


    private static class AccessControlThread extends Thread {
        private String ip = null;
        private int port = 0;
        private int sn;

        public AccessControlThread(String ip, int port, int sn){
            this.ip = ip;
            this.port = port;
            this.sn = sn;
        }

        @Override
        public void run() {
            InetAddress address = null;
            try {
                address = InetAddress.getByName(this.ip);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }

            DatagramSocket socket = null;
            try {
                socket = new DatagramSocket();
            } catch (SocketException e) {
                e.printStackTrace();
            }

            byte[] data = getData(this.sn);
            DatagramPacket packet = new DatagramPacket(data, 64, address,
                    this.port);

            try {
                socket.send(packet);
                socket.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private byte[] getData(int sn) {
            byte[] data = new byte[64];
            Arrays.fill(data, (byte)0);
            data[0] = 0x17;
            data[1] = 0x40;

            data[4] = (byte)(sn & 0xFF);
            data[5] = (byte)((sn >> 8) & 0xFF);
            data[6] = (byte)((sn >> 16) & 0xFF);
            data[7] = (byte)((sn >> 24) & 0xFF);

            data[8] = 0x01; // door 01

            return data;
        }
    }

}
