package com.adups.fota.utils;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.Process;
import android.os.StatFs;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.text.TextUtils;
import android.text.format.Formatter;

import com.adups.fota.MyApplication;
import com.adups.fota.bean.VersionBean;
import com.adups.fota.config.Const;
import com.adups.fota.config.Setting;
import com.adups.fota.config.Status;
import com.adups.fota.query.QueryInfo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.StringTokenizer;

public class StorageUtil {
    public static final int SDCARD_STATE_UNMOUNT = 1;
    public static final int SDCARD_STATE_INSUFFICIENT = 2;
    public static final int SDCARD_STATE_OK = 3;
    public static final String PATH_INTERNAL = "internal";
    public static final String PATH_EXTERNAL = "external";
    private static String upgradePath1 = null;
    private static String upgradePath2 = null;
    private static String upgradePath3 = null;
    private static double COUNT = 2.5;

    public static void init(final Context context) {
        //初始化目录
        try {
            context.getExternalFilesDir(null);
            new File(context.getFilesDir() + "/adupsfota").mkdirs();
            new File(context.getExternalFilesDir(null) + "/adupsfota").mkdirs();
            new File(context.getExternalFilesDir(null) + "/fota").mkdirs();
            new Thread() {
                @Override
                public void run() {
                    super.run();
                    isSdcardAvailable(context);
                }
            }.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean isSdcardAvailable(Context context) {
        String externalStorageState = null;

        // download path
        String download_path = PreferencesUtils.getString(context, Setting.FOTA_DOWNLOAD_PATH, null);
        String[] temp;

        if (!TextUtils.isEmpty(download_path) && (download_path.contains("#"))) {
            temp = download_path.split("#");

            if (temp.length == 3) {
                setUpgradePath(temp[0], temp[1], temp[2]);
            }
        }

        if ((upgradePath1 != null) && (upgradePath1.length() > 0) && (!upgradePath1.equals("null"))) {
            externalStorageState = getExternalStorageStateExt(upgradePath1);
        } else {
            externalStorageState = getExternalStorageStateExt(context);
        }

        LogUtil.d("externalStorageState = " + externalStorageState);
        return Environment.MEDIA_MOUNTED.equals(externalStorageState);
    }

    public static boolean checkSpaceAvailable(Context context, long miniSize) {
        long needSize = getNeedSize(miniSize);
        if (!isSdcardMounted(context)) {
            return isDataSpaceEnough(context, needSize);
        } else {
            boolean isEnough = isSpaceEnough(getStoragePath(context, true), needSize);
            if (!isEnough)
                isEnough = isDataSpaceEnough(context, needSize);
            return isEnough;
        }
    }

    public static boolean checkInsideSpaceAvailable(Context context, long miniSize) {
        return isDataSpaceEnough(context, getNeedSize(miniSize));
    }

    public static boolean checkOutsideSpaceAvailable(Context context, long miniSize) {
        return isSpaceEnough(getStoragePath(context, true), getNeedSize(miniSize));
    }

    private static long getNeedSize(long miniSize) {
        int isOldPkg = 0;
        VersionBean model = QueryInfo.getInstance().getVersionInfo();
        if (model != null)
            isOldPkg = model.getIsOldPkg();
        COUNT = isOldPkg == 0 ? 2.5 : 1;
        return Double.valueOf(COUNT * miniSize).longValue();
    }

    private static long getNeedSize() {
        VersionBean model = QueryInfo.getInstance().getVersionInfo();
        if (model != null) {
            COUNT = model.getIsOldPkg() == 0 ? 2.5 : 1;
            return Double.valueOf(COUNT * model.getFileSize()).longValue();
        }
        return 0;
    }

    private static boolean isDataSpaceEnough(Context context, long size) {
        return isSpaceEnough(context.getFilesDir().getAbsolutePath(), size);
    }

    public static int checkIsAvailable(Context context, long miniSize) {
        int isOldPkg = 0;
        int size = 0;
        try {
            //通过后台下发判断是否需要2.5倍的空间
            final VersionBean model = QueryInfo.getInstance().getVersionInfo();
            if (model != null) {
                isOldPkg = model.getIsOldPkg();
            }

            COUNT = isOldPkg == 0 ? 2.5 : 1;
            size = checkSdcardIsAvailable(context, (long) COUNT * miniSize);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return size;
    }

    public static int checkSdcardIsAvailable(Context context, long miniSize) {
        long size = checkSdcardSpaceNeeded(context, miniSize);
        LogUtil.d("size = " + size);
        if (size == 0) {
            return SDCARD_STATE_OK;
        } else if (size == -1) {
            return SDCARD_STATE_UNMOUNT;
        } else {
            return SDCARD_STATE_INSUFFICIENT;
        }
    }

    public static boolean isSpaceEnough(String path, long minSize) {
        if (TextUtils.isEmpty(path)) return false;
        File file = new File(path);
        if (!file.exists() || file.isFile()) return false;
        long total = file.getFreeSpace();
        LogUtil.d("path ：" + path + " ; total : " + Formatter.formatFileSize(MyApplication.getAppContext(), total)
                + " ; minSize : " + Formatter.formatFileSize(MyApplication.getAppContext(), minSize));
        return total - minSize > 0;
    }

    private static long checkSdcardSpaceNeeded(Context context, long miniSize) {
        long totalSize = 0;
        try {
            File sdcardSystem = getExternalStorageDirectoryExt(context);
            StatFs statfs = new StatFs(sdcardSystem.getPath());
            long blockSize = statfs.getBlockSize();
            long blockCount = statfs.getAvailableBlocks();
            totalSize = blockSize * blockCount;
            //2017年5月11日17:52:43 借鉴红石的判断,是否可以下载
            File pkgFile = new File(StorageUtil.getPackageFileName(context));
            if (pkgFile.exists()) {
                miniSize = miniSize - pkgFile.length();
            }
            LogUtil.d("totalSize = " + totalSize + "; miniSize = " + miniSize);
            if (totalSize < miniSize) {
                return miniSize - totalSize;
            }
        } catch (Exception e) {
            LogUtil.e("card mount error");
            return -1;
        }
        if (totalSize > 10 + miniSize) {
            return 0;
        }
        return -1;
    }

    private static File getExternalStorageDirectoryExt(Context context) {
        return new File(getSdcardRoot(context, true));
    }

    private static String getExternalStorageStateExt(Context context) {
        return getExternalStorageStateExt(getExternalStorageDirectoryExt(context).toString());
    }

    public static String getExternalStorageStateExt(String path) {
        LogUtil.d("path = " + path);
        try {
            File sdcardSystem = new File(path);
            StatFs statfs = new StatFs(sdcardSystem.getPath());
            long blockSize = statfs.getBlockSize();
            long totalBlock = statfs.getBlockCount();
            long needSize = 0;
            //added by cai.huang for start  the sdcard is not smaller than 50M
            if (totalBlock * blockSize <= 1024 * 32)
                return Environment.MEDIA_REMOVED;
        } catch (Exception e) {
            LogUtil.d(e.toString());
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {   //6.0以下就直接返回removed 状态  2015年12月22日9:47:51
                return Environment.MEDIA_REMOVED;
            }
            return Environment.MEDIA_UNMOUNTED;
        }
        if (path.contains(Environment.getExternalStorageDirectory().getAbsolutePath())) {//modify by brave 20150915
            LogUtil.d("Environment.getExternalStorageState = " + Environment.getExternalStorageState());
            return Environment.getExternalStorageState();
        }
        //if /data/data开头的地址，就直接返回
        if (path.startsWith("/data/data"))
            return Environment.MEDIA_MOUNTED;
        if (isCanBuildFile(path))
            return Environment.MEDIA_MOUNTED;
        return Environment.MEDIA_MOUNTED;
    }

    public static String getSdcardRoot(Context context, boolean needLog) {
        boolean isPath1Set = false;
        boolean isPath2Set = false;
        boolean isPath3Set = false;
        String sdcard;

        if ((upgradePath1 != null) && (upgradePath1.length() > 0) && (!upgradePath1.equals("null"))) {
            isPath1Set = true;
        }

        if ((upgradePath2 != null) && (upgradePath2.length() > 0) && (!upgradePath2.equals("null"))) {
            isPath2Set = true;
        }

        if ((upgradePath3 != null) && (upgradePath3.length() > 0) && (!upgradePath3.equals("null"))) {
            isPath3Set = true;
        }
        LogUtil.d("download_path_server : " + QueryInfo.getInstance().getPolicyValue(QueryInfo.DOWNLOAD_PATH_SERVER, Integer.class));
        //修改后台下发下载路径 按地址1顺序 2016年11月10日10:54:56
        if (isPath1Set && Environment.MEDIA_MOUNTED.equals(getExternalStorageStateExt(upgradePath1))) {
            sdcard = upgradePath1;
        } else if (isPath2Set && Environment.MEDIA_MOUNTED.equals(getExternalStorageStateExt(upgradePath2))) {
            sdcard = upgradePath2;
        } else if (isPath3Set && Environment.MEDIA_MOUNTED.equals(getExternalStorageStateExt(upgradePath3))) {
            sdcard = upgradePath3;
        } else {
            sdcard = getSdcardRootExt(context);
        }
        LogUtil.d(needLog, "sdcard = " + sdcard);
        return sdcard;
    }

    //无线升级 升级地址
    private static String getSdcardRootExt(Context context) {
        //5.0后下载地址流程
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (isMultiPartition(context))
                return context.getFilesDir().toString();
            //首先判断是否仅限外置sd或者内置存储下载：1为只下载外置T卡，2为只下载内置存储 0为忽略 默认值为0
            int download_path_server = QueryInfo.getInstance().getPolicyValue(QueryInfo.DOWNLOAD_PATH_SERVER, Integer.class);
            LogUtil.d("download_path_server : " + download_path_server);
            if (download_path_server == 1) {//外置T卡
                return checkOutStorage(context);
            } else if (download_path_server == 2) {//内置T卡
                if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.Q) {
                    return Const.INTERNEL_UPDATE_PATH_FOR_Q;
                }
                return context.getFilesDir().getAbsolutePath();
            }
            switch (DeviceInfoUtil.getInstance().getPath()) {
                case PATH_INTERNAL:
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        return Const.INTERNEL_UPDATE_PATH_FOR_Q;
                    }
                    return context.getFilesDir().getAbsolutePath();
                case PATH_EXTERNAL:
                    return checkOutStorage(context);
            }
            String outPath = checkOutStorage(context);
            if (!TextUtils.isEmpty(outPath) && isSpaceEnough(outPath, getNeedSize())) {
                return outPath;//外置T卡
            } else {
                if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.Q) {
                    return Const.INTERNEL_UPDATE_PATH_FOR_Q;
                }
                return context.getFilesDir().getAbsolutePath();//内置T卡
            }
        } else {
            String sdcard;
            String external_storage = Environment.getExternalStorageDirectory().getAbsolutePath();
            List<StorageInfo> list = getStorageList();
            if (list != null && list.size() > 0) {
                for (int i = 0; i < list.size(); i++) {
                    StorageInfo info = list.get(i);
                    if (info != null) {
                        sdcard = info.path;
                        if (!sdcard.equals(external_storage)
                                && ((Environment.MEDIA_MOUNTED.equals(getExternalStorageStateExt(sdcard))))
                        ) {
                            if (isCanBuildFile(sdcard)) {
                                return sdcard;
                            }
                        }
                    }
                }
            }
            return external_storage;
        }
    }

    //抓取log及导出log地址
    public static String getCatchLogPath(Context context) {
        if (isSdcardMounted(context) && FileUtil.hasWritePermission(context)) {
            String path = getStoragePath(context, true) + "/Android/data/" + context.getPackageName() + "/files";
            if (StorageUtil.isSpaceEnough(path, 1024 * 1024))
                return path;
        }
        return getExternalMidWatchPackagePath(context);
    }

    public static String getExternalMidWatchPath() {
        return Environment.getExternalStorageDirectory() + File.separator + "Android/";
    }

    public static String getExternalMidWatchPackagePath(Context context) {
        return context.getExternalFilesDir(null) + File.separator;
    }

    public static File getErrorLogFile() {
        try {
            File logFile = new File("/cache/recovery/last_error_log");
            if (logFile.exists()) {
                LogUtil.d("last_log exist");
                return logFile;
            }
        } catch (Exception e) {
            LogUtil.d(e.getMessage());
        }
        LogUtil.d("last_log not exist");
        return null;
    }

    public static void deleteErrorLogFile() {
        File file = getErrorLogFile();
        if (file != null)
            file.delete();
    }

    /*
     * 获取当前文件路径的root路径
     * @param path 需要判断的路径
     * **/
    public static String getRootPath(Context context, String path) {
        if (TextUtils.isEmpty(path)) {
            return "";
        }
        String outPath = getStoragePath(context, true);
        String innerPath = getStoragePath(context, false);
        if (outPath != null && path.startsWith(outPath)) {
            return outPath;
        } else if (innerPath != null && path.startsWith(innerPath)) {
            return innerPath;
        }
        return "";
    }

    /*
     * 判断是路径是否可写
     * @param isRoot 是否返回根路径
     * **/
    private static boolean isCanBuildFile(String sdcard) {
        if (TextUtils.isEmpty(sdcard) || !isSdcardNotEnough(sdcard)) {
            LogUtil.d("invalid path " + sdcard);
            return false;
        }
        try {
            //判断是否有权限生成文件做为判断 2016年3月14日20:21:57
            File file = new File(sdcard + "/test/test.txt");
            if (file.exists()) {
                LogUtil.d("file.exists,so delete=" + file.delete());
            }

            File fileDir = new File(sdcard + "/test/");
            if (fileDir.exists()) {
                LogUtil.d("fileDir.exists,so delete=" + fileDir.delete());
            }

            if (fileDir.mkdirs() || fileDir.mkdir()) {
                LogUtil.d("mkdirs  success");
            } else {
                LogUtil.d("mkdirs  failed!!");
            }

            if (file.createNewFile()) {
                file.delete();
                fileDir.delete();
            } else {
                LogUtil.d("createNewFile failed!!");
                return false;
            }
            LogUtil.d("sdcard = " + sdcard);
            return true;
        } catch (Exception e) {
            LogUtil.d(e.getMessage());
        }
        return false;
    }

    //判断传的路径是不是内部存储路径
    public static boolean isInnerSdcard(Context context, String path) {
        String innerPath = getStoragePath(context, false);
        return innerPath != null && path.startsWith(innerPath);
    }

    public static boolean isSdcardMounted(Context context) {
        StorageManager storageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
        if (storageManager != null) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                try {
                    Method getVolumeList = storageManager.getClass().getMethod("getVolumeList");
                    Object result = getVolumeList.invoke(storageManager);
                    int size = Array.getLength(result);
                    LogUtil.d("storage count : " + size);
                    return size > 1;
                } catch (Exception ignored) {
                }
            } else {
                List<StorageVolume> storageVolumes = storageManager.getStorageVolumes();
                int size = storageVolumes.size();
                LogUtil.d("storage count : " + size);
                return size > 1;
            }
        }
        return Environment.getExternalStorageState().equalsIgnoreCase(Environment.MEDIA_MOUNTED);
    }

    public static String getStoragePath(Context mContext, boolean outSdcard) {
        LogUtil.d("outSdcard : " + outSdcard);
        StorageManager mStorageManager = (StorageManager) mContext.getSystemService(Context.STORAGE_SERVICE);
        if (mStorageManager != null) {
            try {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                    Class<?> storageVolumeClazz = Class.forName("android.os.storage.StorageVolume");
                    Method getVolumeList = mStorageManager.getClass().getMethod("getVolumeList");
                    Method getPath = storageVolumeClazz.getMethod("getPath");
                    Method isEmulated = storageVolumeClazz.getMethod("isEmulated");
                    Object result = getVolumeList.invoke(mStorageManager);
                    final int length = Array.getLength(result);
                    for (int i = 0; i < length; i++) {
                        Object storageVolumeElement = Array.get(result, i);
                        String path = (String) getPath.invoke(storageVolumeElement);
                        boolean emulated = (Boolean) isEmulated.invoke(storageVolumeElement);
                        LogUtil.d("emulated = " + emulated + "; path = " + path);
                        if (outSdcard == !emulated && !path.startsWith("/dev/null")) {
                            return path;
                        }
                    }
                } else {
                    List<StorageVolume> storageVolumes = mStorageManager.getStorageVolumes();
                    for (StorageVolume storageVolume : storageVolumes) {
                        boolean emulated = storageVolume.isEmulated();
                        Method getPath = storageVolume.getClass().getMethod("getPath");
                        String path = getPath.invoke(storageVolume).toString();
                        LogUtil.d("emulated = " + emulated + "; path = " + path);
                        if (outSdcard == !emulated && !path.startsWith("/dev/null")) {
                            return path;
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private static boolean isSdcardNotEnough(String path) {
        boolean result = false;
        try {
            File sdcardSystem = new File(path);
            if (!sdcardSystem.exists() && !(sdcardSystem.mkdir() || sdcardSystem.mkdirs())) {
                LogUtil.d(path + ",mkdir fail");
                return false;
            }
            StatFs statfs = new StatFs(sdcardSystem.getPath());
            long blockSize = statfs.getBlockSize();
            long availableBlock = statfs.getAvailableBlocks();

            //空间至少是差分包2.5倍
            long needSize = 0;
            double count = 1;
            final VersionBean model = QueryInfo.getInstance().getVersionInfo();
            if (model != null) {
                needSize = model.getFileSize();
                count = model.getIsOldPkg() == 0 ? 2.5 : 1;
            }
            LogUtil.d("availableBlock = " + availableBlock * blockSize + "; needSize = " + needSize);
            //2016年8月10日 11:16:30 只需要判断空间大于差分包的总大小  1.5或者2.5倍即可
            if (needSize == 0 || availableBlock * blockSize > needSize * count) {
                result = true;
            }
        } catch (Exception e) {
            LogUtil.e(path + " card mount error");
        }
        return result;
    }

    //added by cai.huang for start specify upgrade path
    public static void setUpgradePath(String path1, String path2, String path3) {
        if ((path1 != null) && (path1.length() > 0) && (!path1.equals("null"))) {
            upgradePath1 = path1;
        }

        if ((path2 != null) && (path2.length() > 0) && (!path2.equals("null"))) {
            upgradePath2 = path2;
        }

        if ((path3 != null) && (path3.length() > 0) && (!path3.equals("null"))) {
            upgradePath3 = path3;
        }

        LogUtil.d("upgradePath1 = " + upgradePath1 + ", upgradePath2 = " + upgradePath2 + ", upgradePath3 = " + upgradePath3);
    }


    public static boolean isPathSeted() {
        boolean isPath1Set = false;
        boolean isPath2Set = false;
        boolean isPath3Set = false;
        if ((upgradePath1 != null) && (upgradePath1.length() > 0) && (!upgradePath1.equals("null"))) {
            isPath1Set = true;
        }

        if ((upgradePath2 != null) && (upgradePath2.length() > 0) && (!upgradePath2.equals("null"))) {
            isPath2Set = true;
        }

        if ((upgradePath3 != null) && (upgradePath3.length() > 0) && (!upgradePath3.equals("null"))) {
            isPath3Set = true;
        }

        return isPath1Set || isPath2Set || isPath3Set;
    }

    public static List<StorageInfo> getStorageList() {
        List<StorageInfo> list = new ArrayList<StorageInfo>();
        String def_path = Environment.getExternalStorageDirectory().getPath();
        boolean def_path_internal = !Environment.isExternalStorageRemovable();
        String def_path_state = Environment.getExternalStorageState();
        boolean def_path_available = def_path_state.equals(Environment.MEDIA_MOUNTED) ||
                def_path_state.equals(Environment.MEDIA_MOUNTED_READ_ONLY);
        boolean def_path_readonly =
                Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED_READ_ONLY);
        BufferedReader buf_reader = null;
        try {
            HashSet<String> paths = new HashSet<String>();
            buf_reader = new BufferedReader(new FileReader("/proc/mounts"));
            String line;
            int cur_display_number = 1;
            while ((line = buf_reader.readLine()) != null) {
                if (line.contains("vfat") || line.contains("/mnt")) {
                    StringTokenizer tokens = new StringTokenizer(line, " ");
                    tokens.nextToken(); //device
                    String mount_point = tokens.nextToken(); //mount point
                    if (paths.contains(mount_point)) {
                        continue;
                    }
                    List<String> flags = Arrays.asList(tokens.nextToken().split(",")); //flags
                    boolean readonly = flags.contains("ro");

                    if (mount_point.equals(def_path)) {
                        paths.add(def_path);
                        list.add(0, new StorageInfo(def_path, def_path_internal, readonly, -1));
                    } else if (line.contains("/dev/block/vold")) {
                        if (!line.contains("/mnt/secure") && !line.contains("/mnt/asec") &&
                                !line.contains("/mnt/obb") && !line.contains("/dev/mapper") &&
                                !line.contains("tmpfs")) {
                            paths.add(mount_point);
                            list.add(new StorageInfo(mount_point, false, readonly, cur_display_number++));
                        }
                    }
                }
            }

            if (!paths.contains(def_path) && def_path_available) {
                list.add(0, new StorageInfo(def_path, def_path_internal, def_path_readonly, -1));
            }

        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (buf_reader != null) {
                try {
                    buf_reader.close();
                } catch (IOException ignored) {
                }
            }
        }
        return list;
    }

    public static String getPackagePathName(Context context) {
        /*获取下载路径并判断是否为空*/
        int status = Status.getVersionStatus(context);
        String packagePath = PreferencesUtils.getString(context, Setting.UPDATE_PACKAGE_PATH, "");
        if ((status == Status.STATE_DL_PKG_COMPLETE || status == Status.STATE_REBOOT) && !TextUtils.isEmpty(packagePath))
            return packagePath;
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O_MR1 && DeviceInfoUtil.getInstance().isSupportAbUpdate())
            return Const.INTERNEL_UPDATE_PATH_FOR_Q;
        return getSdcardRoot(context, true) + Const.FOTA_FOLDER;
    }

    public static String getPackageFileName(Context context) {
        String path = getPackagePathName(context) + Const.PACKAGE_NAME;
        LogUtil.d("filename = " + path);
        return path;
    }

    private static boolean deleteErrFileExt(File destfile) {
        LogUtil.d("destfile = " + destfile);
        if (destfile.isDirectory()) {
            String[] children = destfile.list();
            if (children != null) {
                for (String aChildren : children) {
                    boolean success = deleteErrFileExt(new File(destfile, aChildren));
                    if (!success) {
                        LogUtil.d("deleteErrFileExt err");
                        return false;
                    }
                }
            }
        }
        return destfile.delete();
    }

    public static void deleteErrFile(final Context context, final String filePath) {
        new Thread() {
            @Override
            public void run() {
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                LogUtil.d("filePath = " + filePath);
                deleteModemFile(getSdcardRoot(context, false));
                File destfile = new File(filePath);
                deleteErrFileExt(destfile);
                deleteV4File(context);
            }
        }.start();
    }

    private static void deleteV4File(Context context) {
        try {
            File file = new File(context.getFilesDir().getPath() + "/adupsfota/update.zip");
            if (file.exists()) {
                LogUtil.d("delete " + file);
                file.delete();
            }
            File file2 = new File(context.getFilesDir().getPath() + "/update.zip");
            if (file2.exists()) {
                LogUtil.d("delete " + file2);
                file2.delete();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void deleteModemFile(String path) {
        LogUtil.d("path = " + path);
        File file = new File(path + "/modem.bin");
        if (file.exists()) {
            LogUtil.d("delete " + file);
            file.delete();
        }

        file = new File(path + "/nvitem.bin");
        if (file.exists()) {
            LogUtil.d("delete " + file);
            file.delete();
        }

        file = new File(path + "/dsp.bin");
        if (file.exists()) {
            LogUtil.d("delete " + file);
            file.delete();
        }

        file = new File(path + "/vmjaluna.bin");
        if (file.exists()) {
            LogUtil.d("delete " + file);
            file.delete();
        }
    }

    //下载外置卡
    private static String checkOutStorage(Context context) {
        String storagePath = PreferencesUtils.getString(context, Const.CUSTOM_DT_SDPATH, "");
        if (!TextUtils.isEmpty(storagePath) && new File(storagePath).exists())
            return storagePath;
        String outPath = getStoragePath(context, true);
        if (!TextUtils.isEmpty(outPath)) {
            outPath = outPath + "/Android/data/" + context.getPackageName() + "/files";
            PreferencesUtils.putString(context, Const.CUSTOM_DT_SDPATH, outPath);
            return outPath;
        }
        return "";
    }

    private static boolean isMultiPartition(Context context) {
        StorageManager storageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
        if (storageManager != null) {
            try {
                Method getVolumes = storageManager.getClass().getMethod("getVolumes");
                Object volumeInfoList = getVolumes.invoke(storageManager);
                for (int i = 0; i < Array.getLength(volumeInfoList); i++) {
                    Object volumeInfo = Array.get(volumeInfoList, i);
                    Method getType = volumeInfo.getClass().getMethod("getType");
                    Method getState = volumeInfo.getClass().getMethod("getState");
                    Method getDisk = volumeInfo.getClass().getMethod("getDisk");
                    if ((Integer) getType.invoke(volumeInfo) == 0 && (Integer) getState.invoke(volumeInfo) == 2) {
                        Object diskInfo = getDisk.invoke(volumeInfo);
                        Field volumeCount = diskInfo.getClass().getField("volumeCount");
                        return (Integer) volumeCount.get(diskInfo) > 1;
                    }
                }
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    public static class StorageInfo {
        public final String path;
        public final boolean internal;
        public final boolean readonly;
        public final int display_number;

        StorageInfo(String path, boolean internal, boolean readonly, int display_number) {
            this.path = path;
            this.internal = internal;
            this.readonly = readonly;
            this.display_number = display_number;
        }

    }

}
