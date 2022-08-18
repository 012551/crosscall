package com.adups.fota.config;

/**
 * Created by xw on 15-12-18.
 */
public class ServerApi {

//    public static final String SERVER_DOMAIN = "https://testing.adups.cn";
//    public static final String SERVER_DOMAIN2 = "https://testing.adups.cn";

    public static final String SERVER_DOMAIN = String.valueOf(new char[]{'h', 't', 't', 'p', 's', ':', '/', '/', 'f', 'o', 't', 'a', '5', 'p', '.', 'a', 'd', 'u', 'p', 's', '.', 'c', 'o', 'm'});
    public static final String SERVER_DOMAIN2 = String.valueOf(new char[]{'h', 't', 't', 'p', 's', ':', '/', '/', 'f', 'o', 't', 'a', '5', 'p', '.', 'a', 'd', 'u', 'p', 's', '.', 'c', 'n'});
    private static final String SERVER_DOMAIN3 = String.valueOf(new char[]{'h', 't', 't', 'p', 's', ':', '/', '/', 'f', 'r', 'u', 'e', 't', '.', 'a', 'd', 'u', 'p', 's', '.', 'c', 'o', 'm'});
    // 欧盟条约
    public static final String EU_REPORT = SERVER_DOMAIN3 +
            String.valueOf(new char[]{'/', 'e', 'u', 'f', 't', '/', 'r', 'e', 'p', 's', 't', 'a'});
    private static final String SERVER_PATH = String.valueOf(new char[]{'/', 'o', 't', 'a', 'i', 'n', 't', 'e', 'r', '-', '5', '.', '0', '/', 'f', 'o', 't', 'a', '5', '/',});
    // 差分包检测
    public static final String QUERY = SERVER_PATH + "detectSchedule.do";
    // 全量检测
    public static final String QUERY_FULL = SERVER_PATH + "fullDetectSchedule.do";
    // 升级流程上报
    public static final String REPORT = SERVER_PATH + "submitReport.do";
    // fcm上报
    public static final String FCM_REPORT = SERVER_PATH +
            String.valueOf(new char[]{'f', 'c', 'm', 'R', 'e', 'p', 'o', 'r', 't', '.', 'd', 'o'});

}
