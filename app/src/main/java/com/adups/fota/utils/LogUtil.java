package com.adups.fota.utils;

import android.util.Log;

import com.adups.fota.MyApplication;

public class LogUtil {

    private static final String TAG = "FotaUpdate";
    public static boolean logOut = false;//是否打印日志
    private static boolean saveLog = false;
    private static String saveLogPath = "";

    public static void d(String msg) {
        msg = ect(msg);
        log(TAG, msg, null, 'd');
    }

    private static String ect(String msg) {
        if (true) {  //暂时关闭log混淆
            return msg;
        } else {
            try {
                //abcdefghijklmnopqrstuvwxyz
                String[] x = {"a", "b", "c"};
                //1."hc"+msg
                //2.h-3 c-4 o-5 m-6 a-7 d-8 x-9 p-1 f-2
                //3.msg
                msg = "hc" + msg;
                msg = msg.replace("h", "g1");
                msg = msg.replace("c", "b2");
                msg = msg.replace("o", "t3");
                msg = msg.replace("m", "n4");
                msg = msg.replace("a", "v7");
                msg = msg.replace("d", "k8");
                msg = msg.replace("x", "u9");
                msg = msg.replace("p", "r1");
                msg = msg.replace("f", "w2");
                msg = msg + x[(int) (Math.random() % 3)];
                return msg;
            } catch (Exception e) {
                return msg + e;
            }
        }
    }

    public static void d(boolean needLog, String msg) {
        msg = ect(msg);
        if (needLog) {
            log(TAG, createMessage(msg), null, 'd');
        }
        if (saveLog) {
            FileUtil.write2Sd(saveLogPath, TAG + ": " + createMessage(msg));
        }
    }

    public static void d(String tag, boolean needLog, String msg) {
        msg = ect(msg);
        if (needLog) {
            log(TAG, createMessage(msg), null, 'd');
        }
        if (saveLog) {
            FileUtil.write2Sd(saveLogPath, tag + ": " + createMessage(msg));
        }
    }

    public static void e(String msg) {
        msg = ect(msg);
        e(TAG, msg, null);
    }

    public static void e(String tag, String msg, Throwable e) {
        msg = ect(msg);
        log(tag, msg, e, 'e');
    }

    /**
     * 根据tag, msg和等级，输出日志
     *
     * @param tag
     * @param msg
     * @param level
     */
    private static void log(String tag, String msg, Throwable tr, char level) {
        msg = ect(msg);
        // 日志文件总开关
        if (saveLog || logOut || FileUtil.isExistTraceFile(MyApplication.getAppContext())) {
            // 输入日志类型，v代表输出所有信息,w则只输出警告...
            if ('e' == level) { // 输出错误信息
                Log.e(tag, createMessage(msg), tr);
            } else if ('w' == level) {
                Log.w(tag, createMessage(msg), tr);
            } else if ('d' == level) {
                Log.d(tag, createMessage(msg), tr);
            } else if ('i' == level) {
                Log.i(tag, createMessage(msg), tr);
            } else {
                Log.v(tag, createMessage(msg), tr);
            }
            if (saveLog) {
                FileUtil.write2Sd(saveLogPath, tag + ": " + createMessage(msg));
            }
        }
    }

    private static String getFunctionName() {
        StackTraceElement[] sts = Thread.currentThread().getStackTrace();
        for (StackTraceElement st : sts) {
            if (st.isNativeMethod()) {
                continue;
            }
            if (st.getClassName().equals(Thread.class.getName())) {
                continue;
            }
            if (st.getFileName().equals("LogUtil.java")) {
                continue;
            }
            return "[" + st.getFileName()
                    + ":" + st.getLineNumber() + "] " + st.getMethodName();
        }
        return null;
    }

    private static String createMessage(String msg) {
        msg = ect(msg);
        String functionName = getFunctionName();
        return (functionName == null ? msg
                : (functionName + " -> " + msg));
    }

    public static boolean getSaveLog() {
        return saveLog;
    }

    public static void setSaveLog(boolean isSave) {
        saveLog = isSave;
    }

    public static void setSaveLogPath(String path) {
        saveLogPath = path;
    }

    public static void setLogOut(boolean logOut) {
        LogUtil.logOut = logOut;
    }
}
