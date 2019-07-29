package com.hzmt.IDCardFdvUsb;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.hzmt.IDCardFdvUsb.CameraUtil.CameraActivity;

public class BootCompletedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            Intent newIntent = new Intent(context, CameraActivity.class);
            newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(newIntent);
        }
    }
}
