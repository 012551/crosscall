package com.adups.fota.activity;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.StatFs;
import android.text.Html;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.adups.fota.MyApplication;
import com.adups.fota.R;
import com.adups.fota.bean.EventMessage;
import com.adups.fota.callback.ClickCallback;
import com.adups.fota.config.Const;
import com.adups.fota.config.Event;
import com.adups.fota.config.Setting;
import com.adups.fota.config.Status;
import com.adups.fota.install.Install;
import com.adups.fota.utils.DeviceInfoUtil;
import com.adups.fota.utils.DialogUtil;
import com.adups.fota.utils.LogUtil;
import com.adups.fota.utils.PreferencesUtils;
import com.adups.fota.utils.StorageUtil;
import com.adups.fota.view.TitleView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SdcardUpdateActivity extends BaseActivity {

    private static final int DIALOG_UPDATE_CONFIRM = 2;
    private static final int COPY_FILE_TO_DATA = 11;
    private static final int COPY_FILE_TO_23 = 12;
    private static final int COPY_FILE_TO_DATA_OTA = 13;
    private static final int MSG_REBOOT = 15;
    //error tips message
    private static final int MSG_COPY_ERROR = -2;
    private static ExecutorService mCopyService;
    private static boolean isCopying;
    Button btn_install;
    TextView wv_tips;
    private String selected_file;
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {//定义一个Handler，用于处理下载线程与UI间通讯
            if (!Thread.currentThread().isInterrupted()) {
                switch (msg.what) {
                    case DIALOG_UPDATE_CONFIRM:
                        LogUtil.d("LocalSdUpdate = " + selected_file);
                        File selectFile = new File(selected_file);
                        int sdkVer = Build.VERSION.SDK_INT;
                        if (selectFile.exists()) {
                            LogUtil.d("LocalSdUpdate : selectFile.exists true");
                            if (!selectFile.getName().equals("LocalSdUpdate.zip")) {
                                File recoveryFile = new File(selectFile.getParent() + Const.SD_PACKAGE_NAME);
                                if (sdkVer < Build.VERSION_CODES.M && selectFile.renameTo(recoveryFile)) {
                                    LogUtil.d("rename to " + recoveryFile);
                                    selected_file = selectFile.getPath();
                                }
                            }
                        }
                        LogUtil.d("sdkVer = " + sdkVer);
                        if (sdkVer >= Build.VERSION_CODES.M &&
                                (!StorageUtil.isInnerSdcard(SdcardUpdateActivity.this, selected_file) || selected_file.contains("/emulated/0"))) {//6.0 以上的版本外置T卡的处理
                            if (!isEnough(new File(selected_file).length(), getFileSdcardRoot(selected_file))) {
                                return;
                            }
                            LogUtil.d("23, copy to android/data/...");
                            showCopyFileDialog();
                            if ((Build.VERSION.SDK_INT > Build.VERSION_CODES.O_MR1 && DeviceInfoUtil.getInstance().isSupportAbUpdate()) ||
                                    (Build.VERSION.SDK_INT>=Build.VERSION_CODES.Q && selected_file.contains(StorageUtil.getStoragePath(MyApplication.getAppContext(),false)))) {
                                handler.sendMessageDelayed(handler.obtainMessage(COPY_FILE_TO_DATA_OTA), 100);
                            } else if (selected_file.contains("/emulated/0")) {
                                handler.sendMessageDelayed(handler.obtainMessage(COPY_FILE_TO_DATA), 100);
                            } else {
                                handler.sendMessageDelayed(handler.obtainMessage(COPY_FILE_TO_23), 100);
                            }
                        } else if (sdkVer >= Build.VERSION_CODES.LOLLIPOP) {
                            if (StorageUtil.isInnerSdcard(SdcardUpdateActivity.this, selected_file)) { //不是外置t卡就复制至data
                                if (!isEnough(new File(selected_file).length(), getFileSdcardRoot(selected_file))) {
                                    return;
                                }
                                LogUtil.d("updatePackage, copy to data");
                                showCopyFileDialog();
                                handler.sendMessageDelayed(handler.obtainMessage(COPY_FILE_TO_DATA), 100);
                            } else {
                                update();
                            }
                        } else {
                            update();
                        }
                        break;
                    case -1:
                        DialogUtil.showPositiveDialog(SdcardUpdateActivity.this,
                                getString(R.string.sdCard_upgrade_find_update_file_fail),
                                getString(R.string.sdCard_upgrade_update_file_fail_prompt));
                        break;
                    case MSG_COPY_ERROR:
                        closeDialog();
                        DialogUtil.showPositiveDialog(SdcardUpdateActivity.this,
                                getString(R.string.sdCard_upgrade_copy_file_fail),
                                getString(R.string.sdCard_upgrade_copy_file_fail_prompt));
                        break;
                    case COPY_FILE_TO_DATA:
                        LogUtil.d("LocalSdUpdate : COPY_FILE1_TO_DATA !");
                        mCopyService.execute(new Runnable() {

                            @Override
                            public void run() {
                                try {
                                    new File(SdcardUpdateActivity.this.getFilesDir() + "/adupsfota").mkdirs();
                                } catch (Exception e) {
                                }
                                boolean isCopyOk = copy(selected_file, Const.UPDATE_FILE3, false);
                                if (isCopyOk) {
                                    selected_file = Const.UPDATE_FILE3;
                                    handler.sendMessage(handler.obtainMessage(MSG_REBOOT));
                                } else {
                                    handler.sendMessage(handler.obtainMessage(MSG_COPY_ERROR));
                                }
                            }
                        });
                        break;
                    case COPY_FILE_TO_23:
                        LogUtil.d("LocalSdUpdate : COPY_FILE23_TO_DATA !");
                        mCopyService.execute(new Runnable() {

                            @Override
                            public void run() {
                                String path = StorageUtil.getStoragePath(SdcardUpdateActivity.this, true) + Const.SD_FILE4;
                                LogUtil.d("LocalSdUpdate : path = " + path);
                                boolean isCopyOk = copy(selected_file, path, false);
                                if (isCopyOk) {
                                    selected_file = path;
                                    handler.sendMessage(handler.obtainMessage(MSG_REBOOT));
                                } else {
                                    handler.sendMessage(handler.obtainMessage(MSG_COPY_ERROR));
                                }
                            }
                        });
                        break;
                    case COPY_FILE_TO_DATA_OTA:
                        LogUtil.d("LocalSdUpdate : COPY_FILE_TO_DATA_OTA !");
                        mCopyService.execute(new Runnable() {

                            @Override
                            public void run() {
                                String path = "/data/ota_package" + Const.PACKAGE_NAME;
                                LogUtil.d("LocalSdUpdate : path = " + path);
                                boolean isCopyOk = copy(selected_file, path, false);
                                if (isCopyOk) {
                                    selected_file = path;
                                    handler.sendMessage(handler.obtainMessage(MSG_REBOOT));
                                } else {
                                    handler.sendMessage(handler.obtainMessage(MSG_COPY_ERROR));
                                }
                            }
                        });
                        break;
                    case MSG_REBOOT:
                        File update1 = new File(Const.UPDATE_FILE1);
                        File update2 = new File(Const.UPDATE_FILE2);
                        File update3 = new File(Const.UPDATE_FILE3);
                        if (update1.exists()) {
                            update1.delete();
                        }
                        if (update2.exists()) {
                            update2.delete();
                        }
                        update();
                        break;
                }
            }
            super.handleMessage(msg);
        }
    };

    @Override
    protected void setTitleView(TitleView titleView) {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        isCopying=false;
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if (isCopying) {
            showCopyFileDialog();
        }
    }

    @Override
    public void initWidget() {
        setContentView(R.layout.activity_sdcard_update);
        if (mCopyService == null)
            mCopyService = Executors.newFixedThreadPool(3);
        Bundle bl = getIntent().getExtras();
        if (bl != null)
            selected_file = bl.getString("selected_file");
        LogUtil.d("selected_file = " + selected_file);
        initViews();
    }

    private void initViews() {
        TextView file_name = findViewById(R.id.sdcard_update_file_name);
        if (selected_file != null) {
            File f = new File(selected_file);
            file_name.setText(getString(R.string.selected_update_zip) + f.getName());
        }
        wv_tips = findViewById(R.id.sdcard_update_webview);
        wv_tips.setText(Html.fromHtml(getString(R.string.sdCard_update_tips_content)));
        btn_install = findViewById(R.id.sdcard_update_install);
        if (DeviceInfoUtil.getInstance().isNoTouch())
            btn_install.requestFocus();
        btn_install.setTag(R.id.sdcard_update_install);
        btn_install.setOnClickListener(this);
    }

    private void showCopyFileDialog() {
        DialogUtil.showCustomDialog(this, R.layout.dialog_loading_copy_file, null);
    }

    public boolean copy(String oldPath, String newPath, Boolean isDelete) {
        LogUtil.d("copy, oldPath = " + oldPath);
        LogUtil.d("copy, newPath = " + newPath);
        int isCopyOk = 0;
        long bytesum=0;
        isCopying=true;
        try {
            int byteRead;
            File toFile = new File(newPath);
            File oldFile = new File(oldPath);
            if (toFile.exists()) {
                toFile.delete();
            } else {
                LogUtil.d("copy, newPath = " + newPath + " is not exist!");
            }
            if (oldFile.exists()) {
                InputStream inStream = new FileInputStream(oldPath);
                FileOutputStream fs = new FileOutputStream(newPath);
                byte[] buffer = new byte[1024 * 32];
                while ((byteRead = inStream.read(buffer)) != -1) {
                    if (Thread.currentThread().isInterrupted()) {
                        isCopyOk = 1;
                        break;
                    }
                    bytesum += byteRead;
                    System.out.println(bytesum);
                    fs.write(buffer, 0, byteRead);
                }
                inStream.close();
                fs.close();
                if (isCopyOk == 1) {
                    return false;
                }
                isCopyOk = 2;
                if (isDelete) {
                    LogUtil.d("copy success to delete" + oldFile.delete());
                }
            } else {
                LogUtil.d("copy, oldPath = " + oldPath + " is not exist!");
            }
        } catch (Exception e) {
            LogUtil.d("copy, Exception" + e.toString());
        }finally {
            isCopying=false;
        }
        LogUtil.d("copy, isOk " + isCopyOk);
        return isCopyOk == 2;
    }


    private void update() {
        LogUtil.d("doUpdate : selected_file " + selected_file);
        Status.setVersionStatus(this, Status.STATE_QUERY_NEW_VERSION);
        PreferencesUtils.putBoolean(this, Setting.FOTA_UPDATE_LOCAL, true);
        PreferencesUtils.putString(this, Setting.FOTA_UPDATE_LOCAL_PATH, selected_file);
        PreferencesUtils.putInt(this, "query_count", 0);
        PreferencesUtils.putInt(this, "download_install_later", 0);
        Install.enterRecovery(this, selected_file);
    }

    @SuppressLint("StringFormatInvalid")
    private void onClickInstall() {
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
        int level = DeviceInfoUtil.getInstance().getBattery();
        if (!Install.isBatteryAbility(this, level)) {
            DialogUtil.showBaseCustomDialog(this, R.mipmap.ota_battery, getString(R.string.ota_battery_low, level));
            return;
        }
        DialogUtil.showDialog(this,
                getString(R.string.not_support_fota_title), getString(R.string.sdcard_update_prompt),
                new ClickCallback() {
                    @Override
                    public void onClick() {
                        Status.idleReset(SdcardUpdateActivity.this);
                        EventBus.getDefault().post(new EventMessage(Event.QUERY, Event.QUERY_INIT_VERSION, 0, 0, null));
                        if (DeviceInfoUtil.getInstance().isSupportAbUpdate())
                            Status.setVersionStatus(SdcardUpdateActivity.this, Status.STATE_AB_UPDATING);
                        local_update();
                    }
                });
    }


    private boolean getSdcardAvailable() {
        boolean result = false;
        try {
            String state = StorageUtil.getExternalStorageStateExt(getFileSdcardRoot(selected_file));
            result = state.equals(Environment.MEDIA_MOUNTED);
        } catch (Exception e) {
            LogUtil.e("getSdcardAvailable error " + e.toString());
            e.printStackTrace();
        }
        return result;
    }

    private boolean isSdcardRootZip(String selected_file) {
        boolean result = false;
        if (selected_file == null) {
            return false;
        }
        File file = new File(selected_file);
        String parentPath = file.getParent();
        //5.0后获取sd卡根目录流程
        if (Build.VERSION.SDK_INT >= 21) {
            String outSdcard = StorageUtil.getStoragePath(this, true);
            String innerSdcard = StorageUtil.getStoragePath(this, false);
            LogUtil.d("isSdcardRootZip,outSdcard=" + outSdcard + " ,innerSdcard=" + innerSdcard);
            if (!TextUtils.isEmpty(outSdcard) && parentPath.equals(outSdcard)) result = true;
            if (!TextUtils.isEmpty(innerSdcard) && parentPath.equals(innerSdcard)) result = true;
        } else {
            List<StorageUtil.StorageInfo> listPaths = StorageUtil.getStorageList();
            if (listPaths != null) {
                for (int i = 0; i < listPaths.size(); i++) {
                    StorageUtil.StorageInfo info = listPaths.get(i);
                    LogUtil.d("isSdcardRootZip, i = " + i + ",info path = " + info.path + ",parentPath = " + parentPath);
                    if (parentPath.equals(info.path)) {
                        result = true;
                        break;
                    }
                }
            }
        }
        return result;
    }

    @Override
    public void widgetClick(View v) {
        int id = (Integer) v.getTag();
        if (id == R.id.sdcard_update_install) {
            LogUtil.d("onItemClick, install now");
            onClickInstall();
        }
    }

    @Override
    protected void onDestroy() {
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        closeDialog();
    }

    private void local_update() {
        File f = new File(selected_file);
        if (!f.exists()) {
            DialogUtil.showPositiveDialog(this,
                    getString(R.string.sdCard_upgrade_find_update_file_fail),
                    getString(R.string.sdCard_upgrade_copy_file_fail_prompt));
            return;
        }
        if (!getSdcardAvailable()) {
            DialogUtil.showPositiveDialog(this,
                    getString(R.string.not_support_fota_title),
                    getString(R.string.unmount_sdcard));
            return;
        }
        if (isSdcardRootZip(selected_file)) {
            File recoveryFile = new File(f.getParent() + Const.SD_PACKAGE_NAME);
            if (Build.VERSION.SDK_INT < 23 && f.renameTo(recoveryFile)) {
                selected_file = recoveryFile.getAbsolutePath();
            }
            handler.sendEmptyMessage(2);
        } else {
            if (!isEnough(f.length(), getFileSdcardRoot(selected_file))) {
                return;
            }
            mCopyService.execute(new Runnable() {
                public void run() {
                    //added by brave 20150917
                    LogUtil.d("sdCardUpdate, copy to ota_root file");
                    if (Build.VERSION.SDK_INT < 23) {
                        String toFilePath = getFileSdcardRoot(selected_file) + Const.SD_PACKAGE_NAME;
                        boolean isCopyOk = copy(selected_file, toFilePath, true);
                        if (isCopyOk) {
                            selected_file = toFilePath;
                        }
                    }
                    handler.sendEmptyMessage(2);
                }
            });
        }
    }

    private String getFileSdcardRoot(String selected_file) {
        String sdcardRoot = selected_file;
        if (selected_file != null) {
            sdcardRoot = StorageUtil.getRootPath(SdcardUpdateActivity.this, selected_file);
            if (TextUtils.isEmpty(sdcardRoot)) {
                sdcardRoot = selected_file;
            } else {
                return sdcardRoot;
            }
            List<StorageUtil.StorageInfo> listPaths = com.adups.fota.utils.StorageUtil.getStorageList();
            if (listPaths != null) {
                for (int i = 0; i < listPaths.size(); i++) {
                    StorageUtil.StorageInfo info = listPaths.get(i);
                    if ((info != null) && (selected_file.startsWith(info.path))) {
                        sdcardRoot = info.path;
                        break;
                    }
                }
            }
        }
        return sdcardRoot;
    }

    private boolean checkSdcardIsAvailable(long miniSize, String path) {
        try {
            StatFs statfs = new StatFs(path);
            long blockSize = statfs.getBlockSize();
            long blockCount = statfs.getAvailableBlocks();
            long totalSize = blockSize * blockCount;
            LogUtil.d("checkSdcardSpaceNeeded totalSize = " + totalSize);
            if (totalSize > miniSize) {
                LogUtil.e("checkSdcardSpaceNeeded true, totalSize = " + totalSize);
                return true;
            }
        } catch (Exception e) {
            LogUtil.e("checkSdcardSpaceNeeded false, card mount error");
            return false;
        }
        return false;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(EventMessage event) {
        switch (event.getWhat()) {
            case Event.INSTALL:
                install_callback(event);
                break;
        }
    }

    private void install_callback(EventMessage evt) {
        closeDialog();
        finish();
    }

    //判断copy路径是否空间足够
    public boolean isEnough(long miniSize, String path) {
        if (!checkSdcardIsAvailable(miniSize, path)) {
            DialogUtil.showPositiveDialog(this,
                    getString(R.string.sdCard_upgrade_memory_space_not_enough),
                    getString(R.string.sdcard_crash_or_unmount));
            LogUtil.d("isEnough false");
            return false;
        }
        return true;
    }

}
