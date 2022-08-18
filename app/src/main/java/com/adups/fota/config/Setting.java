package com.adups.fota.config;


import com.adups.fota.utils.DeviceInfoUtil;

/**
 * 配置文件参数字段
 * Created by xw on 15-12-18.
 * preference params and default value
 */
public final class Setting {

    // setting params
    public static final String MID = "mid";

    // 累计激活总时间
    public static final String ACTIVATE_TOTAL_TIME = "activate_total_time";

    // 检测URL
    public static final String QUERY_URL = "check_url";
    public static final String QUERY_SERVER_FREQ = "check_freq";                 // 检测频率
    public static final String QUERY_LOCAL_FREQ = "check_local_freq";           // 设置选择频率
    public static final String QUERY_LAST_TIME = "check_last_time";              // 最后检测时间
    public static final String QUERY_FAIL_COUNTS = "check_fail_counts";          // 检测失败次数，最多检测失败3次，恢复默认域名
    public static final String QUERY_FULL = "isFull";                   // 是否显示全量包升级
    public static final String QUERY_JOB_SCHEDULE_TIME = "job_schedule_time";       // 代替网络切换，
    public static final String QUERY_JOB_SCHEDULE_DOWNLOADING_TIME = "job_schedule_downloading_time";// 网络切换实时监听

    public static final String FOTA_ORIGINAL_VERSION = "ota_original_version";   // 升级原始版本号
    public static final String FOTA_UPDATE_VERSION = "ota_update_version";       // 升级的版本号
    public static final String FOTA_UPDATE_STATUS = "ota_update_status";        // 更新步骤状态
    public static final String FOTA_UPDATE_LOCAL = "ota_update_local";        // ab升级区分是否是本地升级
    public static final String FOTA_UPDATE_LOCAL_PATH = "ota_update_local_path";        // ab本地升级升级包路径

    public static final String RECOVERY_FROM_THIRD = "recovery_from_third";      //进入recovery标识位
    public static final String FOTA_ENTER_RECOVERY = "ota_enter_recovery";       // 升级进入recovery标识
    public static final String FOTA_INSTALL_DELAY_SCHEDULE = "ota_install_delay_schedule";    // 延时更新时间
    public static final String FOTA_AB_PROGRESS = "ota_ab_progress";    // ab update progress


    public static final String DOWNLOAD_ONLY_WIFI = "download_only_wifi";        // 设置仅wifi下载开关
    public static final String DOWNLOAD_WIFI_AUTO = "download_wifi_auto";        // 设置wifi下自动下载开关
    public static final String SLIENT_DOWNLOAD = "slient_download";                 //设置夜间下载

    public static final String DOWNLOAD_SEGMENT = "segment_number";        // 当前下载第几分段
    public static final String DOWNLOAD_SEGMENT_FAIL_SIMULATE_SIZE = "segment_number_fail_simulate_size";        // 下载失败是记录当前进度条显示的虚拟进度，防止手机关机数据丢失

    public static final String MID_SYN_TIME_FAIL = "sync_time_fail_count";       // MID  同步时间失败次数

    public static final String DEBUG_SWITCH = "debug_status";                  // 是否开启debug
    public static final String DEBUG_LOG_PATH = "debug_log_path";                // debug输出文件地址

    // 0: can update , 1:can't update   ROM 检测被破坏时升级策略
    public static final String FOTA_UPGRADE = "isupgrade";

    public static final String FOTA_ROM_DAMAGED = "rom_damaged";            // ROM 是否被破破坏
    public static final String FOTA_ROM_DAMAGED_VERSION = "rom_damaged_version"; // ROM 破坏当前版本号
    public static final String FOTA_FULL_QUERY = "ota_full_query";  //是否允许全量检测，每次检测（差分/全量）都将其置为true，只有在全量检测未检测到全量包后将此字段设为false，以便于后面的差分检测

    // 1: normal update  2: full update
    public static final String FOTA_UPDATE_TYPE = "ota_update_type";

    public static final String FOTA_CHECK_ONCE_DAY = "ota_check_once_day";  // 进入界面自动检测，每天最多一次

    public static final String FOTA_INSTALL_RESULT_POP = "ota_install_result_pop";  // show dialog after update success

    public static final String FOTA_DOWNLOAD_PATH = "ota_download_path";  // save download path   xxx#xxx#xxx

    public static final String OTA_PRE_STATUS = "downlaodStatus";         //  old version   update status
    public static final String OTA_FEEDBACK_NEW_VERSION = "newVersion";   //

    public static final String OTA_FEEDBACK_OLD_VERSION = "feedoldversion";
    public static final String OTA_OLD_POP_FLAG = "noPopWinFlag";         // 0 :  show
    public static final String OTA_OLD_NOTIFY_FLAG = "notifyFlag";         // 0 :  show

    // 升级包存储路径记录
    public static final String UPDATE_PACKAGE_PATH = "update_package_path";

    //升级失败次数   大于5次以后不允许再去做自动下载
    public static final String FOTA_INSTALL_FAIL_COUNTS = "ota_install_fail_count";
    //安装失败的版本号记录
    public static final String FOTA_INSTALL_FAIL_VERSION = "ota_install_fail_version";

    //欧盟
    public static final String REJECT_STATUS = "reject_status";
    public static final String REPORT_STATUS = "report_status";
    public static final String EU_CONNECT_NET = "connect_net";
    public static final String EU_NO_REPORT = "no_report";
    /***********************************************************************************************
     ***********************************************************************************************
     */
    // 设置默认检测周期（1 天， 3天， 7天）
    public static final int SCHEDULE_1 = 1 * 12 * 60;
    public static final int SCHEDULE_2 = 3 * 24 * 60;
    public static final int SCHEDULE_3 = 7 * 24 * 60;

    public static final int DEFAULT_LOCAL_FREQ = SCHEDULE_1;       // unit:  min

    //服务器下发检测频率，如果值为2940，则以本地设置配置为准
    public static final long DEFAULT_SERVER_FREQ = 2940;             // unit:  min   2940 is default on Server

    // 安装延时更新（1小时，4小时，8小时）
    public static final long INSTALL_DELAY_SCHEDULE_1 = 1000 * 60 * 60;
    public static final long INSTALL_DELAY_SCHEDULE_2 = 1000 * 60 * 60 * 4;
    public static final long INSTALL_DELAY_SCHEDULE_3 = 1000 * 60 * 60 * 8;


    public static final long MAX_MID_SYN_FAIL_COUNTS = 5;
    public static final long MAX_QUERY_RETRY_COUNTS = 3;    // 域名切换，出错后切换

    // 检测激活时间，满足激活后才可以后台检测版本
    public static final long ELAPSEDREAL_TIME = (1000 * 60 * 15);   // 默认开机时间达到后自动激活

    public static final String FOTA_DELTA_URL = "deltaurl";        // 差分包URL
    public static final String FOTA_POLICY_CONTENT = "policy_content";        //策略信息
    /*
     * fcm相关
     * */
    public static final String INTENT_PARAM_NOTIFY_ID = "notify_id";
    public static final String INTENT_PARAM_TASK_ID = "task_id";

    public static final String INTENT_PARAM_FLAG = "flag";
    public static final String INTENT_PARAM_FLAG_INSTALL = "install";

    //crosscall定制
    public static final String INTENT_PARAM_FLAG_CONTINUE = "continue";
    public static final String INTENT_PARAM_FLAG_RESET = "reset";
    public static final String INTENT_PARAM_DOWNLOAD = "download";
    public static final String INTENT_PARAM_INSTALL = "install";
    public static final String INTENT_PARAM_NIGHT = "night";//zhangzhou

    //稍后升级的次数
    public static final String INSTALL_LATER_COUNT = "install_later_count";

    public static int getDefaultFreq() {

        String check_cycle = DeviceInfoUtil.getInstance().getCheckCycle();

        if ("0".equals(check_cycle)) {
            return Setting.DEFAULT_LOCAL_FREQ;
        } else if ("1".equals(check_cycle)) {
            return Setting.SCHEDULE_1;
        } else if ("3".equals(check_cycle)) {
            return Setting.SCHEDULE_2;
        } else if ("7".equals(check_cycle)) {
            return Setting.SCHEDULE_3;
        } else {
            return Setting.DEFAULT_LOCAL_FREQ;
        }

    }

}
