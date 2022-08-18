package com.adups.fota.config;

import com.adups.fota.BuildConfig;

/**
 * 常量配置
 */
public class Const {

    // 激活后在SD卡存储的文件
    public static final String ACTIVATE_FILE = String.valueOf(new char[]{'.', 'd', 'o', 'g', '_', 'w', 'a', 't', 'c', 'h',});

    // 新版本信息文件
    public static final String VERSION_FILE = String.valueOf(new char[]{'f', 'i', 'r', 'm', 'w', 'a', 'r', 'e', '.', 't', 'x', 't',});

    public static final String SHOWFINALIZINGPRO = "ShowFinalizingPro";

    // 升级包文件夹目录
    public static final String FOTA_FOLDER = "/adupsfota";
    // 升级包名
    public static final String PACKAGE_NAME = "/update.zip";

    public static final String SD_PACKAGE_NAME = "/LocalSdUpdate.zip";

    //ab升级设备或者androidQ以及以上内置存储升级路径修改
    public static final String INTERNEL_UPDATE_PATH_FOR_Q = "/data/ota_package";
//    public static final String INTERNEL_UPDATE_PATH_FOR_Q = "/data/user/0/com.adups.fotas/files/adupsfota";


    //定制广播
    public static final String DOWNLOAD_STOP_BROADCAST = String.valueOf(new char[]{'c', 'o', 'm', '.', 'a', 'd', 'u', 'p', 's', '.', 'f', 'o', 't', 'a', '.', 'D', 'O', 'W', 'N', 'L', 'O', 'A', 'D', '_', 'S', 'T', 'O', 'P', }); //com.adups.fota.DOWNLOA_STOP
    public static final String DOWNLOAD_NOW_BROADCAST = String.valueOf(new char[]{'c', 'o', 'm', '.', 'a', 'd', 'u', 'p', 's', '.', 'f', 'o', 't', 'a', '.', 'D', 'O', 'W', 'N', 'L', 'O', 'A', 'D', '_', 'N', 'O', 'W', }); //com.adups.fota.DOWNLOAD_NOW
    public static final String INSTALL_NOW_BROADCAST = String.valueOf(new char[]{'c', 'o', 'm', '.', 'a', 'd', 'u', 'p', 's', '.', 'f', 'o', 't', 'a', '.', 'I', 'N', 'S', 'T', 'A', 'L', 'L', '_', 'N', 'O', 'W', }); //com.adups.fota.INSTALL_NOW

    public static final String FOTA_BOOT_PACKAGE_NAME = String.valueOf(new char[]{'c', 'o', 'm', '.', 'a', 'd', 'u', 'p', 's', '.', 'p', 'r', 'i', 'v', 'a', 'c', 'y', 'p', 'o', 'l', 'i', 'c', 'y'});
    public static final String FOTA_BOOT_ACTIVITY_NAME = String.valueOf(new char[]{'c', 'o', 'm', '.', 'a', 'd', 'u', 'p', 's', '.', 'p', 'r', 'i', 'v', 'a', 'c', 'y', 'p', 'o', 'l', 'i', 'c', 'y', '.', 'a', 'c', 't', 'i', 'v', 'i', 't', 'y', '.', 'G', 'd', 'p', 'r', 'A', 'c', 't', 'i', 'v', 'i', 't', 'y'});

    // download path
    public static final String UPDATE_FILE1 = "/data/media/0/adupsfota/update.zip";
    public static final String UPDATE_FILE2 = "/data/media/adupsfota/update.zip";
    public static final String UPDATE_FILE3 = String.valueOf(new char[]{'/', 'd', 'a', 't', 'a', '/', 'd', 'a', 't', 'a', '/', 'c', 'o', 'm', '.', 'a', 'd', 'u', 'p', 's', '.', 'f', 'o', 't', 'a', '/', 'f', 'i', 'l', 'e', 's', '/', 'a', 'd', 'u', 'p', 's', 'f', 'o', 't', 'a', '/', 'u', 'p', 'd', 'a', 't', 'e', '.', 'z', 'i', 'p',});///data/data/com.adups.fota/files/adupsfota/update.zip
    public static final String UPDATE_FILE4 = "/storage/emulated/0/adupsfota/update.zip";

    public static final String SD_FILE4 = String.valueOf(new char[]{'/', 'A', 'n', 'd', 'r', 'o', 'i', 'd', '/', 'd', 'a', 't', 'a', '/', 'c', 'o', 'm', '.', 'a', 'd', 'u', 'p', 's', '.', 'f', 'o', 't', 'a', '/', 'f', 'i', 'l', 'e', 's', '/', 'L', 'o', 'c', 'a', 'l', 'S', 'd', 'U', 'p', 'd', 'a', 't', 'e', '.', 'z', 'i', 'p',});///Android/data/com.adups.fota/files/LocalSdUpdate.zip

    public static final String SEND_UPDATE_SUCCESS_BROADCAST = String.valueOf(new char[]{'c', 'o', 'm', '.', 'a', 'd', 'u', 'p', 's', '.', 'f', 'o', 't', 'a', '.', 'O', 'U', 'T', '_', 'U', 'P', 'D', 'A', 'T', 'E', '_', 'S', 'U', 'C', 'C', 'E', 'S', 'S',});//com.adups.fota.OUT_UPDATE_SUCCESS 升级成功广播
    public static final String SEND_NEW_VERSION_BROADCAST = String.valueOf(new char[]{'c', 'o', 'm', '.', 'a', 'd', 'u', 'p', 's', '.', 'f', 'o', 't', 'a', '.', 'O', 'U', 'T', '_', 'B', 'R', 'O', 'A', 'D', 'C', 'A', 'S', 'T', '_', 'N', 'E', 'W', 'V', 'E', 'R', 'S', 'I', 'O', 'N',});//com.adups.fota.OUT_BROADCAST_NEWVERSION 检测到新版本广播
    public static final String SEND_BROADCAST_PERMISSION = String.valueOf(new char[]{'c', 'o', 'm', '.', 'a', 'd', 'u', 'p', 's', '.', 'f', 'o', 't', 'a', '.', 'p', 'e', 'r', 'm', 'i', 's', 's', 'i', 'o', 'n',});//广播的权限，只有有这个权限的第三方才能接收该广播
    public static final String SEND_CUSTOM_SERVICE_ACTION = String.valueOf(new char[]{'c', 'o', 'm', '.', 'a', 'd', 'u', 'p', 's', '.', 'f', 'o', 't', 'a', '.', 'c', 'u', 's', 't', 'o', 'm', '_', 's', 'e', 'r', 'v', 'i', 'c', 'e'}); //解决激活alarm和稍后安装提示的alarm不启动的问题

    public final static int TAG_CHECK = 0;
    public final static int TAG_DOWNLOAD = 1;
    public final static int TAG_DOWNLOAD_PAUSE = 2;
    public final static int TAG_DOWNLOAD_RESUME = 3;
    public final static int TAG_DOWNLOAD_RETRY = 4;
    public final static int TAG_DOWNLOAD_CANCEL = 5;
    public final static int TAG_UPDATE_NOW = 7;
    public final static int TAG_UPDATE_LATER = 8;
    public final static int TAG_LEFT_MENU = 9;
    public final static int TAG_BTN_POP = 10;
    public final static int TAG_APP_NAME = 11;
    public final static int TAG_REBOOT_NOW = 12;
    public final static int TAG_DOWNLOAD_LATER = 13;
    public final static int TAG_DOWNLOAD_INSTALL_LATER = 14;

    public final static boolean DEBUG_MODE = BuildConfig.DEBUG;
    public final static String DEBUG_MODE_PROJECT_NAME = "adups_T21_zh_other";
    public final static String DEBUG_MODE_IMEI = "354534061711612";
    public final static String DEBUG_MODE_VERSION = "v1";//"M20-T_C2_L56_V1.0.0_20160630";

    //lirenqi 20171023 add for QueryVersion
    public final static boolean QUERY_VERSION_BY_FULL = false;

    public static final String CUSTOM_DT_SDPATH = "CustomDtPath";

}