package com.hzmt.IDCardFdvUsb;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.hzmt.IDCardFdvUsb.util.AppUtils;

public class ServicesRunCheckReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if(!AppUtils.isServiceRunning(context,"com.hzmt.idcardfdvupload.UploadSrv")) {
            Intent uploadIntent = new Intent();
            ComponentName cn = new ComponentName("com.hzmt.idcardfdvupload", "com.hzmt.idcardfdvupload.UploadSrv");
            uploadIntent.setComponent(cn);
            uploadIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startService(uploadIntent);
        }

        if(!AppUtils.isServiceRunning(context,"com.hzmt.idcardfdvupgrade.UpgradeSrv")) {
            Intent upgradeIntent = new Intent();
            ComponentName cn2 = new ComponentName("com.hzmt.idcardfdvupgrade", "com.hzmt.idcardfdvupgrade.UpgradeSrv");
            upgradeIntent.setComponent(cn2);
            upgradeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startService(upgradeIntent);
        }
    }
}
