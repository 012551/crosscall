package com.adups.fota.install;

import android.content.Context;
import android.os.PowerManager;
import android.text.TextUtils;

import com.adups.fota.config.Const;
import com.adups.fota.config.Setting;
import com.adups.fota.config.Status;
import com.adups.fota.query.QueryRootVerify;
import com.adups.fota.query.QueryVersion;
import com.adups.fota.utils.DeviceInfoUtil;
import com.adups.fota.utils.FileUtil;
import com.adups.fota.utils.LogUtil;
import com.adups.fota.utils.PreferencesUtils;
import com.adups.fota.utils.StorageUtil;

import java.io.File;

public class OldInstallVerify extends BaseInstallVerify {

    @Override
    public int verify(Context context, String zipFilePath, String md5Code) {
        LogUtil.d("start");
        boolean isSuccess = FileUtil.renameFile(
                StorageUtil.getPackageFileName(context),
                StorageUtil.getPackagePathName(context) + "/package.zip");
        if (!isSuccess) {
            LogUtil.d("rename fail");
            return Status.UPDATE_FOTA_RENAME_FAIL;
        }
        LogUtil.d("rename success");
        File pkgFile = new File(StorageUtil.getPackagePathName(context) + "/package.zip");
        String deltaPath = pkgFile.getParent();
        if (deltaPath == null) {
            LogUtil.d("deltaPath  null ");
            return Status.UPDATE_FOTA_NO_PKG;
        }
        PowerManager manager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = null;
        if (manager != null)
            wakeLock = manager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "fota:unzipwakeup");
        if (wakeLock != null)
            wakeLock.acquire(10 * 60 * 1000L /*10 minutes*/);
        boolean result = FileUtil.unZipFile(pkgFile, deltaPath);
        if (wakeLock != null) {
            wakeLock.release();
        }
        if (!result) {
            LogUtil.d("unzip  fail ");
            return Status.UPDATE_STATUS_UNZIP_ERROR;
        }
        LogUtil.d("unzip  success ");
        String md5File = FileUtil.getFileMD5(deltaPath + Const.PACKAGE_NAME);
        String md5 = FileUtil.getMD5sum(deltaPath + "/md5sum");
        if (md5File == null) {
            return Status.UPDATE_FOTA_PKG_MD5_FAIL;
        } else if (!md5File.equalsIgnoreCase(md5)) {
            InstallResult.setVerifiedRecord(context);
            return Status.UPDATE_STATUS_CKSUM_ERROR;
        }
        LogUtil.d("md5Encode equal");
        FileUtil.writeMD5File(context, deltaPath);
        if (!QueryVersion.getInstance().isFullUpdate(context)) {
            String rootResult = QueryRootVerify.isRomDamaged(deltaPath + Const.PACKAGE_NAME);
            LogUtil.d("isRomDamaged  result = " + rootResult);
            if (!TextUtils.isEmpty(rootResult)) {
                setReason(rootResult);
                PreferencesUtils.putBoolean(context, Setting.FOTA_ROM_DAMAGED, true);
                PreferencesUtils.putString(context, Setting.FOTA_ROM_DAMAGED_VERSION, DeviceInfoUtil.getInstance().getLocalVersion());
                LogUtil.d("rom  are damaged  ");
                if (PreferencesUtils.getInt(context, Setting.FOTA_UPGRADE, 0) == 1) {
                    LogUtil.d("rom  are damaged, upgrade == 1  ");
                    return Status.UPDATE_STATUS_ROM_DAMAGED;
                }
            }
        }
        LogUtil.d("finish  success");
        return Status.UPDATE_STATUS_OK;
    }

}


