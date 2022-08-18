package com.adups.fota.config;

/**
 * Created by xw on 15-12-30.
 */
public class Event {

    public static final int QUERY = 100;
    public static final int DOWNLOAD = 200;
    public static final int INSTALL = 300;

    // query

    public static final int QUERY_ONGOING = 1000;
    public static final int QUERY_NEW_VERSION = 1001;
    public static final int QUERY_SAME_VERSION = 1002;
    public static final int QUERY_NO_VERSION = 1003;
    public static final int QUERY_CHANGE_VERSION = 1004;
    public static final int QUERY_POLICY_CHANGE = 1005;
    public static final int QUERY_INIT_VERSION = 1006;

    public static final int QUERY_FULL_ROM = 1007;
    public static final int QUERY_RUNNING = 1009;

    public static final int LEFT_MENU_OPEN = 1111;
    public static final int LEFT_MENU_CLOSE = 1112;


    // download
    public static final int DOWNLOAD_START = 1000;
    public static final int DOWNLOAD_SUCCESS = 1001;
    public static final int DOWNLOAD_PROGRESS = 2000;
    public static final int DOWNLOAD_PAUSE = 2001;
    public static final int DOWNLOAD_FAIL = 3000;


    // error reason
    public static final int ERROR_RESPONSE_ERROR = 3001;
    public static final int ERROR_RESPONSE_TIMEOUT = 3002;
    public static final int ERROR_SDCARD_DISABLE = 3003;
    public static final int ERROR_SDCARD_NOT_ENOUGH = 3005;

    public static final int ERROR_UNKNOWNHOST = 3006;
    public static final int ERROR_MALFORMEDURL = 3007;
    public static final int ERROR_IO = 3008;
    public static final int ERROR_ILLEGALARGUMENT = 3009;
    public static final int ERROR_UNKNOWN = 3010;
    public static final int ERROR_PAUSE = 3011;
    public static final int ERROR_UNDONE = 3012;
    public static final int ERROR_FILE_TOO_LARGE = 3013;

    // network
    public static final int NETWORK_TYPE_WIFI_TO_MOBILE = 5001;
    public static final int NETWORK_TYPE_MOBILE_TO_WIFI = 5002;

}


