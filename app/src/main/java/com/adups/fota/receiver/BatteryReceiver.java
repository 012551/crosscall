package com.adups.fota.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;
import android.os.Build;
import android.os.PowerManager;
import android.text.TextUtils;

import com.adups.fota.config.Status;
import com.adups.fota.install.Install;
import com.adups.fota.report.ReportData;
import com.adups.fota.utils.DeviceInfoUtil;
import com.adups.fota.utils.LogUtil;

public class BatteryReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null) {
            String action = intent.getAction();
            if (!TextUtils.isEmpty(action)) {
                int status = Status.getVersionStatus(context);
                LogUtil.d("status = " + status + ",action = " + action);
                if (status == Status.STATE_DL_PKG_COMPLETE && action.equalsIgnoreCase(Intent.ACTION_BATTERY_CHANGED)) {
                    PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                    boolean isScreenOn = false;
                    if (powerManager != null) {
                        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT)
                            isScreenOn = powerManager.isInteractive();
                        else
                            isScreenOn = powerManager.isScreenOn();
                    }
                    int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);//得到系统当前电量
                    LogUtil.d("battery level = " + level + ",isScreenOn = " + isScreenOn);
                    if (level >= DeviceInfoUtil.getInstance().getBattery() && !isScreenOn) {//当电量大于30%且灭屏时触发
                        ReportData.postInstall(context, ReportData.INSTALL_STATUS_AUTO);
                        Install.update(context);
                    }
                }
            }
        }
    }

}
