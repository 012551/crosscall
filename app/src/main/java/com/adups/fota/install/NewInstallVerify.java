package com.adups.fota.install;

import android.content.Context;
import android.text.TextUtils;

import com.adups.fota.config.Setting;
import com.adups.fota.config.Status;
import com.adups.fota.query.QueryRootVerify;
import com.adups.fota.query.QueryVersion;
import com.adups.fota.utils.DeviceInfoUtil;
import com.adups.fota.utils.EncryptUtil;
import com.adups.fota.utils.LogUtil;
import com.adups.fota.utils.PreferencesUtils;

import java.io.File;

public class NewInstallVerify extends BaseInstallVerify {

    @Override
    public int verify(Context context, String zipFilePath, String sha256) {
        LogUtil.d("start");
        File pkgFile = new File(zipFilePath);
        if (!pkgFile.exists()) {
            LogUtil.d("deltaPath is null");
            return Status.UPDATE_STATUS_UNZIP_ERROR;
        }
        LogUtil.d("zipFilePath = " + zipFilePath + ",server sha256  = " + sha256);
        String sh256File = EncryptUtil.sha256File(zipFilePath);
        PreferencesUtils.putString(context, "sha", sh256File);
        if (TextUtils.isEmpty(sh256File)) {
            LogUtil.d("get file sha256 error!");
            return Status.UPDATE_STATUS_SH256_ERROR;
        }
        LogUtil.d("file sha256  = " + sh256File);
        if (!sh256File.equalsIgnoreCase(sha256)) {
            InstallResult.setVerifiedRecord(context);
            LogUtil.d("sha256 is different");
            return Status.UPDATE_STATUS_CKSUM_ERROR;
        }
        LogUtil.d("sha256 is equal");
        if (!QueryVersion.getInstance().isFullUpdate(context)) {
            String rootResult = QueryRootVerify.isRomDamaged(zipFilePath);
            LogUtil.d("isRomDamaged  result = " + rootResult);
            if (!TextUtils.isEmpty(rootResult)) {
                LogUtil.d("rom  are damaged ");
                setReason(rootResult);
                PreferencesUtils.putBoolean(context, Setting.FOTA_ROM_DAMAGED, true);
                PreferencesUtils.putString(context, Setting.FOTA_ROM_DAMAGED_VERSION, DeviceInfoUtil.getInstance().getLocalVersion());
                if (PreferencesUtils.getInt(context, Setting.FOTA_UPGRADE, 0) == 1) {
                    LogUtil.d("rom  are damaged, upgrade == 1  ");
                    return Status.UPDATE_STATUS_ROM_DAMAGED;
                }
            }
        }
        LogUtil.d(" finish  success");
        return Status.UPDATE_STATUS_OK;
    }

}


